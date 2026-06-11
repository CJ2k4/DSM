# DSM Backend — Project Status & Technical Reference

_A decentralized (federated) social media platform. This document is the developer
reference for everything built so far._

**Last updated:** 2026-06-11

---

## 1. Overview

DSM is a federated social platform (Mastodon-style) where users register, post, follow
each other, and interact across multiple cooperating servers.

**Current status — backend Phases 1–3 are complete and on `main`:**

| Phase | Area | Status |
|-------|------|--------|
| 1 | Project foundation (Spring Boot, config, error handling) | ✅ Done |
| 2 | Authentication (JWT, refresh-token rotation) | ✅ Done |
| 3 | User system (profile, search) **+ Follow/unfollow** | ✅ Done |
| 4 | Posts & Feed | ✅ Done |
| 5 | Likes & comments, notifications | ⏳ Next |
| 6–11 | Frontend, federation, real-time, deploy, etc. | 🔴 Not started |
| — | Frontend (React) | 🔴 Skeleton only |

All HTTP routes are versioned under **`/api/v1`**.

---

## 2. Tech Stack

| Layer | Technology |
|-------|------------|
| Language / runtime | Java 21 |
| Framework | Spring Boot 3.2.5 — Web, Data JPA, Security, Validation, Actuator |
| Auth / JWT | JJWT (`jjwt-api`, `jjwt-impl`, `jjwt-jackson`) |
| Database | H2 in-memory (dev, PostgreSQL mode) · PostgreSQL (prod) |
| Build | Maven (wrapper `./mvnw`) |
| Misc | Lombok, Jackson |
| Frontend | React (currently Create React App / `react-scripts`; Vite + Tailwind planned) |

Build/run from the repo root:

```bash
./mvnw -f backend/pom.xml spring-boot:run     # run (H2 by default)
./mvnw -f backend/pom.xml test                # run the test suite
```

---

## 3. Module / Package Layout

Feature-based packages under `com.DSM.Platform` (`backend/src/main/java/com/DSM/Platform`):

| Package | Responsibility |
|---------|----------------|
| `auth` | Register/login/refresh/logout/me, `RefreshToken` entity + rotation service |
| `user` | `User` entity, profile get/update, user search |
| `follow` | `Follow` entity, follow/unfollow, follower/following lists |
| `post` | `Post` entity, create/delete post, personalized feed, per-user posts |
| `security` | JWT service, auth filter, `AuthenticatedUser` principal, entry-point/handlers |
| `config` | `SecurityConfig`, JWT & CORS properties |
| `common` | Global exception handler + standard API error envelope |
| `PlatformApplication` | Spring Boot entry point |

---

## 4. API Reference

Base URL: `/api/v1`. Auth column = whether a valid `Authorization: Bearer <accessToken>`
is required. All bodies are JSON.

### 4.1 Auth — `/api/v1/auth` (`AuthController`)

| Method | Path | Auth | Request | Success |
|--------|------|------|---------|---------|
| POST | `/register` | No | `RegisterRequest` | `201` + `AuthResponse` |
| POST | `/login` | No | `LoginRequest` | `200` + `AuthResponse` |
| POST | `/refresh` | No | `RefreshTokenRequest` `{ refreshToken }` | `200` + `AuthResponse` |
| POST | `/logout` | No | `LogoutRequest` `{ refreshToken }` | `204` |
| GET | `/me` | Yes | — | `200` + `UserResponse` |

**`RegisterRequest`** — `username` (3–30, `^[A-Za-z0-9_.]+$`), `email` (valid, ≤320),
`password` (8–128), `displayName` (optional, ≤80).
**`LoginRequest`** — `identifier` (email *or* username), `password`.
**`AuthResponse`** — `accessToken`, `refreshToken`, `tokenType` (`"Bearer"`),
`expiresIn` (seconds), `user` (`UserResponse`).
**`UserResponse`** — `id`, `username`, `email`, `displayName`, `bio`, `avatarUrl`,
`bannerUrl`, `role`, `emailVerified`, `createdAt`.

### 4.2 Users — `/api/v1/users` (`UserController`)

| Method | Path | Auth | Request | Success |
|--------|------|------|---------|---------|
| GET | `/me` | Yes | — | `200` + `UserProfileResponse` (own) |
| PATCH | `/me` | Yes | `UpdateProfileRequest` | `200` + `UserProfileResponse` |
| GET | `/search?q=&page=&size=` | No | — | `200` + `Page<UserSearchResponse>` |
| GET | `/{username}` | No | — | `200` + `UserProfileResponse` (public) |

