package com.repl.asset.master.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * One machine, of ANY device type — the row the user side's ADD DEVICE writes.
 *
 * ONE TABLE FOR EVERY DEVICE, keyed by device_id, and that is the whole point of it. The
 * earlier design gave each device type its own table, which meant a device the admin added in
 * Device Master had nowhere to save to and so could not be offered on the user side at all.
 * Every new device type needed a table, an entity, a service and a line in a switch before it
 * would appear. Here it needs nothing: device_id points at whatever Device Master holds, so a
 * device added there works the moment it is saved.
 *
 * That is the opposite of the reasoning behind RAM, Processor, HDD and OS having separate
 * tables. Those are different KINDS of thing that happen to share a shape. These are the same
 * thing — a machine — and the device type is a property of it, not a different species.
 *
 * UNIQUE on (device_id, name): a desktop and a laptop may both be called 101, but one device
 * type cannot hold the same name twice.
 *
 * The columns are the DESKTOP and LAPTOP sheets of the asset workbook, minus two. SR is the
 * row number, which is this table's id. AGE is derived on read from the warranty date and
 * term, because it changes by itself as months pass.
 */
@Entity
@Table(name = "ams_add_device_master")
public class AssetMaster {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;

	/** Whatever Device Master holds — no list of allowed types anywhere. */
	@Column(name = "device_id", nullable = false)
	private Long deviceId;

	/** Excel E — the machine's name or number. */
	@Column(name = "name", nullable = false, length = 200)
	private String name;

	@Column(name = "po_no", length = 100)
	private String poNo;

	@Column(name = "asset_code", length = 100)
	private String assetCode;

	/**
	 * Auto-generated on first save, never changed after: TT-NAME-FY-SEQ, e.g. DE-402-2026-001.
	 * ref_fy and ref_seq hold the year and running number apart, so the next number is a
	 * plain MAX() and uk_ref_type_fy_seq stops two saves taking the same one.
	 */
	@Column(name = "reference_no", length = 100)
	private String referenceNo;

	@Column(name = "ref_fy")
	private Integer refFy;

	@Column(name = "ref_seq")
	private Integer refSeq;

	public String getReferenceNo() {
		return referenceNo;
	}

	public void setReferenceNo(String referenceNo) {
		this.referenceNo = referenceNo;
	}

	public Integer getRefFy() {
		return refFy;
	}

	public void setRefFy(Integer refFy) {
		this.refFy = refFy;
	}

	public Integer getRefSeq() {
		return refSeq;
	}

	public void setRefSeq(Integer refSeq) {
		this.refSeq = refSeq;
	}

	@Column(name = "asset_sn", length = 150)
	private String assetSn;

	@Column(name = "device_status", length = 50)
	private String deviceStatus;

	@Column(name = "ip_address", length = 100)
	private String ipAddress;

	@Column(name = "plant", length = 150)
	private String plant;

	@Column(name = "department", length = 100)
	private String department;

	@Column(name = "location", length = 150)
	private String location;

	@Column(name = "model", length = 200)
	private String model;

	@Column(name = "service_tag", length = 150)
	private String serviceTag;

	/** Exp. Service Code — the maker's Express Service Code (Dell), the asset sheet's "EXP. SERVICE CODE". */
	@Column(name = "exp_service_code", length = 100)
	private String expServiceCode;

	public String getExpServiceCode() {
		return expServiceCode;
	}

	public void setExpServiceCode(String expServiceCode) {
		this.expServiceCode = expServiceCode;
	}

	/** ASSIGNED EMPLOYEE CODE — the code of the employee who holds the device now (was employee_code). */
	@Column(name = "assigned_employee_code", length = 50)
	private String employeeCode;

	/** ASSIGNED TO — the employee who holds the device now (was employee_name). */
	@Column(name = "assigned_to_employee", length = 150)
	private String employeeName;

	/**
	 * ADDED BY — the signed-in employee who first submitted this device (ADD DEVICE or BULK
	 * UPLOAD). Set once when the device is added; a later edit never changes it.
	 */
	/**
	 * AGE as YEAR.MONTH ("5.01" = 5 years 1 month), as the ADD DEVICE form shows it. Saved on
	 * every add / edit and refreshed daily by AgeRefreshJob, since it changes as months pass.
	 */
	@Column(name = "age", length = 10)
	private String age;

	@Column(name = "added_by_emp_code", nullable = false, length = 50)
	private String addedByEmpCode;

	@Column(name = "added_by_emp_name", nullable = false, length = 150)
	private String addedByEmpName;

	@Column(name = "processor", length = 200)
	private String processor;

	@Column(name = "ram", length = 100)
	private String ram;

	/** Storage on a tablet, beside ram. Nothing else records one. */
	@Column(name = "rom", length = 100)
	private String rom;

