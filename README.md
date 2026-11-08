# МигрениНЕТ — онлайн-поликлиника

Spring Boot 3 (Java 21) + JPA + PostgreSQL/H2, Telegram-бот и Mini App.
Интерфейс: `src/main/resources/static/index.html`. Если сервер недоступен, он работает в демо-режиме.

## Запуск
    mvn spring-boot:run            # http://localhost:8080 (H2, без Telegram)
    docker compose up --build      # с PostgreSQL

## Telegram-бот
1. В Telegram откройте @BotFather → `/newbot`, получите токен.
2. Передайте приложению переменные окружения (секреты в коде не хранятся):

| Переменная | Назначение |
|---|---|
| `TELEGRAM_TOKEN` | токен бота. Без него бот выключен |
| `WEBAPP_URL` | https-адрес интерфейса, который откроет кнопка «Заполнить дневник» |
| `DOCTOR_CHAT_ID` | chat id врача для сводки отклонений в 11:30 |
| `DOCTOR_KEY` | ключ доступа к панели врача (`?dk=...`) |
| `REMINDER_TZ` | часовой пояс напоминаний, по умолчанию Europe/Moscow |
| `CORS_ORIGIN` | адрес страницы, если интерфейс открыт не с сервера, например GitHub Pages |

3. Напоминание приходит каждый день в 08:00. Подписка: `/start`, отключение: `/stop`.
4. Mini App требует HTTPS. Подойдёт Render, Railway, Fly.io или VPS с Caddy/nginx.

Узнать свой chat id: напишите боту @userinfobot.

## GitHub Secrets
Secrets доступны только workflow и Codespaces, запущенный сервер их не видит. Сохраните `TELEGRAM_TOKEN` и `DOCTOR_KEY` в Settings → Secrets and variables, а на хостинге задайте те же значения как переменные окружения. Образ `ghcr.io/<логин>/migrenenet:latest` собирает workflow `docker.yml`.

## API
- `POST /api/diary`, `GET /api/patients/{id}/entries`, `GET /api/patients/{id}/stats`
- `GET /api/doctor/alerts` — заголовок `X-Doctor-Key`
- `GET /api/health`

С токеном бота пациент определяется по проверенной подписи Telegram (`X-Telegram-Init-Data`), а не по полю в запросе.

Сервис не заменяет очный осмотр.
