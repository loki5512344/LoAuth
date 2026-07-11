<div align="center">

# LoAuth

Discord-linked authentication plugin with verification system for Paper servers.

![Java](https://img.shields.io/badge/Java-21+-orange?style=flat-square&logo=openjdk&logoColor=white)
![Paper](https://img.shields.io/badge/Paper-1.19.2+-blue?style=flat-square)
![Folia](https://img.shields.io/badge/Folia-supported-purple?style=flat-square)
![License](https://img.shields.io/badge/license-GPLv3-blue?style=flat-square&logo=gnu&logoColor=white)
![version](https://img.shields.io/badge/version-0.0.1--dev-yellow?style=flat-square)
![status](https://img.shields.io/badge/status-WIP-red?style=flat-square)

[English](#english) | [Русский](#russian)

</div>

---

> **Note:** LoAuth is in early development (v0.0.1). API and config may change.

<a name="english"></a>

## English

### Overview

LoAuth is a Discord-linked authentication plugin. Players verify their identity through a Discord bot to log in to the server, linking their Minecraft account to their Discord account with optional role-based rewards.

### Features

| Feature | Description |
|---------|-------------|
| Discord verification | Link Minecraft account to Discord via bot |
| Login protection | Only verified players can log in |
| Role-based access | Different Discord roles grant different permissions |
| Bot integration | Built-in JDA Discord bot |
| Login reactions | React to a Discord message to confirm login |
| Multi-module | Common, Discord bot, and Paper plugin modules |
| Configurable | Full control over auth flow and messages |

### Commands

| Command | Description |
|---------|-------------|
| `/login <code>` | Authenticate with a verification code |

### Architecture

```
LoAuth/
├── common/   - Shared code between modules
├── discord/  - JDA Discord bot
└── plugin/   - Paper plugin (main module)
```

### Dependencies

- Required: Paper 1.19.2+, Java 21+, Discord bot token
- Optional: PlaceholderAPI

### Installation

1. Drop the jar into `plugins/`
2. Configure Discord bot token in `plugins/LoAuth/config.yml`
3. Restart the server
4. Invite the Discord bot to your server

---

<a name="russian"></a>

## Русский

### Обзор

LoAuth - плагин авторизации с привязкой к Discord. Игроки подтверждают личность через Discord-бота для входа на сервер, связывая Minecraft-аккаунт с Discord-аккаунтом.

### Возможности

| Возможность | Описание |
|-------------|----------|
| Верификация через Discord | Привязка Minecraft-аккаунта к Discord через бота |
| Защита входа | Только верифицированные игроки могут войти |
| Роли Discord | Разные роли Discord дают разные права |
| Интеграция с ботом | Встроенный JDA Discord-бот |
| Реакции для входа | Подтверждение входа реакцией в Discord |
| Мульти-модульность | Common, Discord-бот и Paper-плагин |
| Настройка | Полный контроль над процессом авторизации |

### Команды

| Команда | Описание |
|---------|----------|
| `/login <код>` | Авторизация с кодом верификации |

### Архитектура

```
LoAuth/
├── common/   - Общий код между модулями
├── discord/  - JDA Discord-бот
└── plugin/   - Paper плагин (основной)
```

### Зависимости

- Обязательные: Paper 1.19.2+, Java 21+, токен Discord-бота
- Опциональные: PlaceholderAPI

### Установка

1. Положите jar в папку `plugins/`
2. Настройте токен Discord-бота в `plugins/LoAuth/config.yml`
3. Перезапустите сервер
4. Пригласите Discord-бота на свой сервер

---

### Links

- [Releases](../../releases)
- [Issues](../../issues)
- [License](LICENSE)

### License

GNU General Public License v3.0