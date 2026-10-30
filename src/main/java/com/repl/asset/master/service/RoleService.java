package com.repl.asset.master.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.repl.asset.constant.Constants;
import com.repl.asset.exception.ApplicationException;
import com.repl.asset.login.AmsRoles;
import com.repl.asset.login.repository.AmsLoginAccessRepository;
import com.repl.asset.master.bo.RoleMasterBO;
import com.repl.asset.master.model.RoleMaster;
import com.repl.asset.master.repository.RoleMasterRepository;
import com.repl.asset.message.IResponseCode;

/**
 * The role master — db_asset.ams_role_master — and how Login Access saves the roles it gives.
 *
 * ADMIN, USER and VIEWER are built in. An admin can add more ("Super Admin", "Approval") by
 * name; each keeps an access type — ADMIN, USER or VIEWER, USER for an added role — that says
 * what it may do, since every permission in the app is written against those three. Login Access stores the roles given as their ids
 * (ams_login_access.role_id, "1,4") and their access types (ams_login_access.role,
 * "ADMIN,USER"), which is what login and the screens read.
 */
@Service
public class RoleService {

	/** The built-in roles, in the order they are listed: Admin, User, Viewer. */
	private static final List<String> BUILT_IN = List.of(AmsRoles.ADMIN, AmsRoles.USER, AmsRoles.VIEWER);

	@Autowired
	private RoleMasterRepository repository;

	@Autowired
	private AmsLoginAccessRepository amsLoginAccessRepository;

	/** Active and inactive, not deleted: the built-in three first, then added roles by name. */
	@Transactional(readOnly = true)
	public List<RoleMasterBO> list() {
		return repository.findAll().stream()
				.filter(row -> row.getStatus() != Constants.STATUS_DELETED)
				.sorted(Comparator.comparingInt((RoleMaster row) -> position(row.getRoleCode()))
						.thenComparing(RoleMaster::getRoleName, String.CASE_INSENSITIVE_ORDER))
				.map(this::toBO)
				.toList();
	}

