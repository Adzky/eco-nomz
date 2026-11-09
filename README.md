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

### Free hosting: Hugging Face Spaces + Neon (no card needed)

1. **Database:** create a free PostgreSQL project on [Neon](https://neon.tech). Convert its connection string `postgresql://USER:PASS@HOST/DB?...` into the `DB_URL`, `DB_USERNAME` and `DB_PASSWORD` values described in the Render section below.
2. **Space:** on [huggingface.co/new-space](https://huggingface.co/new-space), choose the **Docker** SDK and the **Blank** template. Then upload the two files from [`deploy/huggingface/`](deploy/huggingface/) (`Dockerfile` and `README.md`). The Space clones this GitHub repo and builds it.
3. **Secrets:** in the Space's **Settings → Variables and secrets**, add as *Secrets*: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET` (from `openssl rand -base64 48`), `MIDTRANS_SERVER_KEY`, `MIDTRANS_CLIENT_KEY`, `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, `CLOUDINARY_API_SECRET`. Add `MIDTRANS_PUBLIC_BASE_URL=https://<username>-<space>.hf.space`.
4. Open the app at `https://<username>-<space>.hf.space`. It doesn't load inside the embedded Hugging Face page, because the app blocks being framed.
5. **Auto-redeploy (optional):** in this GitHub repo's Settings → Secrets and variables → Actions, add the secret `HF_TOKEN` (a Hugging Face token with write access) and the variable `HF_SPACE` (`<username>/<space>`). Each push to `main` then rebuilds the Space through `.github/workflows/deploy-huggingface.yml`.

Free Spaces sleep after a period of inactivity, and the first request after that is slow.

### Alternative: Render + Neon

The repo includes a `Dockerfile` (a multi-stage build tuned for 512 MB instances) and a `render.yaml` Blueprint.

1. **Database:** create a free PostgreSQL project on [Neon](https://neon.tech). Convert its connection string `postgres://USER:PASS@HOST/DB` into:
   - `DB_URL=jdbc:postgresql://HOST/DB?sslmode=require`
   - `DB_USERNAME=USER`
   - `DB_PASSWORD=PASS`
2. **App:** on [Render](https://render.com), choose **New → Blueprint** and select this repository. Render reads `render.yaml`, creates a free Docker web service, and generates `JWT_SECRET` for you.
3. Fill in the prompted secrets: the DB values, Midtrans Sandbox keys, Cloudinary credentials, and `MIDTRANS_PUBLIC_BASE_URL=https://<your-service>.onrender.com`.
4. Deploy. Flyway migrates the database on first start, and every push to `main` redeploys.
5. In the Midtrans Sandbox dashboard, set the payment notification URL to `https://<your-service>.onrender.com/payments/midtrans/notifications`.

Free-tier notes: the service sleeps after about 15 minutes idle, and the first request after that takes 30–60 seconds while it wakes up.

To try the production image locally:

```bash
docker build -t eco-nomz .
docker run -p 8080:8080 -e DB_URL=... -e DB_USERNAME=... -e DB_PASSWORD=... -e JWT_SECRET=... eco-nomz
```

### More

See [docs/DEPLOYMENT.md](docs/DEPLOYMENT.md) and [docs/SECURITY-CHECKLIST.md](docs/SECURITY-CHECKLIST.md). Production runs with the `prod` Spring profile and reads every credential from environment variables.
