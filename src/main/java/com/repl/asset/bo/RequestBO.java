package com.repl.asset.bo;

public class RequestBO {

	private String quick_finder;
	private boolean include_inactive_data;

	public String getQuick_finder() {
		return quick_finder;
	}

	public void setQuick_finder(String quick_finder) {
		this.quick_finder = quick_finder;
	}

	public boolean isInclude_inactive_data() {
		return include_inactive_data;
	}

	public void setInclude_inactive_data(boolean include_inactive_data) {
		this.include_inactive_data = include_inactive_data;
	}
}
