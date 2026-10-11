# CRRE Academic Data & Support Digitalization Platform — Guidelines

## Command Execution & Tool Permissions
- **Docker Commands**: Always allow and execute commands starting with `docker` (e.g. `docker ps`, `docker compose`, etc.) directly without requiring interactive confirmation.

## Environment & Architecture
- **Database**: PostgreSQL 16 Alpine running in Docker on port `5433` (Container: `crre-academic-db`, Database: `crre_academic_dms`).
- **Backend**: Spring Boot 3.x Java 21 (`backend/`), port 8080.
- **Frontend**: React + TypeScript + Vite + TailwindCSS (`frontend/`), port 5173.
