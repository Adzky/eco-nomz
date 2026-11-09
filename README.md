# Eco-Nomz

Eco-Nomz is a local marketplace for surplus food. Restaurants list unsold food at a discount, customers reserve and pay for it, and they pick it up with a pickup code.

- **Customers** browse local offers, reserve food, check out through Midtrans (Sandbox), and show a pickup code.
- **Restaurant owners** manage their restaurants and offers, upload logos and photos, and verify pickups.
- **Administrators** review platform activity and moderate accounts and listings.

Prices are in Indonesian rupiah (IDR), and pickup times use the Asia/Jakarta time zone.

## Tech stack

- Java 25, Spring Boot 4 (Spring MVC, Thymeleaf, Spring Security, Spring Data JPA)
- PostgreSQL with Flyway migrations
- JWT authentication in an HttpOnly cookie, plus CSRF protection
- React 19 + TypeScript + Vite for interactive pieces (for example the logo cropper), built into `src/main/resources/static` and served by Spring
- Midtrans Sandbox for payments, Cloudinary for image storage

## Getting started

### Prerequisites

- JDK 25
- PostgreSQL running locally
- Node.js is optional: Maven downloads its own copy through `frontend-maven-plugin`

### 1. Create the database

```sql
CREATE ROLE economz_user WITH LOGIN PASSWORD 'choose-a-password';
CREATE DATABASE economz OWNER economz_user;
```

Flyway creates the schema the first time the app starts.

### 2. Configure environment variables

Copy `.env.example` and fill in your values. **Never commit real secrets.** `.env` files and `application-local.properties` are git-ignored.

| Variable | Required | Description |
| --- | --- | --- |
| `DB_URL` | no | JDBC URL (default `jdbc:postgresql://localhost:5432/economz`) |
| `DB_USERNAME` | no | Database user (default `economz_user`) |
| `DB_PASSWORD` | yes | Database password |
| `JWT_SECRET` | yes | High-entropy secret, at least 256 bits |
| `JWT_COOKIE_SECURE` | no | Set to `false` only for local plain-HTTP development (default `true`) |
| `MIDTRANS_SERVER_KEY` / `MIDTRANS_CLIENT_KEY` | for checkout | Midtrans Sandbox keys |
| `MIDTRANS_BASE_URL` | no | Default `https://app.sandbox.midtrans.com` |
| `MIDTRANS_PUBLIC_BASE_URL` | no | Public URL of this app (default `http://localhost:8080`) |
| `CLOUDINARY_CLOUD_NAME` / `CLOUDINARY_API_KEY` / `CLOUDINARY_API_SECRET` | for uploads | Cloudinary credentials (kept server-side) |

Spring does not read `.env` files on its own. Export the variables in your shell or IDE run configuration, or put them in `src/main/resources/application-local.properties` (the default `local` profile).

### 3. Run

```bash
./mvnw spring-boot:run
```

On Windows, use `mvnw.cmd spring-boot:run`. The app runs at http://localhost:8080.

To work on the frontend with hot reload:

```bash
cd frontend
npm install
npm run dev
```

## Tests

```bash
./mvnw test
```

```bash
cd frontend && npm test
```

## Project layout

```
src/main/java/id/co/economz/eco_nomz/
  admin/        administration and moderation
  auth/         login, registration, JWT
  config/       security and app configuration
  offer/        food offers
  order/        reservations and pickups
  payment/      Midtrans integration
  restaurant/   restaurants and image storage
  user/         user accounts
src/main/resources/
  db/migration/ Flyway migrations
  templates/    Thymeleaf views
frontend/       React + TypeScript sources
docs/           deployment and security docs
```

## Deployment

See [docs/DEPLOYMENT.md](docs/DEPLOYMENT.md) and [docs/SECURITY-CHECKLIST.md](docs/SECURITY-CHECKLIST.md). Production runs with the `prod` Spring profile and reads every credential from environment variables.
