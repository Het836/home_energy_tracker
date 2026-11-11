# Home Energy Tracker

Home Energy Tracker is a Spring Boot microservice-based application for tracking household energy usage, aggregating it over time, sending threshold alerts, and generating AI-assisted insights for energy-saving opportunities.

The repository contains multiple independent services rather than a single Maven multi-module build. Each service has its own `pom.xml` and Maven wrapper and runs on its own port.

## Project overview

The application flow is:

- User and device records are stored in MySQL through `user-service` and `device-service`.
- Energy usage samples are submitted through `ingestion-service`.
- `ingestion-service` publishes `EnergyUsageEvent` messages to Kafka.
- `usage-service` consumes those events, writes the data to InfluxDB, aggregates usage by device and by user, and checks user energy thresholds.
- When a threshold is exceeded, `usage-service` publishes an `AlertingEvent` to Kafka.
- `alert-service` consumes the alert and sends an email through Spring Mail.
- `insight-service` fetches usage data and uses Spring AI with Ollama to provide energy summaries and recommendations.
- `api-gateway` exposes the public routes and routes requests to the relevant backend services.

## Key features

- User management with `name`, `surname`, `email`, `address`, alert toggle, and threshold configuration
- Device management grouped by user with device types such as `SPEAKER`, `CAMERA`, `THERMOSTAT`, `LIGHT`, `LOCK`, and `DOORBELL`
- Energy usage ingestion API for recording device consumption events
- Kafka-based asynchronous event processing for usage and alerts
- Time-series storage in InfluxDB for device energy queries and aggregated usage reports
- Hourly aggregated threshold checking per user
- Email alert delivery through Mailpit in local development
- AI-powered summary and savings tips using Spring AI + Ollama
- API gateway with circuit-breaker fallbacks and OpenAPI aggregation
- Prometheus and Grafana monitoring configuration included in Docker Compose

## Technology stack

- Java 21
- Spring Boot 4.x (`spring-boot-starter-parent` with `4.0.9` in most services; `alert-service` uses `4.1.2`)
- Spring Cloud Gateway
- Spring Web MVC
- Spring Data JPA
- Flyway migrations
- MySQL 8.3.0
- Apache Kafka
- InfluxDB 2.7
- Spring AI + Ollama (`spring-ai-starter-model-ollama`)
- Spring Security + OAuth2 Resource Server + Keycloak
- Spring Mail + Mailpit
- Docker Compose
- Prometheus + Grafana
- Lombok
- Springdoc OpenAPI UI

## Repository structure

```text
home-energy-tracker/
├── AGENTS.md
├── docker/
│   ├── grafana/
│   ├── kafka_data/
│   ├── keycloak/
│   ├── mysql/
│   └── prometheus/
├── alert-service/
│   ├── src/
│   ├── pom.xml
│   ├── mvnw
│   └── mvnw.cmd
├── api-gateway/
│   ├── src/
│   ├── pom.xml
│   ├── mvnw
│   └── mvnw.cmd
├── device-service/
│   ├── src/
│   ├── pom.xml
│   ├── mvnw
│   └── mvnw.cmd
├── ingestion-service/
│   ├── src/
│   ├── pom.xml
│   ├── mvnw
│   └── mvnw.cmd
├── insight-service/
│   ├── src/
│   ├── pom.xml
│   ├── mvnw
│   └── mvnw.cmd
├── usage-service/
│   ├── src/
│   ├── pom.xml
│   ├── mvnw
│   └── mvnw.cmd
├── user-service/
│   ├── src/
│   ├── pom.xml
│   ├── mvnw
│   └── mvnw.cmd
├── docker-compose.yml
├── .gitignore
└── README.md
```

## Service responsibilities

### api-gateway
- Port: `9000`
- Routes `/api/v1/user/**`, `/api/v1/device/**`, `/api/v1/ingestion/**`, and `/api/v1/insight/**`
- Includes Resilience4j circuit-breaker fallbacks
- Uses Keycloak JWT validation for protected routes
- Aggregates Swagger/OpenAPI docs from backend services

### user-service
- Port: `8080`
- Stores user records in MySQL
- Flyway schema is in `user-service/src/main/resources/db/migration/`
- JPA entity: `User`
- REST endpoints: `POST /api/v1/user`, `GET /api/v1/user/{id}`, `PUT /api/v1/user/{id}`, `DELETE /api/v1/user/{id}`

