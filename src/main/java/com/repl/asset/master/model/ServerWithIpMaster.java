package com.repl.asset.master.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * One server, in full — the row the user side's ADD SERVER writes.
 *
 * Over ams_server_with_ip_master, which already existed and already had every column this
 * screen needs. It came from the workbook's SERVER sheet, so its shape is that sheet's.
 *
 * NOT ams_server_master, which is the thin admin master of server names and is maintained
 * from the Setting page. Two tables, two jobs: one is the list of servers that exist, this is
 * everything known about each of them.
 *
 * server_name is UNIQUE. AGE is not a column — it is worked out from the warranty date and
 * term on every read, because it changes by itself as months pass.
 */
@Entity
@Table(name = "ams_server_with_ip_master")
public class ServerWithIpMaster {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;

	/** NAME — what the server is called. UNIQUE. */
	@Column(name = "server_name", nullable = false, length = 100)
	private String serverName;

	/** IP ADDRESS */
	@Column(name = "ip_address", length = 100)
	private String ipAddress;

	/** PLANT */
	@Column(name = "plant", length = 50)
	private String plant;

	/** DEPT. */
	@Column(name = "department", length = 100)
	private String department;

	/** LOCATION */
	@Column(name = "location", length = 100)
	private String location;

	/** TYPE */
	@Column(name = "server_type", length = 255)
	private String serverType;

	/** S/N */
	@Column(name = "serial_no", length = 100)
	private String serialNo;

	/** PROCESSOR Ghz */
	@Column(name = "processor", length = 255)
	private String processor;

	/** RAM */
	@Column(name = "ram", length = 100)
	private String ram;

	/** HDD (GB) */
	@Column(name = "hdd", length = 255)
	private String hdd;

	/** Warranty Status */
	@Column(name = "warranty_status", length = 50)
	private String warrantyStatus;

	/** owner, kept from the sheet though not on the form */
	@Column(name = "owner_name", length = 150)
	private String ownerName;

	/** OS */
	@Column(name = "os_name", length = 150)
	private String osName;

	/** OS service pack */
	@Column(name = "os_service_pack", length = 100)
	private String osServicePack;

	/** System type */
	@Column(name = "system_type", length = 50)
	private String systemType;


	/** Other software 1 */
	@Column(name = "other_software_1", length = 255)
	private String otherSoftware1;

	/** Other software 2 */
	@Column(name = "other_software_2", length = 255)
	private String otherSoftware2;

	/** A free-text note */
	@Column(name = "description", length = 255)
	private String description;

	/** Warranty Start Date — the day cover begins (as on ADD DEVICE). */
	@Column(name = "warranty_start_date")
	private LocalDate warrantyStartDate;

	public LocalDate getWarrantyStartDate() {
		return warrantyStartDate;
	}

	public void setWarrantyStartDate(LocalDate warrantyStartDate) {
		this.warrantyStartDate = warrantyStartDate;
	}

	/** Last Date Of Warranty — the day cover ends. */
	@Column(name = "last_date_of_warranty")
	private LocalDate lastDateOfWarranty;

	/** Total Warranty (In Years). Half years allowed, so 1.5 is eighteen months. */
	// Two decimals, as the total is worked out (4.83) — one decimal stored 4.8 and logged 4.8 → 4.83 on every UPDATE.
	@Column(name = "total_warranty_years", precision = 5, scale = 2)
	private BigDecimal totalWarrantyYears;

	@Column(name = "create_date")
	private LocalDate createDate;

	@Column(name = "create_time")
	private LocalTime createTime;

	/** 1 = active, 0 = inactive, -1 = deleted. The ROW's status, not the server's. */
	@Column(name = "status", nullable = false)
	private int status;

	@Column(name = "reg_by")
	private Long regBy;

	@Column(name = "reg_time")
	private String regTime;

	/** How the row was added: "FORM" (ADD SERVER) or "BULK" (Bulk Upload). */
	@Column(name = "added_via", nullable = false, length = 10)
	private String addedVia = "FORM";

	public String getAddedVia() {
		return addedVia;
	}

	public void setAddedVia(String addedVia) {
		this.addedVia = addedVia;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getServerName() {
		return serverName;
	}

	public void setServerName(String serverName) {
		this.serverName = serverName;
	}

	public String getIpAddress() {
		return ipAddress;
	}

	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
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

	public String getServerType() {
		return serverType;
	}

	public void setServerType(String serverType) {
		this.serverType = serverType;
	}

	public String getSerialNo() {
		return serialNo;
	}

	public void setSerialNo(String serialNo) {
		this.serialNo = serialNo;
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

	public String getWarrantyStatus() {
		return warrantyStatus;
	}

	public void setWarrantyStatus(String warrantyStatus) {
		this.warrantyStatus = warrantyStatus;
	}

	public String getOwnerName() {
		return ownerName;
	}

	public void setOwnerName(String ownerName) {
		this.ownerName = ownerName;
	}

	public String getOsName() {
		return osName;
	}

	public void setOsName(String osName) {
		this.osName = osName;
	}

	public String getOsServicePack() {
		return osServicePack;
	}

	public void setOsServicePack(String osServicePack) {
		this.osServicePack = osServicePack;
	}

	public String getSystemType() {
		return systemType;
	}

	public void setSystemType(String systemType) {
		this.systemType = systemType;
	}

	public String getOtherSoftware1() {
		return otherSoftware1;
	}

	public void setOtherSoftware1(String otherSoftware1) {
		this.otherSoftware1 = otherSoftware1;
	}

	public String getOtherSoftware2() {
		return otherSoftware2;
	}

	public void setOtherSoftware2(String otherSoftware2) {
		this.otherSoftware2 = otherSoftware2;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public LocalDate getLastDateOfWarranty() {
		return lastDateOfWarranty;
	}

	public void setLastDateOfWarranty(LocalDate lastDateOfWarranty) {
		this.lastDateOfWarranty = lastDateOfWarranty;
	}

	public BigDecimal getTotalWarrantyYears() {
		return totalWarrantyYears;
	}

	public void setTotalWarrantyYears(BigDecimal totalWarrantyYears) {
		this.totalWarrantyYears = totalWarrantyYears;
	}

	public LocalDate getCreateDate() {
		return createDate;
	}

	public void setCreateDate(LocalDate createDate) {
		this.createDate = createDate;
	}

	public LocalTime getCreateTime() {
		return createTime;
	}

	public void setCreateTime(LocalTime createTime) {
		this.createTime = createTime;
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public Long getRegBy() {
		return regBy;
	}

	public void setRegBy(Long regBy) {
		this.regBy = regBy;
	}

	public String getRegTime() {
		return regTime;
	}

	public void setRegTime(String regTime) {
		this.regTime = regTime;
	}
}
