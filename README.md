# deface-telegram

Telegram bot that anonymizes images using external API.

## Setup

Set required environment variables:

- `TELEGRAM_BOT_TOKEN`
- `DEFACE_ENDPOINT`

Optional:

- `TELEGRAM_BOT_USERNAME`
- `DEFAULT_FILTER_NAME`
- `DEFAULT_PASTE_STYLE`

## Build

mvn package

## Run

java -jar target/deface-telegram.jar

## Docker

Build and run:

```bash
docker build -t deface-telegram .
docker run --rm \
  -e TELEGRAM_BOT_TOKEN=... \
  -e DEFACE_ENDPOINT=... \
  -e TELEGRAM_BOT_USERNAME=... \
  -e DEFAULT_FILTER_NAME=... \
  -e DEFAULT_PASTE_STYLE=... \
  deface-telegram
```

Compose (environment variables are read from your shell):

```bash
docker compose up --build
```
