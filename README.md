# Credit Processing Platform

Мульти-модульный Maven проект с тремя сервисами:

- `credit-application-service` — принимает заявки, пишет в Postgres и публикует событие в Kafka.
- `scoring-service` — слушает заявки, принимает решение (approve/reject) и публикует событие.
- `notification-service` — слушает решения и пишет лог о доставке уведомления.

## Локальный запуск

1. Запустите инфраструктуру:

```bash
docker-compose up -d
```

Kafka будет доступна на `localhost:9092`, Postgres — на `localhost:5432` (db=`creditdb`, user=`credit`, pass=`credit`).

2. Запустите сервисы из IDE (или через Maven), по портам:

- `credit-application-service` — `8080`
- `scoring-service` — `8081`
- `notification-service` — `8082`

## Пример запроса

```bash
curl -X POST http://localhost:8080/applications \
  -H 'Content-Type: application/json' \
  -d '{"clientName":"Иван Иванов","amount":7500.00}'
```

Ожидаемое поведение:

- заявка сохраняется в Postgres со статусом `CREATED`;
- в Kafka топик `credit.application.created` уходит событие;
- `scoring-service` публикует решение в `credit.application.approved` или `credit.application.rejected`;
- `notification-service` логирует `Notification sent...`.

## Retry и DLQ

Для слушателей Kafka настроены 3 попытки (1 первичная + 2 ретрая). После этого сообщение попадает в DLQ топик с суффиксом `.DLQ`, например:

- `credit.application.created.DLQ`
- `credit.application.approved.DLQ`
- `credit.application.rejected.DLQ`

Проверить DLQ можно, например, через `kafka-console-consumer`:

```bash
kafka-console-consumer --bootstrap-server localhost:9092 --topic credit.application.created.DLQ --from-beginning
```

## P0 implementation backlog (на основе Tech Lead Audit)

> Ниже только P0-область: Validation, global error handling, `GET /applications/{id}`, тесты, OpenAPI, Dockerfile, full docker-compose и обновление README.

### 1) Validation
**Цель:**
- Добавить валидацию входных DTO для API заявок, чтобы некорректные данные не попадали в БД и Kafka.

**Почему это важно:**
- Снижает количество "грязных" данных и нештатных отказов в downstream-сервисах.
- Позволяет возвращать предсказуемые `400 Bad Request` вместо случайных `500`.

**Какие файлы менять:**
- `credit-application-service/pom.xml` (добавить `spring-boot-starter-validation`).
- `credit-application-service/src/main/java/com/example/creditapplicationservice/dto/ApplicationRequest.java`.
- `credit-application-service/src/main/java/com/example/creditapplicationservice/controller/ApplicationController.java`.

**Какие классы/конфиги добавить:**
- Аннотации Bean Validation в `ApplicationRequest` (`@NotBlank`, `@NotNull`, `@DecimalMin`).
- Использование `@Valid` в контроллере.

**Критерии готовности:**
- Пустой `clientName`, `amount = null`, `amount <= 0` возвращают `400`.
- Валидный запрос продолжает создавать заявку со статусом `201`.

**Пример ручной проверки:**
```bash
curl -i -X POST http://localhost:8080/applications \
  -H 'Content-Type: application/json' \
  -d '{"clientName":"","amount":0}'
```
Ожидание: HTTP `400` + тело с ошибкой валидации.

---

### 2) Global error handling
**Цель:**
- Сделать единый формат ошибок для REST API (`timestamp`, `status`, `error`, `message`, `path`).

**Почему это важно:**
- Единый контракт ошибок нужен для фронта, QA и интеграций.
- Упрощает диагностику и исключает разрозненные error-response.

**Какие файлы менять:**
- `credit-application-service/src/main/java/com/example/creditapplicationservice/controller/ApplicationController.java` (минимально, при необходимости).

**Какие классы/конфиги добавить:**
- `credit-application-service/src/main/java/com/example/creditapplicationservice/exception/GlobalExceptionHandler.java` (`@RestControllerAdvice`).
- `credit-application-service/src/main/java/com/example/creditapplicationservice/exception/ErrorResponse.java`.
- `credit-application-service/src/main/java/com/example/creditapplicationservice/exception/ApplicationNotFoundException.java`.

**Критерии готовности:**
- Ошибки валидации, отсутствие сущности и неожиданные исключения возвращаются в одном JSON-формате.
- Для not found возвращается `404`, для validation — `400`, для unexpected — `500`.

**Пример ручной проверки:**
```bash
curl -i http://localhost:8080/applications/00000000-0000-0000-0000-000000000000
```
Ожидание: HTTP `404` + стандартизованный JSON ошибки.

---

### 3) GET /applications/{id}
**Цель:**
- Реализовать чтение заявки по UUID.

**Почему это важно:**
- Базовая функциональность API: клиент должен иметь возможность получить статус ранее созданной заявки.

**Какие файлы менять:**
- `credit-application-service/src/main/java/com/example/creditapplicationservice/controller/ApplicationController.java`.
- `credit-application-service/src/main/java/com/example/creditapplicationservice/service/ApplicationService.java`.
- `credit-application-service/src/main/java/com/example/creditapplicationservice/repository/ApplicationRepository.java`.

**Какие классы/конфиги добавить:**
- `ApplicationResponse` DTO (например, `id`, `clientName`, `amount`, `status`).
- Метод `findByIdOrThrow(UUID id)` в сервисе.

**Критерии готовности:**
- `GET /applications/{id}` возвращает `200` и DTO существующей заявки.
- Для несуществующего ID возвращается `404` через global handler.

**Пример ручной проверки:**
1. Создать заявку через POST.
2. Взять `id` из ответа.
3. Выполнить:
```bash
curl -i http://localhost:8080/applications/{id}
```
Ожидание: HTTP `200` + данные заявки.

