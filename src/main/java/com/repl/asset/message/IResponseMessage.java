package com.repl.asset.message;

/**
 * Message text that travels in the body of BaseResponseBO as "message".
 * HRMS-specific wording (candidate/onboarding/payroll) is intentionally left out —
 * add AMS wording here as the asset modules are built.
 */
public interface IResponseMessage {

	String SUCCESS = "success";
	String SERVER_ERROR = "Server Error.";
	String DATA_NOT_FOUND = "Data Not Found";

	String UNAUTHORIZED_USER = "User Not Found.";
	String USER_NOT_FOUND = "User Not Found";
	String NOTACCESS = "Your Login Disabled ";
	String ALREADY_LOGGEDOUT = "Already Logout.";
	String LOGGEDOUT_SUCCESSFULLY = "Logout successfully";

	String FORGET_PASSWORD = "Forget Password Link Send Your Registered Mail Id.";
	String RESET_PASSWORD = "Your Password Changed.";
	String PASSWORDCHANGE = "Password Changes Error.";

	String INVALID_MASTER_ID = "Invalid Master Id";
	String WRONG_FILE_FORMAT = "Wrong file format";

	String REQUEST_ALREADY_APPROVED = "Request Already Approved";
	String REQUEST_ALREADY_REJECTED = "Request Already Rejected";
	String REQUEST_ALREADY_SUBMITTED = "Request Already Submitted";
}
