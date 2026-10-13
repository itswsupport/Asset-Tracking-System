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
	int VALUE_ONE = 1;
	int VALUE_ZERO = 0;
	int DEFAULT = -1;
	String STATUS = "status";

	/*
	 * HRMS also keeps APPROVED / REJECTED / PENDING / REPENDING / RESUBMITED here for its
	 * approval workflow. AMS has no approval flow yet, so they are deliberately left out —
	 * add them back when the asset request approval module is actually built.
	 */

	/**
	 * URL fragments used by @RequestMapping. HRMS keeps its candidate/payroll paths in the
	 * same place; these are the AMS equivalents, matching the routes the frontend already
	 * uses under /AdminDashboard and /UserDashboard.
	 */
	interface IMappingConstants {

		String LOGIN = "/login";
		String LOGOUT = "/logout";
		String FORGETPASSWORD = "/forgetPassword";
		String RESETPASSWORD = "/changePassword";

		String LIST = "/list";
		String SAVE = "/save";
		String UPDATE = "/update";
		String DELETE = "/delete";
		String LAST_ID = "/last_id";

		String ASSET = "/asset";
		String ASSET_DETAILS = "/asset_details";
		String ALLOCATION = "/allocation";
		String ASSIGN_ASSET = "/assign_asset";
		String RETURN_ASSET = "/return_asset";
		String ALLOCATION_HISTORY = "/allocation_history";

		String MAINTENANCE = "/maintenance";
		String SERVICE_REQUEST = "/service_request";
		String SERVICE_HISTORY = "/service_history";

		String CATEGORY = "/category";
		String DEPARTMENT = "/department";
		String LOCATION = "/location";
		String VENDOR = "/vendor";

		String MY_ASSETS = "/my_assets";
		String REQUEST_ASSET = "/request_asset";
		String MY_REQUESTS = "/my_requests";

		String REPORTS = "/reports";
	}
}
