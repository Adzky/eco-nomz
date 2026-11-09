package id.co.economz.eco_nomz.admin;

import java.time.Instant;
import java.time.OffsetDateTime;
import id.co.economz.eco_nomz.offer.OfferStatus;

public record AdminOfferSummary(
        Long id,
        String title,
        String restaurantName,
        String ownerEmail,
        OfferStatus status,
        int quantityAvailable,
        OffsetDateTime pickupEnd,
        boolean moderated,
        String moderationNote,
        Instant createdAt
) {
}
