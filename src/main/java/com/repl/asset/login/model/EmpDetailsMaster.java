package com.repl.asset.login.model;

import org.hibernate.annotations.Immutable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Read-only view of db_all_master.emp_details — the SHARED employee master.
 *
 * Two things about this class are deliberate and must not be "tidied up":
 *
 * 1. catalog = "db_all_master". AMS's own tables live in db_asset (see the datasource URL),
 *    so this entity names the other database explicitly to reach across. Drop the attribute
 *    and Hibernate looks for db_asset.emp_details, which does not exist.
 *
 *    catalog, NOT schema. MySQL has no schema/catalog split — a database IS the catalog —
 *    so Hibernate ignores @Table(schema=...) here and emits a bare "from emp_details",
 *    which then resolves against db_asset and fails with "table doesn't exist".
 *
 * 2. @Immutable, and no setters. This table is written by HR systems and read by several
 *    applications; a stray save() here would corrupt data other teams depend on. AMS reads
 *    it and never writes it. spring.jpa.hibernate.ddl-auto is set to none for the same
 *    reason — Hibernate must never issue DDL against this schema.
 *
 * Only the columns AMS actually needs are mapped. The real table has 130+ columns.
 */
@Entity
@Immutable
@Table(name = "emp_details", catalog = "db_all_master")
public class EmpDetailsMaster {

	@Id
	@Column(name = "emp_id")
	private Long empId;

	@Column(name = "emp_code")
	private Long empCode;

	/** BCrypt hash ($2a$, 60 chars). 14 of the 1009 rows hold NULL — treat as "cannot log in". */
	@Column(name = "emp_pass")
	private String empPass;

	@Column(name = "emp_fname")
	private String empFname;

	@Column(name = "emp_mname")
	private String empMname;

	@Column(name = "emp_lname")
	private String empLname;

	@Column(name = "display_name")
	private String displayName;

	@Column(name = "emp_email")
	private String empEmail;

	@Column(name = "company_email_id")
	private String companyEmailId;

	@Column(name = "emp_mobile")
	private String empMobile;

	/** 1 = active, 0 = left/inactive. */
	@Column(name = "emp_status")
	private Integer empStatus;

	/**
	 * Designation as free text despite the _id suffix — "ENGINEER", "SR OFFICER", "MD".
	 * There is no designation master to join to. Used for display only; AMS authority comes
	 * from ams_login_access, not from this column.
	 */
	@Column(name = "desig_id")
	private String desigId;

	@Column(name = "dept_id")
	private String deptId;

	@Column(name = "grade")
	private String grade;

	@Column(name = "emp_level")
	private String empLevel;

	@Column(name = "comp_id")
	private Integer compId;

	@Column(name = "company_name")
	private String companyName;

	@Column(name = "plant_code")
	private Long plantCode;

	public Long getEmpId() {
		return empId;
	}

	public Long getEmpCode() {
		return empCode;
	}

	public String getEmpPass() {
		return empPass;
	}

	public String getEmpFname() {
		return empFname;
	}

	public String getEmpMname() {
		return empMname;
	}

	public String getEmpLname() {
		return empLname;
	}

	public String getDisplayName() {
		return displayName;
	}

	public String getEmpEmail() {
		return empEmail;
	}

	public String getCompanyEmailId() {
		return companyEmailId;
	}

	public String getEmpMobile() {
		return empMobile;
	}

	public Integer getEmpStatus() {
		return empStatus;
	}

	public String getDesigId() {
		return desigId;
	}

	public String getDeptId() {
		return deptId;
	}

	public String getGrade() {
		return grade;
	}

	public String getEmpLevel() {
		return empLevel;
	}

	public Integer getCompId() {
		return compId;
	}

	public String getCompanyName() {
		return companyName;
	}

	public Long getPlantCode() {
		return plantCode;
	}
}