### device-service
- Port: `8081`
- Stores device records in MySQL
- JPA entity: `Device`
- REST endpoints: `GET /api/v1/device/{id}`, `POST /api/v1/device/create`, `PUT /api/v1/device/{id}`, `DELETE /api/v1/device/{id}`, `GET /api/v1/device/user/{userId}`

### ingestion-service
- Port: `8082`
- Accepts energy usage records and publishes them to Kafka topic `energy-usage`
- Endpoint: `POST /api/v1/ingestion`
- Continuous simulation settings are configured in `src/main/resources/application.properties`

### usage-service
- Port: `8083`
- Consumes `EnergyUsageEvent` from Kafka
- Writes energy usage measurements to InfluxDB
- Aggregates usage over time windows and computes per-user totals
- Checks each user's `energyAlertingThreshold`
- Publishes `AlertingEvent` to Kafka topic `energy-alerts`
- Endpoint: `GET /api/v1/usage/{userId}?days={n}`

### alert-service
- Port: `8084`
- Consumes `AlertingEvent` from Kafka
- Sends email via Spring Mail
- Stores sent/failed alert state in MySQL table `alert`

### insight-service
- Port: `8085`
- Calls `usage-service` to fetch usage data
- Uses Spring AI `OllamaChatModel` to generate `overview` and `saving tips` responses
- Endpoints: `GET /api/v1/insight/overview/{userId}`, `GET /api/v1/insight/saving-tips/{userId}`

## Prerequisites

Before running the project locally, ensure the following are installed:

- Java 21
- Docker Desktop or Docker Engine with Docker Compose
- Maven (or use the project-provided Maven wrappers in each service)
- An Ollama instance with the configured model available to `insight-service`

The project expects the following local infrastructure containers:

- MySQL at `localhost:3306`
- Kafka at `localhost:9094`
- Kafka UI at `http://localhost:8070`
- InfluxDB at `http://localhost:8072`
- Mailpit at `http://localhost:8025`
- Keycloak at `http://localhost:8091`
- Prometheus at `http://localhost:9090`
- Grafana at `http://localhost:3000`

## Local infrastructure setup

From the repository root, start the infrastructure services:

```bash
docker compose up -d
```

This starts MySQL, Kafka, Kafka UI, InfluxDB, Mailpit, Keycloak, Prometheus, and Grafana as defined in `docker-compose.yml`.

## Environment and configuration

Key configuration values are set in the service `application.properties` files.

### MySQL

