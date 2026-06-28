# NPCI Switch Service

A Java & Spring Boot microservice simulating the central NPCI (National Payments Corporation of India) switch. It acts as the routing orchestrator, resolves Virtual Payment Addresses (VPAs), coordinates distributed transactions between payer and payee banks, and manages transaction idempotency and state changes.

---

## 🛠️ Tech Stack & Ports
* **Language:** Java 21+
* **Framework:** Spring Boot 3.x (with WebClient for asynchronous HTTP calls)
* **Database:** PostgreSQL (with Spring Data JPA)
* **Database Migration:** Flyway
* **Default Port:** `3002`

---

## 📝 Detailed Step-by-Step Implementation Plan

We will build and integrate this service in the following order:

### Phase 1: Service Setup and Database Integration
* Goal: Initialize the project folder, set up build configurations, and define connection profiles.
1. **Initialize Directory Structure:** Create the `npci-service` root containing the Gradle build files (`build.gradle.kts`, `settings.gradle.kts`) and the standard Maven directory structure (`src/main/java`, `src/main/resources`).
2. **Configure Dependencies:** Update Gradle with:
   - Spring Web (REST API support)
   - Spring Data JPA (Database access)
   - WebFlux/WebClient (Non-blocking HTTP client to communicate with the Bank Service)
   - PostgreSQL Driver
   - Flyway (Database migrations)
   - Lombok & Validation starter
3. **Database Configuration (`application.yaml`):** Configure the service to run on port `3002` and hook it up to a new PostgreSQL database `npci_db`. Define a shared HMAC secret key for secure message signing.

---

### Phase 2: Schema Migrations & JPA Entities
* Goal: Model the persistence layer for transaction histories and VPA resolution.
1. **Flyway Migration `V1__create_vpa_registry.sql`:**
   - Map VPAs to bank endpoints (e.g., `saad@okaxis` resolves to Axis Bank's server URL).
   - Fields: `vpa` (PK), `bank_code`, `bank_api_url`, `account_number`.
2. **Flyway Migration `V2__create_transactions.sql`:**
   - Log transaction records and states.
   - Fields: `transaction_id` (PK, UUID), `payer_vpa`, `payee_vpa`, `amount_paise`, `status`, `payer_rrn`, `payee_rrn`, `failure_reason`, `version` (optimistic locking), `created_at`, `updated_at`.
3. **JPA Entity Models:**
   - `VpaRegistry.java`
   - `Transaction.java` (using `@Version` for stale-callback safety / concurrency management)
   - `TransactionStatus` (Enum: `INITIATED`, `DEBIT_SUCCESS`, `SUCCESS`, `FAILED`, `REVERSED`, `CREDIT_PENDING`)

---

### Phase 3: VPA Registry APIs & Lookup Engine
* Goal: Set up registration endpoints and internal routing resolver.
1. **POST `/switch/vpa/register`:** API endpoint for banks or users to register VPAs. Saves routing data to `vpa_registry`.
2. **VPA Resolution Service:** Internal component that takes a VPA, checks the database (or cache), and returns the API URL of the bank hosting the account.

---

### Phase 4: Core Payment Orchestration (`POST /switch/initiate`)
* Goal: Build the central endpoint coordinating the payment flow.
1. **Idempotency Check:** Scan database for incoming `transaction_id`. If it already exists, return the recorded state to prevent duplicate operations.
2. **State Transition to `INITIATED`:** Write the transaction record with `INITIATED` status.
3. **Step A — Call Payer Bank `/bank/debit`:**
   - Perform HMAC signature signing on the request payload.
   - Dispatch POST request to Payer Bank.
   - On response `SUCCESS`: Update Switch transaction to `DEBIT_SUCCESS` and record payer RRN.
   - On response `FAILURE` (e.g., `INVALID_PIN`, `INSUFFICIENT_BALANCE`): Update Switch transaction to `FAILED`, record reason, and return response immediately.
4. **Step B — Call Payee Bank `/bank/credit`:**
   - Dispatch credit request to Payee Bank.
   - On response `SUCCESS`: Update Switch transaction to `SUCCESS`, record payee RRN.
   - On timeout/failure: Trigger Phase 5 recovery immediately.

---

### Phase 5: Reversal Processing & Error Handling
* Goal: Implement safety checks to make sure money is never lost when networks disconnect.
1. **Immediate Auto-Reversal:** If the Payee Bank credit request fails or times out (e.g., 5 seconds timeout limit), call the Payer Bank's `/bank/reversal` endpoint to refund the customer.
2. **Update State:** Update the Switch transaction status to `REVERSED`.
3. **Error Handler:** Map remote exceptions to proper client-facing error statuses.

---

### Phase 6: Multi-Service Environment (Docker Compose)
* Goal: Run the Bank and NPCI Switch services in parallel.
1. **Update `docker-compose.yml`:** 
   - Configure a single compose file spin-up for both services.
   - Spin up two independent PostgreSQL databases: `bank_db` and `npci_db`.
   - Boot `bank-service` on port `3003` and `npci-service` on port `3002`.
2. **Execute Tests:** Validate the entire happy-path flow using Postman/curl.