---

### 4) Unit tests
**Цель:**
- Покрыть ключевую бизнес-логику модульными тестами.

**Почему это важно:**
- Быстрая обратная связь при изменениях и снижение риска регрессий.

**Какие файлы менять:**
- `credit-application-service/pom.xml`.
- `scoring-service/pom.xml`.
- `notification-service/pom.xml`.

**Какие классы/конфиги добавить:**
- `credit-application-service/src/test/java/.../service/ApplicationServiceTest.java`.
- `credit-application-service/src/test/java/.../controller/ApplicationControllerTest.java` (WebMvc).
- `scoring-service/src/test/java/.../listener/ApplicationScoringListenerTest.java`.
- `notification-service/src/test/java/.../listener/NotificationListenerTest.java`.

**Критерии готовности:**
- Проверены happy-path и базовые negative-path.
- Тесты запускаются локально одной командой Maven и проходят стабильно.

**Пример ручной проверки:**
```bash
mvn -q test
```
Ожидание: `BUILD SUCCESS`.

---

### 5) Integration tests with Testcontainers
**Цель:**
- Проверить интеграцию с реальными Postgres/Kafka в изолированном окружении.

**Почему это важно:**
- Ловит проблемы конфигурации и сериализации, не видимые в unit-тестах.

**Какие файлы менять:**
- `credit-application-service/pom.xml`.
- `scoring-service/pom.xml`.
- `notification-service/pom.xml`.

**Какие классы/конфиги добавить:**
- Общий тестовый базовый класс с контейнерами (Postgres + Kafka).
- Интеграционные тесты, как минимум:
  - `credit-application-service`: POST сохраняет запись в БД и публикует событие.
  - E2E smoke: событие проходит цепочку `created -> approved/rejected -> notification`.

**Критерии готовности:**
- Интеграционные тесты запускаются в CI/локально без внешних зависимостей.
- Есть как минимум один сквозной сценарий с Kafka.

**Пример ручной проверки:**
```bash
mvn -q -Dtest='*IT' test
```
Ожидание: контейнеры поднимаются автоматически, тесты завершаются успешно.

---

### 6) OpenAPI
**Цель:**
- Зафиксировать API-контракт и сделать его доступным через Swagger UI.

**Почему это важно:**
- Ускоряет интеграции и снижает неоднозначность по запросам/ответам/кодам ошибок.

**Какие файлы менять:**
- `credit-application-service/pom.xml`.
- `credit-application-service/src/main/java/com/example/creditapplicationservice/controller/ApplicationController.java`.

**Какие классы/конфиги добавить:**
- Зависимость springdoc-openapi.
- Опционально `OpenApiConfig` с базовой мета-информацией.
- Аннотации к endpoint’ам (описания, коды ответов, схемы).

**Критерии готовности:**
- `/v3/api-docs` и `/swagger-ui/index.html` доступны.
- В документации отражены `POST /applications` и `GET /applications/{id}` + ошибки `400/404/500`.

**Пример ручной проверки:**
- Открыть `http://localhost:8080/swagger-ui/index.html` и выполнить endpoint через UI.

---

### 7) Dockerfile for all services
**Цель:**
- Добавить рабочие Dockerfile для каждого сервиса.

**Почему это важно:**
- Нужна воспроизводимая сборка/запуск в любом окружении и для CI/CD.

**Какие файлы менять:**
- `credit-application-service/Dockerfile`.
- `scoring-service/Dockerfile`.
- `notification-service/Dockerfile`.

**Какие классы/конфиги добавить:**
- Многостадийная сборка (Maven build stage + JRE runtime stage).
- Единый шаблон запуска (`java -jar app.jar`).

**Критерии готовности:**
- Все 3 образа собираются локально без ручных правок.
- Контейнеры стартуют и читают переменные окружения для Kafka/Postgres.

**Пример ручной проверки:**
```bash
docker build -t credit-application-service:local ./credit-application-service
```
(Повторить для остальных сервисов.)

---

### 8) Full docker-compose for app + infra
**Цель:**
- Поднять одним compose всю систему: инфраструктуру и все 3 сервиса.

**Почему это важно:**
- Быстрый локальный smoke-тест, единая точка входа для разработчиков и QA.

**Какие файлы менять:**
- `docker-compose.yml`.

**Какие классы/конфиги добавить:**
- Сервисы: `postgres`, `kafka` (и необходимые компоненты), `credit-application-service`, `scoring-service`, `notification-service`.
- `depends_on`, healthcheck (минимально для реалистичного старта), environment-переменные.

**Критерии готовности:**
- `docker compose up -d` поднимает весь стек.
- Можно выполнить POST в `credit-application-service` и увидеть полный проход события до notification.

**Пример ручной проверки:**
```bash
docker compose up -d
curl -X POST http://localhost:8080/applications \
  -H 'Content-Type: application/json' \
  -d '{"clientName":"Test User","amount":5000}'
```
Ожидание: сервисы в `Up` и в логах scoring/notification есть обработка.

---

### 9) README update
**Цель:**
- Обновить документацию под итоговый P0 scope и фактический процесс запуска/проверки.

**Почему это важно:**
- README — основной onboarding-документ; без него P0-функции трудно быстро проверить.

**Какие файлы менять:**
- `README.md`.

**Какие классы/конфиги добавить:**
- Не требуется.

**Критерии готовности:**
- README содержит:
  - как запустить стек через docker compose;
  - примеры `POST /applications` и `GET /applications/{id}`;
  - где смотреть OpenAPI;
  - как запускать unit/integration тесты.

**Пример ручной проверки:**
- Новый разработчик, следуя только README, поднимает проект и выполняет оба endpoint без дополнительных устных инструкций.

