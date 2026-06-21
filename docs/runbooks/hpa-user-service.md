# Runbook — HPA user-service

## Configuration
- HPA autoscaling/v2 : cible CPU 70 % (averageUtilization, % du request)
- min 2 / max 5 replicas
- Request CPU 100m (réglage démo, cf. note) / limit 500m
- Prérequis : metrics-server (vendorisé, --kubelet-insecure-tls pour kind)

## Comportement observé
Test : `hey -z 90s -c 150 http://localhost/actuator/health` (via l'Ingress, charge répartie sur tous les pods).

1. Repos : 2 replicas, CPU ~3-6 %.
2. Montée : 16 % → 27 % → 63 % → pic ~499 % du request (pods throttlés à la limite 500m).
3. Scale-up : 2 → 4 (à 70 % dépassé) → 5 (plafond maxReplicas).
4. Démarrage des nouveaux pods : 0/1 pendant ~60-90 s (warmup JVM + throttling CPU sous charge ; MEM 51Mi vs ~355Mi pour les pods chauds).
5. Métriques transitoires `<unknown>` + events `FailedGetResourceMetric` ("pods might be unready") → normal le temps que les nouveaux pods soient Ready et scrapés.
6. Stabilisation : charge répartie → moyenne redescend (144 % → 59 % → 29 %).
7. 7. Scale-down : maintenu à 5 pendant la fenêtre de stabilisation (~5 min), puis descente **graduelle** 5 → 4 → 3 → 2 (un palier ~par minute), reason "All metrics below target". Retour au min ~8 min après le pic.

## Enseignements
- L'utilisation cible est relative au **request CPU** → un request réaliste est obligatoire (sinon `<unknown>`).
- Scale-up lent sous saturation : les nouveaux pods sont affamés en CPU pendant leur démarrage → garder de la marge / scaler avant saturation.
- Scale-down temporisé (anti-flapping) → ~5 min avant retour au min.
- `/actuator/health` est I/O-bound : pour scaler sur le CPU, le levier est le débit (RPS), pas le coût unitaire.
- Scale-down doublement prudent : temporisé (fenêtre) ET par paliers, vs un scale-up qui peut sauter plusieurs replicas d'un coup.

## Notes / tech debt
- Request CPU 100m = réglage démo pour rendre l'autoscaling observable ; en prod, dimensionner par profiling.
- Request mémoire 256Mi un peu basse (usage réel ~355Mi = 138 % du request) → envisager 384Mi.
- Métrique CPU brute = simpliste pour ce service ; envisager RPS/latence (custom metrics) en cible.