# SsuBench: Руководство по использованию

Платформа для размещения заданий. Заказчики публикуют задачи, исполнители откликаются, оплата проходит виртуальными баллами.

## 1. Запуск

**База данных:**
```bash
docker compose up -d postgres
```

**Приложение:**
```bash
./gradlew bootRun
```

**Переменные окружения (.env):**
```bash
JWT_SECRET=DA09229F-B779-48E4-ABBC-CA0AD454FA19
JWT_ACCESS_EXPIRATION=900000
JWT_REFRESH_EXPIRATION=604800000
POSTGRES_DB=db
POSTGRES_USER=user
POSTGRES_PASSWORD=password
POSTGRES_URL=localhost
POSTGRES_PORT=5432
```

---

## 2. Сценарий: Полный цикл работы

### Шаг 1. Регистрация участников

**Заказчик:**
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username": "customer_ivan", "password": "password123", "role": "CUSTOMER"}'
```
*Сохраните `accessToken` из ответа.*

**Исполнитель:**
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username": "executor_alex", "password": "password123", "role": "EXECUTOR"}'
```

**Администратор:**
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username": "admin_boss", "password": "password123", "role": "ADMIN"}'
```

---

### Шаг 2. Создание задачи (Заказчик)

```bash
curl -X POST http://localhost:8080/api/tasks \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <CUSTOMER_TOKEN>" \
  -d '{"description": "Разработать API", "reward": 500.00}'
```
*Запомните `id` задачи (например, `1`).*

---

### Шаг 3. Отклик на задачу (Исполнитель)

Найти доступные задачи:
```bash
curl -X GET "http://localhost:8080/api/tasks/available" \
  -H "Authorization: Bearer <EXECUTOR_TOKEN>"
```

Откликнуться:
```bash
curl -X POST http://localhost:8080/api/bids/task/1 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <EXECUTOR_TOKEN>" \
  -d '{"description": "Готов выполнить за 2 дня"}'
```

---

### Шаг 4. Выбор исполнителя (Заказчик)

Посмотреть отклики:
```bash
curl -X GET "http://localhost:8080/api/bids/task/1" \
  -H "Authorization: Bearer <CUSTOMER_TOKEN>"
```

Принять отклик (например, id=1):
```bash
curl -X POST http://localhost:8080/api/bids/1/accept \
  -H "Authorization: Bearer <CUSTOMER_TOKEN>"
```
*Статус задачи сменится на `IN_PROGRESS`.*

---

### Шаг 5. Выполнение и оплата

**Исполнитель завершает работу:**
```bash
curl -X POST http://localhost:8080/api/tasks/1/complete \
  -H "Authorization: Bearer <EXECUTOR_TOKEN>"
```

**Заказчик подтверждает и оплачивает:**
```bash
curl -X POST http://localhost:8080/api/tasks/1/confirm \
  -H "Authorization: Bearer <CUSTOMER_TOKEN>"
```
*Происходит списание с баланса заказчика и начисление исполнителю.*

---

### Шаг 6. Администрирование

Проверить баланс исполнителя:
```bash
curl -X GET "http://localhost:8080/api/admin/users/2" \
  -H "Authorization: Bearer <ADMIN_TOKEN>"
```

Заблокировать пользователя:
```bash
curl -X POST http://localhost:8080/api/admin/users/2/block \
  -H "Authorization: Bearer <ADMIN_TOKEN>"
```

---

## 3. Шпаргалка по статусам

**Задачи:**
1. `OPEN` — Поиск исполнителя.
2. `IN_PROGRESS` — Работа идет.
3. `COMPLETED` — Ждет подтверждения.
4. `CONFIRMED` — Оплачено.

**Отклики:**
1. `PENDING` — На рассмотрении.
2. `ACCEPTED` — Принят.
3. `REJECTED` — Отклонен.

---

## 4. Запуск тестов

Проект покрыт тестами (Testcontainers + MockMvc):
```bash
./gradlew test
```