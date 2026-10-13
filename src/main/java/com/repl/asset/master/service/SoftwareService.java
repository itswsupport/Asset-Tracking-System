package com.repl.asset.master.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.repl.asset.constant.Constants;
import com.repl.asset.exception.ApplicationException;
import com.repl.asset.master.bo.SoftwareBO;
import com.repl.asset.master.model.SoftwareMaster;
import com.repl.asset.master.repository.SoftwareMasterRepository;
import com.repl.asset.message.IResponseCode;

/**
 * The software licence register, over ams_software_master.
 *
 * One row per licence entry — the same product may appear more than once: how many licences were bought, how many over or short, what kind they
 * are and when they renew.
 */
@Service
public class SoftwareService {

	/** The product_name column is VARCHAR(200). */
	private static final int NAME_MAX = 200;

	@Autowired
	private SoftwareMasterRepository repository;

	/** What each UPDATE changed — the USER LOG (ams_software_user_log). */
	@Autowired
	private com.repl.asset.master.repository.SoftwareChangeLogRepository changeLogRepository;

	/**
	 * Every product, active AND inactive, newest first — the row just saved belongs at the
	 * top where it can be seen.
	 */
	@Transactional(readOnly = true)
	public List<SoftwareBO> list() {
		return repository.findAll().stream()
				.filter(row -> row.getStatus() != Constants.STATUS_DELETED)
				.sorted(Comparator.comparing(SoftwareMaster::getId).reversed())
				.map(this::toBO)
				.toList();
	}

	@Transactional
	public SoftwareBO save(SoftwareBO in) throws ApplicationException {
		String name = requireProduct(in);

		/*
		 * The same product may be added more than once (e.g. licences bought at different
		 * times or of another licence type), so every save is a new row.
		 */
		SoftwareMaster row = new SoftwareMaster();
		apply(row, in, name);
		stamp(row);
		row.setAddedVia(addedVia(in));

		return toBO(repository.save(row));
	}

	@Transactional
	public SoftwareBO update(SoftwareBO in) throws ApplicationException {
		if (in == null || in.getId() == null) {
			throw new ApplicationException(IResponseCode.INVALID_DATA, "Nothing to update.");
		}
		String name = requireProduct(in);

		SoftwareMaster row = repository.findById(in.getId())
				.orElseThrow(() -> new ApplicationException(IResponseCode.INVALID_ENTITY,
						"That software no longer exists."));

		// The same product may be on more than one row, so no clash to check.
		// The USER LOG: every field as it was, then as saved — one log row per field changed.
		java.util.Map<String, String> before = snapshot(row);
		apply(row, in, name);
		SoftwareMaster saved = repository.save(row);
		changeLogRepository.saveAll(ChangeLogSupport.changes(saved.getId(), before, snapshot(saved),
				in.getChanged_by_code(), in.getChanged_by_name(), com.repl.asset.master.model.SoftwareChangeLog::new));
		return toBO(saved);
	}

	/** One software entry's USER LOG, newest first — shown below the View / Edit Software form. */
	@Transactional(readOnly = true)
	public List<com.repl.asset.master.bo.AssetChangeLogBO> changeLog(Long softwareId) throws ApplicationException {
		if (softwareId == null) {
			throw new ApplicationException(IResponseCode.INVALID_DATA, "Software is required.");
		}
		return changeLogRepository.findBySoftwareIdOrderByIdDesc(softwareId).stream()
				.map(ChangeLogSupport::toBO)
				.toList();
	}

