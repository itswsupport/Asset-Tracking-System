package com.repl.asset.config;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

import javax.crypto.SecretKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Deserializer;
import io.jsonwebtoken.io.Serializer;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * JWT SIGN-IN TOKENS — issued by /login, checked on every other request (JwtRequestFilter).
 *
 * The same scheme as the TMS backend's JwtTokenUtil: HS256, signed with jwt.secret, the
 * employee code as the subject. Two differences, both deliberate:
 *
 *   - The secret comes from the JWT_SECRET environment variable in a deployment (Jenkins), not
 *     from a value fixed in git. The default below is for a developer's machine only, and a
 *     WARN is logged at start-up whenever it is the one in use.
 *   - How long a token lasts is a setting (jwt.validity-minutes). The browser already signs
 *     out after 30 idle minutes, so this is the hard limit for someone working all day.
 *
 * JJWT is given a Jackson 3 (de)serializer of its own: its ready-made jjwt-jackson module is
 * built on Jackson 2, which this project does not carry.
 */
@Component
public class JwtTokenUtil {

	private static final Logger logger = LoggerFactory.getLogger(JwtTokenUtil.class);

	/** Developer-machine default. Never used by a deployment: Jenkins sets JWT_SECRET. */
	static final String DEV_SECRET = "ams-local-development-secret-not-for-production-use-0001";

	/** The role, as ams_login_access holds it (ADMIN, USER, ADMIN,USER …). */
	public static final String CLAIM_ROLE = "role";

	@Value("${jwt.secret}")
	private String secret;

	@Value("${jwt.validity-minutes:600}")
	private long validityMinutes;

	private SecretKey key;

	private final ObjectMapper json = JsonMapper.builder().build();

	private final Serializer<Map<String, ?>> serializer = claims -> json.writeValueAsBytes(claims);

	@SuppressWarnings("unchecked")
	private final Deserializer<Map<String, ?>> deserializer = bytes -> json.readValue(bytes, Map.class);

	@PostConstruct
	void init() {
		// HS256 needs a key of at least 256 bits; JJWT refuses a shorter one, so say why.
		byte[] bytes = secret == null ? new byte[0] : secret.getBytes(StandardCharsets.UTF_8);
		if (bytes.length < 32) {
			throw new IllegalStateException("jwt.secret (JWT_SECRET) must be at least 32 characters long.");
		}
		key = Keys.hmacShaKeyFor(bytes);
		if (DEV_SECRET.equals(secret)) {
			logger.warn("jwt.secret is the DEVELOPMENT default. Set JWT_SECRET before this runs "
					+ "anywhere but a developer's machine.");
		}
	}

	/** A token for this employee, carrying their AMS role, valid for jwt.validity-minutes. */
	public String generateToken(Long empCode, String role) {
		long now = System.currentTimeMillis();
		return Jwts.builder()
				.serializeToJsonWith(serializer)
				.setSubject(String.valueOf(empCode))
				.claim(CLAIM_ROLE, role)
				.setIssuedAt(new Date(now))
				.setExpiration(new Date(now + validityMinutes * 60_000L))
				.signWith(key, SignatureAlgorithm.HS256)
				.compact();
	}

	/**
	 * The token's claims when it is genuine and not expired; null otherwise — a bad signature,
	 * an expired or malformed token all mean the same thing to the caller: not signed in.
	 */
	public Claims parse(String token) {
		if (token == null || token.isBlank()) {
			return null;
		}
		try {
			return Jwts.parser()
					.deserializeJsonWith(deserializer)
					.setSigningKey(key)
					.parseClaimsJws(token)
					.getBody();
		} catch (JwtException | IllegalArgumentException | JacksonException e) {
			// JacksonException: a token whose parts are not even JSON (garbage, not a JWT).
			logger.debug("Rejected JWT: {}", e.getMessage());
			return null;
		}
	}
}
