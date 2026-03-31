# Tech Lead Audit: credit-processing-platform

## Overview

Проект — это учебная event-driven платформа обработки кредитных заявок на Java 17 + Spring Boot с тремя микросервисами и асинхронным обменом через Kafka.

Бизнес-задача, которую он закрывает в текущем виде:
- принять кредитную заявку по HTTP;
- сохранить её в PostgreSQL;
- асинхронно принять решение по скорингу;
- зафиксировать отправку уведомления.

Это хороший каркас для демонстрации базовых принципов microservices + messaging, но пока без production-grade глубины (надёжность, безопасность, наблюдаемость, тестовое покрытие, CI/CD).

## Current Architecture

### High-level flow
1. `credit-application-service` принимает `POST /applications`.
2. Сохраняет заявку в таблицу `applications` (PostgreSQL).
3. Публикует событие `credit.application.created` в Kafka.
4. `scoring-service` читает событие, вычисляет решение по порогу суммы (`<= 10000` -> APPROVED, иначе REJECTED), публикует в `credit.application.approved` или `credit.application.rejected`.
5. `notification-service` слушает топики решений и логирует отправку уведомления.

### Runtime/infra shape
- Multi-module Maven root (`pom` packaging).
- Локальная инфраструктура: Zookeeper + Kafka + Postgres через `docker-compose`.
- Миграции БД через Flyway только в `credit-application-service`.

## What is Done

### REST API
- Реализован endpoint `POST /applications` с ответом `201 Created` и телом `{id, status}`.
- Других endpoint’ов (чтение заявки, health/api versioning, idempotency ключи) нет.

### Бизнес-логика
- В application-service: создание заявки со статусом `CREATED` + публикация Kafka-события.
- В scoring-service: простое правило принятия решения по `amount`.
- В notification-service: имитация отправки уведомления через лог.

### PostgreSQL integration
- Есть подключение datasource, JPA, Flyway migration `V1__init.sql`.
- Используется таблица `applications` с UUID, суммой, статусом, created_at.

### Kafka producers/consumers
- Producer в application-service.
- Consumer + producer в scoring-service.
- Consumer в notification-service.
- Настроены manual ack и DefaultErrorHandler + DLQ (`.DLQ`) для listener’ов scoring/notification.

### Docker / docker-compose
- Есть `docker-compose.yml` для локального старта Kafka/Zookeeper/Postgres.
- Нет Dockerfile для сервисов (только инфраструктура).

### Kubernetes manifests / Helm
- В репозитории отсутствуют.

### Security
- Во всех сервисах подключён Spring Security, но фактически `permitAll` для всех запросов и отключен CSRF.
- Нет аутентификации/авторизации, secrets management, TLS, политики доступа между сервисами.

### Tests
- Unit/integration/e2e тестов в репозитории нет.

### Observability / logging / metrics
- Базовые лог-сообщения + уровень логирования Kafka.
- Нет Micrometer/Prometheus метрик, трассировки, корреляционных id, health/readiness checks, structured logging.

## What is Missing

1. **Контрактная и доменная зрелость API**
   - Нет валидации входных данных (`@Valid`, ограничения на сумму/имя).
   - Нет стандартной ошибки (`ProblemDetails`/единый error model).
   - Нет идемпотентности на создание заявки.

2. **Надёжность event-driven потока**
   - Нет outbox-паттерна (DB write + Kafka publish не атомарны).
   - Нет явной обработки дубликатов событий (idempotent consumers).
   - Нет версионирования событий и контракт-тестов.

3. **Безопасность**
   - REST открыт полностью.
   - Нет OAuth2/JWT/mTLS/API key, rate limiting, audit trail.
   - Секреты хранятся как plaintext в `application.yml`.

4. **Операционная готовность**
   - Нет Dockerfile для сервисов.
   - Нет Kubernetes/Helm/деплойных манифестов.
   - Нет CI pipeline (build/test/lint/security scan).

5. **Качество и поддерживаемость**
   - Нет тестов и минимальных quality gates.
   - Нет явной модульной доменной структуры (application/domain/infrastructure).
   - Нет linting/static analysis (Checkstyle/SpotBugs/PMD).

