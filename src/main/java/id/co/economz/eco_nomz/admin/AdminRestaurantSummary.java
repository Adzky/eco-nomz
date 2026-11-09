package id.co.economz.eco_nomz.admin;

import java.time.Instant;

public record AdminRestaurantSummary(
        Long id,
        String name,
        String ownerName,
        String ownerEmail,
        boolean published,
        boolean moderated,
        Instant createdAt
) {
}
