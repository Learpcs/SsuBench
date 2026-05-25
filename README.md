# SsuBench — Платформа для размещения заданий

REST API сервис, где заказчики размещают задания, исполнители откликаются на них, а оплата выполненной работы производится виртуальными баллами.

## 🚀 Быстрый старт

### 1. Запуск базы данных

```bash
docker compose up -d postgres
```

### 2. Запуск приложения

```bash
./gradlew bootRun
```

Или собрать JAR:

```bash
./gradlew build
java -jar build/libs/SsuBench-0.0.1-SNAPSHOT.jar
```

### 3. Переменные окружения

Создайте `.env` файл или экспортируйте переменные:

```bash
export JWT_SECRET="your-super-secret-key-at-least-32-chars"
export JWT_ACCESS_EXPIRATION=900000
export JWT_REFRESH_EXPIRATION=604800000

export POSTGRES_DB=db
export POSTGRES_USER=user
export POSTGRES_PASSWORD=password
export POSTGRES_URL=localhost
export POSTGRES_PORT=5432
```

---

## 📋 Основные сценарии использования

### Сценарий 1: Заказчик создаёт задачу и находит исполнителя

#### Шаг 1.1 — Регистрация заказчика

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "customer_ivan",
    "password": "SecurePass123!",
    "role": "CUSTOMER"
  }'
```

**Ответ:**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

Сохраните токены — они понадобятся для всех последующих запросов.

#### Шаг 1.2 — Создание задачи

```bash
curl -X POST http://localhost:8080/api/tasks \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <customer_token>" \
  -d '{
    "description": "Разработать REST API для сервиса уведомлений",
    "reward": 500.00
  }'
```

**Ответ:**
```json
{
  "id": 1,
  "description": "Разработать REST API для сервиса уведомлений",
  "customerId": 1,
  "customerUsername": "customer_ivan",
  "status": "OPEN",
  "reward": 500.00,
  "acceptedBidId": null,
  "createdAt": "2025-05-25T10:00:00Z"
}
```

Задача создана в статусе `OPEN`. Запомните `id` задачи — он понадобится для работы с откликами.

---

### Сценарий 2: Исполнитель находит задачу и откликается

#### Шаг 2.1 — Регистрация исполнителя

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "executor_alex",
    "password": "SecurePass123!",
    "role": "EXECUTOR"
  }'
```

#### Шаг 2.2 — Поиск доступных задач

```bash
curl -X GET "http://localhost:8080/api/tasks/available?page=0&size=10" \
  -H "Authorization: Bearer <executor_token>"
```

**Ответ:**
```json
{
  "items": [
    {
      "id": 1,
      "description": "Разработать REST API для сервиса уведомлений",
      "customerUsername": "customer_ivan",
      "status": "OPEN",
      "reward": 500.00
    }
  ],
  "page": 0,
  "size": 10,
  "totalElements": 1,
  "hasNext": false
}
```

#### Шаг 2.3 — Отклик на задачу

```bash
curl -X POST http://localhost:8080/api/bids/task/1 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <executor_token>" \
  -d '{
    "description": "Готов выполнить за 3 дня. Есть опыт разработки REST API на Spring Boot."
  }'
```

**Ответ:**
```json
{
  "id": 1,
  "executorId": 2,
  "executorUsername": "executor_alex",
  "taskId": 1,
  "description": "Готов выполнить за 3 дня...",
  "status": "PENDING",
  "createdAt": "2025-05-25T11:00:00Z"
}
```

Отклик создан в статусе `PENDING` и ожидает решения заказчика.

---

### Сценарий 3: Заказчик выбирает исполнителя

#### Шаг 3.1 — Просмотр всех откликов на задачу

```bash
curl -X GET "http://localhost:8080/api/bids/task/1?page=0&size=10" \
  -H "Authorization: Bearer <customer_token>"
```

**Ответ:**
```json
{
  "items": [
    {
      "id": 1,
      "executorUsername": "executor_alex",
      "description": "Готов выполнить за 3 дня...",
      "status": "PENDING"
    },
    {
      "id": 2,
      "executorUsername": "executor_maria",
      "description": "Сделаю быстро и качественно",
      "status": "PENDING"
    }
  ]
}
```

