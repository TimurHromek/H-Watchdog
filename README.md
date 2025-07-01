# H-Watchdog

A robust monitoring and accountability plugin for PaperMC servers, designed to deter administrator abuse by logging privileged command usage and providing real-time public alerts through Discord.

![Build Status](https://img.shields.io/badge/Build-passing-brightgreen)
![PaperMC Version](https://img.shields.io/badge/Paper-1.21+-blue)
![Java Version](https://img.shields.io/badge/Java-17+-orange)
![License](https://img.shields.io/badge/License-Apache--2.0-blue)

## Project Overview

H-Watchdog addresses a critical need for transparency in server administration. It operates as a neutral third-party observer, creating a permanent and public record of actions taken by operators (OPs). By moving administrator actions from a "black box" into the public eye, it fosters trust within the community, holds staff accountable, and provides a powerful deterrent against the misuse of power.

Its core design philosophy is to be comprehensive in its monitoring, clear in its reporting, and minimal in its performance impact.

## Table of Contents

- [Core Features](#core-features)
- [Technical Deep Dive](#technical-deep-dive)
- [Installation and Setup](#installation-and-setup)
- [Configuration Guide (`config.yml`)](#configuration-guide-configyml)
  - [`watched-commands`](#watched-commands)
  - [`discord`](#discord)
  - [`auto-demote`](#auto-demote)
  - [`excluded-ops`](#excluded-ops)
  - [Global Settings](#global-settings)
- [Commands and Permissions](#commands-and-permissions)
- [Building From Source](#building-from-source)
- [License](#license)

## Core Features

*   **Comprehensive Operator Session Auditing**: From the moment an operator joins until they leave, H-Watchdog maintains a session that tracks their activity, creating a clear context for all monitored actions.

*   **Real-Time Discord Integration**: When a monitored command is used, an alert is dispatched to a public Discord channel. This alert is non-intrusive (no `@everyone` by default) and clearly details who did what, when, and why (if a reason was provided).

*   **Proactive Justification (`/reason`)**: Empowers administrators to provide context for their actions *before* using a privileged command. This reason is attached to the subsequent alert, preemptively answering community questions.

*   **Confidential Player Reporting (`/reportop`)**: Provides a safe and confidential channel for any player to report suspicious operator behavior. Reports are sent directly and privately to a staff-only Discord channel, ensuring that concerns are heard by the appropriate people.

*   **Automated Failsafe System**: The auto-demotion feature acts as a safeguard against rapid, potentially malicious command execution. If an operator exceeds a configurable command threshold, their privileges are automatically revoked, and a public alert is issued.

*   **Immutable File-Based Logging**: For long-term auditing and record-keeping, every monitored action is written to a daily-rotating log file on the server. This provides a permanent, searchable history of all operator activity.

*   **Granular Exclusion Controls**: Acknowledging that server owners or core developers may need to perform actions without triggering public alerts, a simple exclusion system allows specific users to bypass alerts and auto-demotion. Their actions are still logged for internal accountability.

## Technical Deep Dive

H-Watchdog is designed to be as performant as possible by leveraging PaperMC's event-driven system and asynchronous processing.

1.  **Session Management**: The plugin listens for `PlayerJoinEvent` and `PlayerQuitEvent`. When a player with operator privileges joins, the `OPSessionManager` instantiates a lightweight `OPSession` object for them. This object is stored in a `ConcurrentHashMap` and holds transient data like the last provided reason and a list of recent violation timestamps. This session is destroyed upon logout to free up memory.

2.  **Command Interception**: The primary logic is hooked into the `PlayerCommandPreprocessEvent` at `HIGHEST` priority. This allows H-Watchdog to inspect and act upon a command just before the server executes it.

3.  **Execution Workflow**: For any command issued by an operator, the following sequence occurs:
    a. The command string is parsed to check if it matches an entry in the `watched-commands` list.
    b. **Logging First**: If the command is on the watch list, the action is immediately logged to the daily log file. This file I/O operation is handled by an **asynchronous task** to prevent disk latency from impacting the main server thread (tick loop).
    c. **Exclusion Check**: The system checks if the operator's username exists in the `excluded-ops` list. If a match is found, the process terminates for that user.
    d. **Reason Retrieval**: The operator's `OPSession` is queried for a non-expired reason. If found, the reason is retrieved and immediately cleared from the session, ensuring it is only used once.
    e. **Violation & Demotion Check**: If auto-demotion is active, a violation timestamp is added to the session. The session then prunes any timestamps older than the `cooldown-seconds` window and returns the count of recent violations. If this count meets or exceeds the `threshold`, a task is scheduled on the main thread to de-op the player, and the original command is cancelled.
    f. **Discord Alert**: A JSON payload for the Discord webhook is constructed. The entire network request to the Discord API is executed on an **asynchronous thread**, guaranteeing that network latency or a slow response from Discord will not cause server lag.

## Installation and Setup

1.  Download the latest release JAR from the [Releases](https://github.com/TimurHromek/H-Watchdog/releases) page.
2.  Place the JAR file into your server's `/plugins` directory.
3.  Start the server. H-Watchdog will generate its configuration folder at `/plugins/H-Watchdog/`.
4.  Stop the server.
5.  Navigate to the configuration folder and open `config.yml`.
6.  **Crucially, you must provide valid Discord webhook URLs** for `public-webhook-url` and `report-webhook-url`.
7.  Customize other settings as desired, then save the file.
8.  Start the server to apply your new configuration.

## Configuration Guide (`config.yml`)

The `config.yml` file allows for extensive customization of the plugin's behavior.

```yaml
# A list of all commands that H-Watchdog should monitor.
# Commands must start with a '/' and be in lowercase.
watched-commands:
  - "/gamemode"
  - "/give"
  - "/kill"
  - "/tp"
  - "/ban"
  - "/kick"
  - "/op"
  - "/deop"

# Configuration for all Discord-related features.
discord:
  enabled: true
  public-webhook-url: "YOUR_PUBLIC_DISCORD_WEBHOOK_URL_HERE"
  report-webhook-url: "YOUR_PRIVATE_DISCORD_WEBHOOK_URL_HERE"
  mention-role-id: ""
  use-embeds: true

# Settings for the automatic de-op system.
auto-demote:
  enabled: true
  threshold: 3
  cooldown-seconds: 60

# A list of operators to exclude from public alerts and auto-demotion.
# Usernames are not case-sensitive. Their actions are still logged to file.
excluded-ops:
  - ServerOwner
  - Console

# How long a reason set with /reason remains valid, in seconds.
reason-expiry-seconds: 120

# Cooldown for the /reportop command to prevent spam, in seconds.
report-cooldown-seconds: 60

# If true, includes operator coordinates in the local log files.
# Coordinates are never sent to Discord, regardless of this setting.
include-coordinates-in-logs: false
```

#### `watched-commands`
This list defines the scope of H-Watchdog's monitoring.
*   **Best Practice**: Be selective. Only add commands that grant significant advantages or have the potential for abuse. Adding informational commands like `/list` will only create unnecessary noise.

#### `discord`
*   `enabled`: A master switch for all Discord functionality. Set to `false` to run in a logging-only mode.
*   `public-webhook-url`: The webhook for the channel where the community can see operator command usage.
*   `report-webhook-url`: The webhook for a private staff channel where player reports are sent.
*   `mention-role-id`: (Optional) If you want to ping a specific role (e.g., "Staff") on command usage, place the role ID here in the format `<@&ROLE_ID>`.
*   `use-embeds`: Toggles between clean, formatted Discord embeds (`true`) and simple plain-text messages (`false`).

#### `auto-demote`
*   `enabled`: Toggles the entire auto-demotion system.
*   `threshold`: The number of commands that triggers a demotion. For example, if set to `3`, the third command within the cooldown window will trigger the demotion.
*   `cooldown-seconds`: The time window for counting violations. If an operator uses 2 commands and then waits for this duration to pass, their violation count resets to zero.

#### `excluded-ops`
This list provides immunity from public-facing consequences. It is ideal for the server owner or bots that perform automated tasks with OP privileges.

#### Global Settings
*   `reason-expiry-seconds`: Balances convenience for admins with the need for timely justifications.
*   `report-cooldown-seconds`: A simple anti-spam mechanism for the public reporting command.
*   `include-coordinates-in-logs`: A privacy-conscious option for internal use only. Useful for verifying an admin's claim that they were at a specific location to fix an issue.

## Commands and Permissions

| Command | Permission | Description | Default Access |
| :--- | :--- | :--- | :--- |
| `/reason <text...>` | `hwatchdog.reason` | Sets a justification that will be attached to the next monitored command. | Operators |
| `/reportop <player> <reason...>` | `hwatchdog.report` | Sends a confidential report about an operator to the staff Discord. | All Players |

## Building From Source

To compile the plugin from its source code, you will need JDK 17 (or newer), Git, and Apache Maven.

1.  **Clone the repository:**
    ```sh
    git clone https://github.com/your-username/H-Watchdog.git
    cd H-Watchdog
    ```
2.  **Compile with Maven:**
    ```sh
    mvn clean install
    ```
3.  The compiled artifact will be located in the `target/` directory.

## License

This project is licensed under the Apache License, Version 2.0.

Copyright 2025 Timur Hromek

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   https://github.com/TimurHromek/H-Watchdog/blob/main/LICENSE

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUTHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
