# Lead Nurturing System (LNS) - AIESEC Agentic Marketing Flow

An enterprise-grade, agentic AI platform designed for **AIESEC** to automate lead nurturing, smart opportunity matching, marketing campaign management, and performance analytics. 

Built with **Spring Boot 3 / 4**, **Spring AI**, **Model Context Protocol (MCP)**, **Ollama (Llama 3.2)**, **MySQL**, and **GraphQL**.

---

## Architecture Overview

The system follows a microservice architecture communicating via the open **Model Context Protocol (MCP)** over HTTP:

```
                      +---------------------------------------+
                      |               End User                |
                      +---------------------------------------+
                                          |
                                          v  (HTTP / Web Browser)
+-----------------------------------------------------------------------------------+
|  Agent Client (Microservice 1 - Port 8080)                                         |
|                                                                                   |
|   +-----------------------+     +--------------------+     +------------------+   |
|   |   Web Chat Interface  | --> |   ChatController   | --> |   ChatService    |   |
|   |  (AIESEC-themed UI)   |     +--------------------+     +------------------+   |
|   +-----------------------+                                         |             |
|                                                                     v             |
|                                                      +------------------------+   |
|   +-----------------------+                          |   ChatClient (Spring   |   |
|   |  Ollama (llama3.2)    | <----------------------- |   AI + Window Memory)  |   |
|   +-----------------------+                          +------------------------+   |
|                                                                     |             |
|                                                      +------------------------+   |
|                                                      |   Spring AI MCP Client |   |
|                                                      +------------------------+   |
+------------------------------------------------------------------|----------------+
                                                                   | Streamable HTTP (MCP)
                                                                   v
+-----------------------------------------------------------------------------------+
|  MCP Tools Server (Microservice 2 - Port 9090)                                     |
|                                                                                   |
|   +---------------------------------------------------------------------------+   |
|   |                            Spring AI MCP Server                           |   |
|   +---------------------------------------------------------------------------+   |
|          |                  |                  |                  |               |
|          v                  v                  v                  v               |
|   +-------------+    +-------------+    +-------------+    +-------------+        |
|   | Lead Match  |    | Opportunity |    |   Campaign  |    | EXPA People |        |
|   |  Pipeline   |    | Catalog Sync|    |  Management |    |  Analytics  |        |
|   +-------------+    +-------------+    +-------------+    +-------------+        |
|          |                  |                  |                  |               |
|          +------------------+------------------+                  |               |
|                             |                                     |               |
|                             v                                     v               |
|                      +--------------+                      +--------------+       |
|                      | MySQL Server |                      |  EXPA / GIS  |       |
|                      |  (Local DB)  |                      | GraphQL API  |       |
|                      +--------------+                      +--------------+       |
+-----------------------------------------------------------------------------------+
```

---

## Core Features

- **Automated Lead Matching & Recommendation Pipeline**:
  - Search candidates in EXPA by name (`find_student_by_name`).
  - Score and match candidates against available opportunities across **OGV** (Global Volunteer), **OGTe** (Global Teacher), and **OGTa** (Global Talent).
  - Deduplicate recommendations so students receive fresh suggestions on subsequent inquiries.

- **Safe Two-Step Email Dispatch**:
  - Generates email drafts (`draft_suggested_opportunities_email`) for candidate suggestions.
  - Requires explicit user confirmation prior to actual SMTP dispatch (`send_suggested_opportunities_email`).

- **Opportunity Catalog Synchronization**:
  - Live GraphQL synchronization from `gis-api.aiesec.org` into local MySQL storage.
  - Periodic and on-demand syncing with configurable pagination and date filters.

- **Campaign Management & Marketing Analytics**:
  - Full CRUD and search capabilities for marketing campaigns in MySQL (`CampaignTools`).
  - Performance dataset analytics from static/historical CSV files: impressions, clicks, CTR, conversions, and spend (`CampaignDatasetTools`).

- **EXPA Aggregated Registration Statistics**:
  - Real-time querying of yearly registration figures and status distributions from EXPA GraphQL.

- **Conversational Web Interface**:
  - Responsive web chat interface with quick action prompts, branded according to AIESEC design identity.

