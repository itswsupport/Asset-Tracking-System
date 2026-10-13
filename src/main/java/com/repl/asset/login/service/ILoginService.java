package com.repl.asset.login.service;

import com.repl.asset.exception.ApplicationException;
import com.repl.asset.login.bo.LoginUserBO;

public interface ILoginService {

	/**
	 * Verify an employee against the shared master and this application's access list.
	 *
	 * @throws ApplicationException with an IResponseCode when the employee does not exist,
	 *                              has left, has no usable password, fails the password
	 *                              check, or has not been granted AMS access.
	 */
	LoginUserBO login(Long empCode, String password) throws ApplicationException;
}
