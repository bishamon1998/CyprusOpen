# Railway: проект развёртывания

Конфигурация для будущего skeleton. Ресурсы, домены и automatic production deploys не создавались. Нужны отдельное разрешение на выкладку, бюджет и проверенные Dockerfiles.

| Сервис | Build context / Dockerfile | Сеть и health |
| --- | --- | --- |
| Frontend | Репозиторий `/`, `frontend/Dockerfile` | Публичный HTTP; `/healthz`; proxy к backend |
| Statistics | Репозиторий `/`, `backend/statistics-service/Dockerfile` | Private HTTP; `/actuator/health/readiness`; своя база |
| Parking | Репозиторий `/`, `backend/parking-service/Dockerfile` | Private HTTP; `/actuator/health/readiness`; БД необязательна до static metadata |
| PostgreSQL | Один Railway PostgreSQL | Private network; две логические базы и роли |

Для backend сохраняем root context, чтобы build видел root POM и common-library. [Railway monorepo docs](https://docs.railway.com/deployments/monorepo) указывают, что Root Directory ограничивает передаваемые файлы; каталог отдельного backend не подходит для сборки с общей библиотекой.

Watch paths backend: свой `backend/<service>/**`, `backend/common-library/**`, root `pom.xml`, Maven Wrapper и build config. Frontend следит за своими и proxy/build файлами. Документационные коммиты не должны пересобирать приложения. Common JAR не является Railway service.

Приложения слушают `0.0.0.0:$PORT`. HTTP-сервер frontend получает внутренние адреса backend через env и сохраняет `/api/v1/...` path; адреса не включаются в Vite browser bundle. PostgreSQL variables преобразуются в JDBC URL и отдельные service credentials; superuser не используется приложениями. `infra/postgres` автоматически применяется только локальным Docker image: в Railway базы/роли создаются отдельным контролируемым provisioning-шагом.

## Первая выкладка

1. После разрешения создать проект/сервисы, private PostgreSQL и роли; настроить секреты вне Git.
2. Проверить приложения с `EXTERNAL_FETCH_ENABLED=false`, `SYNC_ENABLED=false`. Liquibase применяется каждым владельцем своей схемы. Health не обращается к госисточникам.
3. Проверить frontend proxy, API 404, языки, mobile, атрибуцию. Backend и БД не выставлять публично.
4. Подтвердить условия источников, контакт User-Agent, одного владельца scheduler на host. Только затем включить fetch и наблюдать первую ограниченную синхронизацию.
5. Перед следующим deployment scheduler остановить старого владельца и дождаться окончания задания; не допускать overlap при rolling deploy.

Откат: выключить fetch, вернуть предыдущий образ, проверить health/API. Миграции не откатываются автоматически; перед изменением схемы нужны backup и проверка восстановления. Схемы должны быть обратно совместимы. Parking snapshot при рестарте исчезает — unavailable до нового успешного refresh. Статистика остаётся в PostgreSQL.