**`UpdateProfileRequest`** — all optional: `displayName` (≤80), `bio` (≤500),
`avatarUrl` (≤2048), `bannerUrl` (≤2048). Blank `displayName` → `400 DISPLAY_NAME_REQUIRED`.
**`UserProfileResponse`** — `id`, `username`, `displayName`, `bio`, `avatarUrl`,
`bannerUrl`, `createdAt`, `ownProfile`, **`followerCount`**, **`followingCount`**,
**`following`** (true only when the authenticated viewer follows this profile; `false`
when unauthenticated).
**`UserSearchResponse`** — `id`, `username`, `displayName`, `bio`, `avatarUrl`.
Search requires `q` ≥ 2 chars → otherwise `400 SEARCH_QUERY_TOO_SHORT`.

### 4.3 Follow — `/api/v1/users` (`FollowController`)

| Method | Path | Auth | Success |
|--------|------|------|---------|
| POST | `/{username}/follow` | Yes | `200` + `FollowResponse` |
| DELETE | `/{username}/follow` | Yes | `200` + `FollowResponse` |
| GET | `/{username}/followers?page=&size=` | No | `200` + `Page<UserSearchResponse>` |
| GET | `/{username}/following?page=&size=` | No | `200` + `Page<UserSearchResponse>` |

**`FollowResponse`** — `userId`, `username`, `following`, `followerCount`, `followingCount`.

