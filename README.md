# Hospital Appointment Booking System

A web application where **patients** search doctors by specialty and book, view and cancel appointments,
**doctors** see their schedule and mark visits as completed, and **admins** manage doctors and their time slots.
Built as a college project with Java 17, Spring Boot 3, Thymeleaf + Bootstrap 5, MySQL, Spring Security and Spring Mail.

## Features
| Role | What they can do |
|------|------------------|
| Patient | Register, log in, search doctors by specialty, see free slots, book, view own appointments, cancel |
| Doctor | Log in, see today's and upcoming appointments, mark visits as completed |
| Admin | Add / edit / delete doctors (creates their login), add and delete time slots |

Business rules: a slot can be booked only once (database unique constraint), no booking in the past,
confirmation e-mail on booking and on cancellation, passwords stored only as BCrypt hashes.

## Tech stack
Java 17, Spring Boot 3, Maven, Thymeleaf + Bootstrap 5, MySQL + Spring Data JPA, Spring Security (form login, roles, CSRF), Spring Mail.

## Project structure
```
src/main/java/com/hospital/booking
  controller/  web layer: receives requests, returns pages (Auth, Admin*, Patient, Doctor, Home, Dashboard)
  service/     business rules: UserService, DoctorService, TimeSlotService, AppointmentService,
               DoctorScheduleService, EmailService, CustomUserDetailsService
  repository/  database access (Spring Data JPA interfaces)
  entity/      database tables: User, Doctor, TimeSlot, Appointment (+ Role, AppointmentStatus)
  dto/         form objects with @Valid rules, and read-only view objects
  event/       AppointmentEvent + listener that sends e-mail after the transaction commits
  exception/   BusinessRuleException, ResourceNotFoundException, EmailAlreadyExistsException
  config/      SecurityConfig, AsyncConfig, DataInitializer (first admin), DemoDataSeeder (sample data)
src/main/resources
  templates/   Thymeleaf pages (admin/, patient/, doctor/, fragments/, error/)
  application.properties
src/test/java  unit tests (no database needed)
```

## Setup
1. Install **JDK 17**, **Maven** and **MySQL**.
2. Create the database: `CREATE DATABASE hospital_db;`
3. Give the app your credentials. Pick one way:
   - edit the placeholders in `src/main/resources/application.properties`, or
   - set environment variables: `DB_USERNAME`, `DB_PASSWORD`, `MAIL_USERNAME`, `MAIL_PASSWORD`, or
   - create a git-ignored file `application-local.properties` in the project root containing those properties.
4. Run: `mvn spring-boot:run`, then open http://localhost:8080
   (tables are created automatically on first start).

Never commit real passwords. The `.gitignore` already excludes `application-local.properties` and `.env`.

## Demo accounts
On the first start with an empty database the app creates:

| Role | Email | Password |
|------|-------|----------|
| Admin | `admin@hospital.com` | `ChangeMe@123` |
| Doctors | `dr.carter@hospital.com`, `dr.mehta@hospital.com`, `dr.khan@hospital.com`, `dr.mathew@hospital.com` | `Demo@1234` |
| Patient | `patient@demo.com` | `Demo@1234` |

- Sample doctors come with slots (09:00 to 17:00, every 30 min) for today and the next 5 days.
- Change the admin login with `ADMIN_EMAIL` / `ADMIN_PASSWORD`, and the demo password with `SEED_PASSWORD`.
- Set `SEED_DEMO_DATA=false` to skip the sample doctors and patient.
- Real patients can also self-register at `/register`.

## E-mail
A confirmation e-mail is sent to the patient when an appointment is booked, and another when it is cancelled.
- Sending happens in the background and only after the booking is saved; a mail failure is logged and never undoes a booking.
- Gmail: turn on 2-step verification, create an **App Password**, then set `MAIL_USERNAME` and `MAIL_PASSWORD`.
- No SMTP account? Set `MAIL_ENABLED=false`; e-mails are then only printed in the console log.

## Running the tests
`mvn test` runs the unit tests. They use mocks, so **no database or mail server is needed**.
They cover: no past bookings, double-booking, the unique-constraint error path, cancel ownership,
completing appointments, slot generation, and form validation.

## Manual test checklist
Do these in order after `mvn spring-boot:run` (use two browsers or a private window for step 8).

1. **Register:** open `/register`, submit empty fields (errors show), a weak password (rejected), then a valid patient.
2. **Duplicate email:** register the same email again; you get "already registered".
3. **Login / roles:** log in as the patient, then try opening `/admin/doctors` (expect "Access denied").
4. **Admin:** log in as admin, add a doctor, edit them, add slots for tomorrow, check the table shows "Available".
5. **Search:** as the patient, filter doctors by a specialty and open one.
6. **Book:** book a slot; you land on "My appointments" with status Booked, and an e-mail arrives (or a log line if `MAIL_ENABLED=false`).
7. **Admin sees it:** the slot now shows "Booked" and cannot be deleted; deleting that doctor is refused.
8. **Double booking:** open the same doctor page in two sessions (two patients), click the same slot in both; the second gets "just booked by someone else".
9. **Cancel:** cancel the appointment; status becomes Cancelled, a cancellation e-mail is sent, and the slot is bookable again.
10. **Doctor:** log in as that doctor; the appointment appears under "Upcoming" (or "Today" for a today slot).
    "Mark completed" appears once the start time has arrived; use it and check the patient now sees Completed.
11. **Logout** and confirm protected pages redirect to the login page.

## Viva cheat sheet
- **Layered architecture:** controller (web) -> service (rules) -> repository (database) -> entity (tables); DTOs carry form data.
- **Security:** BCrypt hashing, form login with email as username, role-based URL rules, CSRF tokens on every form.
- **No double booking:** the `appointments` table has a unique constraint on (doctor, date, time, slot_active).
  `slot_active` is TRUE while the booking holds the slot and NULL after cancellation (NULLs never clash in MySQL),
  so cancelled slots can be rebooked but two active bookings are impossible, even under simultaneous clicks.
- **Server-side checks:** past dates, slot ownership, and "cancel only your own appointment" are enforced in services, not just in the UI.
- **E-mail:** the service publishes an event; a `@TransactionalEventListener(AFTER_COMMIT)` sends the mail asynchronously,
  so a rolled-back booking never sends a confirmation and a slow mail server never blocks the user.
- **Validation:** Jakarta Bean Validation annotations on DTOs plus `@Valid` in controllers.
- **Post-Redirect-Get:** successful forms redirect, so refreshing the page does not resubmit them.

## Troubleshooting
- *Access denied for user* / *Communications link failure*: check MySQL is running and `DB_USERNAME` / `DB_PASSWORD` are right.
- *Unknown database 'hospital_db'*: run the `CREATE DATABASE` command in setup step 2.
- Port 8080 busy: add `server.port=8081` to `application-local.properties`.
- No e-mails: look for "Could not send e-mail" in the console; check the App Password, or use `MAIL_ENABLED=false`.

## Build phases
- [x] Phase 1: project setup, entities, repositories
- [x] Phase 2: security, registration, login
- [x] Phase 3: admin (doctors and time slots)
- [x] Phase 4: patient booking and cancelling
- [x] Phase 5: confirmation e-mails
- [x] Phase 6: doctor dashboard
- [x] Phase 7: sample data, tests, README
