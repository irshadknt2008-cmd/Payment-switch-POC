# Payment Switch

Enterprise ISO 8583 payment switch built as a Maven multi-module project.

## Overview

This repository contains the core pieces of a switch that:

- accepts acquirer traffic over TCP
- parses and assembles ISO 8583 messages
- routes transactions by BIN
- forwards messages to issuer endpoints
- persists transaction data in PostgreSQL
- runs database migrations on startup

The application entry point is `switch-bootstrap`, which wires the modules together and starts the switch.

## Modules

- `switch-common` - shared constants, models, utilities, and base exceptions
- `switch-iso8583` - ISO 8583 codec, parser, assembler, and frame/message encoders
- `switch-routing` - BIN table lookup and routing logic
- `switch-persistence` - PostgreSQL, HikariCP, MyBatis, and Flyway configuration
- `switch-acquirer-server` - TCP server and handlers for incoming acquirer messages
- `switch-issuer-client` - outbound issuer connection pool and response handling
- `switch-bootstrap` - application startup, configuration loading, and shutdown wiring

## Requirements

- Java 8
- Maven 3.x
- PostgreSQL

## Configuration

Runtime configuration is loaded from `switch-bootstrap/src/main/resources/switch.properties`.

Key settings:

- `acquirer.port` - acquirer TCP listen port, default `9998`
- `DB_HOST` - PostgreSQL host, default `localhost`
- `DB_PORT` - PostgreSQL port, default `5432`
- `DB_NAME` - database name, default `payment_switch`
- `DB_USER` - database user, default `postgres` in the bundled properties file
- `DB_PASSWORD` - database password
- `DB_POOL_SIZE` - HikariCP pool size, default `10`
- `issuers` - comma-separated issuer IDs
- `issuer.<id>.host` - issuer host
- `issuer.<id>.port` - issuer port
- `issuer.<id>.pool` - issuer connection pool size
- `default.issuer` - default issuer ID for network-management messages

Note: the bootstrap module loads `switch.properties` first. `DB_*` environment variables are only used when the corresponding property is not already set in that file.

## Build

From the repository root:

```bash
mvn clean package
```

This builds all modules and produces the runnable bootstrap jar under:

```text
switch-bootstrap/target/payment-switch-jar-with-dependencies.jar
```

## Run

Set the database password and start the bootstrap jar:

```bash
$env:DB_PASSWORD='your_password'
java -jar switch-bootstrap/target/payment-switch-jar-with-dependencies.jar
```

The application:

- runs Flyway migrations from `classpath:db/migration`
- creates the datasource and MyBatis session factory
- loads the BIN routing service
- starts the acquirer TCP server
- connects configured issuer endpoints

## Database

Flyway migrations live in `switch-persistence/src/main/resources/db/migration`.

The repository currently includes:

- `V1__initial_schema.sql`
- `V2__seed_bin_table.sql`

## Project layout

```text
payment-switch/
  pom.xml
  switch-bootstrap/
  switch-acquirer-server/
  switch-issuer-client/
  switch-routing/
  switch-persistence/
  switch-iso8583/
  switch-common/
```

## Notes

- The switch currently defaults to issuer `9009`.
- The acquirer server defaults to port `9998`.
- Logs are written through Logback using the bootstrap module's `src/main/resources/logback.xml`.
