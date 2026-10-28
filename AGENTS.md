# AGENTS.md — michi-server

Monorepo-in-name-only: 4 independent Spring Boot / Maven services, no root aggregator pom, no shared build, no CI. Work per-service:

- `gateway/` — Spring Cloud Gateway (WebFlux) + Security, port `8080`
- `productos/` — products API (Mongo, OpenFeign/Uber, MapStruct), port `8081`
- `reservas/` — reservations API (Mongo, Mail + Thymeleaf, MapStruct), port `8082`
- `oauth/` — Google OAuth2 login + JWT issuer (Mongo), port `8083`

Each service has its own `mvnw` / `mvnw.cmd` — `cd` into the service dir first (e.g. `cd productos`).

## Commands (per service)

- Run: `./mvnw spring-boot:run` (Windows: `mvnw.cmd spring-boot:run`)
- Tests: `./mvnw test` — currently only `*ApplicationTests.contextLoads()` placeholders, so a green build proves wiring only
- Build: `./mvnw -q -DskipTests package`

Java 21, Spring Boot 3.5.x. No lint/format/typecheck config — don't add any.

## Env / config (all required at runtime)

No committed `.env`, Docker, or infra files. Everything comes from env vars referenced in each `src/main/resources/application.properties`:

- Gateway: `PORT` (8080), `JWT_SECRET`, `FRONTEND_URL`, `PRODUCTS_URL`, `RESERVATIONS_URL`, `OAUTH_URL`
- Productos: `PORT` (8081), `MONGODB_URI`, `UBER_CLIENT_ID`, `UBER_CLIENT_SECRET`, `UBER_STORE_UUID`, `UBER_ORG_UUID`
- Reservas: `PORT` (8082), `MONGODB_URI`, `MAIL_USERNAME`, `MAIL_PASSWORD` (+ optional `SPRING_MAIL_*` overrides, Gmail/587 defaults)
- OAuth: `PORT` (8083), `JWT_SECRET`, `FRONTEND_URL`, `MONGODB_URI`, `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`
- `oauth` also depends on `dotenv-java`, so a local `.env` file works there; other services use plain env placeholders.

Gateway CORS (`CorsConfig`) allows only `FRONTEND_URL` with credentials; `reservas` forces `spring.jackson.time-zone=UTC`.

## Auth flow (don't reimplement)

1. `oauth` does Google `oauth2Login` (`SecurityConfig` permits all, `OAuth2SuccessHandler` + `UserService` issue JWT) and sets it in an HttpOnly `token` cookie (SameSite=None, Secure — see oauth `application.properties` + `server.forward-headers-strategy=native`, must run behind HTTPS/proxy for cookies to stick).
2. `gateway` is the only public entrypoint. Its `SecurityConfig` permits `GET /auth/**`, `GET /products/**`, `GET /reservations/hours`, `POST /reservations`; everything else requires auth. `jwtCookieFilter` reads the `token` cookie, validates with `JWT_SECRET` (must match oauth's), and forwards `User-Id/User-Role/User-Name/User-Picture/User-Email` headers.
3. Downstream services trust those headers — no token validation there. `ReservationController.checkAdmin` requires `User-Role: ROLE_ADMIN`; `OAuthController.user` falls back to `ROLE_USER`/`Invitado` defaults when headers are absent. Preserve header names exactly when touching auth.

Routes are path-based (`/products/**`, `/reservations/**`, `/auth/**`); keep controller `@RequestMapping` prefixes aligned with gateway predicates.

## Conventions

- Lombok (`@RequiredArgsConstructor`, `@Builder`) everywhere; `productos` and `reservas` use MapStruct (`@Mapper(componentModel="spring")`, Lombok-MapStruct binding in pom) — edit the `*Mapper.java` interface, never `target/generated-sources/*`.
- DTO validation via `spring-boot-starter-validation`; reservas controllers use `@Validated`.
- `reservas` email sending is currently commented out in `ReservationController.create`; template lives in `src/main/resources/templates/reservation-email.html`.
