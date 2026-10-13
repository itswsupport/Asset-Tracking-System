package com.repl.asset.master.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import com.repl.asset.constant.Constants;
import com.repl.asset.exception.ApplicationException;
import com.repl.asset.master.bo.SpecBO;
import com.repl.asset.master.model.SpecEntity;
import com.repl.asset.message.IResponseCode;

/**
 * The CRUD each hardware spec master performs on its OWN table.
 *
 * RamService, ProcessorService, HddService and OsService are four separate services over
 * four separate entities and repositories, each with its own endpoints. This base class
 * holds only the steps that are word-for-word identical between them — the duplicate check,
 * the status clamping, retire-instead-of-delete — so a fix to any of those is a fix in all
 * four rather than one fixed and three forgotten.
 *
 * Each subclass supplies its own repository and its own entity, so nothing here can write
 * into a table other than the one that service owns.
 *
 * @param <T> the concrete entity — RamMaster, ProcessorMaster, HddMaster or OsMaster
 */
public abstract class AbstractSpecService<T extends SpecEntity> {

	/** The repository for this master's table, and nothing else. */
	protected abstract JpaRepository<T, Long> repository();

	/** A new, empty row of this master's entity. */
	protected abstract T newRow();

	/** What this master is called in an error message, e.g. "RAM". */
	protected abstract String label();

	/** Active AND inactive, everything but deleted — the master screen needs both. */
	@Transactional(readOnly = true)
	public List<SpecBO> list() {
		return repository().findAll().stream()
				.filter(row -> row.getStatus() != Constants.STATUS_DELETED)
				.sorted(Comparator.comparing(SpecEntity::getName, String.CASE_INSENSITIVE_ORDER))
				.map(this::toBO)
				.toList();
	}

