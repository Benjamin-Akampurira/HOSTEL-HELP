# Hostel Help

## Student-to-Student Borrowing and Peer-Support Platform

Hostel Help is a web application intended to organize student borrowing and peer support in a university hostel community. Students can request everyday items, offer assistance, and manage borrowing activities. Authorized wardens can access member information and incident reports.

### Group Members

|No.|Group Member|Registration Number|

|No.|Group Member|Registration Number|
|-|-|-|
|1|Nakaayi Shamirah|M25B13/031|
|2|Kasana Brenda Daphine|S25B13/115|
|3|Benjamin Akampurira|M25B13/012|
|4|Aheebwa Abraham|S24B13/042|
|<br />5|Nankwanga Fridah|M25B13/033|
|6|Ayebare Samuel|S25B13/029|



### Technology Stack

* Java 21
* Spring Boot REST API
* Spring Data JPA
* Spring Security
* Flyway
* MySQL
* React
* TypeScript
* Vite
* Maven Wrapper

### Main Functionality

**Student workspace:** registration/authentication, borrowing requests, offers, offer acceptance, borrowing management, receipt/return confirmation, helper ratings, notifications (verify each against source).

**Warden workspace:** authorized member-directory access, member details, incident reports, report details, and report-status management.

### Repository Structure

```text
Hostel/
├── backend/
├── frontend/
├── docs/
│   ├── diagrams/
│   ├── Hostel\\\_Help\\\_Project\\\_Report.md
│
├── .env.example
├── .gitignore
└── README.md
```

Update this structure to match the actual repository.

### Requirements

Install Java 21, MySQL, Node.js/npm, and a browser. The backend includes a Maven Wrapper.

### Database Setup

1. Install and start MySQL.
2. Create the database and application database user according to the backend configuration.
3. Configure the required environment variables locally.
4. Start the backend and confirm Flyway migrations complete.

Known configuration keys from the project notes:
`MYSQL\\\_DB\\\_HOST`, `MYSQL\\\_DB\\\_PORT`, `MYSQL\\\_DB\\\_NAME`, `MYSQL\\\_DB\\\_USERNAME`, `MYSQL\\\_DB\\\_PASSWORD`.

Do not commit `.env` or any secrets.

### Backend

Open PowerShell in the backend directory and run:

```powershell
.\\\\mvnw.cmd spring-boot:run
```

Project notes indicate backend port 8081. Confirm the actual configuration and startup output.

### Frontend

Open another terminal in the frontend directory:

```bash
npm install
npm run dev
```

Open the local URL printed by Vite. The project notes indicate port 5173, but Vite may choose another port if it is occupied.

### Authentication and Roles

Students use the student workspace. Authorized wardens use the warden workspace. Warden account provisioning is handled server-side according to the backend configuration. Do not include passwords in this repository.

### Testing

Backend:

```powershell
.\\\\mvnw.cmd test
```

Frontend: inspect `frontend/package.json` and run its available build/lint scripts. Record real results in `docs/testing.md`.

### Documentation

See `docs/Hostel\\\_Help\\\_Project\\\_Report.md`, UML files in `docs/diagrams/`, `docs/oop-concepts.md`, `docs/testing.md`, `docs/group-contributions.md`, `docs/presentation-outline.md`, and `docs/submission-checklist.md`.

### Documentation Accuracy

This documentation pack is based on project details shared by the team, not a fresh source-code audit. Verify implementation claims, exact class names, routes, schema, and tests before academic submission.

