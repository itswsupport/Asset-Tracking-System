package com.repl.asset.master.bo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * One server's full record, in and out — what the user side's ADD SERVER and its list view
 * both use.
 *
 * Fields are snake_case to match the rest of the masters here, and NON_NULL so a response
 * carries only the cells that were actually filled in.
 *
 * Only server_name is required. The sheet this came from is sparse, and a form that insisted
 * on more could not record the servers the company actually has.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ServerAssetBO {

	@JsonProperty("id")
	private Long id;

	/** NAME. UNIQUE, and the only required field. */
	@JsonProperty("server_name")
	private String server_name;

	/** IP ADDRESS. */
	@JsonProperty("ip_address")
	private String ip_address;

	/** PLANT — the plant code. */
	@JsonProperty("plant")
	private String plant;

	/** DEPT. */
	@JsonProperty("department")
	private String department;

	/** LOCATION. */
	@JsonProperty("location")
	private String location;

	/** TYPE. */
	@JsonProperty("server_type")
	private String server_type;

	/** S/N. */
	@JsonProperty("serial_no")
	private String serial_no;

	/** PROCESSOR Ghz. */
	@JsonProperty("processor")
	private String processor;

	/** RAM. */
	@JsonProperty("ram")
	private String ram;

	/** HDD (GB). */
	@JsonProperty("hdd")
	private String hdd;

	/** Warranty Status, from the warranty master. */
	@JsonProperty("warranty_status")
	private String warranty_status;

	/** Owner, kept from the sheet though not on the form. */
	@JsonProperty("owner_name")
	private String owner_name;

	/** OS. */
	@JsonProperty("os_name")
	private String os_name;

	/** OS service pack. */
	@JsonProperty("os_service_pack")
	private String os_service_pack;

	/** System type. */
	@JsonProperty("system_type")
	private String system_type;


	/** Other software 1. */
	@JsonProperty("other_software_1")
	private String other_software_1;

	/** Other software 2. */
	@JsonProperty("other_software_2")
	private String other_software_2;

	/** A free-text note. */
	@JsonProperty("description")
	private String description;

	/** DERIVED on the way out and ignored on the way in. */
	@JsonProperty("age")
	private String age;

	/** Warranty Start Date. */
	@JsonProperty("warranty_start_date")
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
	private LocalDate warranty_start_date;

	public LocalDate getWarranty_start_date() {
		return warranty_start_date;
	}

	public void setWarranty_start_date(LocalDate warranty_start_date) {
		this.warranty_start_date = warranty_start_date;
	}

	/** Last Date Of Warranty. */
	@JsonProperty("last_date_of_warranty")
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
	private LocalDate last_date_of_warranty;

	/** Total Warranty (In Years). */
	@JsonProperty("total_warranty_years")
	private BigDecimal total_warranty_years;

	@JsonProperty("create_date")
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
	private LocalDate create_date;

	@JsonProperty("create_time")
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
	private LocalTime create_time;

	/** 1 = active, 0 = inactive. -1 is set by delete and never arrives from a form. */
	@JsonProperty("status")
	private Integer status;

	/** Who is saving — written beside every field an UPDATE changes (the USER LOG). */
	@JsonProperty("changed_by_code")
	private String changed_by_code;

	@JsonProperty("changed_by_name")
	private String changed_by_name;

	public String getChanged_by_code() {
		return changed_by_code;
	}

	public void setChanged_by_code(String changed_by_code) {
		this.changed_by_code = changed_by_code;
	}

	public String getChanged_by_name() {
		return changed_by_name;
	}

	public void setChanged_by_name(String changed_by_name) {
		this.changed_by_name = changed_by_name;
	}

	/** "FORM" or "BULK" — set by Bulk Upload on every row, otherwise FORM. */
	@JsonProperty("added_via")
	private String added_via;

	public String getAdded_via() {
		return added_via;
	}

	public void setAdded_via(String added_via) {
		this.added_via = added_via;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getServer_name() {
		return server_name;
	}

	public void setServer_name(String server_name) {
		this.server_name = server_name;
	}

	public String getIp_address() {
		return ip_address;
	}

	public void setIp_address(String ip_address) {
		this.ip_address = ip_address;
	}

	public String getPlant() {
		return plant;
	}

	public void setPlant(String plant) {
		this.plant = plant;
	}

	public String getDepartment() {
		return department;
	}

	public void setDepartment(String department) {
		this.department = department;
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public String getServer_type() {
		return server_type;
	}

	public void setServer_type(String server_type) {
		this.server_type = server_type;
	}

	public String getSerial_no() {
		return serial_no;
	}

	public void setSerial_no(String serial_no) {
		this.serial_no = serial_no;
	}

	public String getProcessor() {
		return processor;
	}

	public void setProcessor(String processor) {
		this.processor = processor;
	}

	public String getRam() {
		return ram;
	}

	public void setRam(String ram) {
		this.ram = ram;
	}

	public String getHdd() {
		return hdd;
	}

	public void setHdd(String hdd) {
		this.hdd = hdd;
	}

	public String getWarranty_status() {
		return warranty_status;
	}

	public void setWarranty_status(String warranty_status) {
		this.warranty_status = warranty_status;
	}

	public String getOwner_name() {
		return owner_name;
	}

	public void setOwner_name(String owner_name) {
		this.owner_name = owner_name;
	}

	public String getOs_name() {
		return os_name;
	}

	public void setOs_name(String os_name) {
		this.os_name = os_name;
	}

	public String getOs_service_pack() {
		return os_service_pack;
	}

	public void setOs_service_pack(String os_service_pack) {
		this.os_service_pack = os_service_pack;
	}

	public String getSystem_type() {
		return system_type;
	}

	public void setSystem_type(String system_type) {
		this.system_type = system_type;
	}

	public String getOther_software_1() {
		return other_software_1;
	}

	public void setOther_software_1(String other_software_1) {
		this.other_software_1 = other_software_1;
	}

	public String getOther_software_2() {
		return other_software_2;
	}

	public void setOther_software_2(String other_software_2) {
		this.other_software_2 = other_software_2;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getAge() {
		return age;
	}

	public void setAge(String age) {
		this.age = age;
	}

	public LocalDate getLast_date_of_warranty() {
		return last_date_of_warranty;
	}

	public void setLast_date_of_warranty(LocalDate last_date_of_warranty) {
		this.last_date_of_warranty = last_date_of_warranty;
	}

	public BigDecimal getTotal_warranty_years() {
		return total_warranty_years;
	}

	public void setTotal_warranty_years(BigDecimal total_warranty_years) {
		this.total_warranty_years = total_warranty_years;
	}

	public LocalDate getCreate_date() {
		return create_date;
	}

	public void setCreate_date(LocalDate create_date) {
		this.create_date = create_date;
	}

	public LocalTime getCreate_time() {
		return create_time;
	}

	public void setCreate_time(LocalTime create_time) {
		this.create_time = create_time;
	}

	public Integer getStatus() {
		return status;
	}

	public void setStatus(Integer status) {
		this.status = status;
	}
}
