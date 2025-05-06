package com.trxmgr.hometask.controllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.trxmgr.hometask.entities.BankingTransaction;
import com.trxmgr.hometask.entities.PageResponse;
import com.trxmgr.hometask.services.TransactionStorage;

@RestController
@Validated
@RequestMapping("/transactions")
public class TrxMgrController {

	@Autowired
	private TransactionStorage trxStorage;

	@GetMapping(value = "/{id}", produces = "application/json")
	public ResponseEntity<BankingTransaction> getTransaction(@PathVariable Long id) {
		BankingTransaction trx = trxStorage.get(id);
		return getResponse(trx);
	}

	@PostMapping(produces = "application/json")
	public ResponseEntity<BankingTransaction> createTransaction(@RequestBody BankingTransaction newTrx) {
		boolean addSuccess = trxStorage.add(newTrx);
		BankingTransaction trx = addSuccess ? trxStorage.get(newTrx.getId()) : null;
		return trx == null ? ResponseEntity.badRequest().build() : ResponseEntity.ok(trx);
	}

	@DeleteMapping(value = "/{id}", produces = "application/json")
	public ResponseEntity<BankingTransaction> deleteTransaction(@PathVariable Long id) {
		BankingTransaction trx = trxStorage.delete(id);
		return getResponse(trx);
	}

	@PostMapping(value = "/{id}/status", produces = "application/json")
	public ResponseEntity<BankingTransaction> updateTransactionStatus(@PathVariable Long id, @RequestParam @NonNull Integer status) {
		BankingTransaction trx = trxStorage.updateStatus(id, status);
		return getResponse(trx);
	}

	@PostMapping(value = "/{id}/amount", produces = "application/json")
	public ResponseEntity<BankingTransaction> updateTransactionAmount(@PathVariable Long id,
			@RequestParam @NonNull Long amount) {
		BankingTransaction trx = trxStorage.updateAmount(id, amount);
		return getResponse(trx);
	}

	@GetMapping(produces = "application/json")
	public ResponseEntity<PageResponse> getTransactions(@RequestParam(defaultValue = "1") int pageNo,
			@RequestParam(defaultValue = "10") int pageSize) {

		List<BankingTransaction> pagedTrx = trxStorage.getPage(pageNo, pageSize);
		int totalTrx = trxStorage.size();
		int totalPage = trxStorage.getTotalPages(pageSize);

		PageResponse page = new PageResponse(pageNo, pageSize, totalPage, totalTrx, pagedTrx);
		return ResponseEntity.ok(page);
	}

	private ResponseEntity<BankingTransaction> getResponse(BankingTransaction trx) {
		return trx == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(trx);
	}

}