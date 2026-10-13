package com.repl.asset.login.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.repl.asset.constant.Constants;
import com.repl.asset.exception.ApplicationException;
import com.repl.asset.login.bo.LoginUserBO;
import com.repl.asset.login.model.AmsLoginAccess;
import com.repl.asset.login.model.EmpDetailsMaster;
import com.repl.asset.login.repository.AmsLoginAccessRepository;
import com.repl.asset.login.repository.EmpDetailsMasterRepository;
import com.repl.asset.message.IResponseCode;
import com.repl.asset.message.IResponseMessage;

/**
 * Login is two lookups against two schemas, and both must pass:
 *
 *   db_all_master.emp_details   who the person is, and their BCrypt password  (read-only)
 *   db_asset.ams_login_access   whether they may use AMS, and as which role   (AMS owns)
 *
 * Being a valid employee is not enough. 1009 people exist in the master; only those granted
 * a row here can sign in.
 */
@Service
public class LoginServiceImpl implements ILoginService {

	private static final Logger logger = LoggerFactory.getLogger(LoginServiceImpl.class);

	@Autowired
	private EmpDetailsMasterRepository empDetailsMasterRepository;

	@Autowired
	private AmsLoginAccessRepository amsLoginAccessRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	/**
	 * Whether the password is actually compared. Local development only.
	 *
	 * emp_pass lives in db_all_master, the SHARED employee master — a test password written
	 * there is written for every application that reads it. So the way to sign in locally
	 * without knowing real passwords is to stop comparing, not to invent a password. Same
	 * reasoning as the TMS backend's login.password.verify.
	 *
	 * Defaults to true, so an environment that forgets to set it stays secure. Every
	 * sign-in logs a WARN while it is false, which puts the evidence next to each affected
	 * login rather than in a boot log nobody re-reads.
	 */
	@Value("${login.password.verify:true}")
	private boolean passwordVerifyEnabled;

	@Override
	@Transactional(readOnly = true)
	public LoginUserBO login(Long empCode, String password) throws ApplicationException {

		if (empCode == null) {
			throw new ApplicationException(IResponseCode.UNAUTHORIZED, IResponseMessage.UNAUTHORIZED_USER);
		}
		// An empty password is rejected only when passwords are actually being checked.
		if (passwordVerifyEnabled && (password == null || password.isEmpty())) {
			throw new ApplicationException(IResponseCode.UNAUTHORIZED, IResponseMessage.UNAUTHORIZED_USER);
		}

		EmpDetailsMaster emp = empDetailsMasterRepository.findByEmpCode(empCode)
				.orElseThrow(() -> new ApplicationException(IResponseCode.USER_NOT_FOUND,
						IResponseMessage.USER_NOT_FOUND));

		// emp_status 0 means the person has left. 158 of the 1009 rows are in that state.
		if (emp.getEmpStatus() == null || emp.getEmpStatus() != Constants.STATUS_ACTIVE) {
			throw new ApplicationException(IResponseCode.UNAUTHORIZED, IResponseMessage.NOTACCESS);
		}

		/*
		 * 14 master rows hold a NULL emp_pass. BCryptPasswordEncoder.matches(raw, null) throws
		 * rather than returning false, so the null is checked first and reported as a normal
		 * failed sign-in — never as a server error, which would tell an attacker the account
		 * exists but has no password set.
		 */
		if (passwordVerifyEnabled) {
			String hash = emp.getEmpPass();
			if (hash == null || hash.isEmpty() || !passwordEncoder.matches(password, hash)) {
				logger.warn("Failed AMS sign-in for emp_code {}", empCode);
				throw new ApplicationException(IResponseCode.UNAUTHORIZED, IResponseMessage.UNAUTHORIZED_USER);
			}
		} else {
			logger.warn("login.password.verify=false — emp_code {} signed in WITHOUT a password check. "
					+ "This must never be the case outside local development.", empCode);
		}

		AmsLoginAccess access = amsLoginAccessRepository
				.findByEmpCodeAndStatus(empCode, Constants.STATUS_ACTIVE)
				.orElseThrow(() -> new ApplicationException(IResponseCode.UNAUTHORIZED,
						IResponseMessage.NOTACCESS));

		return toLoginUser(emp, access);
	}

	private LoginUserBO toLoginUser(EmpDetailsMaster emp, AmsLoginAccess access) {
		LoginUserBO bo = new LoginUserBO();

		bo.setUser_id(String.valueOf(emp.getEmpCode()));
		bo.setEmp_code(emp.getEmpCode());
		bo.setUserFname(emp.getEmpFname());
		bo.setUserMname(emp.getEmpMname());
		bo.setUserLname(emp.getEmpLname());
		bo.setDisplayName(emp.getDisplayName());

		// Authority comes from the AMS access row, never from the master's own role column.
		bo.setRole(access.getRole());

		bo.setDesignation(emp.getDesigId());
		bo.setDepartment(emp.getDeptId());
		// company_email_id is mostly NULL in the master, so emp_email is the usable one.
		bo.setEmail(emp.getEmpEmail() != null ? emp.getEmpEmail() : emp.getCompanyEmailId());
		bo.setMobile(emp.getEmpMobile());
		bo.setGrade(emp.getGrade());
		bo.setCompanyName(emp.getCompanyName());

		// Their scope, as set in Reporting Person: plant and device types.
		bo.setPlant_code(access.getPlantCode());
		bo.setPlant_codes(access.getPlantCodeList());
		bo.setPlant_company(access.getCompanyName());
		bo.setDevice_ids(access.getDeviceIdList());

		return bo;
	}
}