## Technical Debt / Risks

- **Риск потери события**: при падении после `save()` и до `kafkaTemplate.send()` заявка останется в БД без события.
- **Риск неконсистентных повторов**: при ретраях Kafka без идемпотентности возможны дубликаты обработки.
- **Риск безопасности**: сервисы фактически не защищены.
- **Риск регрессий**: отсутствие тестов делает любое изменение высокорискованным.
- **Риск «демо-only» статуса**: проект сложно защищать на собеседовании как production-ready backend system.

## Priority Next Steps

### P0 (сделать в первую очередь)
1. Добавить тестовый минимум:
   - unit tests для `ApplicationService` и scoring logic;
   - integration tests для REST + DB (Testcontainers Postgres);
   - integration tests Kafka flow (Testcontainers Kafka).
2. Добавить валидацию и error handling:
   - `@Valid` + bean validation constraints;
   - глобальный `@ControllerAdvice`.
3. Закрыть security baseline:
   - минимум JWT resource server или API key gateway;
   - убрать `permitAll` для business endpoint’ов.

### P1
4. Внедрить outbox pattern (или transactional messaging alternative) для надёжной публикации событий.
5. Добавить observability baseline:
   - `spring-boot-starter-actuator`, Prometheus metrics, health/readiness;
   - correlation id в логи.
6. Упаковать сервисы в Dockerfile и описать запуск всех сервисов + infra через compose.

### P2
7. Добавить Kubernetes manifests или Helm chart.
8. Подключить CI/CD (GitHub Actions): build + test + static analysis + dependency scanning.
9. Ввести versioning/event contracts (например, schema registry/JSON schema + contract tests).

## Portfolio Project Assessment (Java Backend)

Текущая оценка как проект для резюме: **6/10**.

Сильные стороны:
- Понятная микросервисная декомпозиция;
- Event-driven взаимодействие;
- Kafka retry/DLQ;
- Flyway + Postgres.

Что мешает уровню «сильный проект»:
- почти нет engineering depth (tests, security, observability, deployability, reliability patterns);
- нет production-story (как поддерживать, мониторить, безопасно выкатывать и восстанавливать).

После закрытия P0+P1 проект может выйти на **8/10** и уже хорошо смотреться на middle+/senior backend интервью.

## Exact files/folders to change

### 1) API validation + errors
- `credit-application-service/src/main/java/com/example/creditapplicationservice/dto/ApplicationRequest.java`
- `credit-application-service/src/main/java/com/example/creditapplicationservice/controller/ApplicationController.java`
- `credit-application-service/src/main/java/com/example/creditapplicationservice/config/` (добавить `GlobalExceptionHandler`)

### 2) Security
- `credit-application-service/src/main/java/com/example/creditapplicationservice/config/SecurityConfig.java`
- `scoring-service/src/main/java/com/example/scoringservice/config/SecurityConfig.java`
- `notification-service/src/main/java/com/example/notificationservice/config/SecurityConfig.java`
- `*/src/main/resources/application.yml` (убрать хардкод секретов, добавить env placeholders)

### 3) Reliability (outbox + idempotency)
- `credit-application-service/src/main/java/com/example/creditapplicationservice/service/ApplicationService.java`
- `credit-application-service/src/main/resources/db/migration/` (новые миграции для outbox/idempotency)
- `scoring-service/src/main/java/com/example/scoringservice/listener/ApplicationScoringListener.java`
- `notification-service/src/main/java/com/example/notificationservice/listener/NotificationListener.java`

### 4) Tests
- `credit-application-service/src/test/java/...`
- `scoring-service/src/test/java/...`
- `notification-service/src/test/java/...`
- root `pom.xml` + module `pom.xml` (test dependencies/plugins)

### 5) Observability
- `*/pom.xml` (actuator, micrometer)
- `*/src/main/resources/application.yml` (metrics, health probes, log format)

### 6) Containerization/Delivery
- `credit-application-service/Dockerfile`
- `scoring-service/Dockerfile`
- `notification-service/Dockerfile`
- `docker-compose.yml` (добавить сервисы приложений)
- `.github/workflows/` (CI pipeline)
- `k8s/` или `helm/` (манифесты/чарты)
