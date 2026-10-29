package com.repl.asset.constant;

public interface Constants {

	int STATUS_ACTIVE = 1;
	int STATUS_INACTIVE = 0;

	/**
	 * Deleted from the user's point of view. The row stays in the table so anything already
	 * pointing at it still resolves, but every list query filters on STATUS_ACTIVE so it is
	 * gone from every screen.
	 *
	 * -1, deliberately distinct from STATUS_INACTIVE: 0 means "switched off, can be switched
	 * back on", while -1 means "the user deleted this". Collapsing the two would make a
	 * deleted row indistinguishable from a deactivated one.
	 */
	int STATUS_DELETED = -1;

	/**
	 * URL fragments used by @RequestMapping — only those a controller actually maps (the
	 * others write their paths out in full). Add one here when the endpoint it names is built.
	 */
	interface IMappingConstants {

		String LOGIN = "/login";
		String LOGOUT = "/logout";
	}
}