	@Column(name = "hdd", length = 100)
	private String hdd;

	/*
	 * EXTERNAL parts — anything fitted or attached beyond what the machine came with, typed in
	 * by the user beside the matching field: an added processor, extra RAM, an external drive.
	 * All optional, so a machine with none leaves them empty.
	 */
	@Column(name = "external_processor", length = 200)
	private String externalProcessor;

	@Column(name = "external_ram", length = 100)
	private String externalRam;

	@Column(name = "external_rom", length = 100)
	private String externalRom;

	@Column(name = "external_hdd", length = 100)
	private String externalHdd;

	@Column(name = "os", length = 150)
	private String os;

	@Column(name = "os_service_pack", length = 100)
	private String osServicePack;

	@Column(name = "ms_office", length = 150)
	private String msOffice;

	@Column(name = "charger", length = 100)
	private String charger;

	/** KEYBOARD — the keyboard's serial number, as on the asset sheet. Desktops only. */
	@Column(name = "keyboard", length = 150)
	private String keyboard;

	/** MOUSE — the mouse's serial number, as on the asset sheet. Desktops only. */
	@Column(name = "mouse", length = 150)
	private String mouse;

	public String getKeyboard() {
		return keyboard;
	}

	public void setKeyboard(String keyboard) {
		this.keyboard = keyboard;
	}

	public String getMouse() {
		return mouse;
	}

	public void setMouse(String mouse) {
		this.mouse = mouse;
	}

	@Column(name = "software_1", length = 200)
	private String software1;

	@Column(name = "software_2", length = 200)
	private String software2;

	@Column(name = "classification", length = 100)
	private String classification;

	@Column(name = "system_type", length = 50)
	private String systemType;

	@Column(name = "warranty_status", length = 50)
	private String warrantyStatus;

	/** The day warranty cover starts — "From". Total Warranty is the span from this to the end. */
	@Column(name = "warranty_start_date")
	private LocalDate warrantyStartDate;

	@Column(name = "last_date_of_warranty")
	private LocalDate lastDateOfWarranty;

	@Column(name = "total_warranty_years", precision = 4, scale = 1)
	private BigDecimal totalWarrantyYears;

	@Column(name = "description", length = 255)
	private String description;

	@Column(name = "create_date")
	private LocalDate createDate;

	@Column(name = "create_time")
	private LocalTime createTime;

	/** 1 = active, 0 = inactive, -1 = deleted. */
	@Column(name = "status", nullable = false)
	private int status;

	@Column(name = "reg_by")
	private Long regBy;

	@Column(name = "reg_time")
	private String regTime;

	/*
	 * No `source` column any more. It told rows imported from the old asset workbook (IMPORT,
	 * a bare name) from real entries (FORM). The imported rows are gone — the catalogue of names
	 * lives in ams_device_name_master — nothing wrote IMPORT any longer, and every row read
	 * FORM, so it said nothing. HOW a device was added is added_via below.
	 */

	/**
	 * HOW THE DEVICE WAS ADDED: "FORM" through ADD DEVICE, "BULK" through BULK UPLOAD. Set
	 * once, when the device is first saved, and never changed by a later edit.
	 */
	@Column(name = "added_via", nullable = false, length = 10)
	private String addedVia = "FORM";

	public String getAddedVia() {
		return addedVia;
	}

	public void setAddedVia(String addedVia) {
		this.addedVia = addedVia;
	}

	/**
	 * THE LATEST ACTION, copied onto the machine itself.
	 *
	 * The full chain lives in ams_asset_action_history, one row per handover, and always will —
	 * these four are a summary of its newest entry, written by AssetActionHistoryService every
	 * time that log changes. They answer "where is this machine and since when" without a join,
	 * which is what LIST DEVICE asks about every row it draws.
	 *
	 * DERIVED, NEVER TYPED. Nothing outside that service writes them, so they cannot drift from
	 * the log: delete the newest entry and they fall back to the one before it. employeeCode and
	 * employeeName above are stamped from the same place, and are the current holder.
	 */
	@Column(name = "last_action_id")
	private Long lastActionId;

	/** When the current holder got it — the newest entry's start date. */
	@Column(name = "last_action_date")
	private LocalDate lastActionDate;

	/** Why it last changed hands, as typed on that entry. */
	@Column(name = "last_action_remarks", length = 500)
	private String lastActionRemarks;

	/** When that entry was recorded, which is not when the handover happened. */
	@Column(name = "last_action_on")
	private LocalDateTime lastActionOn;

	public Long getId() {
		return id;
	}

	public Long getLastActionId() {
		return lastActionId;
	}

	public void setLastActionId(Long lastActionId) {
		this.lastActionId = lastActionId;
	}

