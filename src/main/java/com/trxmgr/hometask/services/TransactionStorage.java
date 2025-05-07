package com.trxmgr.hometask.services;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;

import org.springframework.stereotype.Service;

import com.trxmgr.hometask.entities.BankingTransaction;
import com.trxmgr.hometask.entities.InvalidTransationException;

import jakarta.annotation.PreDestroy;

@Service
public class TransactionStorage implements Serializable {

	private static final long serialVersionUID = -1801684368463403223L;

	Map<Long, BankingTransaction> trxMap;
	List<BankingTransaction> trxList;
	ForkJoinPool searchPool;

	public TransactionStorage() {
		super();
		this.trxMap = new ConcurrentHashMap<>();
		this.trxList = new ArrayList<>();
		this.searchPool = new ForkJoinPool();
	}

	@PreDestroy
	public void cleanup() {
		searchPool.shutdown();
	}

	public boolean add(BankingTransaction trx) {
		if (trxMap.containsKey(trx.getId())) {
			return false;
		}
		if (trx.getId() < 0) {
			throw new InvalidTransationException(InvalidTransationException.InvalidField.id,
					String.valueOf(trx.getId()));
		}
		if (trx.getType() < 0 || trx.getType() > BankingTransaction.MAX_TYPE) {
			throw new InvalidTransationException(InvalidTransationException.InvalidField.type,
					String.valueOf(trx.getType()));
		}
		if (trx.getUser() < 0) {
			throw new InvalidTransationException(InvalidTransationException.InvalidField.user,
					String.valueOf(trx.getUser()));
		}
		if (trx.getAmount() < 0) {
			throw new InvalidTransationException(InvalidTransationException.InvalidField.amount,
					String.valueOf(trx.getAmount()));
		}

		trx.setStatus(0);
		trx.setCreated(System.currentTimeMillis());
		trx.setUpdated(System.currentTimeMillis());

		trxMap.put(trx.getId(), trx);

		synchronized (trxList) {
			return trxList.add(trx);
		}
	}

	public BankingTransaction delete(long id) {
		BankingTransaction trx = trxMap.remove(id);

		if (trx == null) {
			return trx;
		}

		synchronized (trxList) {
			int targetIdx = search(id);
			if (targetIdx >= 0) {
				trxList.remove(targetIdx);
			}
		}
		return trx;
	}

	public BankingTransaction updateStatus(long id, int status) {
		BankingTransaction trx = trxMap.get(id);
		if (trx == null) {
			return trx;
		}
		if (status != BankingTransaction.COMPLETE_STATUS && status != BankingTransaction.FAILED_STATUS) {
			throw new InvalidTransationException(InvalidTransationException.InvalidField.status,
					String.valueOf(trx.getStatus()));
		}

		trx.setStatus(status);
		trx.setUpdated(System.currentTimeMillis());

		return trx;
	}

	public BankingTransaction updateAmount(long id, long amount) {
		BankingTransaction trx = trxMap.get(id);
		if (trx == null) {
			return trx;
		}
		if (trx.getAmount() < 0) {
			throw new InvalidTransationException(InvalidTransationException.InvalidField.amount,
					String.valueOf(trx.getAmount()));
		}

		trx.setAmount(amount);
		trx.setUpdated(System.currentTimeMillis());

		return trx;
	}

	public List<BankingTransaction> getPage(int pageNo, int pageSize) {
		int start = (pageNo - 1) * pageSize;
		int totalSize = size();

		List<BankingTransaction> pagedTrxs = Collections.emptyList();
		if (start >= totalSize) {
			return pagedTrxs;
		}

		int end = Math.min(start + pageSize, totalSize);
		synchronized (trxList) {
			pagedTrxs = trxList.subList(start, end);
		}
		return pagedTrxs;
	}

	public int size() {
		return trxMap.size();
	}

	public BankingTransaction get(long id) {
		return trxMap.get(id);
	}

	private int search(long id) {
		int targetIdx = searchPool.invoke(new TrxSearcher(trxList, id));
//		System.out.println("storage index for given target[" + id + "] is " + targetIdx);
		TrxSearcher.reset();
		return targetIdx;
	}

	public int getTotalPages(int pageSize) {
		int size = trxMap.size();
		return (size / pageSize) + (size % pageSize == 0 ? 0 : 1);
	}

	private static class TrxSearcher extends RecursiveTask<Integer> {

		private static final long serialVersionUID = -3415402803986145706L;

		private static final int SEQUENTIAL_THRESHOLD = 10000;

		private List<BankingTransaction> all;
		private int start;
		private int end;
		private long target;
		private static volatile int foundIdx = -1;

		TrxSearcher(List<BankingTransaction> data, int start, int end, long target) {
			this.all = data;
			this.start = start;
			this.end = end;
			this.target = target;
		}

		TrxSearcher(List<BankingTransaction> data, long target) {
			this(data, 0, data.size(), target);
		}

		static void reset() {
			foundIdx = -1;
		}

		@Override
		protected Integer compute() {
			final int length = end - start;

			if (length <= SEQUENTIAL_THRESHOLD) {
				return computeDirectly();
			}

			final int split = length / 2;

			final TrxSearcher left = new TrxSearcher(all, start, start + split, target);
			left.fork();

			final TrxSearcher right = new TrxSearcher(all, start + split, end, target);
			return Math.max(right.compute(), left.join());
		}

		private Integer computeDirectly() {
//			System.out.println(Thread.currentThread() + " computing from index " + start + " to " + end);

			for (int i = start; i < end; i++) {
				if (foundIdx < 0 && all.get(i).getId() == target) {
					foundIdx = i;
					return i;
				}
			}

			return -1;
		}

	}

}