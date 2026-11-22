package br.ufmt.periscope.api.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.logging.Logger;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * HS256 JWT service using Jackson for header/payload serialization and parsing.
 * In production mode ({@code PERISCOPE_PRODUCTION=true} or {@code PERISCOPE_REQUIRE_JWT_SECRET=true}),
 * {@code PERISCOPE_JWT_SECRET} must be provided and at least 32 bytes long.
 */
@ApplicationScoped
public class JwtService {

    private static final Logger LOG = Logger.getLogger(JwtService.class.getName());
    private static final String HMAC = "HmacSHA256";
    private static final int MIN_PROD_SECRET_BYTES = 32;
    private static final Base64.Encoder B64URL = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder B64URL_DEC = Base64.getUrlDecoder();
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private byte[] secret;
    private long expirationSeconds = 8 * 3600L;

    @PostConstruct
    void init() {
        init(System::getenv);
    }

    void init(Function<String, String> env) {
        boolean requireSecret = "true".equalsIgnoreCase(env.apply("PERISCOPE_PRODUCTION"))
                || "true".equalsIgnoreCase(env.apply("PERISCOPE_REQUIRE_JWT_SECRET"));
        String envSecret = env.apply("PERISCOPE_JWT_SECRET");
        if (envSecret != null && !envSecret.isBlank()) {
            byte[] candidate = envSecret.getBytes(StandardCharsets.UTF_8);
            if (requireSecret && candidate.length < MIN_PROD_SECRET_BYTES) {
                throw new IllegalStateException(
                        "PERISCOPE_JWT_SECRET must be at least " + MIN_PROD_SECRET_BYTES
                                + " bytes in production mode");
            }
            secret = candidate;
        } else if (requireSecret) {
            throw new IllegalStateException(
                    "PERISCOPE_JWT_SECRET is required when PERISCOPE_PRODUCTION=true");
        } else {
            byte[] generated = new byte[32];
            new SecureRandom().nextBytes(generated);
            secret = HexFormat.of().formatHex(generated).getBytes(StandardCharsets.UTF_8);
            LOG.warning("PERISCOPE_JWT_SECRET not set; using ephemeral in-memory secret "
                    + "(tokens will be invalid after restart)");
        }
        String expHours = env.apply("PERISCOPE_JWT_EXPIRATION_HOURS");
        if (expHours != null && !expHours.isBlank()) {
            try {
                expirationSeconds = Long.parseLong(expHours.trim()) * 3600L;
            } catch (NumberFormatException ignored) {
                LOG.warning("Invalid PERISCOPE_JWT_EXPIRATION_HOURS; using default 8h");
            }
        }
    }

    public long getExpirationSeconds() {
        return expirationSeconds;
    }

    public String issueToken(String username, String userLevel) {
        long iat = Instant.now().getEpochSecond();
        long exp = iat + expirationSeconds;
        String jti = UUID.randomUUID().toString();
        try {
            Map<String, Object> headerMap = Map.of("alg", "HS256", "typ", "JWT");
            Map<String, Object> payloadMap = new LinkedHashMap<>();
            payloadMap.put("sub", username != null ? username : "");
            payloadMap.put("userLevel", userLevel != null ? userLevel : "USER");
            payloadMap.put("jti", jti);
            payloadMap.put("iat", iat);
            payloadMap.put("exp", exp);

            String header = B64URL.encodeToString(MAPPER.writeValueAsBytes(headerMap));
            String payload = B64URL.encodeToString(MAPPER.writeValueAsBytes(payloadMap));
            String signingInput = header + "." + payload;
            String signature = B64URL.encodeToString(hmac(signingInput));
            return signingInput + "." + signature;
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to serialize JWT", ex);
        }
    }

    public JwtClaims parseAndValidate(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Missing token");
        }
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Malformed token");
        }
        Map<String, Object> headerMap = decodeJsonMap(parts[0], "header");
        Object alg = headerMap.get("alg");
        if (!"HS256".equals(alg)) {
            throw new IllegalArgumentException("Unsupported token algorithm");
        }

        String signingInput = parts[0] + "." + parts[1];
        byte[] expected = hmac(signingInput);
        byte[] actual;
        try {
            actual = B64URL_DEC.decode(parts[2]);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid token signature encoding");
        }
        if (!MessageDigest.isEqual(expected, actual)) {
            throw new IllegalArgumentException("Invalid token signature");
        }

        Map<String, Object> payloadMap = decodeJsonMap(parts[1], "payload");
        long exp = readLong(payloadMap, "exp");
        if (Instant.now().getEpochSecond() >= exp) {
            throw new IllegalArgumentException("Token expired");
        }
        String sub = readString(payloadMap, "sub");
        if (sub == null || sub.isBlank()) {
            throw new IllegalArgumentException("Token missing subject");
        }
        String userLevel = readString(payloadMap, "userLevel");
        if (userLevel == null || userLevel.isBlank()) {
            userLevel = "USER";
        }
        long iat = readLong(payloadMap, "iat");
        String jti = readString(payloadMap, "jti");
        return new JwtClaims(sub, userLevel, exp, iat, jti);
    }

    void setSecretForTests(byte[] secretBytes) {
        this.secret = secretBytes;
    }

    void setExpirationSecondsForTests(long seconds) {
        this.expirationSeconds = seconds;
    }

    private byte[] hmac(String data) {
        try {
            Mac mac = Mac.getInstance(HMAC);
            mac.init(new SecretKeySpec(secret, HMAC));
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("HMAC-SHA256 unavailable", ex);
        }
    }

    private static Map<String, Object> decodeJsonMap(String base64Part, String label) {
        try {
            byte[] decoded = B64URL_DEC.decode(base64Part);
            return MAPPER.readValue(decoded, MAP_TYPE);
        } catch (IllegalArgumentException | IOException ex) {
            throw new IllegalArgumentException("Malformed token " + label, ex);
        }
    }

    private static String readString(Map<String, Object> map, String claim) {
        Object val = map.get(claim);
        return val instanceof String s ? s : null;
    }

    private static long readLong(Map<String, Object> map, String claim) {
        Object val = map.get(claim);
        if (val instanceof Number n) {
            return n.longValue();
        }
        throw new IllegalArgumentException("Token missing claim " + claim);
    }

    public record JwtClaims(String subject, String userLevel, long exp, long iat, String jti) {
        public JwtClaims(String subject, String userLevel, long exp, long iat) {
            this(subject, userLevel, exp, iat, null);
        }
    }
}
