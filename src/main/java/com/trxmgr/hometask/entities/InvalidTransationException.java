package com.trxmgr.hometask.entities;

public class InvalidTransationException extends IllegalArgumentException {

	private static final long serialVersionUID = 5449543259582020780L;

	public static enum InvalidField {
		id, type, user, amount, status
	}

	public InvalidTransationException(InvalidField field, String val) {
		super();
		this.field = field;
		this.invalidValue = val;
	}

	private InvalidField field;
	private String invalidValue;

	public InvalidField getField() {
		return field;
	}

	public void setField(InvalidField field) {
		this.field = field;
	}

	public String getInvalidValue() {
		return invalidValue;
	}

	public void setInvalidValue(String invalidValue) {
		this.invalidValue = invalidValue;
	}

	@Override
	public String toString() {
		return "invalid transaction with field-" + field + "[" + invalidValue + "]";
	}
}