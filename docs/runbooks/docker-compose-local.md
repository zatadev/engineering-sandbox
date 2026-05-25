# Runbook — Local Environment (Docker Compose)

## Prerequisites
- Docker Desktop installed and running
- `.env` file configured (see Setup)

## Setup

Copy the example env file and fill in your values:
```bash
cp infrastructure/docker-compose/.env.example \
   infrastructure/docker-compose/.env
```

## Start the stack
```bash
docker compose -f infrastructure/docker-compose/docker-compose.yml up --build
```

## Stop the stack
```bash
docker compose -f infrastructure/docker-compose/docker-compose.yml down
```

## Reset the database (wipe all data)
```bash
docker compose -f infrastructure/docker-compose/docker-compose.yml down -v
docker compose -f infrastructure/docker-compose/docker-compose.yml up --build
```

> `-v` removes the Postgres volume. Flyway re-applies all migrations from scratch.
> Use this when switching branches with incompatible DB state, or after changing
> credentials in `.env`.

## View logs
```bash
# All services
docker compose -f infrastructure/docker-compose/docker-compose.yml logs -f

# Single service
docker compose -f infrastructure/docker-compose/docker-compose.yml logs -f user-service
```

## Services

| Service      | URL                          | Notes              |
|--------------|------------------------------|--------------------|
| user-service | http://localhost:8080         |                    |
| postgres     | localhost:5432 (db: userdb)  |                    |
| redis        | localhost:6379               | No auth (local dev)|

## Testing

Import the Postman collection: `docs/postman/user-service.postman_collection.json`

See `docs/runbooks/user-service-local.md` for endpoint details and test order.

## Inspecting the Redis cache

Connect to Redis CLI:
```bash
docker exec -it sandbox-redis redis-cli
```

Useful commands:
```bash
# List all cached keys
KEYS *

# Get a cached value
GET "users::<uuid>"

# Flush all cache entries (useful when testing or after config changes)
FLUSHALL
```

Cache behavior in user-service:
- `GET /api/v1/users/{id}` → cache miss on first call, cache hit on subsequent calls
- `PUT /api/v1/users/{id}` → evicts the cache entry for that id
- `DELETE /api/v1/users/{id}` → evicts the cache entry for that id
- `GET /api/v1/users` (list) → not cached (intentional, see ADR-009)
- TTL: 10 minutes