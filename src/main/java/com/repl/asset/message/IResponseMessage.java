package com.repl.asset.message;

/**
 * Message text that travels in the body of BaseResponseBO as "message".
 * Add AMS wording here as the asset modules are built.
 */
public interface IResponseMessage {

	String SUCCESS = "success";
	String SERVER_ERROR = "Server Error.";

	String UNAUTHORIZED_USER = "User Not Found.";
	String USER_NOT_FOUND = "User Not Found";
	String NOTACCESS = "Your Login Disabled ";
	String LOGGEDOUT_SUCCESSFULLY = "Logout successfully";
}
