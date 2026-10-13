package com.repl.asset.exception;

public class ApplicationException extends Exception {

	private static final long serialVersionUID = 1L;

	private int code;
	private String message = null;
	private Object msg;

	/*
	 * The HRMS copy names this parameter dataNotFound and then assigns this.code = code,
	 * which assigns the field to itself — getCode() there always returns 0. Assigning the
	 * parameter is the whole point of the constructor, so it is assigned properly here.
	 */
	public ApplicationException(int code, String message) {
		super(message);
		this.code = code;
		this.message = message;
	}

	public int getCode() {
		return code;
	}

	@Override
	public String getMessage() {
		return message;
	}

	public Object getMsg() {
		return msg;
	}

	public void setMsg(Object msg) {
		this.msg = msg;
	}

	public void setCode(int code) {
		this.code = code;
	}

	public void setMessage(String message) {
		this.message = message;
	}
}
