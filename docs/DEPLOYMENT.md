# Production Deployment and Operations

## Production environment

Run the application with the `prod` Spring profile. Supply these values through the hosting platform's secret/environment-variable facility; do not put production values in source control or an image:

| Variable | Required value |
| --- | --- |
| `DB_URL` | PostgreSQL JDBC URL, for example `jdbc:postgresql://db-host:5432/economz` |
| `DB_USERNAME` | Application database role; grant only the required schema/table permissions |
| `DB_PASSWORD` | Application database password |
| `JWT_SECRET` | High-entropy secret of at least 256 bits; keep stable across application instances |

Keep the database on a private network, restrict inbound traffic to the application and approved administration paths, and use managed secret storage. Do not enable production payment processing: the current payment integration is for Midtrans Sandbox only.

## HTTPS and reverse proxy

Terminate TLS at a trusted load balancer or reverse proxy and forward requests to the application over a protected private network. The proxy must set the forwarded scheme to `https`, strip client-supplied forwarding headers, and only accept traffic from trusted proxy addresses. The `prod` profile enables Spring forwarded-header handling, HTTPS-only requests, HSTS for this host, and secure JWT cookies. Verify the public site redirects HTTP to HTTPS and that the authentication cookie has `Secure`, `HttpOnly`, and `SameSite=Lax` attributes before opening the pilot. Configure health probes through the trusted proxy or use HTTPS when probing the application directly.

Do not expose the application directly to untrusted networks when relying on forwarded headers.

## Database migration and release

Flyway migrations are versioned in `src/main/resources/db/migration`. Production starts with Hibernate schema validation and Flyway enabled. For each release:

1. Review the migration SQL and confirm it is backward-compatible with the currently deployed application where possible.
2. Take and verify a restorable database backup.
3. Deploy the new application to exactly one instance first. Wait for successful Flyway startup and the readiness health check before adding instances or routing user traffic.
4. Confirm the application version, migration completion, health checks, and representative customer and owner flows.
5. Scale out only after the first instance is healthy.

Do not allow multiple independent release jobs to apply the same migration concurrently. Do not edit an already-applied migration; add a new forward migration instead.

## Database backup and restore

Use PostgreSQL client tools from a trusted host and a least-privilege backup identity. Supply connection details via the platform's secret store or a protected PostgreSQL password file; do not put passwords in command arguments or shell history.

Create a compressed backup:

```text
pg_dump --format=custom --file=eco-nomz-YYYYMMDD-HHMM.dump "$DATABASE_URL"
```

Encrypt backups in transit and at rest, store an off-site copy, restrict access, and apply a documented retention policy. Record the backup timestamp and database/schema version.

Test restoration to a separate, isolated PostgreSQL database before relying on a backup:

```text
createdb eco_nomz_restore_test
pg_restore --no-owner --dbname=eco_nomz_restore_test eco-nomz-YYYYMMDD-HHMM.dump
```

Run migration/schema checks and representative read/write smoke tests against the restored database. Never test a restore by overwriting the production database. Record restore duration and issues, then securely remove the temporary restored data according to the retention policy.

## Rollback

1. Stop or remove the unhealthy release from traffic; retain logs and deployment metadata without copying secrets.
2. If the schema migration is backward-compatible, roll application instances back to the previous known-good image and verify health and core flows.
3. If data/schema changes are not backward-compatible or data was damaged, keep writes stopped and follow the incident recovery decision: apply a reviewed forward fix or restore the verified backup into a clean database and perform a controlled cutover.
4. Do not run ad hoc destructive SQL or attempt automatic Flyway downgrades. Flyway migrations are forward-only.
5. Reconcile reservations and payments with Midtrans Sandbox before resuming checkout. Payment reconciliation and refunds are not yet production-ready.
6. Document the incident, recovery point, recovery time, and follow-up actions.

## Health checks and monitoring

Only Actuator health is exposed. Use `/actuator/health/liveness` to determine whether the process should be restarted and `/actuator/health/readiness` to decide whether it should receive traffic. Health details are hidden. Alert on repeated unhealthy checks, application restarts, database connection failures, failed migrations, and sustained HTTP 5xx responses.

Collect application logs centrally with access controls and retention. The application avoids logging request bodies, query strings, authentication headers, JWTs, passwords, and payment credentials. Restrict log access and never enable request-detail or SQL bind-parameter logging in production.

## Administrator bootstrap

Administrator registration is intentionally unavailable through the public registration page. Create an account through `/register`, then have an authorized database operator promote that account using a protected PostgreSQL session:

```sql
BEGIN;
UPDATE app_user
SET role = 'ADMIN'
WHERE LOWER(email) = LOWER('administrator@example.com')
  AND active = TRUE;
-- Verify exactly one row was updated before committing.
COMMIT;
```

If zero or multiple rows were updated, roll back and investigate. Do not put an administrator password or credentials in SQL scripts. Keep at least two individually assigned administrator accounts; administrator roles/status are deliberately not editable in the web dashboard.

## Deployment checklist

- [ ] Production secrets are injected from the hosting secret store.
- [ ] Database is private, backed up, and restore-tested.
- [ ] TLS, trusted proxy forwarding, HTTP-to-HTTPS redirection, HSTS, and secure cookies are verified.
- [ ] Exactly one instance performs startup migration for a release.
- [ ] Health readiness/liveness probes are configured and alerts are active.
- [ ] Admin accounts are individually assigned and verified.
- [ ] Core marketplace, reservation, and owner flows pass smoke tests.
- [ ] Payment remains in Sandbox until production credentials, HTTPS callback handling, reconciliation, and refunds are implemented and reviewed.
