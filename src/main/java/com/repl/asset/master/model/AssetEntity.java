package com.repl.asset.master.model;

/**
 * The full asset record a laptop or desktop carries, on top of what SpecEntity already covers.
 *
 * SpecEntity is the thin shape — a name, a status, the stamps — and is all the admin masters
 * need. This is the wide one: everything the DESKTOP and LAPTOP sheets of the asset workbook
 * record about a machine, which is what the user side fills in.
 *
 * It EXTENDS SpecEntity rather than replacing it, so DesktopMaster and LaptopMaster stay
 * usable by AbstractSpecService exactly as before. The admin screens keep working on the name
 * alone while the user screens work on the whole row.
 *
 * The workbook's columns map here one to one, minus two:
 *
 *   SR  is the row number on the sheet, which is the table's own id.
 *   AGE is (last date of warranty − term) measured to today. It changes by itself as months
 *       pass, so storing it would be wrong the next day — it is worked out on read.
 *
 * Everything here is optional. The sheet leaves most cells blank on most rows, and a form
 * that refused to save without them could not record the machines the company actually has.
 */
public interface AssetEntity extends SpecEntity {

	/** Excel B — the purchase order this machine came in on. */
	String getPoNo();

	void setPoNo(String poNo);

	/** Excel C — the asset code stuck on the case. */
	String getAssetCode();

	void setAssetCode(String assetCode);

	/** Excel D — the manufacturer's serial number. */
	String getAssetSn();

	void setAssetSn(String assetSn);

	/**
	 * Excel F — IN USE or IN SPARE.
	 *
	 * Named deviceStatus, not status: SpecEntity already has getStatus, and that one is the
	 * row's own 1 / 0 / -1. A machine can perfectly well be an active row that is IN SPARE.
	 */
	String getDeviceStatus();

	void setDeviceStatus(String deviceStatus);

	/** Excel G. */
	String getIpAddress();

	void setIpAddress(String ipAddress);

	/** Excel H. */
	String getPlant();

	void setPlant(String plant);

	/** Excel I. */
	String getDepartment();

	void setDepartment(String department);

	/** Excel J. */
	String getLocation();

	void setLocation(String location);

	/** Excel K, the sheet's TYPE column — "DELL Latitude 3420". */
	String getModel();

	void setModel(String model);

	/** Excel L. */
	String getServiceTag();

	void setServiceTag(String serviceTag);

	/** Excel M — the employee code of whoever holds the machine. */
	String getEmpCode();

	void setEmpCode(String empCode);

	/** Excel U — that employee's name as the sheet writes it. */
	String getUserName();

	void setUserName(String userName);

	/** Excel N. */
	String getProcessor();

	void setProcessor(String processor);

	/** Excel O. */
	String getRam();

	void setRam(String ram);

	/** Excel P. */
	String getHdd();

	void setHdd(String hdd);

	/** Excel V. */
	String getOs();

	void setOs(String os);

	/** Excel W. */
	String getOsServicePack();

	void setOsServicePack(String osServicePack);

	/** Excel Y. */
	String getMsOffice();

	void setMsOffice(String msOffice);

	/** Excel Z. */
	String getCharger();

	void setCharger(String charger);

	/** Excel AA. */
	String getSoftware1();

	void setSoftware1(String software1);

	/** Excel AB. */
	String getSoftware2();

	void setSoftware2(String software2);

	/** Excel AD — critical or non-critical. */
	String getClassification();

	void setClassification(String classification);
}