Configured in `user-service` and `device-service` (and reused by alert-service):

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/home_energy_tracker
spring.datasource.username=root
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.jpa.hibernate.ddl-auto=none
```

The initial database is created by:

```sql
CREATE DATABASE IF NOT EXISTS home_energy_tracker;
USE home_energy_tracker;
```

from `docker/mysql/init.sql`.

### Kafka

Producer/consumer bootstrap server is configured as:

```properties
spring.kafka.bootstrap-servers=localhost:9094
```

Relevant topics include:

- `energy-usage`
- `energy-alerts`

### InfluxDB

Configured in `usage-service`:

```properties
influx.url=http://localhost:8072
influx.token=my-token
influx.org=hetpatel
influx.bucket=usage-bucket
```

### Mailpit

Configured in `alert-service`:

```properties
spring.mail.host=localhost
spring.mail.port=1025
spring.mail.properties.mail.smtp.auth=false
spring.mail.properties.mail.smtp.starttls.enable=false
```

### API Gateway security / Keycloak

Configured in `api-gateway`:

```properties
keycloak.auth.jwk-set-uri=http://localhost:8091/realms/het-security-realm/protocol/openid-connect/certs
spring.security.oauth2.resourceserver.jwt,issuer-uri=http://localhost:8091/realms/het-security-realm http://localhost:8091/realms/het-security-realm
```

Gateway security excludes actuator and Swagger/OpenAPI endpoints:

```properties
security.excluded.urls= /actuator/**, /swagger-ui.html, /swagger-ui/**, /docs/**, /v3/api-docs/**, /swagger-resources/**, /api-docs/**
```

### Insight service

Configured in `insight-service`:

```properties
usage.service.url=http://localhost:8083/api/v1/usage
spring.ai.ollama.chat.options.model=deepseek-coder
spring.ai.ollama.init.pull-model-strategy=never
```

This means the AI insight service expects an external Ollama setup and uses the `deepseek-coder` model.

## Running the project

There is no root Maven build. Start each service independently from its own directory.

### Windows PowerShell

```powershell
docker compose up -d

cd user-service
.\mvnw.cmd spring-boot:run

cd ..\device-service
.\mvnw.cmd spring-boot:run

cd ..\ingestion-service
.\mvnw.cmd spring-boot:run

cd ..\usage-service
.\mvnw.cmd spring-boot:run

cd ..\alert-service
.\mvnw.cmd spring-boot:run

cd ..\insight-service
.\mvnw.cmd spring-boot:run

cd ..\api-gateway
.\mvnw.cmd spring-boot:run
```

### bash / zsh

```bash
docker compose up -d

cd user-service && ./mvnw spring-boot:run
cd ../device-service && ./mvnw spring-boot:run
cd ../ingestion-service && ./mvnw spring-boot:run
cd ../usage-service && ./mvnw spring-boot:run
cd ../alert-service && ./mvnw spring-boot:run
cd ../insight-service && ./mvnw spring-boot:run
cd ../api-gateway && ./mvnw spring-boot:run
```

## Available scripts and commands

The project uses Maven wrappers in each service. Common commands are:

```bash
# Run tests in a service
cd user-service && ./mvnw test

# Or on Windows
cd user-service
.\mvnw.cmd test

# Run the app
cd usage-service && ./mvnw spring-boot:run
```

Additional local infrastructure commands:

```bash
docker compose up -d

docker compose down
```

## API usage examples

### Create a user

```bash
curl -X POST http://localhost:8080/api/v1/user \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Jane",
    "surname": "Doe",
    "email": "jane@example.com",
    "address": "123 Main Street",
    "alerting": true,
    "energyAlertingThreshold": 500
  }'
```

### Create a device

```bash
curl -X POST http://localhost:8081/api/v1/device/create \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Living Room Light",
    "type": "LIGHT",
    "location": "Living Room",
    "userId": 1
  }'
```

### Submit usage data

```bash
curl -X POST http://localhost:8082/api/v1/ingestion \
  -H "Content-Type: application/json" \
  -d '{
    "deviceId": 1,
    "energyConsumed": 23.4,
    "timestamp": "2026-11-11T09:00:00Z"
  }'
```

### Get usage for a user

```bash
curl "http://localhost:8083/api/v1/usage/1?days=3"
```

### Get insight overview

```bash
curl http://localhost:8085/api/v1/insight/overview/1
```

### Get savings tips

```bash
curl http://localhost:8085/api/v1/insight/saving-tips/1
```

### Via API gateway

The gateway exposes the same routes through port `9000`:

```bash
curl "http://localhost:9000/api/v1/usage/1?days=3"
```

Note that the gateway is configured with Spring Security and Keycloak JWT validation; public routes are limited to actuator and Swagger/OpenAPI endpoints as defined in `api-gateway` configuration.

## Monitoring and documentation

### Swagger / OpenAPI

The gateway exposes OpenAPI/Swagger UI at:

- `http://localhost:9000/swagger-ui.html`
- `http://localhost:9000/v3/api-docs`

The project config also exposes service-specific docs behind the gateway via `/docs/<service>/v3/api-docs`.

### Actuator endpoints

Each service exposes actuator endpoints in `application.properties`, including health and Prometheus metrics.

Examples:

- `http://localhost:8080/actuator/health`
- `http://localhost:9000/actuator/health`

### Prometheus / Grafana

The repository includes Prometheus and Grafana services in Docker Compose:

- Prometheus UI: `http://localhost:9090`
- Grafana UI: `http://localhost:3000`

## Database schema and data model

### User schema

The Flyway migration `user-service/src/main/resources/db/migration/V1__user_table.sql` creates a `user` table with:

- `id`
- `name`
- `surname`
- `email` (unique)
- `address`
- `alerting`
- `energy_alerting_threshold`

### Device schema

`V2__device_table.sql` creates a `device` table with:

- `id`
- `name`
- `type`
- `location`
- `user_id`

### Alert schema

`V3__alert_table.sql` creates an `alert` table with:

- `id`
- `user_id`
- `sent`
- `created_at`

## Notes

- There is no root `pom.xml`; every service is built separately.
- `docker-compose.yml` provides local infrastructure only; it does not start the Java services automatically.
- The AI insight functionality depends on an external Ollama service and the configured model in `insight-service/src/main/resources/application.properties`.
- The local mail setup uses Mailpit instead of a real SMTP provider, so email delivery is observed via the Mailpit web UI at `http://localhost:8025`.

## License

This repository does not include an explicit license file in the current project structure, so no license metadata is documented here.
