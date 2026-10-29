package com.repl.asset.common;

import java.lang.reflect.Method;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.persistence.PrePersist;

/**
 * Fills reg_by with the signed-in employee's code on every NEW row, in every table.
 *
 * Registered once for all entities in META-INF/orm.xml, so no master, device, server,
 * software, licence or bulk upload needs its own code for it. The UI sends the employee
 * code in the X-Emp-Code header of every request (AMS_UI src/lib/api.js).
 *
 * Insert only: an update never changes who added the row. A reg_by a service has already
 * set is kept, and a save outside a request (scheduled jobs) leaves it as it is.
 */
public class RegByListener {

	public static final String HEADER = "X-Emp-Code";

	@PrePersist
	public void fillRegBy(Object entity) {
		Long code = currentEmpCode();
		if (code == null) {
			return;
		}
		try {
			Method getter = entity.getClass().getMethod("getRegBy");
			if (getter.invoke(entity) != null) {
				return;
			}
			entity.getClass().getMethod("setRegBy", Long.class).invoke(entity, code);
		} catch (NoSuchMethodException e) {
			// A table without reg_by (the user logs) — nothing to fill.
		} catch (ReflectiveOperationException e) {
			// Never fail a save over who added it.
		}
	}

	/**
	 * The signed-in employee's code: from their sign-in token (JwtRequestFilter) — which cannot
	 * be made up — and only failing that from the X-Emp-Code header. Null when there is neither.
	 */
	public static Long currentEmpCode() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth != null && auth.isAuthenticated() && auth.getName() != null
				&& auth.getName().matches("[0-9]{1,18}")) {
			return Long.valueOf(auth.getName());
		}
		RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
		if (!(attrs instanceof ServletRequestAttributes servlet)) {
			return null;
		}
		String value = servlet.getRequest().getHeader(HEADER);
		if (value == null) {
			return null;
		}
		value = value.trim();
		return value.matches("[0-9]{1,18}") ? Long.valueOf(value) : null;
	}
}
