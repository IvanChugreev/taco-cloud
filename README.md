# Taco Cloud

Taco Cloud — учебное серверное веб-приложение для создания тако и оформления заказов. Пользователь может зарегистрироваться, войти в систему, выбрать ингредиенты, сохранить свой тако и заполнить форму доставки и оплаты.

## Стек

- Java 17;
- Spring Boot 3.5.6;
- Spring MVC и Thymeleaf;
- Spring Security;
- REST API и OpenAPI/Swagger UI;
- Spring Data JPA и Hibernate;
- Jakarta Bean Validation;
- PostgreSQL 17;
- Flyway;
- Docker Compose;
- Lombok;
- Maven Wrapper;
- JUnit 5 и Spring Boot Test.

## Требования

- JDK 17;
- Docker Desktop или Docker Engine с Compose;
- свободный TCP-порт `8080`.

Устанавливать Maven отдельно не требуется: проект использует Maven Wrapper. Данные PostgreSQL сохраняются в Docker volume и переживают перезапуск приложения и контейнера.

## Запуск

Windows PowerShell:

```powershell
Copy-Item .env.example .env
# Задайте безопасный DB_PASSWORD в .env
docker compose up -d postgres
.\mvnw.cmd test
.\mvnw.cmd spring-boot:run
.\mvnw.cmd clean package
```

Linux и macOS:

```sh
cp .env.example .env
# Задайте безопасный DB_PASSWORD в .env
docker compose up -d postgres
./mvnw test
./mvnw spring-boot:run
./mvnw clean package
```

После запуска приложение доступно по адресу [http://localhost:8080](http://localhost:8080). Для начала работы откройте [страницу регистрации](http://localhost:8080/register), создайте пользователя и войдите с указанными данными.

## REST API

REST API версионировано префиксом `/api/v1` и использует HTTP Basic с данными
зарегистрированного пользователя. Пользователи имеют одну из ролей: `CUSTOMER`,
`KITCHEN` или `ADMIN`. Swagger UI доступен по адресу
[http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html), описание
OpenAPI — по адресу [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs).

Доступные операции:

- `POST /api/v1/orders` — создать заказ из существующих тако;
- `GET /api/v1/orders/{id}` — получить свой заказ по UUID;
- `GET /api/v1/orders` — получить страницу своих заказов;
- `POST /api/v1/orders/{id}/cancel` — отменить заказ с проверкой версии;
- `GET /api/v1/ingredients` — получить ингредиенты;
- `GET /api/v1/tacos` — получить созданные тако, доступные для заказа;
- `POST /api/v1/kitchen/orders/{id}/status` — изменить статус заказа для кухни;
- `GET /api/v1/admin/orders` и `GET /api/v1/admin/orders/{id}` — просмотреть заказы администратору.

Клиентские endpoints `/api/v1/orders/**` доступны только роли `CUSTOMER` и всегда
ограничены заказами текущего пользователя. Кухонные endpoints доступны ролям
`KITCHEN` и `ADMIN`, административные — только роли `ADMIN`.

Список заказов поддерживает параметры `status`, `createdFrom`, `createdTo`, `page`,
`size` и `sort`. Максимальный размер страницы — 100. Временные границы передаются в
формате ISO 8601, например `2026-07-01T00:00:00Z`.

Пример полного сценария в `curl` после регистрации пользователя и создания тако через
web-интерфейс:

```sh
curl -u user:password http://localhost:8080/api/v1/tacos

curl -u user:password -H "Content-Type: application/json" \
  -d '{
    "tacoIds": [1],
    "deliveryAddress": {
      "recipientName": "Test User",
      "street": "1 Test Street",
      "city": "Test City",
      "state": "TS",
      "zip": "12345"
    },
    "comment": "API order",
    "ccNumber": "4111111111111111",
    "ccExpiration": "12/30",
    "ccCvv": "123"
  }' http://localhost:8080/api/v1/orders

curl -u user:password http://localhost:8080/api/v1/orders/{order-uuid}
```

Стоимость и владелец заказа определяются сервером. Ответы не содержат пароли,
платёжные реквизиты или внутренние идентификаторы сущностей. Ошибки API возвращаются
в формате `application/problem+json`.

Остановить приложение и PostgreSQL без удаления данных:

```sh
docker compose stop
```

Удаление volume и всех локальных данных выполняется только явно:

```sh
docker compose down -v
```

## Конфигурация базы данных

Параметры подключения задаются переменными окружения или локальным файлом `.env`, который исключён из Git. Образец находится в `.env.example`.

- `DB_HOST` — адрес PostgreSQL;
- `DB_PORT` — порт PostgreSQL;
- `DB_NAME` — имя базы;
- `DB_USERNAME` — пользователь базы;
- `DB_PASSWORD` — пароль пользователя базы.

Доступны профили Spring:

- `local` — профиль по умолчанию, использует PostgreSQL из `compose.yaml` и загружает начальные ингредиенты dev-миграцией;
- `test` — запускает временный PostgreSQL 17 через Testcontainers и применяет все миграции;
- `prod` — требует все параметры подключения из окружения и не загружает dev-данные.

Flyway создаёт базовую схему миграциями `V1`–`V4`, загружает локальные ингредиенты через `V100`, расширяет модель заказа миграцией `V101` и добавляет роли пользователей миграцией `V102`. Hibernate работает с `ddl-auto: validate` и только проверяет соответствие JPA-модели созданной схеме.

## Текущее состояние

Исходное состояние зафиксировано 30 июня 2026 года.

- приложение компилируется и запускается на Java 17;
- автоматические тесты проходят;
- регистрация, вход, создание тако и оформление заказа работают;
- схема PostgreSQL управляется Flyway;
- данные сохраняются между перезапусками;
- контроллеры используют DTO и делегируют бизнес-операции сервисному слою;
- операции записи выполняются в транзакциях, сервисы покрыты unit-тестами;
- ошибки контроллеров обрабатываются единым `ControllerAdvice`;
- Hibernate проверяет схему и не создаёт таблицы автоматически.
- заказ имеет UUID, временные метки создания и обновления, итоговую стоимость и версию для optimistic locking;
- стоимость рассчитывается на сервере из цен ингредиентов;
- жизненный цикл заказа ограничен допустимыми переходами, а завершённые заказы нельзя редактировать.
- REST API покрывает создание, чтение, фильтрацию и отмену собственных заказов;
- OpenAPI и Swagger UI публикуют документацию API.
- доступ разделён по ролям `CUSTOMER`, `KITCHEN` и `ADMIN`; правила доступа покрыты автоматическими тестами.

## Архитектура

Основной поток зависимостей: `controller` → `service` → `repository`. JPA-сущности находятся в `domain` и не используются как модели web-форм. Входные данные передаются через `dto`, а преобразования между DTO и сущностями сосредоточены в `mapper`.

- `controller/` — обработка HTTP-запросов, валидация и выбор представления;
- `service/` — бизнес-операции и границы транзакций;
- `repository/` — доступ к данным через Spring Data JPA;
- `domain/` — JPA-сущности;
- `dto/` — формы и данные для представлений;
- `mapper/` — преобразование DTO и сущностей;
- `config/` — конфигурация приложения;
- `security/` — конфигурация Spring Security.
