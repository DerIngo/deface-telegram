# Project Agent Instructions

This project is a Java 21 Telegram bot called "deface-telegram".

## Goal

Build a Telegram bot that:

- Receives images
- Sends them to an external API for processing
- Returns the modified image to the user

## Technology

- Language: Java 21
- Build tool: Maven
- Telegram API: telegrambots library (LongPolling)
- HTTP Client: OkHttp (for multipart upload)
- Config: Environment variables + .env + optional properties
- Deployment: Docker (multi-stage build)

## Configuration

The following environment variables must be supported:

- TELEGRAM_BOT_TOKEN
- TELEGRAM_BOT_USERNAME (optional)
- DEFACE_ENDPOINT
- DEFAULT_FILTER_NAME
- DEFAULT_PASTE_STYLE

.env files are only for local development.

## API Integration

The external API expects:

POST /api/deface-image

- multipart/form-data
- field "image": image file
- query params:
  - filter_name
  - paste_ellipse_name

Returns: processed image bytes

## Bot Features

Commands:

- /start
- /help
- /filter <name>
- /paste <name>
- /status

Supported filters:

blur
pixelate
line_mosaic
facet_effect
verwischung_1

Supported paste styles:

feathered
hard

## Architecture

Packages:

- config
- telegram
- deface
- util

Main components:

- AppConfig
- DefaceClient
- DefaceTelegramBot
- ChatSettingsStore

## Quality Rules

- No Spring / heavy frameworks
- Clean package structure
- Production-ready code
- No hardcoded secrets
- All code must compile
- Avoid unnecessary dependencies

## Development Style

- Implement features step by step
- Prefer small, testable commits
- Ask before introducing major dependencies