	/**
	 * Every logged field of a software entry, labelled and shown as on the form, in form order.
	 * Blank is null, so "" and a missing value never count as a change.
	 */
	private java.util.Map<String, String> snapshot(SoftwareMaster r) {
		java.util.Map<String, String> m = new java.util.LinkedHashMap<>();
		m.put("Product Name", ChangeLogSupport.blankToNull(r.getProductName()));
		m.put("Total Licenses Acquired", r.getTotalLicensesAcquired() == null ? null : String.valueOf(r.getTotalLicensesAcquired()));
		m.put("License Excess or Deficiency", r.getLicenseExcessDeficiency() == null ? null : String.valueOf(r.getLicenseExcessDeficiency()));
		m.put("License Type", ChangeLogSupport.blankToNull(r.getLicenseType()));
		m.put("Renewal Date", r.getRenewalDate() == null ? null
				: r.getRenewalDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
		m.put("Description", ChangeLogSupport.blankToNull(r.getDescription()));
		m.put("Status", r.getStatus() == Constants.STATUS_ACTIVE ? "Active" : "Inactive");
		return m;
	}

	@Transactional
	public void delete(Long id) throws ApplicationException {
		SoftwareMaster row = repository.findById(id)
				.orElseThrow(() -> new ApplicationException(IResponseCode.INVALID_ENTITY,
						"That software no longer exists."));

		// Retired, not removed: anything already recording this product still resolves.
		row.setStatus(Constants.STATUS_DELETED);
		repository.save(row);
	}

	/**
	 * Copy the form's values onto the row.
	 *
	 * Every field but the product is written unconditionally, including when it arrives
	 * blank — this form shows all of them at once, so an empty box means "clear it".
	 */
	private void apply(SoftwareMaster row, SoftwareBO in, String name) {
		row.setProductName(name);
		row.setTotalLicensesAcquired(in.getTotal_licenses_acquired());
		/*
		 * Kept signed. A negative figure is a DEFICIENCY — the number worth acting on — so
		 * unlike the warranty term there is nothing to refuse here.
		 */
		row.setLicenseExcessDeficiency(in.getLicense_excess_deficiency());
		row.setLicenseType(trimToNull(in.getLicense_type()));
		row.setRenewalDate(in.getRenewal_date());
		row.setDescription(trimToNull(in.getDescription()));

		row.setStatus(in.getStatus() != null && in.getStatus() == Constants.STATUS_INACTIVE
				? Constants.STATUS_INACTIVE
				: Constants.STATUS_ACTIVE);
	}

	/** Stamped here, never taken from the request. */
	private void stamp(SoftwareMaster row) {
		row.setCreateDate(LocalDate.now());
		row.setCreateTime(LocalTime.now().withNano(0));
		row.setRegTime(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
	}

	/**
	 * How a NEW row is being added: "BULK" only when BULK UPLOAD says so, otherwise "FORM".
	 * Only on save — an edit never changes how a product was added.
	 */
	private static String addedVia(SoftwareBO in) {
		return in != null && "BULK".equalsIgnoreCase(in.getAdded_via()) ? "BULK" : "FORM";
	}

	private String requireProduct(SoftwareBO in) throws ApplicationException {
		String name = in == null || in.getProduct_name() == null ? "" : in.getProduct_name().trim();
		if (name.isEmpty()) {
			throw new ApplicationException(IResponseCode.INVALID_DATA, "Product name is required.");
		}
		if (name.length() > NAME_MAX) {
			throw new ApplicationException(IResponseCode.INVALID_DATA,
					"Product name is too long — " + name.length() + " characters, and the most "
							+ "allowed is " + NAME_MAX + ".");
		}
		return name;
	}

	private String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	private SoftwareBO toBO(SoftwareMaster row) {
		SoftwareBO bo = new SoftwareBO();
		bo.setId(row.getId());
		bo.setProduct_name(row.getProductName());
		bo.setTotal_licenses_acquired(row.getTotalLicensesAcquired());
		bo.setLicense_excess_deficiency(row.getLicenseExcessDeficiency());
		bo.setLicense_type(row.getLicenseType());
		bo.setRenewal_date(row.getRenewalDate());
		bo.setDescription(row.getDescription());
		bo.setCreate_date(row.getCreateDate());
		bo.setCreate_time(row.getCreateTime());
		bo.setStatus(row.getStatus());
		bo.setAdded_via(row.getAddedVia());
		return bo;
	}
}
