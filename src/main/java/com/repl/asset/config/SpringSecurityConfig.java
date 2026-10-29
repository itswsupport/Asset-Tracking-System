package com.repl.asset.config;

import java.util.Arrays;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.repl.asset.constant.Constants;
import com.repl.asset.login.repository.AmsLoginAccessRepository;

/**
 * SIGN-IN REQUIRED — the same JWT scheme as the TMS backend (WebSecurityConfig there), written
 * against the current API: WebSecurityConfigurerAdapter and antMatchers were removed in Spring
 * Security 6/7, which this project pulls in via Spring Boot 4.1.1.
 *
 *   OPEN       /login and /logout (and CORS preflights, and /error)
 *   EVERYTHING ELSE  a valid "Authorization: Bearer <token>" from /login, or HTTP 401
 *
 * One rule closes every endpoint, unlike TMS, where each controller method checks the header
 * itself and a method that forgets is left open. Stateless: no server session is kept; the
 * token is the session.
 */
@Configuration
@EnableWebSecurity
public class SpringSecurityConfig {

	private final AccessDeniedHandler accessDeniedHandler;
	private final JwtAuthenticationEntryPoint authenticationEntryPoint;
	private final JwtTokenUtil jwtTokenUtil;
	private final AmsLoginAccessRepository accessRepository;

	public SpringSecurityConfig(AccessDeniedHandler accessDeniedHandler,
			JwtAuthenticationEntryPoint authenticationEntryPoint, JwtTokenUtil jwtTokenUtil,
			AmsLoginAccessRepository accessRepository) {
		this.accessDeniedHandler = accessDeniedHandler;
		this.authenticationEntryPoint = authenticationEntryPoint;
		this.jwtTokenUtil = jwtTokenUtil;
		this.accessRepository = accessRepository;
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		String login = Constants.IMappingConstants.LOGIN;
		String logout = Constants.IMappingConstants.LOGOUT;
		http.cors(Customizer.withDefaults())
				.csrf(csrf -> csrf.disable())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth
						// The browser's CORS preflight carries no token.
						.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
						.requestMatchers(login, login + "/", logout, logout + "/").permitAll()
						// Spring's own error page: without this an error inside a request is
						// reported as a 401 instead of the real problem.
						.requestMatchers("/error").permitAll()
						.anyRequest().authenticated())
				.addFilterBefore(new JwtRequestFilter(jwtTokenUtil, accessRepository),
						UsernamePasswordAuthenticationFilter.class)
				.exceptionHandling(ex -> ex
						.authenticationEntryPoint(authenticationEntryPoint)
						.accessDeniedHandler(accessDeniedHandler))
				.headers(headers -> headers.frameOptions(frame -> frame.disable()))
				/*
				 * Spring Security registers its own /logout endpoint by default, and its filter
				 * runs before any controller. Left enabled it answers POST /logout itself — a 405
				 * on anything it does not expect — and LoginController.logout is never reached.
				 */
				.logout(builtIn -> builtIn.disable());
		return http.build();
	}

	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration configuration = new CorsConfiguration();
		// setAllowedOriginPatterns rather than setAllowedOrigins: "*" as a literal origin is
		// rejected once credentials are ever allowed, and the pattern form keeps working.
		configuration.setAllowedOriginPatterns(Arrays.asList("*"));
		configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
		configuration.setAllowedHeaders(Arrays.asList("*"));
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}

	@Bean
	public PasswordEncoder encoder() {
		return new BCryptPasswordEncoder();
	}
}
