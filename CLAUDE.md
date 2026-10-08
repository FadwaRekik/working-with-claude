# CLAUDE.md

- Run: `SPRING_PROFILES_ACTIVE=demo ./mvnw spring-boot:run` (H2, no Docker) or `docker compose up -d db && ./mvnw spring-boot:run`; app on :8080.
- Test: `./mvnw test` (Java, 25) and `npm test` (Jest, 45); one test: `-Dtest=Class#method` or `npx jest <file> -t "<name>"`.
- Backend: Spring Boot 3.2 / Java 17 in `src/main/java/com/marlowefinch/ops/`; controllers -> repositories with plain SQL (Spring JDBC, no JPA), read-only `GET /api/*`.
- Data: Flyway `V1__schema.sql` + `V2__seed.sql`; tests and `demo` use H2 in PostgreSQL mode.
- Frontend: vanilla JS/HTML/CSS in `src/main/resources/static/`, no framework or build step; charts are inline SVG.
- `app.js` wraps everything in `initApp(document, fetchImpl)` so it runs in the browser and in Jest.
- Every element id in `index.html` must be registered in `REGISTERED_IDS` in `src/test/javascript/setup/loadApp.js`.
- "Today" is pinned to 2026-09-21 via the injected `Clock` (`ClockConfig`); never use `LocalDate.now()`.
- Tickets are in `docs/tickets/`; both suites must stay green.
- pom.xml dependencies are frozen. Any change needs a CHG ticket.
