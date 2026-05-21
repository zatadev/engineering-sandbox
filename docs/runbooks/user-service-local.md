# Runbook — user-service local

## Prérequis
- Docker Desktop installé et démarré
- Image `user-service:latest` buildée (voir Build)

---

## Build de l'image

Depuis la **racine du repo** :

```bash
docker build -f services/user-service/Dockerfile -t user-service:latest .
```

> Le build context doit être la racine — le Dockerfile accède au parent `pom.xml`.

---

## Démarrage de l'environnement local

### 1. Démarrer PostgreSQL
```bash
docker run -d \
  --name postgres-dev \
  -e POSTGRES_DB=userdb \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 \
  postgres:16-alpine
```

### 2. Démarrer le user-service
```bash
docker run -d \
  --name user-service-dev \
  -e DB_URL=jdbc:postgresql://host.docker.internal:5432/userdb \
  -e SPRING_PROFILES_ACTIVE=dev \
  -p 8080:8080 \
  user-service:latest
```

### 3. Vérifier que les containers tournent
```bash
docker ps
```

### 4. Suivre les logs
```bash
docker logs -f user-service-dev
```

Attendre `Started UserServiceApplication` avant de tester.

---

## Arrêt

```bash
docker rm -f user-service-dev postgres-dev
```

---

## Rebuild après modification

```bash
docker rm -f user-service-dev
docker build -f services/user-service/Dockerfile -t user-service:latest .
docker run -d \
  --name user-service-dev \
  -e DB_URL=jdbc:postgresql://host.docker.internal:5432/userdb \
  -e SPRING_PROFILES_ACTIVE=dev \
  -p 8080:8080 \
  user-service:latest
```

> PostgreSQL n'a pas besoin d'être redémarré.

---

## Réinitialiser la base de données

```bash
docker rm -f user-service-dev postgres-dev

docker run -d \
  --name postgres-dev \
  -e POSTGRES_DB=userdb \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 \
  postgres:16-alpine

docker run -d \
  --name user-service-dev \
  -e DB_URL=jdbc:postgresql://host.docker.internal:5432/userdb \
  -e SPRING_PROFILES_ACTIVE=dev \
  -p 8080:8080 \
  user-service:latest
```

Flyway réapplique les migrations depuis zéro.

---

## Tester avec Postman

Importer la collection : `docs/postman/user-service.postman_collection.json`

Ordre recommandé :
1. `Auth > Login` — stocke le token automatiquement
2. `Users > POST create user` — stocke le userId automatiquement
3. Lancer les autres requêtes dans l'ordre
4. `Users > DELETE user` en dernier

Pour lancer tous les tests : clic droit sur la collection → **Run collection**.

---

## Endpoints disponibles

| Endpoint | Méthode | Auth | Description |
|----------|---------|------|-------------|
| `/api/v1/auth/login` | POST | Non | Authentification |
| `/api/v1/users` | GET | Oui | Liste paginée |
| `/api/v1/users` | POST | Oui | Créer un user |
| `/api/v1/users/{id}` | GET | Oui | Détail |
| `/api/v1/users/{id}` | PUT | Oui | Modifier |
| `/api/v1/users/{id}` | DELETE | Oui | Supprimer |
| `/actuator/health` | GET | Non | Health check |
| `/actuator/info` | GET | Non | Info |
| `/actuator/prometheus` | GET | Non | Métriques Prometheus |