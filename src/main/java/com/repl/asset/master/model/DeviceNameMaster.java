package com.repl.asset.master.model;

import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * One named machine — "101" — together with the model it is and the kind of device that is.
 *
 * The third level of the chain the admin screens build up:
 *
 *   ams_device_master   desktop                          the kind
 *   ams_model_master    desktop + "HP 39373"             the model of that kind
 *   ams_device_name_master  desktop + HP 39373 + "101"   this actual machine
 *
 * NOT a SpecEntity: that interface is built around a single name, while this row carries a
 * device, a model AND a name.
 *
 * BOTH device_id and model_id are stored, although model_id alone would imply the device.
 * The extra column costs nothing and means the device is available to query and sort on
 * without joining through the model table every time. DeviceNameService is what keeps the two
 * honest: it refuses a model that does not belong to the chosen device, so the pair can never
 * drift apart.
 *
 * device_name is UNIQUE across the whole table, not per model. This name is what identifies a
 * physical machine to the people using it — two machines both called "101" cannot be told
 * apart in any list, whatever models they are.
 *
 * create_date and create_time are stored but not shown on screen, as in every master here.
 */
@Entity
@Table(name = "ams_device_name_master")
public class DeviceNameMaster {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;

	/** Which kind of device this is, as an id into ams_device_master. */
	@Column(name = "device_id", nullable = false)
	private Long deviceId;

	/** Which model it is, as an id into ams_model_master. Always belongs to device_id. */
	@Column(name = "model_id", nullable = false)
	private Long modelId;

	/** What this machine is called — "101", "IT-14". */
	@Column(name = "device_name", nullable = false, length = 200)
	private String deviceName;

	/** A free-text note beside the name. */
	@Column(name = "description", length = 255)
	private String description;

	/**
	 * Whether this machine has been handed out: "Open" from the moment the name is created,
	 * "Assigned" once it is added through ADD DEVICE (AssetService sets it), and "Open" again
	 * if that device is deleted. DEVICE STATUS reads its Open / Assigned counts from here.
	 *
	 * Not to be confused with `status` below, the row's own active/inactive/deleted flag.
	 */
	@Column(name = "allocation_status", nullable = false, length = 20)
	private String allocationStatus = ALLOCATION_OPEN;

	public static final String ALLOCATION_OPEN = "Open";
	public static final String ALLOCATION_ASSIGNED = "Assigned";

	/**
	 * The machine's reference number, e.g. DE26HR01007, generated the moment the name is
	 * created here (ReferenceNumberService). ADD DEVICE gives the machine this same number when
	 * it is registered, so it never changes between the two. ref_fy / ref_seq hold the year and
	 * running number apart, the same as on ams_add_device_master.
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

	public Long getId() {
		return id;
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

	public Long getModelId() {
		return modelId;
	}

	public void setModelId(Long modelId) {
		this.modelId = modelId;
	}

	public String getDeviceName() {
		return deviceName;
	}

	public void setDeviceName(String deviceName) {
		this.deviceName = deviceName;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getAllocationStatus() {
		return allocationStatus;
	}

	public void setAllocationStatus(String allocationStatus) {
		this.allocationStatus = allocationStatus;
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