	@Transactional
	public SpecBO save(SpecBO in) throws ApplicationException {
		String name = in == null || in.getName() == null ? "" : in.getName().trim();
		if (name.isEmpty()) {
			throw new ApplicationException(IResponseCode.INVALID_DATA, label() + " is required.");
		}
		/*
		 * Every one of these tables has a UNIQUE key on its name column, and delete only sets
		 * status = -1, so a deleted "16 GB" still holds that name. Re-adding it therefore has
		 * to revive the row: inserting a second one violates the key.
		 *
		 * That was a real failure, not a theoretical one — requireUnique skips deleted rows,
		 * so the check passed and the INSERT then died on the constraint, surfacing as a 501
		 * Server Error. Deleting a value made it impossible to add back.
		 */
		T existing = findByNameIncludingDeleted(name);

		if (existing != null) {
			if (existing.getStatus() != Constants.STATUS_DELETED) {
				throw new ApplicationException(IResponseCode.EXISTING_ENTITY,
						"\"" + name + "\" already exists.");
			}

			// Re-typed casing wins, so re-adding "16 gb" as "16 GB" corrects the stored value.
			existing.setName(name);
			existing.setStatus(statusOrActive(in.getStatus()));
			existing.setCreateDate(LocalDate.now());
			existing.setDescription(in.getDescription());
			existing.setSystemType(in.getSystem_type());
			existing.setCreateTime(LocalTime.now().withNano(0));
			return toBO(repository().save(existing));
		}

		T row = newRow();
		row.setName(name);
		// Stamped here, never taken from the request — the caller does not get to decide when
		// a row was created. withNano(0) because the column is TIME, which stores whole
		// seconds; leaving the nanos on would only be truncated on the way down.
		row.setCreateDate(LocalDate.now());
		row.setCreateTime(LocalTime.now().withNano(0));
		/*
		 * reg_time alongside the create stamps. reg_by is NOT set: it should be the emp_code of
		 * whoever saved, and the backend has no idea who that is — every endpoint is permitAll
		 * and no token is carried, so anything written here would be a guess or whatever the
		 * browser chose to claim. It stays null until real authentication exists.
		 */
		row.setRegTime(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
		row.setDescription(in.getDescription());
		row.setSystemType(in.getSystem_type());
		row.setStatus(statusOrActive(in.getStatus()));

		return toBO(repository().save(row));
	}

	/**
	 * The row holding this name, deleted or not, or null.
	 *
	 * findAll rather than a derived query because the name column differs per master —
	 * ram_name, processor_name, hdd_name, os_name — so there is no single property name a
	 * shared query could use. These lists are tens of rows, not thousands.
	 */
	private T findByNameIncludingDeleted(String name) {
		return repository().findAll().stream()
				.filter(row -> name.equalsIgnoreCase(row.getName()))
				.findFirst()
				.orElse(null);
	}

	@Transactional
	public SpecBO update(SpecBO in) throws ApplicationException {
		if (in == null || in.getId() == null) {
			throw new ApplicationException(IResponseCode.INVALID_DATA, "Nothing to update.");
		}
		String name = in.getName() == null ? "" : in.getName().trim();
		if (name.isEmpty()) {
			throw new ApplicationException(IResponseCode.INVALID_DATA, label() + " is required.");
		}

		T row = repository().findById(in.getId())
				.orElseThrow(() -> new ApplicationException(IResponseCode.INVALID_ENTITY,
						"That row no longer exists."));

		// Ignores the row being edited, so re-saving an unchanged name is allowed.
		requireUnique(name, in.getId());

		row.setName(name);
		if (in.getStatus() != null) {
		if (in.getSystem_type() != null) {
			row.setSystemType(in.getSystem_type());
		}
		if (in.getDescription() != null) {
			row.setDescription(in.getDescription());
		}
			row.setStatus(statusOrActive(in.getStatus()));
		}
		return toBO(repository().save(row));
	}

	@Transactional
	public void delete(Long id) throws ApplicationException {
		T row = repository().findById(id)
				.orElseThrow(() -> new ApplicationException(IResponseCode.INVALID_ENTITY,
						"That row no longer exists."));

		// Retired, not removed: assets will reference these values, and a hard delete would
		// leave those pointing at nothing. Every list filters status != -1.
		row.setStatus(Constants.STATUS_DELETED);
		repository().save(row);
	}

	/**
	 * Case-insensitive, and it skips deleted rows so a name can be reused after deletion.
	 * The table's UNIQUE key is the real guarantee; this exists so the caller gets a readable
	 * message instead of a constraint violation surfacing as a 500.
	 */
	private void requireUnique(String name, Long ignoreId) throws ApplicationException {
		boolean clash = repository().findAll().stream()
				.filter(row -> row.getStatus() != Constants.STATUS_DELETED)
				.filter(row -> ignoreId == null || !ignoreId.equals(row.getId()))
				.anyMatch(row -> name.equalsIgnoreCase(row.getName()));

		if (clash) {
			throw new ApplicationException(IResponseCode.EXISTING_ENTITY, "\"" + name + "\" already exists.");
		}
	}

	/** Only 1 or 0 may arrive from a form; -1 is reserved for delete. */
	private int statusOrActive(Integer status) {
		return status != null && status == Constants.STATUS_INACTIVE
				? Constants.STATUS_INACTIVE
				: Constants.STATUS_ACTIVE;
	}

	private SpecBO toBO(SpecEntity row) {
		SpecBO bo = new SpecBO();
		bo.setId(row.getId());
		bo.setName(row.getName());
		bo.setCreate_date(row.getCreateDate());
		bo.setCreate_time(row.getCreateTime());
		// Null for RAM/Processor/HDD/OS, which have no system type; NON_NULL omits it there.
		bo.setDescription(row.getDescription());
		bo.setSystem_type(row.getSystemType());
		bo.setWarranty_status(row.getWarrantyStatus());
		bo.setLast_date_of_warranty(row.getLastDateOfWarranty());
		bo.setTotal_warranty_years(row.getTotalWarrantyYears());
		bo.setStatus(row.getStatus());
		return bo;
	}
}
