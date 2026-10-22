# Warehouse Management System — Backend

## Project Overview

The Warehouse Management System is a web-based application designed to support warehouse operations and inventory management.

This repository contains the backend application developed using Spring Boot and Java. It provides REST API services and connects to an Oracle database.

## Technologies Used

- Java 21
- Spring Boot 3.5.6
- Maven
- Oracle Database XE 21c
- Oracle JDBC Driver
- REST API

## Database Configuration

The application uses Oracle Database with the following local connection settings:

- Host: `localhost`
- Port: `1521`
- Service name: `XEPDB1`
- Username: `SYSTEM`

Configure your database credentials through environment variables before starting the application.

## Prerequisites

- Java Development Kit (JDK) 21
- Apache Maven
- Oracle Database XE 21c
- Git

## Getting Started

### 1. Clone the Repository

```bash
git clone https://github.com/veeradurgadeva-stack/warehouse-backend.git
cd warehouse-backend
```

### 2. Configure Environment Variables

Open Windows PowerShell in the backend project folder and set the Oracle connection details:

```powershell
$env:ORACLE_URL="jdbc:oracle:thin:@//localhost:1521/XEPDB1"
$env:ORACLE_USERNAME="SYSTEM"
$env:ORACLE_PASSWORD="your_oracle_password"
```

Replace `your_oracle_password` with your own Oracle password. Do not publish actual passwords in this repository.

### 3. Build the Project

```bash
mvn clean package -DskipTests
```

### 4. Run the Backend

```bash
mvn spring-boot:run
```

The backend runs on port `8080` by default.

## API Endpoints

**API Base URL**

`http://localhost:8080/api`

**Health Check**

`http://localhost:8080/api/health`

## Project Structure

```text
warehouse-backend/
├── src/
│   └── main/
│       ├── java/
│       │   └── com/warehouse/management/
│       └── resources/
│           ├── application.properties
│           └── schema-note.sql
├── .gitignore
├── Dockerfile
├── pom.xml
└── README.md
```

## Frontend Repository

https://github.com/veeradurgadeva-stack/warehouse-frontend

## Backend Repository

https://github.com/veeradurgadeva-stack/warehouse-backend

## Project Objective

To provide backend services and database connectivity for a web-based Warehouse Management System.
