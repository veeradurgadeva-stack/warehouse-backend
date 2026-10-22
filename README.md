# Warehouse Management Backend

Spring Boot 3.5 + Java 21 + Oracle XE 21c REST API.

## 1. Database
Use the existing WarehouseDB / XEPDB1 schema. This project does NOT recreate your existing tables.

Expected tables:
USERS, SUPPLIERS, PRODUCTS, INVENTORY, WAREHOUSES, WAREHOUSE_LOCATIONS, STOCK_RECEIVING, ORDERS, ORDER_ITEMS.

Set the Oracle password before starting:

Windows PowerShell:
```powershell
$env:ORACLE_PASSWORD="your_oracle_password"
```

Or edit `src/main/resources/application.properties`.

## 2. Run
```bash
mvn spring-boot:run
```

API base: `http://localhost:8080/api`
Health: `http://localhost:8080/api/health`

## 3. Frontend
Set Vite environment variable:
```text
VITE_API_BASE_URL=http://localhost:8080/api
```

The frontend `src/utils/api.js` already follows the REST paths implemented here.

## 4. Important
The API uses the existing Oracle schema. Do not run CREATE TABLE statements from scratch if your tables already exist.