---

## Modules

| Module | Port | Description |
| :--- | :---: | :--- |
| **`agent-client`** | `8080` | Client application hosting the conversational web UI, Spring AI `ChatClient`, windowed conversation memory, and Ollama integration. Discovers and invokes tools exposed by the MCP server. |
| **`mcp-tools-server`** | `9090` | MCP Server exposing modular `@McpTool` beans for database operations, GraphQL synchronization, candidate matching, dataset analysis, and email notifications. |

---

## Prerequisites

1. **Java JDK 17+**
2. **Maven 3.8+** (or use the provided `./mvnw` wrapper)
3. **MySQL 8.x** running locally or via Docker
4. **Ollama** installed with `llama3.2`:
   ```bash
   ollama pull llama3.2
   ```

---

## Configuration & Environment Variables

Copy `.env.example` or export the required environment variables:

```bash
# EXPA & AIESEC GraphQL API
export EXPA_API_URL=https://gis-api.aiesec.org/graphql
export EXPA_API_TOKEN=your_expa_graphql_jwt_token

export AIESEC_API_URL=https://gis-api.aiesec.org/graphql
export AIESEC_API_TOKEN=your_aiesec_graphql_token

# MySQL Database
export MYSQL_HOST=localhost
export MYSQL_PORT=3306
export MYSQL_DATABASE=aiesec_agentic_marketingflow
export MYSQL_USER=root
export MYSQL_PASSWORD=your_mysql_password

# SMTP Email Configuration (e.g. Gmail App Password)
export MAIL_SMTP_HOST=smtp.gmail.com
export MAIL_SMTP_PORT=587
export MAIL_SMTP_USERNAME=your_email@gmail.com
export MAIL_SMTP_PASSWORD=your_app_password
export MAIL_FROM=your_email@gmail.com

# Ollama LLM
export OLLAMA_BASE_URL=http://localhost:11434
export OLLAMA_MODEL=llama3.2
```

---

## Quick Start

### 1. Build the Entire Project

```bash
./mvnw clean install
```
*(On Windows: `.\mvnw.cmd clean install`)*

### 2. Start the MCP Tools Server (Port 9090)

```bash
cd mcp-tools-server
../mvnw spring-boot:run
```

### 3. Start the Agent Client (Port 8080)

In a separate terminal window:

```bash
cd agent-client
../mvnw spring-boot:run
```

### 4. Access the Web Interface

Open your browser and navigate to:
```
http://localhost:8080
```

---

## Exposed MCP Tools

| Tool Name | Parameters | Description |
| :--- | :--- | :--- |
| `find_student_by_name` | `name` | Look up students in EXPA by full name or partial name. |
| `suggest_opportunities_for_student` | `personId` | Evaluate candidate profile and compute matching scores against opportunities. |
| `draft_suggested_opportunities_email`| `personId` | Generate an email preview containing recommendations. |
| `send_suggested_opportunities_email` | `personId` | Dispatch the approved email via SMTP. |
| `sync_opportunities_now` | - | Trigger on-demand sync of AIESEC opportunities into MySQL. |
| `get_all_campaigns` | - | Retrieve all marketing campaigns from MySQL. |
| `create_campaign` | `name, status, platform, budget` | Create and store a new campaign record. |
| `get_campaign_dataset_summary` | - | Calculate overall metrics (impressions, clicks, CTR, spend). |
| `get_expa_people_by_year` | `year` | Aggregate registration statistics for a specified year. |
| `send_email` | `to, subject, body` | Send a direct custom notification email. |

---

## Tech Stack

- **Backend Framework**: Spring Boot 4.x / Spring AI 2.0.2
- **Protocol**: Anthropic Model Context Protocol (MCP) Streamable HTTP
- **Language Model**: Llama 3.2 via Ollama
- **Database**: MySQL 8.x + Spring Data JPA (Hibernate)
- **External APIs**: AIESEC / EXPA GraphQL APIs
- **Frontend**: Responsive HTML5, CSS3, Vanilla JS
- **Mailing**: Spring Boot Starter Mail (JavaMailSender)

---

## License

This project is developed for internal educational and operational automation within AIESEC.

