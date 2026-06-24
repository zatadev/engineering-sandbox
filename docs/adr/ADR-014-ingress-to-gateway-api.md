# ADR-014: Migration Ingress → Gateway API

## Status: Accepted

## Context

`ingress-nginx` (controller communautaire `kubernetes/ingress-nginx`) a été retiré en mars 2026 :
dépôt en lecture seule, aucun patch de sécurité à venir. L'API Ingress Kubernetes reste disponible
mais est considérée comme gelée — elle ne recevra plus d'évolutions significatives.

Gateway API (`gateway.networking.k8s.io`) est le successeur officiel, promu par le SIG Network
Kubernetes. Il est en GA depuis 2023 et adopté par tous les providers majeurs (AWS, GCP, Azure).

Le sandbox utilisait `ingress-nginx` depuis la Phase 6 (ZAT-121). Continuer avec un controller
retiré en Phase 7 (déploiement EKS) aurait introduit un risque de sécurité et une dette technique
immédiate.

## Decision

Migration vers **Gateway API** avec **Envoy Gateway** (v1.4.1) comme implémentation.

Envoy Gateway a été retenu après comparaison avec NGINX Gateway Fabric et Traefik (ZAT-190) :

- CNCF incubating — gouvernance ouverte, standard qui émerge clairement
- AWS co-développe Envoy — compatibilité native EKS pour la Phase 7
- Install simple sur kind (manifeste unique, pas de Helm requis)
- Couverture complète de la spec Gateway API standard

Les manifests `ingress-nginx/` et `ingress/` ont été **supprimés** du repo (pas archivés).
L'historique Git conserve la trace de l'état précédent. Un dossier `deprecated/` aurait
introduit du bruit sans valeur ajoutée.

## Consequences

**Modèle à 3 ressources — séparation des rôles :**

| Ressource | Responsabilité | Équipe |
|---|---|---|
| `GatewayClass` | Quel controller | Platform/Infra |
| `Gateway` | Quel point d'entrée (port, protocole) | Ops |
| `HTTPRoute` | Quel trafic vers quel service | Dev |

vs l'ancien `Ingress` : un seul objet, géré par les devs, avec des annotations propriétaires
différentes par controller (`nginx.ingress.kubernetes.io/...`).

**Pas de migration drop-in :** Gateway API n'est pas rétrocompatible avec les manifests Ingress.
Les règles de routing ont été réécrites en `HTTPRoute`.

**Sur kind (local) :** pas de cloud provider → `EXTERNAL-IP: <pending>` sur le Service LoadBalancer
provisionné par Envoy Gateway. Accès local via `kubectl port-forward`. MetalLB résoudrait cela
mais sa valeur est limitée — EKS résout nativement en Phase 7.

**Sur EKS (Phase 7) :** Envoy Gateway sera déployé identiquement. Le Service LoadBalancer recevra
une External IP/DNS automatiquement via AWS Load Balancer Controller. Aucun changement de manifests
applicatifs (`HTTPRoute`) requis — seule la `Gateway` peut nécessiter des annotations AWS.

**Routes configurées :**
- `/api/v1/users` → `user-service:80` (namespace `sandbox`)
- TODO ZAT-193 : `/api/v1/orders` → `order-service:80` et `/api/v1/notifications` → `notification-service:80`