	public LocalDate getLastActionDate() {
		return lastActionDate;
	}

	public void setLastActionDate(LocalDate lastActionDate) {
		this.lastActionDate = lastActionDate;
	}

	public String getLastActionRemarks() {
		return lastActionRemarks;
	}

	public void setLastActionRemarks(String lastActionRemarks) {
		this.lastActionRemarks = lastActionRemarks;
	}

	public LocalDateTime getLastActionOn() {
		return lastActionOn;
	}

	public void setLastActionOn(LocalDateTime lastActionOn) {
		this.lastActionOn = lastActionOn;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getDeviceId() {
		return deviceId;
	}

	public void setDeviceId(Long deviceId) {
		this.deviceId = deviceId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPoNo() {
		return poNo;
	}

	public void setPoNo(String poNo) {
		this.poNo = poNo;
	}

	public String getAssetCode() {
		return assetCode;
	}

	public void setAssetCode(String assetCode) {
		this.assetCode = assetCode;
	}

	public String getAssetSn() {
		return assetSn;
	}

	public void setAssetSn(String assetSn) {
		this.assetSn = assetSn;
	}

	public String getDeviceStatus() {
		return deviceStatus;
	}

	public void setDeviceStatus(String deviceStatus) {
		this.deviceStatus = deviceStatus;
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

	public String getModel() {
		return model;
	}

	public void setModel(String model) {
		this.model = model;
	}

	public String getServiceTag() {
		return serviceTag;
	}

	public void setServiceTag(String serviceTag) {
		this.serviceTag = serviceTag;
	}

	public String getEmployeeCode() {
		return employeeCode;
	}

	public void setEmployeeCode(String employeeCode) {
		this.employeeCode = employeeCode;
	}

	public String getEmployeeName() {
		return employeeName;
	}

	public void setEmployeeName(String employeeName) {
		this.employeeName = employeeName;
	}

	public String getAge() {
		return age;
	}

	public void setAge(String age) {
		this.age = age;
	}

	public String getAddedByEmpCode() {
		return addedByEmpCode;
	}

	public void setAddedByEmpCode(String addedByEmpCode) {
		this.addedByEmpCode = addedByEmpCode;
	}

	public String getAddedByEmpName() {
		return addedByEmpName;
	}

	public void setAddedByEmpName(String addedByEmpName) {
		this.addedByEmpName = addedByEmpName;
	}

	public String getExternalProcessor() {
		return externalProcessor;
	}

	public void setExternalProcessor(String externalProcessor) {
		this.externalProcessor = externalProcessor;
	}

	public String getExternalRam() {
		return externalRam;
	}

	public void setExternalRam(String externalRam) {
		this.externalRam = externalRam;
	}

	public String getExternalRom() {
		return externalRom;
	}

	public void setExternalRom(String externalRom) {
		this.externalRom = externalRom;
	}

	public String getExternalHdd() {
		return externalHdd;
	}

	public void setExternalHdd(String externalHdd) {
		this.externalHdd = externalHdd;
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

	public String getRom() {
		return rom;
	}

	public void setRom(String rom) {
		this.rom = rom;
	}

	public String getHdd() {
		return hdd;
	}

	public void setHdd(String hdd) {
		this.hdd = hdd;
	}

	public String getOs() {
		return os;
	}

	public void setOs(String os) {
		this.os = os;
	}

	public String getOsServicePack() {
		return osServicePack;
	}

	public void setOsServicePack(String osServicePack) {
		this.osServicePack = osServicePack;
	}

	public String getMsOffice() {
		return msOffice;
	}

	public void setMsOffice(String msOffice) {
		this.msOffice = msOffice;
	}

	public String getCharger() {
		return charger;
	}

	public void setCharger(String charger) {
		this.charger = charger;
	}

	public String getSoftware1() {
		return software1;
	}

	public void setSoftware1(String software1) {
		this.software1 = software1;
	}

	public String getSoftware2() {
		return software2;
	}

	public void setSoftware2(String software2) {
		this.software2 = software2;
	}

	public String getClassification() {
		return classification;
	}

	public void setClassification(String classification) {
		this.classification = classification;
	}

	public String getSystemType() {
		return systemType;
	}

	public void setSystemType(String systemType) {
		this.systemType = systemType;
	}

	public String getWarrantyStatus() {
		return warrantyStatus;
	}

	public void setWarrantyStatus(String warrantyStatus) {
		this.warrantyStatus = warrantyStatus;
	}

	public LocalDate getWarrantyStartDate() {
		return warrantyStartDate;
	}

	public void setWarrantyStartDate(LocalDate warrantyStartDate) {
		this.warrantyStartDate = warrantyStartDate;
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

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
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
