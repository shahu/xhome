package com.xhome.comtroller.model;

/**
 * 
 * @author shahu
 * 
 */
public class Result {
	public static final int RC_OK = 0;
	public static final int RC_FAILURE = 1;

	private String message;
	private int errorCode;
	private String result;

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public int getErrorCode() {
		return errorCode;
	}

	public void setErrorCode(int errorCode) {
		this.errorCode = errorCode;
	}

	public String getResult() {
		return result;
	}

	public void setResult(String result) {
		this.result = result;
	}

}
