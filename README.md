# HaLow Simulation Platform

## Overview

HaLow Simulation Platform is a web-based system that wraps the HaLow Simulator (IEEE 802.11ah) with a backend and database. It allows users to configure, submit, and track simulations through a web interface instead of running the simulator directly from the terminal.

The backend exposes a REST API, manages simulation jobs, integrates with the external Python simulator and SimParser, and stores configurations, executions, metrics, and files in PostgreSQL.

For a detailed description of the architecture and design, see  
[docs/backend-overview.md](docs/overview.md).

---

## Technology Stack

| Layer       | Technology                |
|------------|---------------------------|
| Backend    | Kotlin, Spring Boot       |
| Persistence| Spring Data JPA (Hibernate) |
| Database   | PostgreSQL                |
| Simulator  | Python HaLow Simulator + SimPy, SimParser |
| Frontend   | React, JavaScript, JSX    |

---

## Project Structure

```text
├── docs/
│   └── overview.md             # Detailed backend architecture (optional)
├── frontend/
│   ├── src/
│   │   ├── context/       # React context (auth, simulation state, etc.)
│   │   ├── features/      # Feature-specific components
│   │   ├── pages/         # Route-level pages (login, dashboard, simulations, details)
│   │   ├── styles/        # CSS / styling
│   │   ├── App.jsx        # Root React component
│   │   └── main.jsx       # Vite entry point
│   ├── index.html         # Main HTML template used by Vite
│   ├── package.json       # Frontend dependencies and scripts
│   ├── package-lock.json  # Lockfile
│   └── vite.config.js     # Vite config (React plugin, dev server, API proxy)
├── src/
│   ├── main/
│   │   ├── kotlin/backend/
│   │   │   ├── common/         # Shared utilities, exceptions, helpers
│   │   │   ├── controller/     # REST controllers (auth, users, simulations, files, metrics)
│   │   │   ├── model/          # Entities, DTOs, enums
│   │   │   ├── repository/     # Spring Data JPA repositories
│   │   │   ├── security/       # Security config, filters, JWT helpers
│   │   │   ├── service/        # Services, job worker, file/metrics services
│   │   │   ├── simulator/      # ISimulationRunner and integration with Python simulator
│   │   │   └── BackendApplication.kt  # Spring Boot entry point
│   │   └── resources/
│   │       └── application.yml       # Main backend configuration
│   └── test/
│       ├── kotlin/backend/
│       │   ├── repositories/         # Repository tests
│       │   └── service/              # Service + worker tests
│       └── resources/                # application-test.yml
│
├── README.md
└── settings.gradle.kts / gradlew*  # Gradle wrapper
```
---

## Running the Backend

### Prerequisites

- Java 17 or 18
- PostgreSQL running locally
- Python environment with the HaLow Simulator and SimParser (for real runs)

> **Important:**  
> The application has not been deployed and relies on a locally installed HaLow Simulator and SimParser.  
> Without the simulator available locally, only the API and database layers can be started; simulation runs and end‑to‑end testing will fail or be limited.
> 
### Configure

Edit [src/main/resources/application.yml](src/main/resources/application.yml) (and `application-test.yml`) with your DB settings, for example:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/postgres
    username: <your-username>
    password: <your-password>

  jpa:
    hibernate:
      ddl-auto: create
    show-sql: true
    properties:
      hibernate:
        format_sql: true

  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 10MB

jwt:
  secret: <your-jwt-secret>       # e.g. set via environment variable
  expiration: 86400000            # 24 hours in milliseconds

simulator:
  python-path: /path/to/python3   # local Python from HaLow venv
  script-path: /path/to/halowSimulator.py
```

### Run

From the `backend` folder:

```bash
./gradlew bootRun
```

API default: `http://localhost:8080`

Run tests:

```bash
./gradlew test
```

---

## Running the Frontend

From the `frontend` folder:

```bash
cd frontend
npm run dev
```

By default: `http://localhost:3000` (configure it to call the backend at `http://localhost:8080` or your chosen URL).

---

## Testing

The test suite currently includes:

- Repository tests for entity–database mappings
- Service tests for simulation submission, config reuse, reruns, and batch behavior
- Worker tests for job creation and basic simulator integration (using mocks)

Manual API tests (Postman) and browser-based checks cover the main user flows end‑to‑end.

---


