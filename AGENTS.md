# Agent Guide

## Project shape
- This is a set of independent Java 21 / Spring Boot services, not a root Maven multi-module build. Each service has its own `pom.xml` and Maven wrapper.
- `api-gateway` listens on port 9000 and routes `/api/v1/user/**`, `/api/v1/device/**`, `/api/v1/ingestion/**`, and `/api/v1/insight/**` to the corresponding services. Route definitions and their circuit-breaker fallbacks live in `api-gateway/src/main/java/.../route/`.
- `user-service` (8080) owns user persistence; `device-service` (8081) owns device CRUD. Both use MySQL and Spring Data JPA. The schema is created by Flyway migrations in `user-service/src/main/resources/db/migration/`; preserve migration history by adding a new versioned migration rather than editing an applied one.
- `ingestion-service` (8082) accepts usage records and publishes `EnergyUsageEvent` messages to Kafka topic `energy-usage`. `usage-service` (8083) consumes these events into InfluxDB, queries and aggregates time-series usage, and calls device/user services through `DeviceClient` and `UserClient` (`RestTemplate`).
- `usage-service` periodically checks hourly consumption against user thresholds and publishes `AlertingEvent` messages to `energy-alerts`; `alert-service` (8084) consumes them and sends email through Spring Mail. Local email is handled by Mailpit.
- `insight-service` (8085) calls usage-service through `UsageClient`, then uses Spring AI's `OllamaChatModel` to return usage summaries and savings tips. Ollama/model setup is external to `docker-compose.yml`.

## Cross-service contracts and conventions
- REST controllers use `/api/v1/<resource>` paths and delegate to a service class; keep transport DTOs in each service's `dto` package rather than exposing persistence entities.
- Service-to-service URLs and local infrastructure settings are in each service's `src/main/resources/application.properties`. Keep port changes consistent with gateway route URIs and client URL properties.
- Kafka event classes are source-local copies under `com.energy_tracker.kafka.event` in the producer/consumer services, not a shared Maven module. If changing `EnergyUsageEvent` or `AlertingEvent`, update every producer and consumer copy plus the matching `spring.json.type.mapping` in consumer properties.
- Use the underscore Java package roots such as `com.energy_tracker.usage_service`; hyphens are used only in Maven artifact/service names.
- For schema changes, coordinate Flyway SQL migrations with the corresponding JPA entity and repository. Existing MySQL tables are initialized from `user-service` migrations, not `docker/mysql/init.sql` (which only creates the database).
- The gateway's resilience configuration is in `api-gateway/src/main/resources/application.properties`; preserve each route's circuit-breaker name when changing its fallback or timeout settings.

## Local development and tests
- Start local dependencies from the repository root with `docker compose up -d` (MySQL, Kafka, Kafka UI, InfluxDB, and Mailpit). App properties target host-mapped ports, including Kafka at `localhost:9094`; Kafka UI is at `localhost:8070`, InfluxDB at `localhost:8072`, and Mailpit at `localhost:8025`.
- Run an individual service's tests from that service directory, for example `cd usage-service; .\mvnw.cmd test`. Repeat in the affected service directories; there is no root `pom.xml` to build or test everything at once.
- Run a service with `cd <service>; .\mvnw.cmd spring-boot:run`. The gateway exposes actuator health/metrics endpoints as configured in its application properties.
- Tests are under each service's `src/test/java`. Most are Spring Boot context-load tests; `user-service`'s current test class is class-level disabled, so it does not exercise its database behavior.
- `docker-compose.yml` supplies local-only dependency configuration. The Ollama model is configured in `insight-service/src/main/resources/application.properties`, and MySQL/Kafka/InfluxDB/Mailpit URLs and credentials are configured separately in service properties or Compose.

## Useful code landmarks
- Follow a usage event end-to-end in `ingestion-service/.../IngestionController.java`, `ingestion-service/.../IngestionService.java`, and `usage-service/.../UsageService.java`.
- Follow alert processing from `usage-service/.../UsageService.java` to `alert-service/.../AlertService.java` and `EmailService.java`.
- User/device data rules are in `user-service/.../UserService.java` and `device-service/.../DeviceService.java`; their API boundaries are the corresponding `*Controller.java` files.
- The main persistence/time-series configuration is in `usage-service/.../InfluxDBConfig.java`; AI prompt and response behavior is in `insight-service/.../InsightService.java`.
