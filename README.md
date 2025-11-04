# Claude Chat API Server

Чат-сервер на Ktor с интеграцией Claude API от Anthropic.

## Возможности

- ✅ **Веб-интерфейс чата** - красивый UI для общения с Claude
- ✅ REST API для чата с Claude
- ✅ Поддержка JSON запросов/ответов
- ✅ CORS для фронтенд-приложений
- ✅ Health check endpoint
- ✅ Конфигурация через переменные окружения
- ✅ Логирование запросов и ответов

## API Endpoints

### POST /chat
Отправка сообщения в чат с Claude.

**Request:**
```json
{
  "message": "Привет, Claude!"
}
```

**Response:**
```json
{
  "response": "Здравствуйте! Как я могу помочь вам сегодня?"
}
```

### GET /health
Проверка статуса сервера.

**Response:**
```json
{
  "status": "ok"
}
```

### GET /
Перенаправляет на веб-интерфейс чата (`/index.html`).

## Веб-интерфейс

Сервер включает встроенный веб-интерфейс для общения с Claude.

**Доступ:** Откройте браузер и перейдите по адресу `http://localhost:8080`

**Возможности веб-интерфейса:**
- 💬 Удобная переписка с Claude в режиме чата
- ⚡ Автоматическая отправка по Enter (Shift+Enter для новой строки)
- 🎨 Современный дизайн с анимациями
- ⏱️ Индикатор загрузки при ожидании ответа
- ❌ Обработка ошибок и таймаутов (30 секунд)
- 📱 Адаптивный дизайн для мобильных устройств

![Веб-интерфейс чата](https://via.placeholder.com/800x500.png?text=Claude+Chat+Interface)

## Установка и запуск

### 1. Получите API ключ Claude

Зарегистрируйтесь на [console.anthropic.com](https://console.anthropic.com/) и получите API ключ.

### 2. Настройте переменные окружения

Скопируйте `.env.example` в `.env` и установите ваш API ключ:

```bash
cp .env.example .env
```

Отредактируйте `.env`:
```
CLAUDE_API_KEY=sk-ant-api03-...
```

### 3. Загрузите переменные окружения

```bash
export $(cat .env | xargs)
```

### 4. Запустите сервер

**Режим разработки:**
```bash
./gradlew run
```

**Сборка JAR:**
```bash
./gradlew buildFatJar
java -jar build/libs/ktor-firtsAI-0.0.1-all.jar
```

Сервер будет доступен по адресу: `http://localhost:8080`

### 5. Откройте веб-интерфейс

Откройте браузер и перейдите по адресу:
```
http://localhost:8080
```

Вы увидите веб-интерфейс чата. Введите вопрос в нижнем поле и нажмите кнопку отправки или клавишу Enter!

## Тестирование API

### С помощью curl:

```bash
# Отправить сообщение в чат
curl -X POST http://localhost:8080/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "Привет, Claude!"}'

# Проверить health check
curl http://localhost:8080/health
```

### С помощью HTTPie:

```bash
# Отправить сообщение в чат
http POST localhost:8080/chat message="Привет, Claude!"

# Проверить health check
http GET localhost:8080/health
```

## Деплой на VPS

### 1. Подготовка сервера

```bash
# Обновите систему
sudo apt update && sudo apt upgrade -y

# Установите Java 17+
sudo apt install openjdk-17-jre -y

# Проверьте версию
java -version
```

### 2. Сборка приложения

```bash
./gradlew buildFatJar
```

### 3. Загрузите на сервер

```bash
scp build/libs/ktor-firtsAI-0.0.1-all.jar user@your-vps-ip:/opt/claude-chat/
```

### 4. Создайте systemd service

```bash
sudo nano /etc/systemd/system/claude-chat.service
```

Содержимое файла:
```ini
[Unit]
Description=Claude Chat API Server
After=network.target

[Service]
Type=simple
User=www-data
WorkingDirectory=/opt/claude-chat
ExecStart=/usr/bin/java -jar /opt/claude-chat/ktor-firtsAI-0.0.1-all.jar
Restart=on-failure
Environment="CLAUDE_API_KEY=your_api_key_here"

[Install]
WantedBy=multi-user.target
```

### 5. Запустите сервис

```bash
sudo systemctl daemon-reload
sudo systemctl enable claude-chat
sudo systemctl start claude-chat
sudo systemctl status claude-chat
```

### 6. Настройте Nginx (опционально)

```bash
sudo apt install nginx -y
sudo nano /etc/nginx/sites-available/claude-chat
```

Содержимое:
```nginx
server {
    listen 80;
    server_name your-domain.com;

    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }
}
```

Активируйте конфигурацию:
```bash
sudo ln -s /etc/nginx/sites-available/claude-chat /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl restart nginx
```

### 7. SSL сертификат (Let's Encrypt)

```bash
sudo apt install certbot python3-certbot-nginx -y
sudo certbot --nginx -d your-domain.com
```

## Структура проекта

```
src/main/kotlin/com/example/
├── config/
│   └── ClaudeConfig.kt         # Конфигурация Claude API
├── models/
│   ├── ChatRequest.kt          # Модель запроса от пользователя
│   ├── ChatResponse.kt         # Модель ответа
│   └── ClaudeModels.kt         # Модели Claude API
├── routes/
│   └── ChatRoutes.kt           # HTTP endpoints
├── services/
│   └── ClaudeService.kt        # Сервис для работы с Claude API
├── Application.kt              # Главный файл приложения
└── Routing.kt                  # Конфигурация роутинга
```

## Переменные окружения

| Переменная | Обязательная | По умолчанию | Описание |
|-----------|--------------|--------------|----------|
| `CLAUDE_API_KEY` | ✅ Да | - | API ключ Claude |
| `CLAUDE_MODEL` | ❌ Нет | `claude-haiku-4-5-20251001` | Модель Claude |
| `CLAUDE_MAX_TOKENS` | ❌ Нет | `1024` | Максимум токенов в ответе |
| `CLAUDE_TEMPERATURE` | ❌ Нет | `1.0` | Температура генерации (0.0-1.0) |

## Gradle Tasks

| Задача | Описание |
|--------|----------|
| `./gradlew test` | Запустить тесты |
| `./gradlew build` | Собрать проект |
| `./gradlew buildFatJar` | Собрать executable JAR со всеми зависимостями |
| `./gradlew run` | Запустить сервер в режиме разработки |

## Технологии

- **Kotlin** - язык программирования
- **Ktor 3.x** - веб-фреймворк
- **Kotlinx Serialization** - JSON сериализация
- **Ktor Client** - HTTP клиент для запросов к Claude API
- **Netty** - HTTP сервер

## Полезные ссылки

- [Ktor Documentation](https://ktor.io/docs/home.html)
- [Claude API Documentation](https://docs.anthropic.com/claude/reference/getting-started-with-the-api)
- [Ktor GitHub page](https://github.com/ktorio/ktor)

## Лицензия

MIT
