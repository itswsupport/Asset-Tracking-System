package com.repl.asset.master.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.repl.asset.constant.Constants;
import com.repl.asset.exception.ApplicationException;
import com.repl.asset.master.bo.CategoryBO;
import com.repl.asset.login.AmsRoles;
import com.repl.asset.login.model.AmsLoginAccess;
import com.repl.asset.login.model.EmpDetailsMaster;
import com.repl.asset.login.repository.AmsLoginAccessRepository;
import com.repl.asset.login.repository.EmpDetailsMasterRepository;
import com.repl.asset.master.bo.CompanyBO;
import com.repl.asset.master.bo.DepartmentBO;
import com.repl.asset.master.bo.EmployeeBO;
import com.repl.asset.master.bo.ReportingPersonBO;
import com.repl.asset.master.bo.PlantCategoryBO;
import com.repl.asset.master.bo.SubCategoryBO;
import com.repl.asset.master.model.CategoryMaster;
import com.repl.asset.master.model.CompanyMaster;
import com.repl.asset.master.model.DeviceMaster;
import com.repl.asset.master.model.PlantCategory;
import com.repl.asset.master.model.PlantMaster;
import com.repl.asset.master.model.SubCategoryMaster;
import com.repl.asset.master.repository.CategoryMasterRepository;
import com.repl.asset.master.repository.CompanyMasterRepository;
import com.repl.asset.master.repository.DeviceMasterRepository;
import com.repl.asset.master.repository.PlantCategoryRepository;
import com.repl.asset.master.repository.PlantDeptMasterRepository;
import com.repl.asset.master.repository.PlantMasterRepository;
import com.repl.asset.master.repository.SubCategoryMasterRepository;
import com.repl.asset.message.IResponseCode;

@Service
public class MasterServiceImpl implements IMasterService {

	@Autowired
	private CompanyMasterRepository companyMasterRepository;

	@Autowired
	private PlantDeptMasterRepository plantDeptMasterRepository;

	@Autowired
	private PlantMasterRepository plantMasterRepository;

	@Autowired
	private CategoryMasterRepository categoryMasterRepository;

	@Autowired
	private PlantCategoryRepository plantCategoryRepository;

	@Autowired
	private SubCategoryMasterRepository subCategoryMasterRepository;

	// From the login package: the employee master (read-only) and the AMS access list, which
	// saving a reporting person also writes to.
	@Autowired
	private EmpDetailsMasterRepository empDetailsMasterRepository;

	@Autowired
	private AmsLoginAccessRepository amsLoginAccessRepository;

	/** Device types for Reporting Person: checked on save, named in the list. */
	@Autowired
	private DeviceMasterRepository deviceMasterRepository;

	@Autowired
	private RoleService roleService;