Behavior:
- **Idempotent** — following someone you already follow (or unfollowing someone you don't)
  is a no-op that returns the current state.
- **Self-follow** → `400 CANNOT_FOLLOW_SELF`.
- **Unknown / inactive target** → `404 USER_NOT_FOUND`.
- Lists exclude non-`ACTIVE` users; paginated (default `size=20`).
- Target is addressed by **username**; follow rows are stored internally by UUID.

### 4.4 Posts — `/api/v1/posts` (`PostController`)

| Method | Path | Auth | Request | Success |
|--------|------|------|---------|---------|
| POST | `/api/v1/posts` | Yes | `CreatePostRequest` | `201` + `PostResponse` |
| DELETE | `/api/v1/posts/{id}` | Yes (author) | — | `204` |
| GET | `/api/v1/posts/feed?page=&size=` | Yes | — | `200` + `Page<PostResponse>` |
| GET | `/api/v1/posts/user/{username}?page=&size=` | No | — | `200` + `Page<PostResponse>` |

**`CreatePostRequest`** — `content` (required, ≤1000), `imageUrl` (optional, ≤2048).
**`PostResponse`** — `id`, `content`, `imageUrl`, `author` (`UserSearchResponse`),
`createdAt`.

Behavior:
- **Feed** = posts from users you follow **plus your own**, ACTIVE authors only,
  newest first, paginated (default `size=20`).
- **Delete** is author-only and a hard delete → `403 POST_FORBIDDEN` for non-authors,
  `404 POST_NOT_FOUND` if missing.
- **User posts** lists a single user's posts (newest first); `404 USER_NOT_FOUND` for
  unknown/inactive users.

### 4.5 Error envelope (`ApiErrorResponse`)

Every error returns:

```json
{
  "timestamp": "2026-06-11T12:00:00Z",
  "status": 400,
  "code": "VALIDATION_ERROR",
  "message": "Request validation failed",
  "path": "/api/v1/auth/register",
  "errors": [ { "field": "email", "message": "Email must be valid" } ]
}
```

Common codes: `VALIDATION_ERROR`, `UNAUTHORIZED`, `USER_NOT_FOUND`,
`CANNOT_FOLLOW_SELF`, `DISPLAY_NAME_REQUIRED`, `SEARCH_QUERY_TOO_SHORT`,
`POST_NOT_FOUND`, `POST_FORBIDDEN`.

---

## 5. Data Model

UUID primary keys throughout. Schema is auto-generated by Hibernate
(`spring.jpa.hibernate.ddl-auto=update`) — **no migration tool yet**.

### `users` (`user/User.java`)
`id` (UUID, PK) · `username` (unique, ≤30) · `email` (unique, ≤320) · `passwordHash`
· `displayName` (≤80) · `bio` (≤500) · `avatarUrl`/`bannerUrl` (≤2048) ·
`role` (`UserRole`, enum string) · `status` (`UserStatus`: `ACTIVE`/`SUSPENDED`/`DELETED`)
· `emailVerified` · `createdAt` / `updatedAt`. Unique constraints + indexes on
`username` and `email`.

### `refresh_tokens` (`auth/RefreshToken.java`)
`id` (UUID, PK) · `user` (FK → users, lazy) · `tokenHash` (unique, only the **hash** is
stored) · `expiresAt` · `revokedAt` (nullable) · `createdAt`. Supports rotation and
`isActive(now)` checks; indexed on `user_id` and `token_hash`.

### `follows` (`follow/Follow.java`)
`id` (UUID, PK) · `follower` (FK → users, lazy) · `following` (FK → users, lazy) ·
`createdAt`. Unique constraint on `(follower_id, following_id)`; indexes on both FK columns.

### `posts` (`post/Post.java`)
`id` (UUID, PK) · `author` (FK → users, lazy) · `content` (≤1000, required) ·
`imageUrl` (≤2048, nullable) · `createdAt`. Indexes on `author_id` and `created_at`.

---

## 6. Security Model

- **Stateless JWT** — `JwtService` issues/validates HS256 access tokens;
  `JwtAuthenticationFilter` authenticates each request; the principal is the
  `AuthenticatedUser` record (`id`, `email`, `username`, `role`, `status`).
- **Password hashing** — BCrypt (`PasswordEncoder` bean in `SecurityConfig`).
- **Refresh tokens** — long-lived, hashed at rest, **rotated** on each refresh and
  **revoked** on logout (`RefreshTokenService`).
- **Session policy** — `STATELESS`; CSRF disabled (token-based API).
- **Route rules** (`config/SecurityConfig.java`):
  - Public: `POST /auth/register|login|refresh`, `GET /users/search`, `GET /users/*`,
    `GET /users/*/followers`, `GET /users/*/following`, `/actuator/health`, `/h2-console/**`.
  - Authenticated: `GET|PATCH /users/me`, `POST|DELETE /users/*/follow`, and everything
    else by default (`anyRequest().authenticated()`).
- **CORS** — configurable allowed origins (defaults to `localhost:3000` and `:5173`).
- **Errors** — `RestAuthenticationEntryPoint` (401) and `RestAccessDeniedHandler` (403)
  return the standard JSON envelope; `GlobalExceptionHandler` maps `ApiException` and
  validation failures.

---

## 7. Configuration

Env-driven via `backend/src/main/resources/application.properties` (defaults shown for
local dev):

| Setting | Env var | Default |
|---------|---------|---------|
| Server port | `SERVER_PORT` | `8080` |
| Datasource URL | `SPRING_DATASOURCE_URL` | H2 in-memory (PostgreSQL mode) |
| DDL mode | `SPRING_JPA_HIBERNATE_DDL_AUTO` | `update` |
| JWT secret | `JWT_SECRET` | dev placeholder (≥32 bytes) |
| Access-token TTL | `JWT_ACCESS_TOKEN_EXPIRATION` | `PT15M` |
| Refresh-token TTL | `JWT_REFRESH_TOKEN_EXPIRATION` | `P7D` |
| CORS origins | `CORS_ALLOWED_ORIGINS` | `http://localhost:3000,http://localhost:5173` |

- **PostgreSQL profile** — `application-postgres.properties` (+ `.env.example`).
- **Local Postgres** — `docker-compose.yml` at the repo root.

---

## 8. Testing

MockMvc `@SpringBootTest` integration suites run against in-memory H2:

| Suite | Coverage |
|-------|----------|
| `AuthControllerIntegrationTest` | register, login, refresh flow, `/me`, duplicate-email conflict |
| `UserControllerIntegrationTest` | get/update profile, public profile, search, validation |
| `FollowControllerIntegrationTest` | follow/unfollow + counts, idempotency, self-follow, lists, profile flags, 401, 404 |
| `PostControllerIntegrationTest` | create post, feed (followed + own, excludes strangers), delete own/others (403)/unknown (404), user posts, validation, 401 |

**23 tests, all passing.** Run:

```bash
./mvnw -f backend/pom.xml test
```

---

## 9. Roadmap / Not Yet Built

**Next — Phase 5: Likes & Comments**
- `Like` and `Comment` entities tied to posts; like/unlike, add/list comments;
  optional notifications.

**Later phases**
- Phase 6 — Frontend (Vite + Tailwind), auth/feed/profile screens.
- Phase 7 — Federation layer (remote post/user sync via WebClient).
- Phase 8 — WebSockets (live notifications/comments).
- Phases 9–11 — Rate limiting, XSS sanitization, Docker/deploy, Cloudinary images,
  full-text search, trending, Redis cache.

**Known technical follow-ups**
- Introduce **Flyway** migrations and switch prod to `ddl-auto=validate`.
- Build the **frontend** (migrate to Vite + Tailwind per the plan; currently CRA skeleton)
  and wire React auth/feed screens to the API with an Axios JWT interceptor.
