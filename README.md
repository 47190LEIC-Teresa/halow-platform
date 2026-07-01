# HaLow Simulation Platform

## Overview

HaLow Simulation Platform is a web-based system that wraps the HaLow Simulator (IEEE 802.11ah) with a backend, frontend, and PostgreSQL database. It allows users to configure, submit, and track simulations through a web interface instead of running the simulator directly from the terminal.

The backend exposes a REST API, manages simulation jobs, integrates with the external Python simulator and SimParser, and stores configurations, executions, metrics, and files in PostgreSQL. The platform can be run locally or through Docker Compose, depending on the target environment.

For a more detailed description of the architecture and design, see  
[docs/overview.md](docs/overview.md).

---

## Technology Stack

| Layer | Technology |
|------|------------|
| Backend | Kotlin, Spring Boot |
| Persistence | Spring Data JPA (Hibernate) |
| Database | PostgreSQL |
| Simulator | Python HaLow Simulator, SimPy, SimParser |
| Frontend | React, JavaScript, JSX, Vite |
| Deployment | Docker, Docker Compose, Nginx |

---

## Project Structure

```text
├── docs/
│   └── overview.md             # Backend architecture overview
├── frontend/
│   ├── src/
│   │   ├── context/            # React context (auth, simulations, etc.)
│   │   ├── features/           # Feature-specific components
│   │   ├── pages/              # Route-level pages (login, dashboard, simulations, details)
│   │   ├── styles/             # CSS / styling
│   │   ├── App.jsx             # Root React component
│   │   └── main.jsx            # Vite entry point
│   ├── index.html              # Main HTML template used by Vite
│   ├── package.json            # Frontend dependencies and scripts
│   ├── package-lock.json       # Lockfile
│   └── vite.config.js          # Vite config (React plugin, dev server, API proxy)
├── src/
│   ├── main/
│   │   ├── kotlin/backend/
│   │   │   ├── config/         # Job execution and scheduler configuration
│   │   │   ├── controller/     # REST controllers
│   │   │   ├── exception/      # Application exceptions, error codes and global handler
│   │   │   ├── model/          # Entities, DTOs, enums
│   │   │   ├── repository/     # Spring Data JPA repositories
│   │   │   ├── security/
│   │   │   │   ├── authorization/  # Authorization logic
│   │   │   │   ├── config/         # Spring Security configuration
│   │   │   │   └── jwt/            # JWT utilities and filters
│   │   │   ├── service/        # Business services and job orchestration
│   │   │   ├── simulator/      # Integration with Python simulator and parser
│   │   │   └── BackendApplication.kt
│   │   └── resources/
│   │       └── application.yml
│   └── test/
│       ├── kotlin/backend/
│       │   ├── controller/     # Controller tests and HTTP request samples
│       │   ├── repository/     # Repository tests
│       │   └── service/        # Service and worker tests
│       └── resources/          # application-test.yml
├── docker-compose.yml
├── Dockerfile                  # Backend Dockerfile
├── frontend/Dockerfile         # Frontend Dockerfile (build + Nginx)
├── README.md
└── settings.gradle.kts / gradlew*  # Gradle wrapper
```

---

## Running with Docker Compose

The recommended way to run the platform is with Docker Compose. The stack includes three services:

- `db`: PostgreSQL
- `backend`: Spring Boot application
- `frontend`: React application built with Vite and exposed through Nginx

### Prerequisites

Before starting the stack, make sure the following are available:

- Docker and Docker Compose
- The HaLow Simulator and SimParser on the host machine for real simulation runs
- A `.env` file in the project root with the required environment variables

### Required Environment Variables

The backend reads configuration through environment variables referenced from `application.yml` and passed through `docker-compose.yml`.

Main variables include:

- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `MAIL_USERNAME`
- `MAIL_PASSWORD`
- `JWT_SECRET`
- `SIMULATOR_PYTHON_PATH`
- `SIMULATOR_SCRIPT_PATH`
- `SIMULATOR_PARSER_PATH`
- `SIMULATOR_HOST_DIR`
- `SERVER_PORT`
- `SPRING_PROFILES_ACTIVE`

The `SIMULATOR_HOST_DIR` variable is used as a bind mount so the simulator files remain outside the container and can be configured per machine.

### Start the Stack

From the project root:

```bash
docker compose up --build
```

This command:

- starts PostgreSQL
- builds and starts the backend
- builds the frontend and serves it through Nginx

Default endpoints:

- Backend API: `http://localhost:8080`
- Frontend: `http://localhost`

### Notes

The database container includes a health check, and the backend waits until PostgreSQL is healthy before starting.

The simulator and parser are not fully containerized in the current setup. Instead, they are mounted from the host machine through `SIMULATOR_HOST_DIR`.

---

## Running Locally Without Docker

The platform can also be run directly without Docker.

### Backend

#### Prerequisites

- Java 17 or 18
- PostgreSQL running locally
- Python environment with the HaLow Simulator and SimParser for real runs

#### Configuration

Edit `src/main/resources/application.yml` or provide the required environment variables.

Example configuration:

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
  secret: ${JWT_SECRET}
  expiration: 86400000

simulator:
  python-path: ${SIMULATOR_PYTHON_PATH}
  script-path: ${SIMULATOR_SCRIPT_PATH}
  parser-path: ${SIMULATOR_PARSER_PATH}
```

#### Run the Backend

From the project root:

```bash
./gradlew bootRun
```

Default API URL:

```text
http://localhost:8080
```

Run backend tests with:

```bash
./gradlew test
```

### Frontend

From the `frontend` folder:

```bash
cd frontend
npm install
npm run dev
```

Default frontend URL:

```text
http://localhost:3000
```

The frontend should be configured to call the backend at `http://localhost:8080` or the appropriate deployment URL.

---

## Testing

The test suite includes multiple layers of automated validation.

### Controller Tests

Controller tests cover the main API entry points, including authentication, users, and simulations.

Examples:

- `AuthControllerTest`
- `SimulationControllerTest`
- `UserControllerTest`

The test tree also includes `.http` request files for manual request inspection and API experimentation.

### Repository Tests

Repository tests validate persistence logic and entity-database mappings.

Examples:

- `UserRepositoryTest`
- `SimulationRepositoryTest`
- `SimulationJobRepositoryTest`
- `SimulationFileRepositoryTest`
- `SimulationMetricsRepositoryTest`
- `SimulationConfigRepositoryTest`
- `JobSchedulerStateRepositoryTest`

### Service Tests

Service tests cover simulation workflows, file handling, scheduling-related logic, reminders, expiry handling, and metrics operations.

Examples:

- `BaseSimulationServiceJpaTest`
- `LogReminderServiceTest`
- `SimulationBatchExperimentTest`
- `SimulationFileExpiryTest`
- `SimulationFileServiceTest`
- `SimulationJobServiceTest`
- `SimulationMetricsServiceTest`

Service-related worker tests are organized under the `service/worker` area in the test tree.

### Manual Testing

In addition to automated tests, the platform has been validated through manual API testing and browser-based testing of the main user flows, including:

- authentication
- profile updates
- single and batch simulation creation
- simulation detail inspection
- file downloads
- reruns
- metrics calculation from logs

---

## Additional Notes

This project depends on external simulator tooling for full execution flows. The web platform, backend, and database can be started independently, but real simulation runs require the HaLow Simulator and SimParser to be available and correctly configured in the target environment.

For Docker-based execution, secrets and machine-specific paths should be kept outside version control. A local `.env` file should be used for real credentials and host-specific simulator paths.