#### Шаг 3.2 — Принятие отклика

```bash
curl -X POST http://localhost:8080/api/bids/1/accept \
  -H "Authorization: Bearer <customer_token>"
```

**Что происходит:**
- Статус отклика меняется на `ACCEPTED`
- Статус задачи меняется на `IN_PROGRESS`
- Все остальные отклики автоматически отклоняются (`REJECTED`)

**Ответ:**
```json
{
  "id": 1,
  "executorUsername": "executor_alex",
  "status": "ACCEPTED"
}
```

---

### Сценарий 4: Исполнитель выполняет работу

#### Шаг 4.1 — Завершение задачи

После выполнения работы исполнитель помечает задачу как завершённую:

```bash
curl -X POST http://localhost:8080/api/tasks/1/complete \
  -H "Authorization: Bearer <executor_token>"
```

**Ответ:**
```json
{
  "id": 1,
  "description": "Разработать REST API для сервиса уведомлений",
  "status": "COMPLETED",
  "reward": 500.00
}
```

Задача перешла в статус `COMPLETED` и ожидает подтверждения от заказчика.

---

### Сценарий 5: Заказчик подтверждает выполнение и оплачивает

#### Шаг 5.1 — Подтверждение выполнения

```bash
curl -X POST http://localhost:8080/api/tasks/1/confirm \
  -H "Authorization: Bearer <customer_token>"
```

**Что происходит:**
1. Проверяется баланс заказчика (должно быть ≥ 500.00)
2. Списывается 500.00 у заказчика
3. Начисляется 500.00 исполнителю
4. Создаётся запись о платеже
5. Статус задачи меняется на `CONFIRMED`

**Ответ:**
```json
{
  "id": 1,
  "taskId": 1,
  "bidId": 1,
  "amount": 500.00,
  "processedAt": "2025-05-25T15:00:00Z"
}
```

#### Шаг 5.2 — Проверка баланса исполнителя

```bash
curl -X GET http://localhost:8080/api/admin/users/2 \
  -H "Authorization: Bearer <admin_token>"
```

**Ответ:**
```json
{
  "id": 2,
  "username": "executor_alex",
  "role": "EXECUTOR",
  "balance": 500.00,
  "isBlocked": false
}
```

Баланс исполнителя увеличился на 500.00 ✅

---

### Сценарий 6: Администратор управляет пользователями

#### Шаг 6.1 — Регистрация администратора

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin_boss",
    "password": "SecurePass123!",
    "role": "ADMIN"
  }'
```

#### Шаг 6.2 — Просмотр всех пользователей

```bash
curl -X GET "http://localhost:8080/api/admin/users?page=0&size=20" \
  -H "Authorization: Bearer <admin_token>"
```

**Ответ:**
```json
{
  "items": [
    {
      "id": 1,
      "username": "customer_ivan",
      "role": "CUSTOMER",
      "balance": 500.00,
      "isBlocked": false
    },
    {
      "id": 2,
      "username": "executor_alex",
      "role": "EXECUTOR",
      "balance": 500.00,
      "isBlocked": false
    }
  ],
  "totalElements": 2
}
```

#### Шаг 6.3 — Блокировка нарушителя

```bash
curl -X POST http://localhost:8080/api/admin/users/2/block \
  -H "Authorization: Bearer <admin_token>"
```

**Ответ:**
```json
{
  "id": 2,
  "username": "executor_alex",
  "role": "EXECUTOR",
  "isBlocked": true
}
```

Заблокированный пользователь не может войти в систему.

#### Шаг 6.4 — Разблокировка

```bash
curl -X POST http://localhost:8080/api/admin/users/2/unblock \
  -H "Authorization: Bearer <admin_token>"
```

---

## 🔧 Дополнительные операции

### Отмена отклика (исполнитель)

Если исполнитель передумал:

```bash
curl -X POST http://localhost:8080/api/bids/1/cancel \
  -H "Authorization: Bearer <executor_token>"
```

Можно отменить только отклик в статусе `PENDING`.

### Отклонение отклика (заказчик)

```bash
curl -X POST http://localhost:8080/api/bids/1/reject \
  -H "Authorization: Bearer <customer_token>"
