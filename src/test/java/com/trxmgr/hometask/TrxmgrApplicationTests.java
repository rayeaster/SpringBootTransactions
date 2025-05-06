package com.trxmgr.hometask;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;

import com.trxmgr.hometask.entities.BankingTransaction;
import com.trxmgr.hometask.entities.PageResponse;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public class TrxmgrApplicationTests {

	@Autowired
	private TestRestTemplate restTemplate;
	@Autowired
	private Environment environment;

	private static final String API_URL = "/transactions";

	private static long MAX_AMOUNT = 10000000000L;
	private static long MAX_ID = 1000000000000L;
	private static int MAX_TYPE = 10000;
	private static final int DEFAULT_THREAD_COUNT = Runtime.getRuntime().availableProcessors();
	private static int PAGE_SIZE = DEFAULT_THREAD_COUNT;

	private static Random r = null;
	private ExecutorService executor = null;

	@BeforeAll
	public static void initTests() {
		r = createRandomGenerator();
	}

	@BeforeEach
	public void initTest() {
		executor = Executors.newFixedThreadPool(getMaxThreadNumber());
	}

	@AfterEach
	public void wrapTest() {
		executor.shutdown();
	}

	///////////////////////////////////////////////////////////////////
	/// Basic tests
	///////////////////////////////////////////////////////////////////

	@Test
	public void testBasic_Create() {
		List<BankingTransaction> newTrxs = createTrxList(1, 0, r);
		BankingTransaction savedTrx = restTemplate.postForObject(API_URL, newTrxs.get(0), BankingTransaction.class);

		String getUrl = getUrl(savedTrx.getId());
		ResponseEntity<BankingTransaction> response = restTemplate.getForEntity(getUrl, BankingTransaction.class);

		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertEquals(savedTrx.getId(), response.getBody().getId());
		assertEquals(savedTrx.getId(), newTrxs.get(0).getId());

		ResponseEntity<BankingTransaction> responseNotExist = restTemplate.getForEntity(getUrl(savedTrx.getId() + 1),
				BankingTransaction.class);

		assertEquals(HttpStatus.NOT_FOUND, responseNotExist.getStatusCode());
	}

	@Test
	public void testBasic_DuplicateCreate() {
		List<BankingTransaction> newTrxs = createTrxList(1, 0, r);
		restTemplate.postForObject(API_URL, newTrxs.get(0), BankingTransaction.class);
		ResponseEntity<BankingTransaction> response = restTemplate.postForEntity(API_URL, newTrxs.get(0),
				BankingTransaction.class);
		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
	}

	@Test
	public void testBasic_Updates() {
		List<BankingTransaction> newTrxs = createTrxList(1, 0, r);
		BankingTransaction savedTrx = restTemplate.postForObject(API_URL, newTrxs.get(0), BankingTransaction.class);
		String getUrl = getUrl(savedTrx.getId());
		ResponseEntity<BankingTransaction> response = restTemplate.getForEntity(getUrl, BankingTransaction.class);

		assertEquals(newTrxs.get(0).getStatus(), 0);
		assertEquals(newTrxs.get(0).getStatus(), response.getBody().getStatus());
		assertEquals(newTrxs.get(0).getAmount(), response.getBody().getAmount());

		long newAmount = newTrxs.get(0).getAmount() + 123;
		String updateUrl = updateAmountUrl(getUrl(savedTrx.getId()), newAmount);
		BankingTransaction updatedTrx = restTemplate.postForObject(updateUrl, null, BankingTransaction.class);
		assertEquals(newAmount, updatedTrx.getAmount());
		response = restTemplate.getForEntity(getUrl, BankingTransaction.class);
		assertEquals(newAmount, response.getBody().getAmount());

		int newStatus = BankingTransaction.COMPLETE_STATUS;
		updateUrl = updateStatusUrl(getUrl(savedTrx.getId()), newStatus);
		updatedTrx = restTemplate.postForObject(updateUrl, null, BankingTransaction.class);
		assertEquals(newStatus, updatedTrx.getStatus());
		response = restTemplate.getForEntity(getUrl, BankingTransaction.class);
		assertEquals(newStatus, response.getBody().getStatus());
	}

	@Test
	public void testBasic_Delete() {
		List<BankingTransaction> newTrxs = createTrxList(1, 0, r);
		restTemplate.postForObject(API_URL, newTrxs.get(0), BankingTransaction.class);

		String getUrl = getUrl(newTrxs.get(0).getId());
		ResponseEntity<BankingTransaction> response = restTemplate.getForEntity(getUrl, BankingTransaction.class);
		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertEquals(newTrxs.get(0).getId(), response.getBody().getId());

		restTemplate.delete(getUrl);
		response = restTemplate.getForEntity(getUrl, BankingTransaction.class);
		assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
	}

	@Test
	public void testBasic_ExceptionHandle() {
		List<BankingTransaction> newTrxs = createTrxList(1, 0, r);
		newTrxs.get(0).setId(-1);
		ResponseEntity<BankingTransaction> response = restTemplate.postForEntity(API_URL, newTrxs.get(0),
				BankingTransaction.class);
		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

		newTrxs.get(0).setId(r.nextLong(0, MAX_ID));
		restTemplate.postForEntity(API_URL, newTrxs.get(0), BankingTransaction.class);

		String getUrl = getUrl(newTrxs.get(0).getId());
		response = restTemplate.getForEntity(getUrl, BankingTransaction.class);
		assertEquals(HttpStatus.OK, response.getStatusCode());
		
		response = restTemplate.postForEntity(updateStatusUrl(getUrl, -1), null, BankingTransaction.class);
		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
	}

	///////////////////////////////////////////////////////////////////
	/// Stress Tests
	///////////////////////////////////////////////////////////////////

	@Test
	public void testStress_GetSingle() throws Exception {
		List<BankingTransaction> newTrxs = createTrxList(1, 0, r);
		restTemplate.postForObject(API_URL, newTrxs.get(0), BankingTransaction.class);
		String getUrl = getUrl(newTrxs.get(0).getId());

		AtomicInteger successCount = new AtomicInteger(0);
		AtomicInteger errorCount = new AtomicInteger(0);

		CompletableFuture<?>[] futures = IntStream.range(0, getMaxThreadNumber())
				.mapToObj(i -> CompletableFuture
						.runAsync(new GetRunnable(restTemplate, getUrl, newTrxs, successCount, errorCount), executor))
				.toArray(CompletableFuture[]::new);

		CompletableFuture.allOf(futures).join();

		assertEquals(successCount.get(), futures.length);
		assertEquals(errorCount.get(), 0);
	}

	@Test
	public void testStress_GetDifferent() throws Exception {
		List<BankingTransaction> newTrxs = createTrxList(getMaxThreadNumber(), 0, r);
		List<BankingTransaction> addedTrxs = new ArrayList<BankingTransaction>();
		for (BankingTransaction trx : newTrxs) {
			BankingTransaction added = restTemplate.postForObject(API_URL, trx, BankingTransaction.class);
			if (added != null) {
				addedTrxs.add(added);
			}
		}

		AtomicInteger successCount = new AtomicInteger(0);
		AtomicInteger errorCount = new AtomicInteger(0);

		CompletableFuture<?>[] futures = IntStream.range(0, getMaxThreadNumber())
				.mapToObj(
						i -> CompletableFuture.runAsync(new GetRunnable(restTemplate, getUrl(addedTrxs.get(i).getId()),
								addedTrxs.subList(i, i + 1), successCount, errorCount), executor))
				.toArray(CompletableFuture[]::new);

		CompletableFuture.allOf(futures).join();

		assertEquals(successCount.get(), futures.length);
		assertEquals(errorCount.get(), 0);
	}

	@Test
	public void testStress_GetPage() throws Exception {
		List<BankingTransaction> newTrxs = createTrxList(getMaxThreadNumber(), 0, r);
		List<BankingTransaction> addedTrxs = getAllData();
		for (BankingTransaction trx : newTrxs) {
			BankingTransaction added = restTemplate.postForObject(API_URL, trx, BankingTransaction.class);
			if (added != null) {
				addedTrxs.add(added);
			}
		}

		AtomicInteger successCount = new AtomicInteger(0);
		AtomicInteger errorCount = new AtomicInteger(0);

		int maxPage = (addedTrxs.size() / PAGE_SIZE) + (addedTrxs.size() % PAGE_SIZE == 0 ? 0 : 1);

		CompletableFuture<?>[] futures = IntStream.range(0, getMaxThreadNumber())
				.mapToObj(i -> CompletableFuture.runAsync(
						new GetPageRunnable(restTemplate, getPageUrl(i % maxPage == 0 ? maxPage : (i % maxPage)),
								getSubListByPage(addedTrxs, i % maxPage == 0 ? maxPage : (i % maxPage)), successCount,
								errorCount),
						executor))
				.toArray(CompletableFuture[]::new);

		CompletableFuture.allOf(futures).join();

		assertEquals(successCount.get(), futures.length);
		assertEquals(errorCount.get(), 0);
	}

	@Test
	public void testStress_Modification() throws Exception {
		List<BankingTransaction> newTrxs = createTrxList(getMaxThreadNumber(), 0, r);
		List<BankingTransaction> addedTrxs = new ArrayList<BankingTransaction>();
		for (BankingTransaction trx : newTrxs) {
			BankingTransaction added = restTemplate.postForObject(API_URL, trx, BankingTransaction.class);
			if (added != null) {
				addedTrxs.add(added);
			}
		}

		List<BankingTransaction> updatedTrxs = new ArrayList<BankingTransaction>();
		List<BankingTransaction> deletedTrxs = new ArrayList<BankingTransaction>();
		for (BankingTransaction trx : addedTrxs) {
			if (trx.getId() % 2 == 0) {
				trx.setAmount(r.nextLong(0, MAX_AMOUNT));
				trx.setStatus(trx.getAmount() % 2 == 0 ? BankingTransaction.COMPLETE_STATUS
						: BankingTransaction.FAILED_STATUS);
				updatedTrxs.add(trx);
			} else {
				deletedTrxs.add(trx);
			}
		}

		AtomicInteger successCount = new AtomicInteger(0);
		AtomicInteger errorCount = new AtomicInteger(0);

		CompletableFuture<?>[] futures = IntStream.range(0, updatedTrxs.size())
				.mapToObj(i -> CompletableFuture.runAsync(new UpdateRunnable(restTemplate,
						getUrl(updatedTrxs.get(i).getId()), updatedTrxs.subList(i, i + 1), successCount, errorCount),
						executor))
				.toArray(CompletableFuture[]::new);

		CompletableFuture<?>[] deleteFutures = IntStream.range(0, deletedTrxs.size())
				.mapToObj(i -> CompletableFuture.runAsync(new DeleteRunnable(restTemplate,
						getUrl(deletedTrxs.get(i).getId()), deletedTrxs.subList(i, i + 1), successCount, errorCount),
						executor))
				.toArray(CompletableFuture[]::new);

		CompletableFuture.allOf(futures).join();
		CompletableFuture.allOf(deleteFutures).join();

		assertEquals(successCount.get(), futures.length + deleteFutures.length);
		assertEquals(errorCount.get(), 0);
	}

	///////////////////////////////////////////////////////////////////
	/// helper methods
	///////////////////////////////////////////////////////////////////

	private static class BaseRunnable {
		public TestRestTemplate restTemplate;
		public String url;
		public List<BankingTransaction> targets;
		public AtomicInteger successCount;
		public AtomicInteger errorCount;

		public BaseRunnable(TestRestTemplate restTemplate, String url, List<BankingTransaction> targets,
				AtomicInteger successCount, AtomicInteger errorCount) {
			super();
			this.restTemplate = restTemplate;
			this.url = url;
			this.targets = targets;
			this.successCount = successCount;
			this.errorCount = errorCount;
		}

	}

	private static class GetRunnable extends BaseRunnable implements Runnable {

		public GetRunnable(TestRestTemplate restTemplate, String url, List<BankingTransaction> targets,
				AtomicInteger successCount, AtomicInteger errorCount) {
			super(restTemplate, url, targets, successCount, errorCount);
		}

		@Override
		public void run() {
			ResponseEntity<BankingTransaction> response = null;
			try {
				response = restTemplate.getForEntity(url, BankingTransaction.class);
				if (response.getStatusCode().is2xxSuccessful()
						&& response.getBody().getId() == targets.get(0).getId()) {
					successCount.incrementAndGet();
				} else {
					System.out.println("Get-errorCode=" + response.getStatusCode() + ",url=" + url);
					errorCount.incrementAndGet();
				}
			} catch (Exception e) {
				e.printStackTrace();
				errorCount.incrementAndGet();
			}

		}

	}

	private static class UpdateRunnable extends BaseRunnable implements Runnable {

		public UpdateRunnable(TestRestTemplate restTemplate, String url, List<BankingTransaction> targets,
				AtomicInteger successCount, AtomicInteger errorCount) {
			super(restTemplate, url, targets, successCount, errorCount);
		}

		@Override
		public void run() {

			String updateAmountUrl = updateAmountUrl(url, targets.get(0).getAmount());
			restTemplate.postForObject(updateAmountUrl, null, BankingTransaction.class);
			String updateStatusUrl = updateStatusUrl(url, targets.get(0).getStatus());
			restTemplate.postForObject(updateStatusUrl, null, BankingTransaction.class);

			ResponseEntity<BankingTransaction> response = null;
			try {
				response = restTemplate.getForEntity(url, BankingTransaction.class);
				if (response.getStatusCode().is2xxSuccessful() && response.getBody().getId() == targets.get(0).getId()
						&& response.getBody().getAmount() == targets.get(0).getAmount()
						&& response.getBody().getStatus() == targets.get(0).getStatus()) {
					successCount.incrementAndGet();
				} else {
					System.out.println("Update-errorCode=" + response.getStatusCode() + ",url=" + url);
					errorCount.incrementAndGet();
				}
			} catch (Exception e) {
				e.printStackTrace();
				errorCount.incrementAndGet();
			}

		}

	}

	private static class DeleteRunnable extends BaseRunnable implements Runnable {

		public DeleteRunnable(TestRestTemplate restTemplate, String url, List<BankingTransaction> targets,
				AtomicInteger successCount, AtomicInteger errorCount) {
			super(restTemplate, url, targets, successCount, errorCount);
		}

		@Override
		public void run() {
			restTemplate.delete(url);

			ResponseEntity<BankingTransaction> response = null;
			try {
				response = restTemplate.getForEntity(url, BankingTransaction.class);
				if (response.getStatusCode().is4xxClientError()) {
					successCount.incrementAndGet();
				} else {
					System.out.println("Delete-errorCode=" + response.getStatusCode() + ",url=" + url);
					errorCount.incrementAndGet();
				}
			} catch (Exception e) {
				e.printStackTrace();
				errorCount.incrementAndGet();
			}

		}

	}

	private static class GetPageRunnable extends BaseRunnable implements Runnable {

		public GetPageRunnable(TestRestTemplate restTemplate, String url, List<BankingTransaction> targets,
				AtomicInteger successCount, AtomicInteger errorCount) {
			super(restTemplate, url, targets, successCount, errorCount);
		}

		@Override
		public void run() {
			ResponseEntity<PageResponse> response = null;
			try {
				response = restTemplate.getForEntity(url, PageResponse.class);
				if (response.getStatusCode().is2xxSuccessful() && checkPageResult(response)) {
					successCount.incrementAndGet();
				} else {
					System.out.println("GetPage-errorCode=" + response.getStatusCode() + ",url=" + url);
					errorCount.incrementAndGet();
				}
			} catch (Exception e) {
				e.printStackTrace();
				errorCount.incrementAndGet();
			}

		}

		private boolean checkPageResult(ResponseEntity<PageResponse> response) {
			if (response == null || !(response.getBody() instanceof PageResponse)) {
				return false;
			}
			PageResponse pageResponse = response.getBody();
			List<BankingTransaction> pageData = pageResponse.getData();
			if (pageData == null || pageData.size() != targets.size()) {
				return false;
			}
			for (int i = 0; i < targets.size(); i++) {
				if (targets.get(i).getId() != pageData.get(i).getId()) {
					return false;
				}
			}
			return true;
		}

	}

	private static Random createRandomGenerator() {
		return new Random(System.currentTimeMillis());
	}

	private static String getUrl(long id) {
		return API_URL + "/" + id;
	}

	private static String getPageUrl(int pageNo) {
		return API_URL + "?pageNo=" + pageNo + "&pageSize=" + PAGE_SIZE;
	}

	private static String updateAmountUrl(String idURL, long newAmount) {
		return idURL + "/amount?amount=" + newAmount;
	}

	private static String updateStatusUrl(String idURL, int newStatus) {
		return idURL + "/status?status=" + newStatus;
	}

	private static List<BankingTransaction> createTrxList(int size, long startID, Random r) {
		List<BankingTransaction> ret = new ArrayList<>(size);
		for (int i = 0; i < size; i++) {
			BankingTransaction trx = new BankingTransaction();
			trx.setId(startID > 0 ? (startID + i) : r.nextLong(0, MAX_ID));
			trx.setUser(r.nextLong(0, MAX_ID));
			trx.setAmount(r.nextLong(0, MAX_AMOUNT));
			trx.setType(r.nextInt(0, MAX_TYPE));
			ret.add(trx);
		}
		return ret;
	}

	private static List<BankingTransaction> getSubListByPage(List<BankingTransaction> data, int pageNo) {
		int start = (pageNo - 1) * PAGE_SIZE;
		int end = Math.min(start + PAGE_SIZE, data.size());
		return data.subList(start, end);
	}

	private List<BankingTransaction> getAllData() {
		List<BankingTransaction> all = new ArrayList<BankingTransaction>();
		int maxPage = 1;
		long totals = 0;
		int currentPage = 1;
		ResponseEntity<PageResponse> firstPage = restTemplate.getForEntity(getPageUrl(1), PageResponse.class);
		if (firstPage.getStatusCode().is2xxSuccessful() && (firstPage.getBody() instanceof PageResponse)) {
			PageResponse pageResponse = firstPage.getBody();

			maxPage = pageResponse.getTotalPages();
			totals = pageResponse.getTotalElements();
			all.addAll(pageResponse.getData());
			currentPage += 1;

			while (currentPage <= maxPage) {
				ResponseEntity<PageResponse> iterPage = restTemplate.getForEntity(getPageUrl(currentPage),
						PageResponse.class);
				if (iterPage.getStatusCode().is2xxSuccessful() && (iterPage.getBody() instanceof PageResponse)) {
					all.addAll(iterPage.getBody().getData());
				}
				currentPage += 1;
			}
		}
		assertEquals(totals, all.size());
		return all;
	}

	private int getMaxThreadNumber() {
		String acceptCount = environment.getProperty("server.tomcat.accept-count");
		String maxThread = environment.getProperty("server.tomcat.threads.max");
		if (acceptCount != null && maxThread != null) {
			int ac = Integer.parseInt(acceptCount);
			int mt = Integer.parseInt(maxThread);
			return ac > mt ? (ac - mt) : ac;
		} else {
			return DEFAULT_THREAD_COUNT;
		}
	}

}
