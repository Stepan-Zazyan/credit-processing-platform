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
