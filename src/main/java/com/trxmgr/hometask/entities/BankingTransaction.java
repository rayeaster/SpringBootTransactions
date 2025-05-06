package com.trxmgr.hometask.entities;

import java.io.Serializable;

public class BankingTransaction implements Serializable {

	private static final long serialVersionUID = 6099936964604749629L;
	
	public static int COMPLETE_STATUS = 1;
	public static int FAILED_STATUS = 2;
	public static int MAX_TYPE = 10000;

	private long id;
	private int type;
	private long user;
	private long amount;
	private int status;
	private long created;
	private long updated;	

	public BankingTransaction() {
		super();
	}

	public BankingTransaction(long id, int type, long user, long amount, int status, long created, long updated) {
		super();
		this.id = id;
		this.type = type;
		this.user = user;
		this.amount = amount;
		this.status = status;
		this.created = created;
		this.updated = updated;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public long getUser() {
		return user;
	}

	public void setUser(long user) {
		this.user = user;
	}

	public long getAmount() {
		return amount;
	}

	public void setAmount(long amount) {
		this.amount = amount;
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public long getCreated() {
		return created;
	}

	public void setCreated(long created) {
		this.created = created;
	}

	public long getUpdated() {
		return updated;
	}

	public void setUpdated(long updated) {
		this.updated = updated;
	}

	public static long getSerialversionuid() {
		return serialVersionUID;
	}
	
	public boolean duplicate(BankingTransaction other) {
		return (other.getId() == this.id);
	}

	@Override
	public String toString() {
		return "{" 
				+ "id:" + id + ","
				+ "type:" + type + "," 
				+ "user:" + user + "," 
				+ "amount:" + amount + "," 
				+ "status:" + status + ","	            
				+ "created:" + created + "," 
				+ "updated:" + updated 
				+ "}";
	}

}