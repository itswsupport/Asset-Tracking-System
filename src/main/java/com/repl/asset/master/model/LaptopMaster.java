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
 * Every laptop, by the number written on it — EDC712, HR110, QA903 (CMM).
 *
 * Its own table and its own entity, not one shared device table keyed by a type column. Each
 * device type gets its own stack for the same reason RAM, Processor, HDD and OS each do: a
 * change to laptops cannot then reach desktops, and each list has its own endpoints.
 *
 * Implements SpecEntity because the row is one name plus the usual status and stamps, which
 * is exactly what AbstractSpecService already handles — so the duplicate check, the status
 * clamping and retire-instead-of-delete come from there rather than being copied again.
 *
 * laptop_number is UNIQUE. Loaded from the LAPTOP sheet of the asset workbook: 318 rows,
 * 317 distinct — ACS1001 appears twice there and lands here once.
 */
@Entity
@Table(name = "ams_laptop_master")
public class LaptopMaster implements AssetEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;

	@Column(name = "laptop_number", nullable = false, length = 100)
	private String laptopNumber;

	/** From the asset workbook. Null where the sheet left the cell blank. */
	@Column(name = "system_type", length = 50)
	private String systemType;

	/** From the asset workbook. Age is NOT stored — it is derived from these two. */
	@Column(name = "warranty_status", length = 50)
	private String warrantyStatus;

	@Column(name = "last_date_of_warranty")
	private LocalDate lastDateOfWarranty;

	@Column(name = "total_warranty_years", precision = 4, scale = 1)
	private BigDecimal totalWarrantyYears;

	/** A free-text note beside the name. */
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

	@Override
	public Long getId() {
		return id;
	}

	@Override
	public String getName() {
		return laptopNumber;
	}

	@Override
	public void setName(String name) {
		this.laptopNumber = name;
	}

	@Override
	public int getStatus() {
		return status;
	}

	@Override
	public void setStatus(int status) {
		this.status = status;
	}

	@Override
	public LocalDate getCreateDate() {
		return createDate;
	}

	@Override
	public void setCreateDate(LocalDate createDate) {
		this.createDate = createDate;
	}

	@Override
	public LocalTime getCreateTime() {
		return createTime;
	}

	@Override
	public void setCreateTime(LocalTime createTime) {
		this.createTime = createTime;
	}

	@Override
	public String getSystemType() {
		return systemType;
	}

	@Override
	public void setSystemType(String systemType) {
		this.systemType = systemType;
	}

	@Override
	public String getWarrantyStatus() {
		return warrantyStatus;
	}

	@Override
	public void setWarrantyStatus(String warrantyStatus) {
		this.warrantyStatus = warrantyStatus;
	}

	@Override
	public LocalDate getLastDateOfWarranty() {
		return lastDateOfWarranty;
	}

	@Override
	public void setLastDateOfWarranty(LocalDate lastDateOfWarranty) {
		this.lastDateOfWarranty = lastDateOfWarranty;
	}

	@Override
	public BigDecimal getTotalWarrantyYears() {
		return totalWarrantyYears;
	}

	@Override
	public void setTotalWarrantyYears(BigDecimal totalWarrantyYears) {
		this.totalWarrantyYears = totalWarrantyYears;
	}

	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public void setDescription(String description) {
		this.description = description;
	}

	@Override
	public Long getRegBy() {
		return regBy;
	}

	@Override
	public void setRegBy(Long regBy) {
		this.regBy = regBy;
	}

	public String getRegTime() {
		return regTime;
	}

	public void setRegTime(String regTime) {
		this.regTime = regTime;
	}

	/** Excel B — the purchase order this machine came in on. */
	@Column(name = "po_no", length = 100)
	private String poNo;

	/** Excel C — the asset code stuck on the case. */
	@Column(name = "asset_code", length = 100)
	private String assetCode;

	/** Excel D — the manufacturer's serial number. */
	@Column(name = "asset_sn", length = 150)
	private String assetSn;

	/** Excel F — IN USE or IN SPARE. Not the row's own status. */
	@Column(name = "device_status", length = 50)
	private String deviceStatus;

	/** Excel G. */
	@Column(name = "ip_address", length = 100)
	private String ipAddress;

	/** Excel H. */
	@Column(name = "plant", length = 150)
	private String plant;

	/** Excel I. */
	@Column(name = "department", length = 100)
	private String department;

	/** Excel J. */
	@Column(name = "location", length = 150)
	private String location;

	/** Excel K, the sheet's TYPE column. */
	@Column(name = "model", length = 200)
	private String model;

	/** Excel L. */
	@Column(name = "service_tag", length = 150)
	private String serviceTag;

	/** Excel M. */
	@Column(name = "emp_code", length = 50)
	private String empCode;

	/** Excel U. */
	@Column(name = "user_name", length = 150)
	private String userName;

	/** Excel N. */
	@Column(name = "processor", length = 200)
	private String processor;

	/** Excel O. */
	@Column(name = "ram", length = 100)
	private String ram;

	/** Excel P. */
	@Column(name = "hdd", length = 100)
	private String hdd;

	/** Excel V. */
	@Column(name = "os", length = 150)
	private String os;

	/** Excel W. */
	@Column(name = "os_service_pack", length = 100)
	private String osServicePack;

	/** Excel Y. */
	@Column(name = "ms_office", length = 150)
	private String msOffice;

	/** Excel Z. */
	@Column(name = "charger", length = 100)
	private String charger;

	/** Excel AA. */
	@Column(name = "software_1", length = 200)
	private String software1;

	/** Excel AB. */
	@Column(name = "software_2", length = 200)
	private String software2;

	/** Excel AD — critical or non-critical. */
	@Column(name = "classification", length = 100)
	private String classification;

	@Override
	public String getPoNo() {
		return poNo;
	}

	@Override
	public void setPoNo(String poNo) {
		this.poNo = poNo;
	}

	@Override
	public String getAssetCode() {
		return assetCode;
	}

	@Override
	public void setAssetCode(String assetCode) {
		this.assetCode = assetCode;
	}

	@Override
	public String getAssetSn() {
		return assetSn;
	}

	@Override
	public void setAssetSn(String assetSn) {
		this.assetSn = assetSn;
	}

	@Override
	public String getDeviceStatus() {
		return deviceStatus;
	}

	@Override
	public void setDeviceStatus(String deviceStatus) {
		this.deviceStatus = deviceStatus;
	}

	@Override
	public String getIpAddress() {
		return ipAddress;
	}

	@Override
	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}

	@Override
	public String getPlant() {
		return plant;
	}

	@Override
	public void setPlant(String plant) {
		this.plant = plant;
	}

	@Override
	public String getDepartment() {
		return department;
	}

	@Override
	public void setDepartment(String department) {
		this.department = department;
	}

	@Override
	public String getLocation() {
		return location;
	}

	@Override
	public void setLocation(String location) {
		this.location = location;
	}

	@Override
	public String getModel() {
		return model;
	}

	@Override
	public void setModel(String model) {
		this.model = model;
	}

	@Override
	public String getServiceTag() {
		return serviceTag;
	}

	@Override
	public void setServiceTag(String serviceTag) {
		this.serviceTag = serviceTag;
	}

	@Override
	public String getEmpCode() {
		return empCode;
	}

	@Override
	public void setEmpCode(String empCode) {
		this.empCode = empCode;
	}

	@Override
	public String getUserName() {
		return userName;
	}

	@Override
	public void setUserName(String userName) {
		this.userName = userName;
	}

	@Override
	public String getProcessor() {
		return processor;
	}

	@Override
	public void setProcessor(String processor) {
		this.processor = processor;
	}

	@Override
	public String getRam() {
		return ram;
	}

	@Override
	public void setRam(String ram) {
		this.ram = ram;
	}

	@Override
	public String getHdd() {
		return hdd;
	}

	@Override
	public void setHdd(String hdd) {
		this.hdd = hdd;
	}

	@Override
	public String getOs() {
		return os;
	}

	@Override
	public void setOs(String os) {
		this.os = os;
	}

	@Override
	public String getOsServicePack() {
		return osServicePack;
	}

	@Override
	public void setOsServicePack(String osServicePack) {
		this.osServicePack = osServicePack;
	}

	@Override
	public String getMsOffice() {
		return msOffice;
	}

	@Override
	public void setMsOffice(String msOffice) {
		this.msOffice = msOffice;
	}

	@Override
	public String getCharger() {
		return charger;
	}

	@Override
	public void setCharger(String charger) {
		this.charger = charger;
	}

	@Override
	public String getSoftware1() {
		return software1;
	}

	@Override
	public void setSoftware1(String software1) {
		this.software1 = software1;
	}

	@Override
	public String getSoftware2() {
		return software2;
	}

	@Override
	public void setSoftware2(String software2) {
		this.software2 = software2;
	}

	@Override
	public String getClassification() {
		return classification;
	}

	@Override
	public void setClassification(String classification) {
		this.classification = classification;
	}
}
