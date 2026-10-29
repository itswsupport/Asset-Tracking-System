package com.repl.asset.config;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import com.repl.asset.constant.Constants;
import com.repl.asset.login.model.AmsLoginAccess;
import com.repl.asset.login.repository.AmsLoginAccessRepository;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * WHO IS ASKING — reads "Authorization: Bearer <token>" on every request, as the TMS
 * backend's JwtRequestFilter does, and marks the request as signed in when the token is good.
 *
 * A good token is not quite enough: the employee's AMS access is looked up again, so taking
 * someone's access away in Reporting Person locks them out at once rather than when their
 * token runs out. Their role is taken from that row too, not from the token.
 *
 * A missing or bad token does not fail here. The request carries on unauthenticated, and
 * SpringSecurityConfig decides — /login is open, everything else answers 401
 * (JwtAuthenticationEntryPoint).
 *
 * Built by SpringSecurityConfig rather than being a @Component: Spring Boot registers every
 * Filter bean with the servlet container as well, which would run this twice per request.
 */
public class JwtRequestFilter extends OncePerRequestFilter {

	private static final String BEARER = "Bearer ";

	private final JwtTokenUtil jwtTokenUtil;
	private final AmsLoginAccessRepository accessRepository;

	public JwtRequestFilter(JwtTokenUtil jwtTokenUtil, AmsLoginAccessRepository accessRepository) {
		this.jwtTokenUtil = jwtTokenUtil;
		this.accessRepository = accessRepository;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		String header = request.getHeader("Authorization");
		if (header != null && header.startsWith(BEARER)
				&& SecurityContextHolder.getContext().getAuthentication() == null) {
			Claims claims = jwtTokenUtil.parse(header.substring(BEARER.length()).trim());
			Long empCode = claims == null ? null : toLong(claims.getSubject());
			AmsLoginAccess access = empCode == null ? null
					: accessRepository.findByEmpCodeAndStatus(empCode, Constants.STATUS_ACTIVE).orElse(null);
			if (access != null) {
				UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
						String.valueOf(empCode), null, authorities(access.getRole()));
				auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
				SecurityContextHolder.getContext().setAuthentication(auth);
			}
		}
		chain.doFilter(request, response);
	}

	/** "ADMIN,USER" → ROLE_ADMIN, ROLE_USER — ready for role rules when they are wanted. */
	private static List<SimpleGrantedAuthority> authorities(String role) {
		if (role == null || role.isBlank()) {
			return List.of();
		}
		return Arrays.stream(role.split(","))
				.map(String::trim)
				.filter(r -> !r.isEmpty())
				.map(r -> new SimpleGrantedAuthority("ROLE_" + r.toUpperCase()))
				.toList();
	}

	private static Long toLong(String value) {
		return value != null && value.matches("\\d{1,18}") ? Long.valueOf(value) : null;
	}
}
