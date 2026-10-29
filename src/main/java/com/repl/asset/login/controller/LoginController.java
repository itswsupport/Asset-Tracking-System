package com.repl.asset.login.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.repl.asset.basecontroller.BaseController;
import com.repl.asset.bo.BaseResponseBO;
import com.repl.asset.config.JwtTokenUtil;
import com.repl.asset.constant.Constants;
import com.repl.asset.exception.ApplicationException;
import com.repl.asset.login.bo.LoginUserBO;
import com.repl.asset.login.service.ILoginService;
import com.repl.asset.message.IResponseCode;
import com.repl.asset.message.IResponseMessage;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RestController
public class LoginController extends BaseController {

	private static final Logger logger = LoggerFactory.getLogger(LoginController.class);

	@Autowired
	private ILoginService loginService;

	@Autowired
	private JwtTokenUtil jwtTokenUtil;

	/**
	 * POST /login?emp_code=...&emp_pass=...
	 *
	 * POST, not GET. The HRMS equivalent is a @GetMapping, which puts the password in the
	 * query string of every access log and browser history entry along the way.
	 *
	 * As with every BaseController response the HTTP status is 200 even on failure — the
	 * frontend must read status_code out of the body.
	 */
	@PostMapping(value = { Constants.IMappingConstants.LOGIN, Constants.IMappingConstants.LOGIN + "/" })
	public ResponseEntity<BaseResponseBO> login(@RequestParam("emp_code") Long empCode,
			// required=false so an empty password reaches the service, which is what decides
			// whether that is acceptable (login.password.verify).
			@RequestParam(value = "emp_pass", required = false) String empPass) {
		try {
			LoginUserBO user = loginService.login(empCode, empPass);
			// Issued only once sign-in has fully succeeded (password and AMS access both checked).
			user.setToken(jwtTokenUtil.generateToken(user.getEmp_code(), user.getRole()));
			return getResponseModel(user, IResponseCode.SUCCESS, IResponseMessage.SUCCESS, null);
		} catch (ApplicationException e) {
			// Expected sign-in failures: unknown code, inactive employee, bad password, no
			// AMS access. The message is deliberately vague about which one it was.
			return getResponseModel(null, e.getCode(), e.getMessage(), null);
		} catch (Exception e) {
			logger.error("Unexpected error during AMS sign-in", e);
			return getResponseModel(null, IResponseCode.SERVER_ERROR, IResponseMessage.SERVER_ERROR, null);
		}
	}

	/**
	 * POST /logout?emp_code=...
	 *
	 * Honest about what it is: the sign-in token is stateless (nothing is kept server-side),
	 * so there is nothing here to invalidate — the browser forgets its token and the session
	 * is over. This endpoint exists so the sign-out is recorded in the application log.
	 *
	 * It always succeeds. Logging out must never fail and strand someone in the app.
	 */
	@PostMapping(value = { Constants.IMappingConstants.LOGOUT, Constants.IMappingConstants.LOGOUT + "/" })
	public ResponseEntity<BaseResponseBO> logout(
			@RequestParam(value = "emp_code", required = false) Long empCode) {
		logger.info("AMS sign-out for emp_code {}", empCode);
		return getResponseModel(null, IResponseCode.SUCCESS, IResponseMessage.LOGGEDOUT_SUCCESSFULLY, null);
	}
}
