package com.repl.asset.message;

/**
 * Application level status codes. These travel in the body of BaseResponseBO as
 * "status_code" — the HTTP status is always 200, so this is what the frontend checks.
 */
public interface IResponseCode {

	int SUCCESS = 200;
	int UNAUTHORIZED = 401;
	int SERVER_ERROR = 501;

	int EXISTING_ENTITY = 1001;
	int INVALID_ENTITY = 1002;
	int INVALID_DATA = 1004;

	int USER_NOT_FOUND = 500;
}