```

### Отмена задачи (заказчик)

```bash
curl -X POST http://localhost:8080/api/tasks/1/cancel \
  -H "Authorization: Bearer <customer_token>"
```

Нельзя отменить задачу в статусе `COMPLETED` или `CONFIRMED`.

### Обновление задачи (заказчик)

```bash
curl -X PUT http://localhost:8080/api/tasks/1 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <customer_token>" \
  -d '{
    "description": "Обновлённое описание задачи",
    "reward": 600.00
  }'
```

Можно обновить только задачу в статусе `OPEN`.

### Получение информации о платеже

```bash
curl -X GET http://localhost:8080/api/tasks/1/payment \
  -H "Authorization: Bearer <customer_token>"
```

---

## 📊 Статусы задач

| Статус | Описание |
|--------|----------|
| `OPEN` | Задача опубликована, принимаются отклики |
| `IN_PROGRESS` | Исполнитель выбран, работа идёт |
| `COMPLETED` | Исполнитель завершил работу, ждёт подтверждения |
| `CONFIRMED` | Заказчик подтвердил, оплата проведена |
| `CANCELLED` | Задача отменена заказчиком |

## 📊 Статусы откликов

| Статус | Описание |
|--------|----------|
| `PENDING` | Отклик отправлен, ждёт решения |
| `ACCEPTED` | Заказчик принял отклик |
| `REJECTED` | Заказчик отклонил отклик |
| `CANCELLED` | Исполнитель отменил отклик |

## 🔐 Аутентификация

Все запросы кроме `/api/auth/**` требуют JWT токен:

```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

Токен передаётся в заголовке каждого запроса. Время жизни access токена — 15 минут, refresh токена — 7 дней.

### Обновление токенов

```bash
curl -X POST http://localhost:8080/api/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
  }'
```

---

## 🏗 Архитектура

```
src/main/java/com/rodin/SsuBench/
├── Config/          # Конфигурация безопасности, JWT, CORS
├── Controller/      # REST контроллеры
├── Entity/          # JPA сущности
├── Exception/       # Обработка ошибок
├── Middleware/      # Фильтры (logging, request_id, recover)
├── Repository/      # Репозитории
├── Service/         # Бизнес-логика
└── Utils/           # Утилиты (JWT)
```

## 🧪 Тесты

Компонентные тесты для всех endpoints:

```bash
./gradlew test --tests "com.rodin.SsuBench.ComponentTests.*"
```

Тесты используют:
- **Testcontainers** — реальный PostgreSQL в Docker
- **MockMvc** — HTTP запросы к контроллерам
- **58 тестов** покрывают все основные сценарии

## 📄 OpenAPI спецификация

Полная спецификация доступна в файле `src/main/resources/openapi.yaml`.

Можно открыть в Swagger UI или ReDoc:

```bash
# Через Swagger Editor
open https://editor.swagger.io/?url=https://raw.githubusercontent.com/your-repo/SsuBench/main/src/main/resources/openapi.yaml
```

## ⚙️ Конфигурация сервера

| Параметр | Значение |
|----------|----------|
| Connection timeout | 20s |
| Keep-alive timeout | 60s |
| Graceful shutdown | 30s |
| Max threads | 200 |

## 🛡 Безопасность

- Пароли хранятся в bcrypt
- JWT через `Authorization: Bearer`
- Валидация всех входных данных
- Единый формат ошибок
- Request ID для трассировки запросов

---

## Примеры ошибок

### Недостаточно средств

```json
{
  "status": 400,
  "message": "Недостаточно средств на балансе",
  "timestamp": "2025-05-25T15:00:00Z"
}
```

### Задача не найдена

```json
{
  "status": 404,
  "message": "Задача не найдена",
  "timestamp": "2025-05-25T15:00:00Z"
}
```

### Токен невалиден

```json
{
  "status": 401,
  "message": "Токен недействителен или истек",
  "timestamp": "2025-05-25T15:00:00Z"
}
```

---

**SsuBench** — просто и надёжно. Заказчики находят исполнителей, исполнители получают оплату. Всё честно.
