# SmartDine SaaS Platform - Product Requirements Document

## Overview
SmartDine is a multi-tenant SaaS restaurant management platform deployed on Google App Engine.
Live URL: https://smartdine-saas.ew.r.appspot.com

## Core Features

### 1. Multi-Tenant Onboarding (POST /auth/register)
- Register a new restaurant with name, owner, phone, password
- Creates restaurantId (UUID) and syncCode (SD-XXXXXX format)
- Supports tables, areas, menu categories, menu items in payload
- Returns: { success, syncCode, restaurantId, restaurantName, token }

### 2. Authentication (POST /auth/login)
- Login by restaurantName, phone number, or email
- Returns JWT Bearer token, syncCode, restaurantId, environment
- BCrypt password hashing
- Returns 401 on invalid credentials

### 3. Public Licensing Handshake (GET /api/public/provision/activate)
- No auth required
- Query param: ?code=SD-XXXXXX or ?syncCode=SD-XXXXXX
- Returns full RestaurantConfigDTO: tables, menu, areas, waiters, taxes

### 4. Config Sync (POST /api/public/provision/update-config)
- Push menu, tables, areas, waiters from admin panel to cloud
- Requires syncCode in body
- Returns 200 on success

### 5. Outbox Sync Processing (POST /api/sync/process)
- Requires JWT Bearer token
- Processes local outbox events (ORDER, TABLE, MENU)
- Commits to GCP Cloud SQL

## Security
- JWT Bearer token authentication
- BCrypt password hashing  
- Multi-tenant data isolation by restaurantId
- Public endpoints: /auth/**, /api/public/**

## Tech Stack
- Java 21, Spring Boot 3, PostgreSQL, Google App Engine
