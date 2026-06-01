# HaLow Simulation Platform

## Overview

HaLow Simulation Platform is a web-based system that wraps the HaLow Simulator (IEEE 802.11ah) with a backend and database, allowing users to configure, submit, and track simulations through a structured interface instead of manual terminal commands.

The backend exposes a REST API, manages simulation jobs, integrates with the external Python simulator, and stores configurations, executions, metrics, and files in PostgreSQL.

---

## Project Goal

Transform the standalone HaLow Simulator into a multi-user platform that:

- enables controlled, asynchronous simulation execution
- persists configurations, executions, metrics, and files
- supports multiple users with authentication
- exposes a REST API consumed by a React frontend
- prepares for future deployment and scaling

---

## Implementation Summary

| Area                 | Description |
|----------------------|------------|
| Database Modeling    | Relational schema separating configuration, simulations, jobs, metrics, and files |
| Persistence Layer    | Spring Data JPA repositories over PostgreSQL |
| Service Layer        | Coordinates simulation submission, batch runs, reruns, and access control |
| Job Worker           | Background component that runs pending jobs via the simulator |
| File Handling        | `SimulationFileService` manages storage and retrieval of log and output files |
| Metrics              | Metrics service integrates with SimParser when requested |
| Testing              | Repository tests plus service/worker tests for core workflows |

---

## Core Components

### Database Modeling

The schema is designed for reuse and traceability. Main entities:

- **User** – application users, owning simulations and files
- **SimulationConfig** – reusable simulator parameter sets (n, g, dimensions, timing, rate)
- **Simulation** – logical simulation definition (owner, seed, flags, parent id, status, log status)
- **SimulationJob** – individual executions of a simulation, with lifecycle timestamps and status
- **SimulationMetrics** – parsed metrics for a given job (when SimParser is run)
- **SimulationFile** – binary data and metadata for logs and derived files (log, mp, pp, pe, groups, etc.)

Key decisions:

- Separate configuration from simulation and job so the same setup can be reused.
- Attach metrics and files to jobs, not just simulations, to support multiple executions.
- Store only metadata in the database and keep the simulator binary external.

---

### Persistence and Service Layer

The backend is built with Kotlin and Spring Boot, using Spring Data JPA for persistence.

- JPA repositories exist for all entities (user, config, simulation, job, metrics, file).
- Services encapsulate application logic:

    - **SimulationService**
        - submits single simulations and batches
        - reuses or creates `SimulationConfig` based on parameters (`findMatchingConfig`)
        - creates `Simulation` records and enqueues `SimulationJob`s
        - enforces ownership for reruns and lookups

    - **SimulationJobService**
        - creates jobs for new simulations

    - **SimulationFileService**
        - stores log and auxiliary files (`FileType.LOG`, `MP`, `PP`, `PE`, `G`)
        - handles temporary extraction of gzip-compressed logs and group files
        - marks files as downloaded and clears persisted log data when needed

Business rules such as access control, batch parameter validation, and data retention are implemented at the service level.

---

### Simulator Integration and Job Worker

The Python-based HaLow Simulator and SimParser are treated as external processes.

- An `ISimulationRunner` interface abstracts process execution, receiving `SimulatorParams` and optional grouping file paths and returning a `SimulationRunResult`.
- A background **job worker** periodically polls `SimulationJob`s with pending status, runs the simulator, and updates:
    - job status (pending → running → completed/failed)
    - simulation status and log status
    - timestamps (started/finished)
- When configured, the worker triggers SimParser through the metrics service to populate `SimulationMetrics` and stores resulting files via `SimulationFileService`.

This design keeps simulator execution isolated from HTTP request handling and allows multiple jobs to run concurrently without blocking the API.

---

### REST API and Frontend Integration

Although this document focuses on the backend, the system is designed as a REST API consumed by a React SPA frontend.

The API covers:

- authentication (register/login)
- simulation submission (single and batch)
- simulation listing, details, and history
- rerun of previous simulations
- file download and CSV export of configurations and metrics

The frontend uses these endpoints to provide forms for configuration, status dashboards, and access to logs and metrics.

---

### Testing

Testing focuses on the backend:

- **Repository tests** – verify mappings between entities and the PostgreSQL schema.
- **Service tests** – cover configuration reuse/creation, simulation submission, reruns, and batch behavior.
- **Worker-related tests** – exercise job creation and basic worker behavior using mocks for the runner and metrics.

Manual API tests with Postman and exploratory browser tests cover main user flows end‑to‑end in the prototype.

---

## Current Limitations

The current prototype focuses on backend functionality and basic web integration. Open points include:

- limited test coverage for concurrent load and full end‑to‑end flows
- execution still bound to a local simulator binary
- no deployment pipeline or production environment configuration
- some operational features (detailed failure reporting, user notifications, richer metrics views) planned but not yet implemented

---

## Technology Stack

| Layer       | Technology |
|------------|------------|
| Backend    | Kotlin, Spring Boot |
| Persistence| Spring Data JPA (Hibernate) |
| Database   | PostgreSQL |
| Simulator  | Python HaLow Simulator + SimPy, SimParser |
| Frontend   | React, JavaScript, JSX |

---

## Architecture Overview

The architecture follows a layered design:

- **Frontend (React)** – SPA that allows authenticated users to submit simulations, inspect history, run SimParser, and download results.
- **Backend (Spring Boot)** – REST API, services, job worker, simulator integration.
- **Database (PostgreSQL)** – persistent storage for users, configurations, simulations, jobs, metrics, and files.
- **Simulator / SimParser (Python)** – external binaries invoked by the backend to run simulations and compute metrics.

Typical flow for a single simulation:

1. Frontend sends a request to the backend API.
2. `SimulationService` validates the request, resolves or creates `SimulationConfig`, creates `Simulation`, and enqueues a `SimulationJob`.
3. The job worker picks up the job, invokes `ISimulationRunner`, and registers generated files and metrics.
4. The frontend polls or refreshes to see updated status, logs, and metrics.

---

## Future Work

Planned improvements include:

- richer failure reporting and execution feedback in the API and UI
- log file availability windows and notifications when simulations complete
- more robust concurrency handling and higher throughput job processing
- extended test suite (controllers, frontend flows, load/concurrency tests)
- deployment setup and containerization for shared environments

---

## Summary

The HaLow Simulation Platform backend provides the core infrastructure to manage HaLow simulations in a structured, multi-user environment:

- a reusable relational data model
- Spring Boot services and repositories for simulations, jobs, metrics, and files
- a job worker that integrates with the Python simulator and SimParser
- a REST API ready to be consumed by a web frontend

It is intended as a foundation for a full web-based research tool, making simulations easier to configure, run, share, and analyze.