	@Transactional
	public RoleMasterBO save(RoleMasterBO in) throws ApplicationException {
		String name = requireName(in);
		String access = requireAccessType(in == null ? null : in.getAccess_type());
		String code = codeFor(name);

		/*
		 * role_code is UNIQUE and delete only sets status = -1, so a deleted role still holds its
		 * code: adding the same name again brings that row back rather than inserting a second.
		 */
		RoleMaster existing = repository.findAll().stream()
				.filter(r -> code.equals(r.getRoleCode()) || name.equalsIgnoreCase(r.getRoleName()))
				.findFirst()
				.orElse(null);
		if (existing != null && existing.getStatus() != Constants.STATUS_DELETED) {
			throw new ApplicationException(IResponseCode.EXISTING_ENTITY,
					"\"" + existing.getRoleName() + "\" already exists.");
		}

		RoleMaster row = existing != null ? existing : new RoleMaster();
		row.setRoleCode(code);
		row.setRoleName(name);
		row.setAccessType(access);
		row.setDescription(clean(in.getDescription()));
		row.setStatus(statusOrActive(in.getStatus()));
		row.setCreateDate(LocalDate.now());
		row.setCreateTime(LocalTime.now().withNano(0));
		row.setRegTime(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
		return toBO(repository.save(row));
	}

	/** Name, description and status. role_code and access_type stay as they were created. */
	@Transactional
	public RoleMasterBO update(RoleMasterBO in) throws ApplicationException {
		if (in == null || in.getId() == null) {
			throw new ApplicationException(IResponseCode.INVALID_DATA, "Nothing to update.");
		}
		String name = requireName(in);

		RoleMaster row = repository.findById(in.getId())
				.filter(r -> r.getStatus() != Constants.STATUS_DELETED)
				.orElseThrow(() -> new ApplicationException(IResponseCode.INVALID_ENTITY,
						"That role no longer exists."));

		boolean clash = repository.findAll().stream()
				.filter(r -> !r.getId().equals(row.getId()) && r.getStatus() != Constants.STATUS_DELETED)
				.anyMatch(r -> name.equalsIgnoreCase(r.getRoleName()));
		if (clash) {
			throw new ApplicationException(IResponseCode.EXISTING_ENTITY, "\"" + name + "\" already exists.");
		}

		/*
		 * Any role may be made inactive, Admin included: it is then no longer offered in Login
		 * Access. Nobody is locked out — whoever already holds it keeps it and can still sign in.
		 */
		row.setRoleName(name);
		row.setDescription(clean(in.getDescription()));
		row.setStatus(statusOrActive(in.getStatus()));
		return toBO(repository.save(row));
	}

	/**
	 * Added roles only, and only while no one holds it: status = -1, the row is kept (Login
	 * Access rows refer to it by id).
	 */
	@Transactional
	public void delete(Long id) throws ApplicationException {
		RoleMaster row = repository.findById(id)
				.filter(r -> r.getStatus() != Constants.STATUS_DELETED)
				.orElseThrow(() -> new ApplicationException(IResponseCode.INVALID_ENTITY,
						"That role no longer exists."));
		if (BUILT_IN.contains(row.getRoleCode())) {
			throw new ApplicationException(IResponseCode.INVALID_DATA,
					"\"" + row.getRoleName() + "\" is a built-in role and cannot be deleted.");
		}
		long holders = amsLoginAccessRepository.findAll().stream()
				.filter(a -> a.getStatus() != Constants.STATUS_DELETED)
				.filter(a -> ids(a.getRoleId()).contains(row.getId()))
				.count();
		if (holders > 0) {
			throw new ApplicationException(IResponseCode.INVALID_DATA,
					"\"" + row.getRoleName() + "\" is given to " + holders + " employee(s) in Login Access. "
							+ "Change their role first, or make this role Inactive instead.");
		}
		row.setStatus(Constants.STATUS_DELETED);
		repository.save(row);
	}

	/**
	 * THE ROLES LOGIN ACCESS IS SAVING, picked by id ("1,4"). Returns { role, role_id }:
	 * role = their access types for login ("ADMIN,USER"), role_id = the ids in order ("1,4").
	 *
	 * Each must exist and be Active — except one this person already holds (`alreadyIds`, their
	 * saved role_id), so their entry can still be edited after a role was made inactive. A
	 * Viewer-type role stands alone, as Viewer always has.
	 */
	@Transactional(readOnly = true)
	public String[] resolve(String roleIds, String alreadyIds) throws ApplicationException {
		Set<Long> picked = ids(roleIds);
		if (picked.isEmpty()) {
			throw new ApplicationException(IResponseCode.INVALID_DATA, "Select a role.");
		}
		Set<Long> already = ids(alreadyIds);
		Map<Long, RoleMaster> byId = repository.findAll().stream()
				.collect(Collectors.toMap(RoleMaster::getId, r -> r));

		Set<String> access = new LinkedHashSet<>();
		boolean viewer = false;
		for (Long id : picked) {
			RoleMaster role = byId.get(id);
			if (role == null || role.getStatus() == Constants.STATUS_DELETED) {
				throw new ApplicationException(IResponseCode.INVALID_DATA, "That role no longer exists.");
			}
			if (role.getStatus() != Constants.STATUS_ACTIVE && !already.contains(id)) {
				throw new ApplicationException(IResponseCode.INVALID_DATA,
						"The role \"" + role.getRoleName() + "\" is inactive in Role Master.");
			}
			viewer |= AmsRoles.VIEWER.equals(role.getAccessType());
			access.add(role.getAccessType());
		}
		if (viewer && picked.size() > 1) {
			throw new ApplicationException(IResponseCode.INVALID_DATA,
					"A Viewer role cannot be combined with another role.");
		}
		String role = AmsRoles.normalise(String.join(",", access));
		String idText = picked.stream().sorted().map(String::valueOf).collect(Collectors.joining(","));
		return new String[] { role, idText };
	}

	/**
	 * For a request that sends role CODES only ("ADMIN,USER") and no ids: the built-in roles'
	 * ids, "1,2". Each must be Active unless already held (their saved role).
	 */
	@Transactional(readOnly = true)
	public String toRoleIds(String roles, String alreadyRoles) throws ApplicationException {
		Map<String, RoleMaster> byCode = repository.findAll().stream()
				.filter(r -> BUILT_IN.contains(r.getRoleCode()))
				.collect(Collectors.toMap(RoleMaster::getRoleCode, r -> r, (a, b) -> a));
		StringBuilder ids = new StringBuilder();
		for (String code : roles.split(",")) {
			RoleMaster role = byCode.get(code);
			if (role == null) {
				throw new ApplicationException(IResponseCode.INVALID_DATA,
						"The role \"" + code + "\" is not in Role Master.");
			}
			if (role.getStatus() != Constants.STATUS_ACTIVE && !AmsRoles.has(alreadyRoles, code)) {
				throw new ApplicationException(IResponseCode.INVALID_DATA,
						"The role \"" + role.getRoleName() + "\" is inactive in Role Master.");
			}
			ids.append(ids.length() == 0 ? "" : ",").append(role.getId());
		}
		return ids.toString();
	}

	private static String requireName(RoleMasterBO in) throws ApplicationException {
		String name = in == null || in.getRole_name() == null ? "" : in.getRole_name().trim();
		if (name.isEmpty()) {
			throw new ApplicationException(IResponseCode.INVALID_DATA, "Role name is required.");
		}
		if (name.length() > 50) {
			throw new ApplicationException(IResponseCode.INVALID_DATA, "Role name can be at most 50 characters.");
		}
		return name;
	}

	/**
	 * The Role Master screen asks only for a name, so an added role works like USER (its own
	 * plants and device types) unless a request says otherwise.
	 */
	private static String requireAccessType(String raw) throws ApplicationException {
		String access = raw == null || raw.isBlank() ? AmsRoles.USER : raw.trim().toUpperCase();
		if (!BUILT_IN.contains(access)) {
			throw new ApplicationException(IResponseCode.INVALID_DATA,
					"Select what the role may do: Admin, User or Viewer.");
		}
		return access;
	}

	/** "IT Support" -> "IT_SUPPORT", at most 20 characters (the column's size). */
	private static String codeFor(String name) throws ApplicationException {
		String code = name.toUpperCase().replaceAll("[^A-Z0-9]+", "_").replaceAll("^_+|_+$", "");
		if (code.isEmpty()) {
			throw new ApplicationException(IResponseCode.INVALID_DATA,
					"Role name needs at least one letter or number.");
		}
		return code.length() > 20 ? code.substring(0, 20) : code;
	}

	/** "1, 4,4" -> [1, 4]: the ids in a comma-separated value, numbers only. */
	private static Set<Long> ids(String raw) {
		if (raw == null || raw.isBlank()) {
			return Set.of();
		}
		return Arrays.stream(raw.split(","))
				.map(String::trim)
				.filter(s -> s.matches("\\d+"))
				.map(Long::valueOf)
				.collect(Collectors.toCollection(LinkedHashSet::new));
	}

	private static String clean(String s) {
		return s == null || s.isBlank() ? null : s.trim();
	}

	/** Only 1 or 0 may arrive from a form; -1 is reserved for delete. */
	private static int statusOrActive(Integer status) {
		return status != null && status == Constants.STATUS_INACTIVE
				? Constants.STATUS_INACTIVE
				: Constants.STATUS_ACTIVE;
	}

	private static int position(String code) {
		int i = BUILT_IN.indexOf(code);
		return i < 0 ? BUILT_IN.size() : i;
	}

	private RoleMasterBO toBO(RoleMaster row) {
		RoleMasterBO bo = new RoleMasterBO();
		bo.setId(row.getId());
		bo.setRole_code(row.getRoleCode());
		bo.setRole_name(row.getRoleName());
		bo.setAccess_type(row.getAccessType());
		bo.setBuilt_in(BUILT_IN.contains(row.getRoleCode()));
		bo.setDescription(row.getDescription());
		bo.setCreate_date(row.getCreateDate());
		bo.setStatus(row.getStatus());
		return bo;
	}
}
