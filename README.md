# Credit Processing Platform

Проект демонстрирует event-driven архитектуру на базе Kafka и трёх сервисов:

- **credit-service** — REST API для приёма заявок и публикации событий в Kafka.
- **scoring-service** — потребляет заявки, принимает решение approve/reject и публикует результат.
- **notification-service** — получает результат скоринга и имитирует отправку уведомлений.

Поддерживаются retry (3 попытки), manual ack, Dead Letter Topics (`<topic>.DLQ`) и демонстрационный сценарий ошибок.

## Запуск инфраструктуры

```bash
docker-compose up -d
```

## Запуск сервисов из IDE

1. **credit-service** — `com.example.credit.CreditServiceApplication`
2. **scoring-service** — `com.example.scoring.ScoringServiceApplication`
3. **notification-service** — `com.example.notification.NotificationServiceApplication`

Порты по умолчанию:
- credit-service: `8080`
- scoring-service: `8081`
- notification-service: `8082`

## Пример запроса

```bash
curl -X POST http://localhost:8080/api/credit-applications \
  -H "Content-Type: application/json" \
  -d '{"clientName": "Ivan Petrov", "amount": 7000}'
```

В ответ вернётся созданное событие с `applicationId`.

## Как проверить retry + DLQ

1. Отправьте заявку с `clientName`, содержащим `fail`:

```bash
curl -X POST http://localhost:8080/api/credit-applications \
  -H "Content-Type: application/json" \
  -d '{"clientName": "fail-user", "amount": 5000}'
```

2. **scoring-service** выбросит исключение, выполнит 3 попытки и отправит сообщение в `credit-applications.DLQ`.

3. Посмотрите DLQ через Kafka CLI (запуск в контейнере):

```bash
docker exec -it credit-processing-platform-kafka-1 kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic credit-applications.DLQ \
  --from-beginning
```

4. Чтобы проверить DLQ в **notification-service**, можно отправить заявку с `clientName=fail-notification`,
а затем убедиться, что сообщение оказалось в `scoring-results.DLQ`.

## Сборка

```bash
mvn -q -DskipTests package
```
