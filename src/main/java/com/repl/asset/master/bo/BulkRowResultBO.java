package com.repl.asset.master.bo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The outcome of one row of a BULK UPLOAD — in the same order the rows were sent, so the
 * screen can put each result back against its Excel row.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BulkRowResultBO {

	/** Position in the request, from 0. */
	@JsonProperty("index")
	private int index;

	@JsonProperty("success")
	private boolean success;

	/** Why it failed, in the same words the normal form would show; null on success. */
	@JsonProperty("message")
	private String message;

	/** What was saved: the device / server / product name. */
	@JsonProperty("name")
	private String name;

	/** A device's reference number once saved. */
	@JsonProperty("reference_no")
	private String reference_no;

	public static BulkRowResultBO ok(int index, String name, String referenceNo) {
		return ok(index, name, referenceNo, null);
	}

	/** Saved, with a note — e.g. "Assigned to 101099", or why the assignment did not go in. */
	public static BulkRowResultBO ok(int index, String name, String referenceNo, String note) {
		BulkRowResultBO r = new BulkRowResultBO();
		r.index = index;
		r.success = true;
		r.name = name;
		r.reference_no = referenceNo;
		r.message = note;
		return r;
	}

	public static BulkRowResultBO failed(int index, String name, String message) {
		BulkRowResultBO r = new BulkRowResultBO();
		r.index = index;
		r.success = false;
		r.name = name;
		r.message = message;
		return r;
	}

	public int getIndex() {
		return index;
	}

	public boolean isSuccess() {
		return success;
	}

	public String getMessage() {
		return message;
	}

	public String getName() {
		return name;
	}

	public String getReference_no() {
		return reference_no;
	}
}
