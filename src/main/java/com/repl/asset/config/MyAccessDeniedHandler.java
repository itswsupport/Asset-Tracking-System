package com.repl.asset.config;

import java.io.IOException;

/*
 * jakarta.servlet, not javax.servlet. HRMS is on Spring Boot 2.7, where these classes
 * live under javax; Boot 3 moved the whole Servlet API to jakarta, so the HRMS imports
 * do not resolve on this project's Boot 4.1.1.
 */
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

@Component
public class MyAccessDeniedHandler implements AccessDeniedHandler {

	@Override
	public void handle(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse,
			AccessDeniedException e) throws IOException, ServletException {
		httpServletResponse.sendRedirect(httpServletRequest.getContextPath() + "/403");
	}
}
