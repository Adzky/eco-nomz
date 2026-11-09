# Security Review Checklist

This checklist is for each pilot release and must be completed by an operator/reviewer. A checked item is evidence to collect, not a substitute for a security assessment.

## Identity and authorization

- [ ] Confirm public registration only creates customer or restaurant-owner roles; administrator provisioning remains out of band.
- [ ] Verify unauthenticated requests cannot access `/customer/**`, `/owner/**`, or `/admin/**`.
- [ ] Verify customer, owner, and administrator roles cannot access each other's protected routes.
- [ ] Verify owners can access only their own restaurants, offers, and orders.
- [ ] Verify disabled accounts cannot authenticate with a password or reuse an existing JWT.
- [ ] Verify only admins can suspend/reactivate non-admin accounts or moderate listings.
- [ ] Verify administrator accounts cannot be disabled or modified through the dashboard.
- [ ] Verify every state-changing browser form uses CSRF protection.

## Input and data handling

- [ ] Review validation for registration, login, restaurant profiles, offers, search filters, reservations, and moderation notes.
- [ ] Verify entity ownership and role checks in service methods, not only controllers.
- [ ] Verify SQL/JPA queries use bound parameters and public listing queries exclude moderated/disabled content.
- [ ] Verify errors do not reveal stack traces, SQL, database details, submitted secrets, or internal identifiers beyond the intended response.
- [ ] Verify logs and monitoring do not contain passwords, JWTs, authorization headers, payment secrets, or request bodies.
- [ ] Verify logs are access-controlled, retained, and protected against unauthorized modification.

## Runtime and dependencies

- [ ] Run automated tests and dependency vulnerability checks for the release.
- [ ] Verify production runs with the `prod` profile and required secrets are not committed or baked into images.
- [ ] Verify TLS termination, trusted forwarded headers, HTTPS-only requests, HSTS, and secure cookies.
- [ ] Verify only the health endpoint is exposed by Actuator and health details are hidden.
- [ ] Verify database roles follow least privilege and migrations are reviewed before deployment.
- [ ] Verify backups are encrypted, off-site, retained appropriately, and restore-tested.
- [ ] Verify Midtrans notifications validate signatures, amount, currency, and idempotent transitions.
- [ ] Keep production payments disabled until production credentials, reconciliation, refunds, and incident procedures are implemented and tested.
