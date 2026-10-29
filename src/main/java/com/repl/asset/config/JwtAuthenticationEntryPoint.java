package com.repl.asset.config;

import java.io.IOException;

import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * What a request without a valid sign-in token gets: HTTP 401, with a body in the same shape
 * as every other answer (BaseResponseBO — message + status_code), so the UI can read it the
 * way it reads everything else. TMS sends a bare sendError(401) page instead.
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException authException) throws IOException {
		response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding("UTF-8");
		response.getWriter().write(
				"{\"message\":\"Your session has expired or you are not signed in. Please log in again.\","
						+ "\"status_code\":401}");
	}
}
