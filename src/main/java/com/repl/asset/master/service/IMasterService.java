package com.repl.asset.master.service;

import java.util.List;

import com.repl.asset.exception.ApplicationException;
import com.repl.asset.master.bo.CategoryBO;
import com.repl.asset.master.bo.CompanyBO;
import com.repl.asset.master.bo.DepartmentBO;
import com.repl.asset.master.bo.EmployeeBO;
import com.repl.asset.master.bo.ReportingPersonBO;
import com.repl.asset.master.bo.PlantCategoryBO;
import com.repl.asset.master.bo.SubCategoryBO;

public interface IMasterService {

	/** Every active company/plant row from the shared master, for the Category dropdowns. */
	List<CompanyBO> getCompanyList();

	/** Every row of db_all_master.plant_dept_mst, for the read-only Department Master. */
	List<DepartmentBO> getDepartmentList();

	/** The independent category list — IT, HR, OPERATIONS and anything added since. */
	List<CategoryBO> getCategoryList();

	/** Saved company + plant + category rows. */
	List<PlantCategoryBO> getPlantCategoryList();

	/**
	 * Record that a category applies at a company's plant.
	 *
	 * @throws ApplicationException when the category does not exist or the same category is
	 *                              already recorded for that plant.
	 */
	PlantCategoryBO savePlantCategory(PlantCategoryBO bo) throws ApplicationException;

	/** Every active sub category, each carrying the name of the category it belongs to. */
	List<SubCategoryBO> getSubCategoryList();

	/**
	 * Add a sub category under a category.
	 *
	 * @throws ApplicationException when the category does not exist, the name is blank, or
	 *                              that name already exists under the same category.
	 */
	SubCategoryBO saveSubCategory(SubCategoryBO bo) throws ApplicationException;

	/**
	 * Change an existing company + plant + category row. Requires an id.
	 *
	 * @throws ApplicationException when the row or the category does not exist, or the change
	 *                              would collide with another row for the same plant.
	 */
	PlantCategoryBO updatePlantCategory(PlantCategoryBO bo) throws ApplicationException;

	/** Retire a company + plant + category row (status = 0). */
	void deletePlantCategory(Long id) throws ApplicationException;

	/**
	 * Rename a sub category or move it under a different category. Requires an id.
	 *
	 * @throws ApplicationException when the row or the category does not exist, or the new
	 *                              name is already used under that category.
	 */
	SubCategoryBO updateSubCategory(SubCategoryBO bo) throws ApplicationException;

	/** Retire a sub category (status = 0). */
	void deleteSubCategory(Long id) throws ApplicationException;

	/** Every active employee from db_all_master.emp_details, for the employee dropdown. */
	List<EmployeeBO> getEmployeeList();

	/** Saved reporting-person rows. */
	List<ReportingPersonBO> getReportingPersonList();

	/**
	 * Assign an employee to a company + plant + category + sub category with an AMS role.
	 *
	 * Also grants that employee AMS login access: the role is written to ams_login_access,
	 * inserting a row if they had none and updating it if they did.
	 *
	 * @throws ApplicationException when a referenced row does not exist, the employee is not
	 *                              an active employee, or the same assignment already exists.
	 */
	ReportingPersonBO saveReportingPerson(ReportingPersonBO bo) throws ApplicationException;

	/**
	 * Change an existing assignment — plant, category, sub category, role or status.
	 * The employee cannot be changed; that is a new row.
	 */
	ReportingPersonBO updateReportingPerson(ReportingPersonBO bo) throws ApplicationException;

	/** Retire the row. Because it IS the access row, this also revokes their login. */
	void deleteReportingPerson(Long id) throws ApplicationException;
}
