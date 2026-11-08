# МигрениНЕТ — онлайн-поликлиника

Spring Boot 3 (Java 21) + JPA + PostgreSQL/H2. Интерфейс-прототип: `src/main/resources/static/index.html`.

## Запуск
    mvn spring-boot:run            # http://localhost:8080 (H2, без настройки)
    docker compose up --build      # с PostgreSQL

## API
- `POST /api/diary` — сохранить утренний дневник (повтор за ту же дату обновляет запись)
- `GET /api/patients/{id}/entries?days=30`
- `GET /api/patients/{id}/stats?days=30` — 5 критериев эффективности
- `GET /api/doctor/alerts` — красные флаги для врача

## GitHub
- `ci.yml` — сборка и тесты на каждый push
- `pages.yml` — публикует демо-интерфейс на GitHub Pages (Settings → Pages → Source: GitHub Actions)

Сервис не заменяет очный осмотр.
