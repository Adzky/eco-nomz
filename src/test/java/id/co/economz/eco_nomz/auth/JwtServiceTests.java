package id.co.economz.eco_nomz.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

class JwtServiceTests {
    private static final String SECRET = "a-test-secret-containing-more-than-32-bytes";

    @Test
    void signsAndValidatesTokenSubject() {
        JwtService jwtService = new JwtService(SECRET, Duration.ofMinutes(15));

        String token = jwtService.generateToken("customer@example.com");

        assertThat(jwtService.extractSubject(token)).isEqualTo("customer@example.com");
        assertThat(jwtService.getExpiration()).isEqualTo(Duration.ofMinutes(15));
    }

    @Test
    void rejectsTokenSignedWithDifferentKey() {
        JwtService issuer = new JwtService(SECRET, Duration.ofMinutes(15));
        JwtService verifier = new JwtService("a-different-test-secret-with-over-32-bytes", Duration.ofMinutes(15));
        String token = issuer.generateToken("customer@example.com");

        assertThatThrownBy(() -> verifier.extractSubject(token))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void rejectsExpiredToken() {
        Instant now = Instant.now();
        String expiredToken = Jwts.builder()
                .subject("customer@example.com")
                .issuedAt(Date.from(now.minusSeconds(120)))
                .expiration(Date.from(now.minusSeconds(60)))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();
        JwtService jwtService = new JwtService(SECRET, Duration.ofMinutes(15));

        assertThatThrownBy(() -> jwtService.extractSubject(expiredToken))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void refusesWeakSigningSecret() {
        assertThatThrownBy(() -> new JwtService("too-short", Duration.ofMinutes(15)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("32 UTF-8 bytes");
    }
}
