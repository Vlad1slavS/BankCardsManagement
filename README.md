# Система управления банковскими картами

REST API для управления банковскими картами с аутентификацией JWT и ролевым доступом.

## Технологии

- **Java 21**, Spring Boot 4.0.2
- **Spring Security** + JWT (JJWT)
- **Spring Data JPA** + PostgreSQL
- **Liquibase**
- **MapStruct**
- **Lombok**
- **Docker / Docker Compose**
- **SpringDoc OpenAPI** (Swagger UI)

## Запуск

```bash
git clone https://github.com/Vlad1slavS/BankCardsManagement.git
cd BankCardsManagement
docker-compose up --build
```

Приложение будет доступно по адресу: **http://localhost:8080**

Swagger будет доступен по адресу: **http://localhost:8080/swagger-ui/index.html**

## Конфигурация

Необходимо заполнить `.env.example` файл в корне проекта

| Переменная       | По умолчанию | Описание                     |
|------------------|--------------|------------------------------|
| `DB_HOST`        | `localhost`  | Хост PostgreSQL              |
| `DB_PORT`        | `5432`       | Порт PostgreSQL              |
| `DB_NAME`        | `bankcard`   | Имя базы данных              |
| `DB_USER`        | `postgres`   | Пользователь БД              |
| `DB_PASSWORD`    | `postgres`   | Пароль БД                    |
| `JWT_SECRET`     | (hex-строка) | Секретный ключ для JWT       |
| `JWT_EXPIRATION` | `86400000`   | Время жизни токена (мс)      |
| `ENCRYPTION_KEY` | (строка)     | Ключ шифрования номеров карт |

## Учётные данные по умолчанию

| Роль  | Username | Password |
|-------|----------|----------|
| ADMIN | `admin`  | `1234`   |

> Пароль по умолчанию: **`Admin1234`**


