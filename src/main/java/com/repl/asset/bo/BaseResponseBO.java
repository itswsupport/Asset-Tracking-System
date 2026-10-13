package com.repl.asset.bo;

import java.util.ArrayList;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class BaseResponseBO {

	@JsonProperty("status_code")
	public int status_code;

	@JsonProperty("message")
	public String message;

	@JsonProperty("response")
	public Object response;

	@JsonProperty("timestamp")
	public Date timestamp;

	@JsonProperty("ArrayList")
	public ArrayList<?> iterableObject;

	public BaseResponseBO() {
		super();
	}

	public BaseResponseBO(String message) {
		this.message = message;
	}

	public int getStatus_code() {
		return status_code;
	}

	public void setStatus_code(int status_code) {
		this.status_code = status_code;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public Object getResponse() {
		return response;
	}

	public void setResponse(Object response) {
		this.response = response;
	}

	public Date getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}

	public void setIterable(ArrayList<?> iterableObject) {
		this.iterableObject = iterableObject;
	}

	public ArrayList<?> getIterableObject() {
		return iterableObject;
	}
}
