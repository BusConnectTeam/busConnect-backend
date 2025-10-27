🚌 BusConnect

BusConnect is a modular platform for managing and searching bus routes, designed to deliver a simple, secure, and modern experience for both end users and transportation companies.
The system follows a microservices architecture, focusing on scalability, security, and centralized monitoring.

⚠️ Note: BusConnect is a private project, not an open-source initiative.
All rights reserved to the development team.

📘 Table of Contents

Overview

Architecture

Microservices

Authentication and Security

Infrastructure and Tools

User Experience

Development Standards

To Be Added

🧩 Overview

BusConnect provides an intelligent way to search and manage bus routes by date, destination, passenger count, and bus type.
It is built on a microservices ecosystem that enables flexibility and modular scalability for future expansion.

🏗️ Architecture

The project is based on a central repository managing multiple independent microservices, each handling a specific responsibility.
Communication between services is managed through a Service Registry using Eureka Server.

Key principles:

Scalable microservices architecture

Dockerized environment for consistent deployment

Optional orchestration via Kubernetes (under evaluation)

Secure inter-service communication

⚙️ Microservices
Service	Description
User Service	Manages user accounts (registration, updates, deletion).
Routes Service	Handles bus routes, trips, and external API integrations.
Auth Service	Manages authentication and communication between services.
Logging / Eureka Service	Provides logging, monitoring, and service discovery.
Search Service (Landing Page)	Allows users to search by date, route, passengers, and bus type.
🔐 Authentication and Security

A secure authentication system will be implemented (evaluating Magical Link as an option).

Three main roles:

Administrator

User (Customer)

Company

Services and endpoints will be protected using secure session tokens.

The Auth Service will coordinate authentication and authorization across all services.

🛠️ Infrastructure and Tools

Containers: Docker & Docker Compose (used from the start)

Service Registry: Eureka Server

Orchestration (under evaluation): Kubernetes

API Testing: Postman or Insomnia

CI/CD Pipeline: To be implemented (GitHub Actions or Jenkins)

Monitoring: Logging and metrics via the Eureka/Logging Service

🎨 User Experience

The landing page allows users to:

Select travel date

Choose route

Specify number of passengers

Pick bus type (simple / medium / lux)

The design will be clean, accessible, and visually appealing, aimed at users aged 18–70.
The color palette and visual identity will be defined during the upcoming design iterations.

💻 Development Standards

Each microservice is developed independently and documented thoroughly.

Issues format:

[ServiceName #1] Short description


Repository conventions:

Code: English naming conventions

Comments & JavaDoc: Spanish

Each developer works in an individual branch, with optional feature sub-branches.

All bugs and fixes must be properly documented.

⏳ To Be Added

 Add a “How to run locally” section (e.g., Docker commands, environment setup)

 Include badges (build status, Docker, version) for GitHub polish