	@Override
	@Transactional(readOnly = true)
	public List<CompanyBO> getCompanyList() {
		return companyMasterRepository
				.findByStatusOrderByCompanyNameAscPlantCodeAsc(Constants.STATUS_ACTIVE)
				.stream()
				.map(this::toCompanyBO)
				.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public List<DepartmentBO> getDepartmentList() {
		/*
		 * company -> plant -> department. plant_dept_mst holds the company only as company_id,
		 * which plant_mst names (1 = RUCHA ENGINEERS PVT LTD …) — one pass, not a query per row.
		 * The plant is keyed by plant_code: plant_id is not reliable on every row.
		 */
		Map<Long, String> companyNames = plantMasterRepository.findAll().stream()
				.filter(p -> p.getCompanyId() != null && p.getCompanyName() != null)
				.collect(Collectors.toMap(PlantMaster::getCompanyId, p -> p.getCompanyName().trim(),
						(a, b) -> a));

		return plantDeptMasterRepository.findAllByOrderByCompanyIdAscPlantCodeAscDepartmentAsc()
				.stream()
				.map(entity -> {
					DepartmentBO bo = new DepartmentBO();
					bo.setId(entity.getId());
					bo.setDepartment(entity.getDepartment());
					bo.setDept_short_name(entity.getDeptShortName());
					bo.setPlant_name(entity.getPlantName());
					bo.setPlant_code(entity.getPlantCode());
					bo.setCompany_id(entity.getCompanyId());
					bo.setCompany_name(companyNames.get(entity.getCompanyId()));
					return bo;
				})
				.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public List<CategoryBO> getCategoryList() {
		return categoryMasterRepository
				.findByStatusOrderByCategoryNameAsc(Constants.STATUS_ACTIVE)
				.stream()
				.map(this::toCategoryBO)
				.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public List<PlantCategoryBO> getPlantCategoryList() {
		/*
		 * Active AND inactive, everything but deleted. An inactive row still belongs on the
		 * master screen — hiding it there would leave no way to switch it back on. It is the
		 * screens that USE categories which ask for status = 1.
		 */
		List<PlantCategory> rows = plantCategoryRepository
				.findByStatusNotOrderByCompanyNameAscPlantCodeAsc(Constants.STATUS_DELETED);

		/*
		 * The category names are resolved in one pass rather than per row — a findById inside
		 * the loop would be one query per saved row.
		 */
		Map<Long, String> namesById = categoryMasterRepository.findAll().stream()
				.collect(Collectors.toMap(CategoryMaster::getId, CategoryMaster::getCategoryName,
						(a, b) -> a));

		return rows.stream().map(row -> {
			PlantCategoryBO bo = toPlantCategoryBO(row);
			bo.setCategory_name(namesById.get(row.getCategoryId()));
			return bo;
		}).toList();
	}

	/**
	 * The category the caller named, creating it if this is the first time it has been used.
	 *
	 * The Category Master form types a category name rather than choosing from a list, because
	 * there was no other way to create one — ams_category_mst had no save endpoint and no
	 * screen, so the three seeded rows were the entire list forever. Typing a new name here is
	 * what adds a category to the system.
	 *
	 * Matching is case-insensitive so "it", "IT" and "It" are the same category rather than
	 * three; uk_ams_category_name would reject the duplicate anyway, but with a constraint
	 * violation instead of the obvious behaviour.
	 *
	 * A category that was deleted is revived rather than re-inserted, for the same reason as
	 * everywhere else: the UNIQUE key means its name is still taken.
	 *
	 * category_id still wins when it is supplied, so any caller that already knows the id
	 * keeps working unchanged.
	 */
	private CategoryMaster resolveOrCreateCategory(Long categoryId, String categoryName)
			throws ApplicationException {

		if (categoryId != null) {
			return categoryMasterRepository.findById(categoryId)
					.orElseThrow(() -> new ApplicationException(IResponseCode.INVALID_ENTITY,
							"That category does not exist."));
		}

		String name = categoryName == null ? "" : categoryName.trim();
		if (name.isEmpty()) {
			throw new ApplicationException(IResponseCode.INVALID_DATA, "Category is required.");
		}

		Optional<CategoryMaster> existing = categoryMasterRepository.findByCategoryNameIgnoreCase(name);

		if (existing.isPresent()) {
			CategoryMaster found = existing.get();
			if (found.getStatus() == Constants.STATUS_DELETED) {
				found.setStatus(Constants.STATUS_ACTIVE);
				found.setCreateDate(LocalDate.now());
				found.setCreateTime(LocalTime.now().withNano(0));
				return categoryMasterRepository.save(found);
			}
			// Existing spelling wins, so typing "it" maps onto the existing "IT" rather than
			// renaming it and changing how it reads everywhere else it is already used.
			return found;
		}

		CategoryMaster fresh = new CategoryMaster();
		fresh.setCategoryName(name);
		fresh.setStatus(Constants.STATUS_ACTIVE);
		fresh.setRegDate(LocalDate.now().toString());
		fresh.setRegTime(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
		fresh.setCreateDate(LocalDate.now());
		fresh.setCreateTime(LocalTime.now().withNano(0));
		return categoryMasterRepository.save(fresh);
	}

	@Override
	@Transactional
	public PlantCategoryBO savePlantCategory(PlantCategoryBO in) throws ApplicationException {

		if (in == null
				|| (in.getCategory_id() == null
						&& (in.getCategory_name() == null || in.getCategory_name().isBlank()))
				|| in.getCompany_name() == null || in.getCompany_name().isBlank()
				|| in.getPlant_code() == null || in.getPlant_code().isBlank()) {
			throw new ApplicationException(IResponseCode.INVALID_DATA,
					"Company, plant and category are all required.");
		}

		CategoryMaster category = resolveOrCreateCategory(in.getCategory_id(), in.getCategory_name());

		int wanted = in.getStatus() != null && in.getStatus() == Constants.STATUS_INACTIVE
				? Constants.STATUS_INACTIVE
				: Constants.STATUS_ACTIVE;

		/*
		 * A deleted row still holds the plant + category pair, because uk_plant_category is
		 * UNIQUE on it. So "add HR to plant 3001" after HR was deleted from 3001 has to revive
		 * that row — inserting a second one is impossible, and refusing is wrong because the
		 * screen shows nothing there.
		 *
		 * Before this, the check could not tell a deleted row from a live one and answered
		 * "already added" for a combination the user could not see anywhere: deleting a
		 * category from a plant made it impossible to add back.
		 */
		Optional<PlantCategory> existing = plantCategoryRepository
				.findByPlantCodeAndCategoryId(in.getPlant_code(), category.getId());

		if (existing.isPresent()) {
			PlantCategory found = existing.get();

			if (found.getStatus() != Constants.STATUS_DELETED) {
				// Names it rather than saying "that category": the client shows this verbatim,
				// and by the time it arrives the form may have been changed.
				throw new ApplicationException(IResponseCode.EXISTING_ENTITY,
						"\"" + category.getCategoryName() + "\" is already added for plant "
								+ in.getPlant_code() + ". A category can only be added once per plant.");
			}

			// Revived. The company and plant names are refreshed in case the master has
			// changed since, and the stamps are reset because this is a new addition as far
			// as the user is concerned — the old ones described a row that no longer existed.
			found.setCompanyName(in.getCompany_name());
			found.setPlantName(in.getPlant_name());
			found.setDescription(in.getDescription());
			found.setStatus(wanted);
			found.setCreateDate(LocalDate.now());
			found.setCreateTime(LocalTime.now().withNano(0));

			PlantCategoryBO revived = toPlantCategoryBO(plantCategoryRepository.save(found));
			revived.setCategory_name(category.getCategoryName());
			return revived;
		}

		PlantCategory row = new PlantCategory();
		row.setCompanyName(in.getCompany_name());
		row.setPlantCode(in.getPlant_code());
		row.setPlantName(in.getPlant_name());
		row.setDescription(in.getDescription());
		row.setCategoryId(category.getId());
		// Active unless the form says otherwise. -1 is reserved for delete and must never
		// arrive through a save, or a row would be created already deleted.
		row.setStatus(in.getStatus() != null && in.getStatus() == Constants.STATUS_INACTIVE
				? Constants.STATUS_INACTIVE
				: Constants.STATUS_ACTIVE);
		row.setRegDate(LocalDate.now().toString());
		row.setRegTime(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
		// The real DATE/TIME columns, stamped here and never taken from the request.
		row.setCreateDate(LocalDate.now());
		row.setCreateTime(LocalTime.now().withNano(0));

		PlantCategory saved = plantCategoryRepository.save(row);

		PlantCategoryBO out = toPlantCategoryBO(saved);
		out.setCategory_name(category.getCategoryName());
		return out;
	}

	/**
	 * The categories the Category Master screen is currently showing.
	 *
	 * THIS IS THE ONE DEFINITION OF "A CATEGORY EXISTS" for everything downstream. A category
	 * counts as existing once it has been added on that screen for some company and plant,
	 * and stops existing when the last such row is deleted.
	 *
	 * Status is deliberately ignored: active AND inactive both count, because switching a
	 * category off is not the same as removing it — its sub categories must stay visible so
	 * they come back with it. Only STATUS_DELETED takes a category out of circulation.
	 *
	 * Before this existed, the Category Master screen read ams_plant_category while the Sub
	 * Category screen read ams_category_mst, so the two could disagree: a category whose only
	 * plant row had been deleted vanished from one screen and stayed on the other.
	 */
	private Set<Long> mappedCategoryIds() {
		return plantCategoryRepository
				.findByStatusNotOrderByCompanyNameAscPlantCodeAsc(Constants.STATUS_DELETED)
				.stream()
				.map(PlantCategory::getCategoryId)
				.collect(Collectors.toSet());
	}

	@Override
	@Transactional(readOnly = true)
	public List<SubCategoryBO> getSubCategoryList() {
		// One pass for the names, rather than a findById per row.
		Map<Long, String> namesById = categoryMasterRepository.findAll().stream()
				.collect(Collectors.toMap(CategoryMaster::getId, CategoryMaster::getCategoryName,
						(a, b) -> a));

		/*
		 * Only sub categories whose category is still on the Category Master screen.
		 *
		 * Nothing is deleted to achieve this — the filter is applied on every read, so
		 * re-adding the category on that screen brings its sub categories straight back.
		 */
		Set<Long> mapped = mappedCategoryIds();

		// Active AND inactive, everything but deleted — an inactive row still belongs on the
		// master screen, or there is no way to switch it back on.
		return subCategoryMasterRepository
				.findByStatusNotOrderBySubCategoryNameAsc(Constants.STATUS_DELETED)
				.stream()
				.filter(row -> mapped.contains(row.getCategoryId()))
				.map(row -> {
					SubCategoryBO bo = toSubCategoryBO(row);
					bo.setCategory_name(namesById.get(row.getCategoryId()));
					return bo;
				})
				.toList();
	}

	@Override
	@Transactional
	public SubCategoryBO saveSubCategory(SubCategoryBO in) throws ApplicationException {

		if (in == null || in.getCategory_id() == null
				|| in.getSub_category_name() == null || in.getSub_category_name().isBlank()) {
			throw new ApplicationException(IResponseCode.INVALID_DATA,
					"Category and sub category name are both required.");
		}

		String name = in.getSub_category_name().trim();

		CategoryMaster category = categoryMasterRepository.findById(in.getCategory_id())
				.orElseThrow(() -> new ApplicationException(IResponseCode.INVALID_ENTITY,
						"That category does not exist."));

		/*
		 * The same rule the listing applies, enforced on the way in as well. Without it the
		 * API would still accept a sub category under a category the Category Master screen
		 * does not show, and the row would be saved and then immediately filtered out of every
		 * listing — saved successfully, invisible forever.
		 */
		if (!mappedCategoryIds().contains(category.getId())) {
			throw new ApplicationException(IResponseCode.INVALID_DATA,
					"\"" + category.getCategoryName() + "\" is not added in Category Master yet. "
							+ "Add it there for a company and plant first.");
		}

		int wantedStatus = in.getStatus() != null && in.getStatus() == Constants.STATUS_INACTIVE
				? Constants.STATUS_INACTIVE
				: Constants.STATUS_ACTIVE;

		/*
		 * Same revive-don't-insert rule as savePlantCategory. uk_sub_category is UNIQUE on
		 * (category_id, sub_category_name), so a deleted "Laptop Dell" under IT still holds
		 * that name and re-adding it has to bring the row back.
		 */
		Optional<SubCategoryMaster> existingSub = subCategoryMasterRepository
				.findByCategoryIdAndSubCategoryNameIgnoreCase(category.getId(), name);

		if (existingSub.isPresent()) {
			SubCategoryMaster found = existingSub.get();

			if (found.getStatus() != Constants.STATUS_DELETED) {
				throw new ApplicationException(IResponseCode.EXISTING_ENTITY,
						"\"" + found.getSubCategoryName() + "\" already exists under "
								+ category.getCategoryName() + ".");
			}

			// Re-typed casing wins, so re-adding "laptop dell" as "Laptop Dell" corrects it.
			found.setSubCategoryName(name);
			found.setDescription(in.getDescription());
			found.setStatus(wantedStatus);
			found.setCreateDate(LocalDate.now());
			found.setCreateTime(LocalTime.now().withNano(0));

			SubCategoryBO revived = toSubCategoryBO(subCategoryMasterRepository.save(found));
			revived.setCategory_name(category.getCategoryName());
			return revived;
		}

		SubCategoryMaster row = new SubCategoryMaster();
		row.setCategoryId(category.getId());
		row.setSubCategoryName(name);
		row.setDescription(in.getDescription());
		// Active unless the form says otherwise. -1 is reserved for delete and must never
		// arrive through a save, or a row would be created already deleted.
		row.setStatus(in.getStatus() != null && in.getStatus() == Constants.STATUS_INACTIVE
				? Constants.STATUS_INACTIVE
				: Constants.STATUS_ACTIVE);
		row.setRegDate(LocalDate.now().toString());
		row.setRegTime(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
		// The real DATE/TIME columns, stamped here and never taken from the request.
		row.setCreateDate(LocalDate.now());
		row.setCreateTime(LocalTime.now().withNano(0));

		SubCategoryMaster saved = subCategoryMasterRepository.save(row);

		SubCategoryBO out = toSubCategoryBO(saved);
		out.setCategory_name(category.getCategoryName());
		return out;
	}

	@Override
	@Transactional
	public PlantCategoryBO updatePlantCategory(PlantCategoryBO in) throws ApplicationException {

		if (in == null || in.getId() == null) {
			throw new ApplicationException(IResponseCode.INVALID_DATA, "Nothing to update.");
		}

		PlantCategory row = plantCategoryRepository.findById(in.getId())
				.orElseThrow(() -> new ApplicationException(IResponseCode.INVALID_ENTITY,
						"That row no longer exists."));

		// Same rule as the save: the edit form types a name, so editing a row onto a category
		// that does not exist yet creates it rather than failing.
		CategoryMaster category = resolveOrCreateCategory(in.getCategory_id(), in.getCategory_name());

		/*
		 * The duplicate check has to ignore the row being edited — otherwise saving a row
		 * without changing plant or category would collide with itself.
		 */
		boolean movedElsewhere = !row.getPlantCode().equals(in.getPlant_code())
				|| !row.getCategoryId().equals(category.getId());

		if (movedElsewhere) {
			/*
			 * Deleted rows block an update too, unlike a save. The save can revive the row
			 * holding the pair; an update cannot, because it would then be two rows wanting
			 * the same pair and uk_plant_category forbids that. The message says which case
			 * it is, so the user is not told something is already added when the screen shows
			 * nothing there.
			 */
			Optional<PlantCategory> holder = plantCategoryRepository
					.findByPlantCodeAndCategoryId(in.getPlant_code(), category.getId());

			if (holder.isPresent() && !holder.get().getId().equals(row.getId())) {
				throw new ApplicationException(IResponseCode.EXISTING_ENTITY,
						holder.get().getStatus() == Constants.STATUS_DELETED
								? "That category was previously added for this plant and deleted. "
										+ "Add it again from ADD rather than moving this row onto it."
								: "That category is already added for this plant.");
			}
		}

		row.setCompanyName(in.getCompany_name());
		row.setPlantCode(in.getPlant_code());
		row.setPlantName(in.getPlant_name());
		row.setCategoryId(category.getId());
		if (in.getDescription() != null) {
			row.setDescription(in.getDescription());
		}
		// Same guard as save: only 1 or 0 can come from the form. Leaving the existing value
		// when nothing is sent means an update cannot silently reactivate a disabled row.
		if (in.getStatus() != null) {
			row.setStatus(in.getStatus() == Constants.STATUS_INACTIVE
					? Constants.STATUS_INACTIVE
					: Constants.STATUS_ACTIVE);
		}

		PlantCategory saved = plantCategoryRepository.save(row);

		PlantCategoryBO out = toPlantCategoryBO(saved);
		out.setCategory_name(category.getCategoryName());
		return out;
	}

	@Override
	@Transactional
	public void deletePlantCategory(Long id) throws ApplicationException {
		PlantCategory row = plantCategoryRepository.findById(id)
				.orElseThrow(() -> new ApplicationException(IResponseCode.INVALID_ENTITY,
						"That row no longer exists."));

		/*
		 * status = -1, not removed. Assets will reference these rows, and deleting one
		 * outright would leave those pointing at nothing. The list endpoints filter on
		 * status = 1, so it disappears from the screen either way.
		 */
		row.setStatus(Constants.STATUS_DELETED);
		plantCategoryRepository.save(row);
	}

	@Override
	@Transactional
	public SubCategoryBO updateSubCategory(SubCategoryBO in) throws ApplicationException {

		if (in == null || in.getId() == null
				|| in.getSub_category_name() == null || in.getSub_category_name().isBlank()) {
			throw new ApplicationException(IResponseCode.INVALID_DATA,
					"Category and sub category name are both required.");
		}

		String name = in.getSub_category_name().trim();

		SubCategoryMaster row = subCategoryMasterRepository.findById(in.getId())
				.orElseThrow(() -> new ApplicationException(IResponseCode.INVALID_ENTITY,
						"That sub category no longer exists."));

		CategoryMaster category = categoryMasterRepository.findById(in.getCategory_id())
				.orElseThrow(() -> new ApplicationException(IResponseCode.INVALID_ENTITY,
						"That category does not exist."));

		// Ignores the row being edited, so re-saving an unchanged name is allowed.
		boolean changed = !row.getCategoryId().equals(category.getId())
				|| !row.getSubCategoryName().equalsIgnoreCase(name);

		if (changed) {
			// Same reasoning as updatePlantCategory: a deleted row still holds the name, and
			// an update cannot revive it without ending up with two rows on one UNIQUE pair.
			Optional<SubCategoryMaster> holder = subCategoryMasterRepository
					.findByCategoryIdAndSubCategoryNameIgnoreCase(category.getId(), name);

			if (holder.isPresent() && !holder.get().getId().equals(row.getId())) {
				throw new ApplicationException(IResponseCode.EXISTING_ENTITY,
						holder.get().getStatus() == Constants.STATUS_DELETED
								? "\"" + name + "\" was previously used under this category and "
										+ "deleted. Add it again from ADD rather than renaming this row onto it."
								: "That sub category already exists under this category.");
			}
		}

		row.setCategoryId(category.getId());
		row.setSubCategoryName(name);
		// Only 1 or 0 can come from the form; leaving it alone when nothing is sent means an
		// update cannot silently reactivate a disabled row.
		if (in.getStatus() != null) {
			row.setStatus(in.getStatus() == Constants.STATUS_INACTIVE
					? Constants.STATUS_INACTIVE
					: Constants.STATUS_ACTIVE);
		}

		SubCategoryMaster saved = subCategoryMasterRepository.save(row);

		SubCategoryBO out = toSubCategoryBO(saved);
		out.setCategory_name(category.getCategoryName());
		return out;
	}

	@Override
	@Transactional
	public void deleteSubCategory(Long id) throws ApplicationException {
		SubCategoryMaster row = subCategoryMasterRepository.findById(id)
				.orElseThrow(() -> new ApplicationException(IResponseCode.INVALID_ENTITY,
						"That sub category no longer exists."));

		// status = -1, not removed — same reasoning as deletePlantCategory.
		row.setStatus(Constants.STATUS_DELETED);
		subCategoryMasterRepository.save(row);
	}

	@Override
	@Transactional(readOnly = true)
	public List<EmployeeBO> getEmployeeList() {
		return empDetailsMasterRepository
				.findByEmpStatusOrderByEmpFnameAsc(Constants.STATUS_ACTIVE)
				.stream()
				.map(e -> {
					EmployeeBO bo = new EmployeeBO();
					bo.setEmp_code(e.getEmpCode());
					// filter(Boolean)-equivalent: emp_lname is null on some rows, and a plain
					// concatenation would leave a trailing space.
					String first = e.getEmpFname() == null ? "" : e.getEmpFname().trim();
					String last = e.getEmpLname() == null ? "" : e.getEmpLname().trim();
					bo.setEmp_name((first + " " + last).trim());
					bo.setDesignation(e.getDesigId());
					bo.setDepartment(e.getDeptId());
					return bo;
				})
				.toList();
	}

	/*
	 * There is no separate reporting-person table any more: the assignment lives on the
	 * ams_login_access row itself. One table, and setting a role IS granting access rather
	 * than a second write that could drift out of step with the first.
	 *
	 * The trade-off, which the UNIQUE key on emp_code enforces: one row per employee, so one
	 * assignment per employee. Login depends on that uniqueness — it looks up a single row
	 * per emp_code — so it cannot be relaxed without changing how sign-in works.
	 */
	@Override
	@Transactional(readOnly = true)
	public List<ReportingPersonBO> getReportingPersonList() {
		// Device type names in one pass, for the DEVICE TYPE column ("Laptop, Desktop").
		Map<Long, String> deviceNames = deviceTypeNames();

		// Active AND inactive. An inactive person keeps their row here so access can be
		// restored; login requires status = 1, so they cannot sign in meanwhile.
		List<AmsLoginAccess> accessRows = amsLoginAccessRepository
				.findByStatusNotOrderByCompanyNameAscPlantCodeAsc(Constants.STATUS_DELETED);

		/*
		 * Names come from the employee master, not from the emp_name column on the row.
		 *
		 * That column is only a snapshot taken when the row was saved, and it is blank on any
		 * row created before it existed — a row inserted by hand, for instance — which is why
		 * the EMPLOYEE cell showed empty for those. Reading the master makes the listing
		 * self-healing and also picks up a name HR has corrected since.
		 *
		 * One query for all of them (findByEmpCodeIn) rather than a lookup per row.
		 */
		Set<Long> empCodes = accessRows.stream()
				.map(AmsLoginAccess::getEmpCode)
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());

		Map<Long, String> empNames = empCodes.isEmpty()
				? Map.of()
				: empDetailsMasterRepository.findByEmpCodeIn(empCodes).stream()
						.collect(Collectors.toMap(EmpDetailsMaster::getEmpCode, e -> {
							String f = e.getEmpFname() == null ? "" : e.getEmpFname().trim();
							String l = e.getEmpLname() == null ? "" : e.getEmpLname().trim();
							return (f + " " + l).trim();
						}, (a, b) -> a));

		return accessRows.stream()
				.map(row -> {
					ReportingPersonBO bo = toReportingPersonBO(row);
					bo.setDevice_names(joinDeviceNames(row.getDeviceIdList(), deviceNames));

					// Master first; the stored snapshot only as a fallback for an employee
					// who has since been removed from the master entirely.
					String fromMaster = empNames.get(row.getEmpCode());
					bo.setEmp_name(fromMaster != null && !fromMaster.isBlank()
							? fromMaster
							: row.getEmpName());
					return bo;
				})
				.toList();
	}

	/** Device Master id -> name ("laptop"), every row, for showing a person's device types. */
	private Map<Long, String> deviceTypeNames() {
		return deviceMasterRepository.findAll().stream()
				.collect(Collectors.toMap(DeviceMaster::getId, DeviceMaster::getDeviceName, (a, b) -> a));
	}

	/** "Laptop, Desktop" — the names of these ids, in the order given, capitalised. */
	private static String joinDeviceNames(List<Long> ids, Map<Long, String> names) {
		return ids.stream()
				.map(names::get)
				.filter(Objects::nonNull)
				.map(n -> n.isEmpty() ? n : Character.toUpperCase(n.charAt(0)) + n.substring(1))
				.collect(Collectors.joining(", "));
	}

	/**
	 * The device types picked on the Reporting Person form — one or several, each an active
	 * device in Device Master. Returned as the comma-separated ids that device_ids stores.
	 */
	private String requireDeviceTypes(List<Long> ids) throws ApplicationException {
		List<Long> picked = ids == null ? List.of()
				: ids.stream().filter(Objects::nonNull).distinct().toList();
		if (picked.isEmpty()) {
			throw new ApplicationException(IResponseCode.INVALID_DATA, "Select at least one device type.");
		}
		for (Long id : picked) {
			DeviceMaster device = deviceMasterRepository.findById(id)
					.orElseThrow(() -> new ApplicationException(IResponseCode.INVALID_ENTITY,
							"That device type does not exist."));
			if (device.getStatus() != Constants.STATUS_ACTIVE) {
				throw new ApplicationException(IResponseCode.INVALID_DATA,
						"\"" + device.getDeviceName() + "\" is not an active device type.");
			}
		}
		return picked.stream().map(String::valueOf).collect(Collectors.joining(","));
	}

	/**
	 * The plant(s) picked on the Reporting Person form — one ("1001") or several ("1001,1003").
	 * Returned trimmed and without repeats, as plant_code stores them; at least one is required.
	 */
	private static String requirePlants(String codes) throws ApplicationException {
		String joined = codes == null ? "" : java.util.Arrays.stream(codes.split(","))
				.map(String::trim)
				.filter(s -> !s.isEmpty())
				.distinct()
				.collect(Collectors.joining(","));
		if (joined.isEmpty()) {
			throw new ApplicationException(IResponseCode.INVALID_DATA, "Select at least one plant.");
		}
		return joined;
	}

	/**
	 * THE ROLE(S) OF A LOGIN ACCESS ROW, from Role Master.
	 *
	 * The screen sends the roles picked by id (role_id "1,4"): role_id keeps those ids and role
	 * gets their access types ("ADMIN,USER"), which login and the permissions read. A request
	 * with role codes only ("ADMIN,USER", no role_id) maps them onto the built-in roles' ids.
	 * A role the person already holds (savedRole / savedRoleId) stays allowed even if inactive.
	 */
	private void applyRoles(AmsLoginAccess row, ReportingPersonBO in, String savedRole, String savedRoleId)
			throws ApplicationException {
		if (!isBlank(in.getRole_id())) {
			String[] resolved = roleService.resolve(in.getRole_id(), savedRoleId);
			row.setRole(resolved[0]);
			row.setRoleId(resolved[1]);
			return;
		}
		// "USER", "ADMIN", "ADMIN,USER" or "VIEWER" — checked and put in order (AmsRoles).
		String role = AmsRoles.normalise(in.getRole());
		row.setRoleId(roleService.toRoleIds(role, savedRole));
		row.setRole(role);
	}

	private static boolean isBlank(String s) {
		return s == null || s.isBlank();
	}

	@Override
	@Transactional
	public ReportingPersonBO saveReportingPerson(ReportingPersonBO in) throws ApplicationException {

		if (in == null || in.getEmp_code() == null
				|| (isBlank(in.getRole()) && isBlank(in.getRole_id()))
				|| in.getCompany_name() == null || in.getCompany_name().isBlank()
				|| in.getPlant_code() == null || in.getPlant_code().isBlank()) {
			throw new ApplicationException(IResponseCode.INVALID_DATA,
					"Company, plant, device type, employee and role are all required.");
		}

		String deviceIds = requireDeviceTypes(in.getDevice_ids());
		String plantCodes = requirePlants(in.getPlant_code());

		/*
		 * The employee is verified against the shared master rather than trusted from the
		 * request — otherwise a hand-made call could grant AMS access to any number at all.
		 */
		EmpDetailsMaster employee = empDetailsMasterRepository.findByEmpCode(in.getEmp_code())
				.orElseThrow(() -> new ApplicationException(IResponseCode.USER_NOT_FOUND,
						"That employee does not exist."));

		if (employee.getEmpStatus() == null || employee.getEmpStatus() != Constants.STATUS_ACTIVE) {
			throw new ApplicationException(IResponseCode.INVALID_ENTITY,
					"That employee is no longer active.");
		}

		String today = LocalDate.now().toString();
		String now = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));

		/*
		 * ONE ENTRY PER EMPLOYEE, and SAVE only ADDS one. emp_code is UNIQUE; overwriting an
		 * existing row here would silently replace that person's plants, device types and role
		 * (1002, 1003 lost for the 1006 picked now). So someone who already has access is
		 * refused — their own row is changed through UPDATE (Edit on the Reporting Person list).
		 */
		Optional<AmsLoginAccess> already = amsLoginAccessRepository.findByEmpCode(employee.getEmpCode());
		/*
		 * A DELETED entry (status -1 — DELETE only marks the row) is not access any more: the
		 * employee may be ADDED again, and that row is reused below as a fresh grant (new plants,
		 * types and role, active, dated today). Only a live entry — Active or Inactive, both on
		 * the Reporting Person list — refuses the ADD.
		 */
		if (already.isPresent() && already.get().getStatus() == Constants.STATUS_DELETED) {
			AmsLoginAccess revived = already.get();
			revived.setRegDate(today);
			revived.setRegTime(now);
			revived.setCreateDate(LocalDate.now());
			revived.setCreateTime(LocalTime.now().withNano(0));
		} else if (already.isPresent()) {
			AmsLoginAccess a = already.get();
			// "(USER, plant 1002, 1003)" — or just "(USER)" when the row has no plant recorded.
			String plants = a.getPlantCodeList().isEmpty() ? "" : ", plant " + String.join(", ", a.getPlantCodeList());
			throw new ApplicationException(IResponseCode.EXISTING_ENTITY,
					employee.getEmpCode() + " already has AMS access (" + a.getRole() + plants
							+ "). Edit their existing access to add a plant, device type or role.");
		}
		AmsLoginAccess row = already
				.orElseGet(() -> {
					AmsLoginAccess fresh = new AmsLoginAccess();
					fresh.setEmpCode(employee.getEmpCode());
					fresh.setRegDate(today);
					fresh.setRegTime(now);
					/*
					 * Inside orElseGet, so it is stamped only when the row is new. Re-assigning
					 * someone who already has access must not reset when their access began.
					 */
					fresh.setCreateDate(LocalDate.now());
					fresh.setCreateTime(LocalTime.now().withNano(0));
					return fresh;
				});

		row.setCompanyName(in.getCompany_name());
		row.setPlantCode(plantCodes);
		row.setDeviceIds(deviceIds);
		String first = employee.getEmpFname() == null ? "" : employee.getEmpFname().trim();
		String last = employee.getEmpLname() == null ? "" : employee.getEmpLname().trim();
		row.setEmpName((first + " " + last).trim());
		applyRoles(row, in, null, null);
		row.setDescription(in.getDescription());
		// Active unless the form says otherwise. Inactive means the assignment is on record
		// but the person cannot sign in — login filters on status = 1.
		row.setStatus(in.getStatus() != null && in.getStatus() == Constants.STATUS_INACTIVE
				? Constants.STATUS_INACTIVE
				: Constants.STATUS_ACTIVE);

		AmsLoginAccess saved = amsLoginAccessRepository.save(row);

		ReportingPersonBO out = toReportingPersonBO(saved);
		out.setDevice_names(joinDeviceNames(saved.getDeviceIdList(), deviceTypeNames()));
		return out;
	}

	@Override
	@Transactional
	public ReportingPersonBO updateReportingPerson(ReportingPersonBO in) throws ApplicationException {

		if (in == null || in.getId() == null) {
			throw new ApplicationException(IResponseCode.INVALID_DATA, "Nothing to update.");
		}

		AmsLoginAccess row = amsLoginAccessRepository.findById(in.getId())
				.orElseThrow(() -> new ApplicationException(IResponseCode.INVALID_ENTITY,
						"That row no longer exists."));

		String deviceIds = requireDeviceTypes(in.getDevice_ids());
		String plantCodes = requirePlants(in.getPlant_code());

		/*
		 * The employee can be changed, but emp_code is UNIQUE and is the identity login looks
		 * up — so moving this row onto someone who already has an access row would violate
		 * the constraint. Checked here so the caller gets a readable message instead of a 500,
		 * and so one person's access is never silently merged into another's.
		 */
		if (in.getEmp_code() != null && !in.getEmp_code().equals(row.getEmpCode())) {
			EmpDetailsMaster employee = empDetailsMasterRepository.findByEmpCode(in.getEmp_code())
					.orElseThrow(() -> new ApplicationException(IResponseCode.USER_NOT_FOUND,
							"That employee does not exist."));

			if (employee.getEmpStatus() == null || employee.getEmpStatus() != Constants.STATUS_ACTIVE) {
				throw new ApplicationException(IResponseCode.INVALID_ENTITY,
						"That employee is no longer active.");
			}

			Optional<AmsLoginAccess> clash = amsLoginAccessRepository.findByEmpCode(in.getEmp_code());
			if (clash.isPresent() && !clash.get().getId().equals(row.getId())) {
				throw new ApplicationException(IResponseCode.EXISTING_ENTITY,
						"That employee already has AMS access. Edit their own row instead.");
			}

			row.setEmpCode(employee.getEmpCode());
			String f = employee.getEmpFname() == null ? "" : employee.getEmpFname().trim();
			String l = employee.getEmpLname() == null ? "" : employee.getEmpLname().trim();
			row.setEmpName((f + " " + l).trim());
		}

		row.setCompanyName(in.getCompany_name());
		row.setPlantCode(plantCodes);
		row.setDeviceIds(deviceIds);
		// Same as the save; a role this person already holds stays allowed even if inactive.
		applyRoles(row, in, row.getRole(), row.getRoleId());
		row.setDescription(in.getDescription());
		if (in.getStatus() != null) {
			row.setStatus(in.getStatus() == Constants.STATUS_INACTIVE
					? Constants.STATUS_INACTIVE
					: Constants.STATUS_ACTIVE);
		}

		AmsLoginAccess saved = amsLoginAccessRepository.save(row);

		ReportingPersonBO out = toReportingPersonBO(saved);
		out.setDevice_names(joinDeviceNames(saved.getDeviceIdList(), deviceTypeNames()));
		return out;
	}

	@Override
	@Transactional
	public void deleteReportingPerson(Long id) throws ApplicationException {
		AmsLoginAccess row = amsLoginAccessRepository.findById(id)
				.orElseThrow(() -> new ApplicationException(IResponseCode.INVALID_ENTITY,
						"That row no longer exists."));

		/*
		 * With one table, deleting here REVOKES the employee's AMS login access — the row
		 * being retired is the access row. Login filters on status = 1, so they can no
		 * longer sign in. That is the consequence of merging the two tables, and it is worth
		 * being deliberate about: this is not just an assignment being removed.
		 */
		row.setStatus(Constants.STATUS_DELETED);
		amsLoginAccessRepository.save(row);
	}

	private ReportingPersonBO toReportingPersonBO(AmsLoginAccess entity) {
		ReportingPersonBO bo = new ReportingPersonBO();
		bo.setId(entity.getId());
		bo.setCompany_name(entity.getCompanyName());
		bo.setPlant_code(entity.getPlantCode());
		bo.setDevice_ids(entity.getDeviceIdList());
		bo.setEmp_code(entity.getEmpCode());
		bo.setEmp_name(entity.getEmpName());
		bo.setRole(entity.getRole());
		bo.setRole_id(entity.getRoleId());
		bo.setDescription(entity.getDescription());
		bo.setCreate_date(entity.getCreateDate());
		bo.setCreate_time(entity.getCreateTime());
		bo.setStatus(entity.getStatus());
		return bo;
	}

	private SubCategoryBO toSubCategoryBO(SubCategoryMaster entity) {
		SubCategoryBO bo = new SubCategoryBO();
		bo.setId(entity.getId());
		bo.setCategory_id(entity.getCategoryId());
		bo.setSub_category_name(entity.getSubCategoryName());
		bo.setDescription(entity.getDescription());
		bo.setCreate_date(entity.getCreateDate());
		bo.setCreate_time(entity.getCreateTime());
		bo.setStatus(entity.getStatus());
		return bo;
	}

	private CompanyBO toCompanyBO(CompanyMaster entity) {
		CompanyBO bo = new CompanyBO();
		bo.setId(entity.getId());
		bo.setCompany_name(entity.getCompanyName());
		bo.setCompany_short_name(entity.getCompanyShortName());
		bo.setPlant_code(entity.getPlantCode());
		bo.setPlant_name(entity.getPlantName());
		return bo;
	}

	private CategoryBO toCategoryBO(CategoryMaster entity) {
		CategoryBO bo = new CategoryBO();
		bo.setId(entity.getId());
		bo.setDescription(entity.getDescription());
		bo.setCategory_name(entity.getCategoryName());
		bo.setCreate_date(entity.getCreateDate());
		bo.setCreate_time(entity.getCreateTime());
		bo.setStatus(entity.getStatus());
		return bo;
	}

	private PlantCategoryBO toPlantCategoryBO(PlantCategory entity) {
		PlantCategoryBO bo = new PlantCategoryBO();
		bo.setId(entity.getId());
		bo.setCompany_name(entity.getCompanyName());
		bo.setPlant_code(entity.getPlantCode());
		bo.setPlant_name(entity.getPlantName());
		bo.setCategory_id(entity.getCategoryId());
		bo.setDescription(entity.getDescription());
		bo.setCreate_date(entity.getCreateDate());
		bo.setCreate_time(entity.getCreateTime());
		bo.setStatus(entity.getStatus());
		return bo;
	}
}
