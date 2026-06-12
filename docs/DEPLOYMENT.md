# DSM — Running & Deploying a Node

Three ways to run DSM, from quickest to most public:

1. [Local Docker](#1-local-docker-one-command) — one command, full stack
2. [Two-node federation demo](#2-two-node-federation-demo) — a whole federated network on your laptop
3. [Cloud deployment](#3-cloud-deployment-vercel--render--neon) — a public node anyone can federate with

---

## 1. Local Docker (one command)

Requires Docker Desktop (Compose v2).

```bash
docker compose up --build
```

| Service  | URL                   | Notes                          |
|----------|-----------------------|--------------------------------|
| Frontend | http://localhost:3000 | nginx serving the built SPA    |
| Backend  | http://localhost:8080 | Spring Boot, postgres profile  |
| Postgres | localhost:5433        | host port (set `POSTGRES_HOST_PORT` to change); volume `dsm_postgres_data` |

The frontend container proxies `/api/*` and `/ws` to the backend, so the app is
same-origin — no CORS setup needed in the browser. Data persists across
restarts in the named volume; `docker compose down -v` wipes it.

**Gotchas**

- `VITE_API_BASE_URL` is baked into the JS bundle at **build** time. Changing
  it requires `docker compose build frontend`.
- nginx resolves the backend's container IP at startup. If you recreate just
  the backend (`docker compose up -d --force-recreate backend`), restart the
  frontend too if you see 502s.

## 2. Two-node federation demo

```bash
docker compose --profile federation up --build
```

This adds a second, fully independent node: `backend-b` (host port **8081**)
with its own database. To see federation in action:

```bash
# 1. Seed a user + post on node B
TOKEN=$(curl -s -X POST http://localhost:8081/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"bora","email":"bora@nodeb.com","password":"password123","displayName":"Bora of Node B"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['accessToken'])")
curl -s -X POST http://localhost:8081/api/v1/posts \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"content":"Hello from node B!"}'
```

2. Open http://localhost:3000, sign up, go to **Network**, and add the peer
   `http://backend-b:8080` (the containers talk over the Docker network, so
   use the service name, not localhost).
3. Check the **Global** tab in the feed — node B's post appears with a
   "via node-b" badge. Auto-sync pulls new posts every 60 s; "Sync now" is instant.

## 3. Cloud deployment (Vercel + Render + Neon)

A public node = static frontend on **Vercel**, backend container on
**Render**, PostgreSQL on **Neon**. All three have free tiers.

### 3.1 Neon (database)

1. Create a project at https://neon.tech (any region close to your Render region).
2. From the dashboard, note the **host**, **database**, **user**, **password**.
3. Build the JDBC URL yourself:

   ```
   jdbc:postgresql://<host>/<database>?sslmode=require
   ```

   ⚠️ Do **not** paste Neon's connection string directly — it contains
   `channel_binding=require`, which is libpq syntax and breaks the JDBC
   driver. Use the plain (non `-pooler`) host for JPA.

### 3.2 Render (backend)

The repo ships a blueprint (`render.yaml` at the root):

1. https://dashboard.render.com → **New → Blueprint** → pick this repo.
2. Render prompts for the unsynced env vars:

   | Variable | Value |
   |----------|-------|
   | `SPRING_DATASOURCE_URL` | the JDBC URL from 3.1 |
   | `SPRING_DATASOURCE_USERNAME` / `_PASSWORD` | from Neon |
   | `FEDERATION_SELF_BASE_URL` | this service's URL, e.g. `https://dsm-backend.onrender.com` |
   | `CORS_ALLOWED_ORIGINS` | your Vercel domain, e.g. `https://dsm.vercel.app` |

3. Deploy. Health is checked at `/actuator/health`.

Notes: the free tier **cold-starts** after ~15 min idle (first request takes
~1 min — that's normal). WebSockets work on Render out of the box (`wss://`).

### 3.3 Vercel (frontend)

1. https://vercel.com → **Add New → Project** → import this repo.
2. Set **Root Directory** to `frontend` (the SPA rewrite in
   `frontend/vercel.json` is picked up automatically).
3. Add the env var:

   | Variable | Value |
   |----------|-------|
   | `VITE_API_BASE_URL` | `https://<your-render-service>.onrender.com/api/v1` |

4. Deploy. With an absolute API URL the app talks to Render directly and
   derives `wss://...onrender.com/ws` for realtime.

⚠️ `CORS_ALLOWED_ORIGINS` on Render must contain the **exact** Vercel
domain — it gates both REST CORS and the WebSocket handshake. Vercel preview
deployments get unique subdomains, so realtime/API will only work on the
production domain unless you add each preview URL.

### 3.4 Federating two public nodes

On node A's **Network** page add node B's backend URL
(`https://dsm-b.onrender.com`). The announce handshake registers A on B
automatically — posts flow both ways from then on.

## Environment variable reference

| Variable | Default | Purpose |
|----------|---------|---------|
| `SERVER_PORT` | `8080` | HTTP port |
| `SPRING_PROFILES_ACTIVE` | _(none = H2 in-memory)_ | set `postgres` for a real DB |
| `SPRING_DATASOURCE_URL` / `_USERNAME` / `_PASSWORD` | local defaults | database connection |
| `JWT_SECRET` | dev value | ≥32 chars; **must** be unique in production |
| `JWT_ACCESS_TOKEN_EXPIRATION` / `JWT_REFRESH_TOKEN_EXPIRATION` | `PT15M` / `P7D` | token lifetimes |
| `CORS_ALLOWED_ORIGINS` | localhost:3000,localhost:5173 | frontend origins (REST CORS **and** WS handshake) |
| `FEDERATION_SELF_BASE_URL` | `http://localhost:8080` | public URL peers reach this node at |
| `FEDERATION_SERVER_NAME` | `DSM Node` | name announced to peers |
| `FEDERATION_AUTO_SYNC` / `FEDERATION_SYNC_INTERVAL` | `true` / `PT60S` | background peer sync |
| `VITE_API_BASE_URL` (frontend, build-time) | `http://localhost:8080/api/v1` | `/api/v1` behind the nginx proxy, absolute URL for Vercel→Render |
| `JAVA_OPTS` (container) | _(empty)_ | extra JVM flags |
