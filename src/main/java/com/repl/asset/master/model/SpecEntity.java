package com.repl.asset.master.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * The shape shared by the four hardware spec masters — RAM, Processor, HDD and OS.
 *
 * Each has its own table with its own name column (ram_name, processor_name, …), so they
 * cannot be one entity. This interface is what lets one service and one set of endpoints
 * serve all four instead of four near-identical copies: getName/setName hide which column
 * the concrete entity actually maps.
 */
public interface SpecEntity {

	Long getId();

	String getName();

	void setName(String name);

	int getStatus();

	void setStatus(int status);

	LocalDate getCreateDate();

	void setCreateDate(LocalDate createDate);

	/*
	 * Date and time are two fields rather than one LocalDateTime because they are two
	 * columns, DATE and TIME. Keeping the Java side the same shape as the table means no
	 * splitting or recombining on the way in or out.
	 */
	LocalTime getCreateTime();

	void setCreateTime(LocalTime createTime);

	/**
	 * How many bits — "64 - Bit", "32 - Bit" — for the masters that have one.
	 *
	 * A default of null rather than an abstract method, because only the device masters
	 * (laptop, desktop, tablet) carry this. RAM, Processor, HDD and OS have no system type
	 * and simply do not override it; SpecBO is @JsonInclude(NON_NULL), so nothing extra
	 * appears in their responses.
	 *
	 * That keeps one shared service serving both kinds of master without the spec lists
	 * growing a field that means nothing to them.
	 */
	default String getSystemType() {
		return null;
	}

	default void setSystemType(String systemType) {
		// Masters without a system type ignore it.
	}

	/*
	 * The warranty details each device came with in the asset workbook, so the Warranty form
	 * can fill itself in from the device chosen. Defaults again, for the same reason as
	 * above: RAM, Processor, HDD and OS have no warranty of their own.
	 *
	 * Note what is NOT here — age. It is (last date − term) measured to today, so it changes
	 * by itself as months pass; storing it would be wrong the next day. WarrantyService works
	 * it out on every read from these two.
	 */
	default String getWarrantyStatus() {
		return null;
	}

	default void setWarrantyStatus(String warrantyStatus) {
		// Masters without a warranty ignore it.
	}

	default LocalDate getLastDateOfWarranty() {
		return null;
	}

	default void setLastDateOfWarranty(LocalDate lastDateOfWarranty) {
		// Masters without a warranty ignore it.
	}

	default BigDecimal getTotalWarrantyYears() {
		return null;
	}

	default void setTotalWarrantyYears(BigDecimal totalWarrantyYears) {
		// Masters without a warranty ignore it.
	}

	/** A free-text note beside the name. Every master table has this column. */
	String getDescription();

	void setDescription(String description);

	/** The save stamp. Every implementing entity already has this column. */
	String getRegTime();

	void setRegTime(String regTime);

	Long getRegBy();

	void setRegBy(Long regBy);
}
