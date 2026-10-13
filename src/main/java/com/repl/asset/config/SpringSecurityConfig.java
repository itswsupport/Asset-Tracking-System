package com.repl.asset.config;

import java.util.Arrays;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Same policy as the HRMS SpringSecurityConfig — CORS open, CSRF off, every path
 * permitted, custom access-denied handler, frame options off — but written against the
 * current API.
 *
 * HRMS extends WebSecurityConfigurerAdapter and calls http.authorizeRequests().antMatchers(...).
 * All three were removed in Spring Security 6/7, which this project pulls in via Spring Boot
 * 4.1.1, so copying that file verbatim does not compile. The replacement is a SecurityFilterChain
 * bean with the lambda DSL; the rules it applies are unchanged.
 */
@Configuration
@EnableWebSecurity
public class SpringSecurityConfig {

	private final AccessDeniedHandler accessDeniedHandler;

	public SpringSecurityConfig(AccessDeniedHandler accessDeniedHandler) {
		this.accessDeniedHandler = accessDeniedHandler;
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http.cors(Customizer.withDefaults())
				.csrf(csrf -> csrf.disable())
				.authorizeHttpRequests(auth -> auth.requestMatchers("/**").permitAll())
				.exceptionHandling(ex -> ex.accessDeniedHandler(accessDeniedHandler))
				.headers(headers -> headers.frameOptions(frame -> frame.disable()))
				/*
				 * Spring Security registers its own /logout endpoint by default, and its filter
				 * runs before any controller. Left enabled it answers POST /logout itself — a 405
				 * on anything it does not expect — and LoginController.logout is never reached.
				 * AMS keeps no server-side session, so there is nothing for the built-in handler
				 * to do anyway.
				 */
				.logout(logout -> logout.disable());
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
