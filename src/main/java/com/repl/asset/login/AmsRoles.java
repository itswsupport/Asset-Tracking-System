package com.repl.asset.login;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.repl.asset.exception.ApplicationException;
import com.repl.asset.message.IResponseCode;

/**
 * AMS ROLES as stored in ams_login_access.role: one role, or ADMIN and USER together.
 *
 *   "USER"         a user — limited to their plant(s) and device types
 *   "ADMIN"        an admin — every plant and device type, and the masters
 *   "ADMIN,USER"   both at once (always saved in this order)
 *   "VIEWER"       view only — never combined with another role
 *
 * The frontend splits the same string (src/lib/permissions.js), so login needs no change.
 */
public final class AmsRoles {

	public static final String ADMIN = "ADMIN";
	public static final String USER = "USER";
	public static final String VIEWER = "VIEWER";

	/** The order roles are saved in: "ADMIN,USER", never "USER,ADMIN". */
	private static final List<String> ORDER = List.of(ADMIN, USER, VIEWER);

	private AmsRoles() {
	}

	/**
	 * "user, Admin" -> "ADMIN,USER". Refuses an empty value, an unknown role, and VIEWER with
	 * any other role — the same rules as the Reporting Person form, so a hand-made request
	 * cannot save what the screen would not.
	 */
	public static String normalise(String raw) throws ApplicationException {
		Set<String> given = parts(raw);
		if (given.isEmpty()) {
			throw new ApplicationException(IResponseCode.INVALID_DATA, "Select a role.");
		}
		for (String r : given) {
			if (!ORDER.contains(r)) {
				throw new ApplicationException(IResponseCode.INVALID_DATA, "Unknown role: " + r);
			}
		}
		if (given.contains(VIEWER) && given.size() > 1) {
			throw new ApplicationException(IResponseCode.INVALID_DATA,
					"Viewer cannot be combined with another role.");
		}
		return ORDER.stream().filter(given::contains).collect(Collectors.joining(","));
	}

	/** Whether the stored role holds this one: has("ADMIN,USER", "USER") is true. */
	public static boolean has(String stored, String role) {
		return parts(stored).contains(role);
	}

	private static Set<String> parts(String raw) {
		if (raw == null) {
			return Set.of();
		}
		return Arrays.stream(raw.split(","))
				.map(s -> s.trim().toUpperCase())
				.filter(s -> !s.isEmpty())
				.collect(Collectors.toSet());
	}
}
