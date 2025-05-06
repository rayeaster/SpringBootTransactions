package com.trxmgr.hometask.entities;

import java.io.Serializable;
import java.util.List;

public class PageResponse implements Serializable {
	private static final long serialVersionUID = -8415036293654601293L;
	
	private int page;
	private int size;
	private int totalPages;
	private long totalElements;
	private List<BankingTransaction> data;

	public PageResponse() {
		super();
	}

	public PageResponse(int page, int size, int totalPages, long totalElements, List<BankingTransaction> data) {
		super();
		this.page = page;
		this.size = size;
		this.totalPages = totalPages;
		this.totalElements = totalElements;
		this.data = data;
	}

	public int getPage() {
		return page;
	}

	public void setPage(int page) {
		this.page = page;
	}

	public int getSize() {
		return size;
	}

	public void setSize(int size) {
		this.size = size;
	}

	public int getTotalPages() {
		return totalPages;
	}

	public void setTotalPages(int totalPages) {
		this.totalPages = totalPages;
	}

	public long getTotalElements() {
		return totalElements;
	}

	public void setTotalElements(long totalElements) {
		this.totalElements = totalElements;
	}

	public List<BankingTransaction> getData() {
		return data;
	}

	public void setData(List<BankingTransaction> data) {
		this.data = data;
	}
	
	@Override
	public String toString() {
		StringBuilder dataBuilder = new StringBuilder();
		for (int i = 0; i < data.size(); i++) {
			if (i > 0) {
				dataBuilder.append(",");
			}
			dataBuilder.append(data.get(i).toString());
		}
		return "{" 
				+ "page:" + page + ","
				+ "size:" + size + ","
				+ "totalPages:" + totalPages + ","
				+ "totalElements:" + totalElements + ","
				+ "data:["
				+ dataBuilder.toString()
				+ "]}";
		
	}
	
}