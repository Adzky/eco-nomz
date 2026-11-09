package id.co.economz.eco_nomz.admin;

import java.time.Instant;
import id.co.economz.eco_nomz.user.UserRole;

public record AdminUserSummary(
        Long id,
        String name,
        String email,
        UserRole role,
        boolean active,
        Instant createdAt
) {
}
