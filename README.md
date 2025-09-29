# GameVault Backend

## Overview

GameVault Backend is a Spring Boot application that provides authentication, authorization, and library management services for the GameVault platform. It integrates with PostgreSQL for persistent data storage and Redis for caching/session management. JWT (JSON Web Token) with RSA keys is used for secure authentication.

---

## Tech Stack

![Java](https://img.shields.io/badge/Java-17-007396?logo=openjdk\&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5.6-6DB33F?logo=springboot\&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?logo=postgresql\&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-7-DC382D?logo=redis\&logoColor=white)
![Maven](https://img.shields.io/badge/Maven-3.8+-C71A36?logo=apachemaven\&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-2496ED?logo=docker\&logoColor=white)
![JWT](https://img.shields.io/badge/JWT-Security-000000?logo=jsonwebtokens\&logoColor=white)

---

## Project Structure

```
.
├── docker-compose.yml
├── mvnw / mvnw.cmd
├── pom.xml
├── secrets/keys/                # RSA keys for JWT signing and verification
├── src/
│   ├── main/java/com/sg/nusiss/gamevaultbackend/
│   │   ├── GamevaultbackendApplication.java
│   │   ├── advice/GlobalExceptionHandler.java
│   │   ├── config/auth/...
│   │   ├── controller/...
│   │   ├── dto/...
│   │   ├── entity/...
│   │   ├── repository/...
│   │   ├── security/auth/...
│   │   └── service/...
│   └── resources/application.yml
└── test/java/com/sg/nusiss/gamevaultbackend/...
```

---

## Prerequisites

* Java 17+
* Maven 3.8+
* Docker and Docker Compose

---

## Running with Docker

To start PostgreSQL and Redis services:

```bash
docker-compose up -d
```

This will:

* Start PostgreSQL on port `12000`
* Start Redis on port `12003`

Default PostgreSQL credentials are configured in `docker-compose.yml`:

* Database: `gamevault`
* User: `gamevault_user`
* Password: `gamevault_pass`

---

## Running the Application

### Local Development

Ensure Docker containers are running, then start the application with Maven:

```bash
./mvnw spring-boot:run
```

The backend will run on `http://localhost:8080`.

### Profiles

* `application.yml` is the default configuration.
* You can create `application-local.yml` or `application-docker.yml` for environment-specific overrides.

---

## Security

* JWT authentication is configured with RSA keys.
* Keys are stored under `secrets/keys/`:

    * `rsa-private.pem`: used for signing JWTs
    * `rsa-public.pem`: used for verifying JWTs

In Docker deployments, the secrets directory can be mounted into the backend container.

---

## Testing

To run tests:

```bash
./mvnw test
```

---

## Next Steps

* Add CI/CD pipeline integration
* Implement additional library/order APIs
* Extend unit and integration test coverage
