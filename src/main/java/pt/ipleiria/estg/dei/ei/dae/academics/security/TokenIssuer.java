package pt.ipleiria.estg.dei.ei.dae.academics.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Default;

import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@ApplicationScoped
@Default
public class TokenIssuer {
    public static final byte[] SECRET_KEY =
            "veRysup3rstr0nginv1ncible5ecretkeY@academics.dae.ipleiria".getBytes();
    public static final String ALGORITHM = "HMACSHA384";
    public static final long EXPIRY_MINS = 60L;

    public static String issue(String username, String authVersion) {
        var expiryPeriod = LocalDateTime.now().plusMinutes(EXPIRY_MINS);
        var expirationDateTime = Date.from(
                expiryPeriod.atZone(ZoneId.systemDefault()).toInstant()
        );
        Key key = new SecretKeySpec(SECRET_KEY, ALGORITHM);
        return Jwts.builder()
                .subject(username)
                .claim("version", authVersion) // EP - Single Session
                .issuedAt(new Date())
                .expiration(expirationDateTime)
                .signWith(key)
                .compact();
    }
}