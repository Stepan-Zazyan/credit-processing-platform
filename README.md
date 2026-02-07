# Credit Processing Platform

Проект демонстрирует event-driven архитектуру обработки кредитных заявок на базе Apache Kafka.

## Архитектура и event flow

Сервисы:

- **credit-application-service** — REST API для подачи заявок, сохранение в PostgreSQL, публикация события `credit.application.created`.
- **scoring-service** — потребляет заявки, рассчитывает решение (approve/reject), публикует `credit.application.approved` или `credit.application.rejected`.
- **notification-service** — потребляет решения и логирует факт отправки уведомления.

Event flow:

1. Клиент вызывает `POST /applications`.
2. `credit-application-service` сохраняет заявку в БД со статусом `NEW` и публикует `ApplicationCreated`.
3. `scoring-service` получает событие, выполняет скоринг и публикует `ApplicationApproved` или `ApplicationRejected`.
4. `notification-service` отправляет (симулирует) уведомление и пишет в лог: `Notification sent for application {id}`.

## Запуск инфраструктуры (Docker)

```bash
docker-compose up -d
```

- Kafka: `localhost:9092`
- PostgreSQL: `localhost:5432`, db=`creditdb`, user=`credit`, pass=`credit`

## Запуск сервисов из IDE

Каждый сервис — отдельный Spring Boot модуль. Запускайте main-классы:

- `credit-application-service` → `CreditApplicationServiceApplication` (порт `8081`)
- `scoring-service` → `ScoringServiceApplication` (порт `8082`)
- `notification-service` → `NotificationServiceApplication` (порт `8083`)

## Проверка API (Swagger)

Swagger UI включен во всех сервисах:

- `credit-application-service`: http://localhost:8081/swagger-ui.html
- `scoring-service`: http://localhost:8082/swagger-ui.html
- `notification-service`: http://localhost:8083/swagger-ui.html

Пример запроса:

```bash
curl -X POST http://localhost:8081/applications \
  -H 'Content-Type: application/json' \
  -d '{"clientName":"Иван Иванов","amount":7500.00}'
```

## Prometheus

Prometheus метрики доступны:

- `http://localhost:8081/actuator/prometheus`
- `http://localhost:8082/actuator/prometheus`
- `http://localhost:8083/actuator/prometheus`

Health endpoints:

- `http://localhost:8081/actuator/health`
- `http://localhost:8082/actuator/health`
- `http://localhost:8083/actuator/health`

## Retry + DLQ

Kafka consumer-ы настроены на:

- `ack-mode=manual`
- `enable-auto-commit=false`
- 3 попытки с фиксированным backoff
- DLQ топик `<topic>.DLQ`

**Демо DLQ:**

Если `clientName` содержит `fail`, `scoring-service` выбрасывает исключение — сообщение после ретраев попадает в `credit.application.created.DLQ`.

Проверка:

```bash
kafka-console-consumer --bootstrap-server localhost:9092 --topic credit.application.created.DLQ --from-beginning
```
