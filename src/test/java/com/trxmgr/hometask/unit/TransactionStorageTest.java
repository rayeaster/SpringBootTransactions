package com.trxmgr.hometask.unit;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.trxmgr.hometask.entities.BankingTransaction;
import com.trxmgr.hometask.services.TransactionStorage;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class TransactionStorageTest {

	private static long MAX_AMOUNT = 100000000;
	private static Random r = null;

	@BeforeAll
	public static void initTest() {
		r = new Random(System.currentTimeMillis());
	}

	@InjectMocks
	private TransactionStorage trxStorage;

	@Test
	public void testAdd() {

		BankingTransaction trx = createNewTrx(0, 0);
		boolean added = trxStorage.add(trx);
		assertTrue(added);
		assertFalse(trxStorage.add(trx));

		BankingTransaction addedTrx = trxStorage.get(trx.getId());
		assertTrue(addedTrx != null);
		assertEquals(addedTrx.getStatus(), 0);
		assertTrue(addedTrx.duplicate(trx));

		int size = trxStorage.size();
		assertEquals(size, 1);
	}

	@Test
	public void testDelete() {

		int size = 10188;
		Set<Long> toDeleteIds = new HashSet<>();
		BankingTransaction trx = null;
		for (int i = 0; i < size; i++) {
			trx = createNewTrx(0, 0);

			while (!trxStorage.add(trx)) {
				trx = createNewTrx(0, 0);
			}

			if ((new Random()).nextBoolean()) {
				toDeleteIds.add(trx.getId());
			}
		}
		assertEquals(size, trxStorage.size());

		int toDeleteSize = toDeleteIds.size();
		for (Long id : toDeleteIds) {
			assertEquals(id.longValue(), trxStorage.get(id).getId());
			trxStorage.delete(id);
			assertEquals(null, trxStorage.get(id));
		}
		assertEquals(size - toDeleteSize, trxStorage.size());

	}

	@Test
	public void testUpdateStatus() {

		BankingTransaction trx = createNewTrx(0, 0);
		trxStorage.add(trx);

		int newStatus = BankingTransaction.COMPLETE_STATUS;
		BankingTransaction updatedTrx = trxStorage.updateStatus(trx.getId(), newStatus);
		assertEquals(updatedTrx.getStatus(), newStatus);

		updatedTrx = trxStorage.get(trx.getId());
		assertEquals(updatedTrx.getStatus(), newStatus);
	}

	@Test
	public void testUpdateAmount() {

		BankingTransaction trx = createNewTrx(0, 0);
		trxStorage.add(trx);

		long newAmount = trx.getAmount() + 1;
		BankingTransaction updatedTrx = trxStorage.updateAmount(trx.getId(), newAmount);
		assertEquals(updatedTrx.getAmount(), newAmount);

		updatedTrx = trxStorage.get(trx.getId());
		assertEquals(updatedTrx.getAmount(), newAmount);
	}

	@Test
	public void testPagination() {
		int size = 10188;
		int pageSize = 100;

		Map<Integer, Long> checkpoints = new HashMap<>();
		BankingTransaction trx = null;
		for (int i = 0; i < size; i++) {
			trx = createNewTrx(0, 0);

			while (!trxStorage.add(trx)) {
				trx = createNewTrx(0, 0);
			}

			if ((i + 1) % pageSize == 0) {
				int pageNo = (i + 1) / pageSize;
				checkpoints.put(pageNo, trx.getId());
			}
		}
		if (size % pageSize > 0 && trx != null) {
			checkpoints.put(size - 1, trx.getId());
		}

		int totalPages = trxStorage.getTotalPages(pageSize);

		for (int pageNo = 1; pageNo <= totalPages; pageNo++) {
			List<BankingTransaction> page = trxStorage.getPage(pageNo, pageSize);
			BankingTransaction pageLast = page.get(page.size() - 1);
			long chkPoint = -1;
			if (pageNo < totalPages) {
				assertEquals(pageSize, page.size());
				chkPoint = checkpoints.get(pageNo);
			} else {
				assertEquals(size % pageSize > 0 ? (size % pageSize) : pageSize, page.size());
				chkPoint = checkpoints.get(size - 1);
			}
			assertEquals(chkPoint, pageLast.getId());
		}

	}

	private static BankingTransaction createNewTrx(long givenID, int type) {
		BankingTransaction trx = new BankingTransaction();
		trx.setId(givenID > 0 ? givenID : r.nextLong(0, MAX_AMOUNT));
		trx.setUser(r.nextLong(0, MAX_AMOUNT));
		trx.setAmount(r.nextLong(0, MAX_AMOUNT));
		trx.setType(type);
		return trx;
	}

}