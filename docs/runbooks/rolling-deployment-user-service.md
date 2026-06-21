# Runbook — Rolling deployment & rollback (user-service)

## Principe
Modifier le *pod template* d'un Deployment (ex. nouvelle image) fait créer un nouveau
ReplicaSet. Les pods basculent progressivement de l'ancien RS vers le nouveau, et **un
ancien pod n'est supprimé qu'une fois qu'un nouveau pod est `Ready`** (readiness probe)
→ zéro downtime. Le rythme est piloté par `maxSurge` / `maxUnavailable`.
Le rollback (`rollout undo`) revient au template de la révision précédente — c'est lui-même
un rolling update.

## Pré-requis (kind)
- **Charger la nouvelle image AVANT d'appliquer** : `kind load docker-image <tag> --name sandbox`
  (sinon `ImagePullBackOff` → le rollout se bloque, sans downtime grâce à maxUnavailable: 0).
- **Un NOUVEAU tag est obligatoire** : un même tag = pod template inchangé = aucun rollout.
- **maxUnavailable: 0 recommandé explicitement** : user-service est piloté par un HPA (2→5
  replicas) ; à 5 replicas, le défaut 25 % autoriserait 1 pod indisponible. Le fixer à 0
  garantit le zéro-downtime quel que soit le nombre de replicas :
```yaml
  spec:
    strategy:
      type: RollingUpdate
      rollingUpdate:
        maxUnavailable: 0
        maxSurge: 1
```

## Rolling deployment

```bash
# 1. nouvelle image (retag suffit pour démontrer le mécanisme ; rebuild si code modifié)
docker tag user-service:local user-service:v2
kind load docker-image user-service:v2 --name sandbox

# 2. (optionnel mais recommandé) enregistrer la cause du changement
kubectl annotate deployment/user-service -n sandbox \
  kubernetes.io/change-cause="bump image to v2" --overwrite

# 3. déclencher le rolling update
kubectl set image deployment/user-service user-service=user-service:v2 -n sandbox

# 4. observer
kubectl rollout status deployment/user-service -n sandbox
kubectl get pods -n sandbox -w        # l'ancien RS se vide, le nouveau se remplit
```

Vérifier la disponibilité pendant le rollout (autre terminal, lancé AVANT le set image) :

```bash
while true; do curl -s -o /dev/null -w "%{http_code}\n" http://localhost/api/v1/users; sleep 0.5; done
# 401 continu = service dispo (un 401 prouve qu'un pod a répondu). Jamais de 5xx attendu.
```

## Rollback

```bash
# inspecter l'historique des révisions
kubectl rollout history deployment/user-service -n sandbox
kubectl rollout history deployment/user-service -n sandbox --revision=<N>   # voir le template d'une révision

# revenir à la révision précédente
kubectl rollout undo deployment/user-service -n sandbox
kubectl rollout status deployment/user-service -n sandbox

# vérifier le retour à l'image précédente
kubectl get deploy user-service -n sandbox \
  -o jsonpath='{.spec.template.spec.containers[0].image}{"\n"}'   # doit réafficher :local
```

## Comportement observé
- **Rolling deployment** (`:local` → `:v2`) : surge à N+1, l'ancien pod retiré seulement
  après que le nouveau soit `Ready` (~30-60 s/pod, warmup Spring Boot). `rollout status`
  termine en *successfully rolled out*.
- **Rollback** (`undo` → révision précédente) : rolled out OK, image revenue à `:local`.
  Seule différence entre les deux révisions = l'image (resources, probes, config identiques).
- **Disponibilité** : ~210 requêtes pendant le rollback → 401 continus + **1 seul 502** en
  toute fin de terminaison de l'ancien pod.
    - *Cause* : course entre la terminaison du pod (SIGTERM) et la propagation du retrait de
      l'endpoint vers nginx → bref instant où nginx route encore vers un pod qui s'arrête.
    - *Mitigation* : hook `preStop` (sleep ~10 s, laisse propager le retrait d'endpoint) +
      graceful shutdown Spring Boot (`server.shutdown=graceful`) pour drainer l'in-flight.
      → à implémenter dans un ticket dédié (Phase 8 hardening).

## Notes
- Chaque modif du pod template crée une révision (image, resources, `rollout restart`…).
  `revisionHistoryLimit` (défaut 10) = nb de ReplicaSets conservés pour rollback.
- `CHANGE-CAUSE: <none>` est normal si on n'a pas annoté `kubernetes.io/change-cause`
  (le flag `--record` est déprécié).
- `rollout undo` ne supprime pas la « mauvaise » révision : il roule en avant vers une copie
  du template précédent (nouvelle révision courante).
- user-service est piloté par un HPA : ne pas hardcoder `spec.replicas` dans le manifeste
  (conflit avec l'HPA à chaque `apply`).