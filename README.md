# Collaborative Code Editor

## Overview

This is a web-based collaborative code editor that supports real-time editing, version control, and code execution. Users can create projects, edit code together, and manage versions using a simplified VCS system inspired by Git.

The system is built with:
- Spring Boot backend (Java 21)
- Vite + React frontend
- WebSockets for real-time collaboration
- MySQL database (Running on docker container)

## Features

### Collaborative Editing
- Real-time code updates using WebSockets
- Multiple users can edit the same file simultaneously

### Project Management
- Create, delete, and organize projects
- File and folder structure stored as a tree

### Version Control System
- Initialize a `.vcs` directory per project
- Track changes, commits, logs, and revert
- Branching and checkout support
- Fork public or shared projects

### Security
- JWT-based authentication (access + refresh tokens)
- Role-based access control (Admin, Editor, Viewer)

### Code Execution
- Supports Java, C++, and Python
- Execution handled via ProcessBuilder (and Docker in non-container mode)

## Project Structure

- backend/ -> Spring Boot backend
- frontend/ -> Vite frontend
- Dockerfile -> Container configuration
- .gitignore -> Ignored files configuration

## Requirements

- Java 21
- Maven
- Node.js (>= 18)
- MySQL

## How to Run

### 1. Backend

```bash
cd backend
mvn clean install
mvn spring-boot:run
```

Backend runs on:
http://localhost:8080

### 2. Frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend runs on:
http://localhost:5000 or http://localhost:5001

## Notes

- Ensure MySQL is running and configured in application.properties
- Backend must be started before full system functionality works
- WebSocket connections require backend running
