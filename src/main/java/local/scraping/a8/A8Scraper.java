package local.scraping.a8;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.options.LoadState;

/**
 * A8.net プログラム検索ページのスクレイパー
 *
 * 処理の流れ:
 *   1. ログイン
 *   2. プログラム検索ページでカテゴリをクリック
 *   3. カテゴリ結果ページから詳細ページのURLを全ページ分収集
 *   4. 各詳細ページに遷移して7項目を抽出
 */
public class A8Scraper {

	// ---------------------------------------------------------------
	// 定数
	// ---------------------------------------------------------------

	/** ログインページ */
	private static final String LOGIN_URL = "https://www.a8.net/";

	/** プログラム検索ページ（ログイン後） */
	static final String SEARCH_URL = "https://pub.a8.net/a8v2/media/searchAction.do";

	/** リクエスト間隔（ms） */
	static final int REQUEST_DELAY_MS = 1500;

	/** タイムアウト時のリトライ上限 */
	private static final int RETRY_MAX = 3;

	/** リトライ待機時間（ms）。attempt 回目は RETRY_WAIT_MS * attempt 待機する */
	private static final long RETRY_WAIT_MS = 5_000;

	/** 対象カテゴリ（17カテゴリ） */
	static final List<String> TARGET_CATEGORIES = Arrays.asList(
			"総合通販", "健康", "美容", "グルメ・食品", "ファッション",
			"旅行", "金融・投資・保険", "不動産・引越", "仕事情報",
			"学び・資格", "暮らし", "Webサービス", "インターネット接続",
			"エンタメ", "ギフト", "スポーツ・趣味", "結婚・恋愛");

	// ---------------------------------------------------------------
	// ログイン
	// ---------------------------------------------------------------

	public static void login(Page page, String username, String password) {
		System.out.println("ログイン中...");
		page.navigate(LOGIN_URL);
		page.fill("input[name='login']", username);
		page.fill("input[name='passwd']", password);
		page.click("input[name='login_as_btn']");
		page.waitForLoadState(LoadState.NETWORKIDLE);
		System.out.println("ログイン成功: " + page.url());
	}

	// ---------------------------------------------------------------
	// 1カテゴリをスクレイピング（公開メソッド）
	// ---------------------------------------------------------------

	/**
	 * プログラム検索ページに遷移してカテゴリをクリックし、
	 * 全プログラムをスクレイピングして返す。
	 */
	public static List<ProgramInfo> scrapeCategoryByName(Page page, String categoryName)
			throws InterruptedException {
		return scrapeCategoryByName(page, categoryName, Integer.MAX_VALUE);
	}

	public static List<ProgramInfo> scrapeCategoryByName(Page page, String categoryName, int maxItems)
			throws InterruptedException {

		// プログラム検索ページへ遷移してカテゴリをクリック
		page.navigate(SEARCH_URL);
		page.waitForLoadState(LoadState.LOAD);
		Thread.sleep(REQUEST_DELAY_MS);

		Locator links = page.locator("a").filter(
				new Locator.FilterOptions().setHasText(categoryName));
		if (links.count() == 0) {
			System.err.println("  カテゴリリンクが見つかりません: " + categoryName);
			return new ArrayList<>();
		}
		// 要素は DOM に存在するが CSS で非表示のため dispatchEvent で強制実行
		links.first().dispatchEvent("click");
		page.waitForLoadState(LoadState.LOAD);
		Thread.sleep(REQUEST_DELAY_MS);
		System.out.println("  カテゴリページ遷移完了: " + page.url());

		// 全ページから詳細URLを収集（maxItems で上限あり）
		List<String> detailUrls = collectAllDetailUrls(page, maxItems);
		System.out.printf("  詳細URL収集完了: %d件%n", detailUrls.size());

		// 各詳細ページをスクレイピング
		List<ProgramInfo> programs = new ArrayList<>();
		for (int i = 0; i < detailUrls.size(); i++) {
			String detailUrl = detailUrls.get(i);
			String label = detailUrl.contains("insIds=")
					? detailUrl.replaceAll(".*insIds=([^&]+).*", "$1")
					: String.valueOf(i + 1);
			System.out.printf("  詳細取得中 (%d/%d): %s%n", i + 1, detailUrls.size(), label);
			try {
				ProgramInfo info = scrapeDetailPage(page, detailUrl, categoryName);
				programs.add(info);
				System.out.println("    → " + info.programTitle);
			} catch (Exception e) {
				System.err.printf("    取得失敗: %s%n", e.getMessage());
			}
			Thread.sleep(REQUEST_DELAY_MS);
		}
		return programs;
	}

	// ---------------------------------------------------------------
	// 全ページをめぐって詳細URLを収集
	// ---------------------------------------------------------------

	private static List<String> collectAllDetailUrls(Page page, int maxItems)
			throws InterruptedException {
		List<String> all = new ArrayList<>();
		int pageNum = 1;
		while (true) {
			List<String> urls = collectDetailUrlsFromCurrentPage(page);
			all.addAll(urls);
			System.out.printf("  ページ%d: %d件 (累計%d件)%n", pageNum, urls.size(), all.size());

			if (all.size() >= maxItems)
				break;

			boolean hasNext = false;
			PlaywrightException lastException = null;
			for (int attempt = 1; attempt <= RETRY_MAX; attempt++) {
				try {
					hasNext = goToNextPage(page);
					lastException = null;
					break;
				} catch (PlaywrightException e) {
					lastException = e;
					if (attempt < RETRY_MAX) {
						System.err.printf("  [次ページリトライ %d/%d] ページ%d%n", attempt, RETRY_MAX - 1, pageNum);
						Thread.sleep(RETRY_WAIT_MS * attempt);
					}
				}
			}
			if (lastException != null)
				throw lastException;
			if (!hasNext)
				break;
			pageNum++;
			Thread.sleep(REQUEST_DELAY_MS);
		}
		List<String> result = all.stream().distinct().collect(Collectors.toList());
		return result.size() > maxItems ? result.subList(0, maxItems) : result;
	}

	/**
	 * 現在のカテゴリ結果ページから詳細ページへのリンクURLを収集する。
	 * href に "joinPrograms/detail.do" を含むリンクを対象とする。
	 */
	private static List<String> collectDetailUrlsFromCurrentPage(Page page) {
		return page.locator("a[href*='joinPrograms/detail.do']").all().stream()
				.map(el -> el.getAttribute("href"))
				.filter(href -> href != null && !href.isBlank())
				.map(href -> toAbsoluteUrl(href, page.url()))
				.distinct()
				.collect(Collectors.toList());
	}

	// ---------------------------------------------------------------
	// 次ページへ移動。次ページがなければ false を返す
	// ---------------------------------------------------------------

	private static boolean goToNextPage(Page page) throws InterruptedException {
		Locator nextBtn = page.locator(
				"a:has-text('次へ'), a:has-text('次ページ'), a:has-text('次のページ'), " +
						".pager a[rel='next'], .pagination a:last-child");
		if (nextBtn.count() == 0)
			return false;

		Locator first = nextBtn.first();
		String href = first.getAttribute("href");
		if (href != null && !href.startsWith("javascript")) {
			page.navigate(toAbsoluteUrl(href, page.url()));
		} else {
			// JavaScript href（例: javascript:move(...)）は dispatchEvent で強制実行
			first.dispatchEvent("click");
		}
		page.waitForLoadState(LoadState.LOAD);
		Thread.sleep(1000);
		return true;
	}

	// ---------------------------------------------------------------
	// 詳細ページから7項目を抽出
	// ---------------------------------------------------------------

	public static ProgramInfo scrapeDetailPage(Page page, String url, String categoryName)
			throws InterruptedException {
		PlaywrightException lastException = null;
		for (int attempt = 1; attempt <= RETRY_MAX; attempt++) {
			try {
				page.navigate(url);
				page.waitForLoadState(LoadState.LOAD);
				Thread.sleep(500);

				String category = categoryName.isBlank() ? getCategoryFromPage(page) : categoryName;
				String advertiserName = getText(page, ".company");
				String programTitle = getText(page, ".pgName");
				String siteUrl = getHref(page, ".allianceBtnBox a.subBtn:first-child");
				String reward = getText(page, ".amountBox dl.base:first-child dd .bold");
				String epc = getTextByIndex(page, ".amountBox dl.base.other dd", 0);
				String confirmRate = getTextByIndex(page, ".amountBox dl.base.other dd", 1);

				return new ProgramInfo(category, advertiserName, programTitle,
						siteUrl, reward, epc, confirmRate);
			} catch (PlaywrightException e) {
				lastException = e;
				if (attempt < RETRY_MAX) {
					System.err.printf("    [リトライ %d/%d] %s%n", attempt, RETRY_MAX - 1,
							e.getMessage().split("\n")[0]);
					Thread.sleep(RETRY_WAIT_MS * attempt);
				}
			}
		}
		throw lastException;
	}

	/** .status 内の "カテゴリ" dl の dd テキストを返す */
	private static String getCategoryFromPage(Page page) {
		try {
			return page.locator(".status dl.base")
					.filter(new Locator.FilterOptions().setHasText("カテゴリ"))
					.locator("dd")
					.first()
					.innerText()
					.strip();
		} catch (Exception e) {
			return "";
		}
	}

	/** セレクタにマッチする n番目（0始まり）の要素のテキストを返す */
	private static String getTextByIndex(Page page, String selector, int index) {
		try {
			Locator loc = page.locator(selector);
			if (loc.count() <= index)
				return "";
			return loc.nth(index).innerText().strip();
		} catch (Exception e) {
			return "";
		}
	}

	/** セレクタにマッチする最初の要素の href 属性を返す */
	private static String getHref(Page page, String selector) {
		try {
			Locator loc = page.locator(selector);
			if (loc.count() == 0)
				return "";
			String href = loc.first().getAttribute("href");
			return href == null ? "" : href.strip();
		} catch (Exception e) {
			return "";
		}
	}

	/** セレクタにマッチする最初の要素のテキストを返す。なければ "" */
	private static String getText(Page page, String selector) {
		try {
			Locator loc = page.locator(selector);
			if (loc.count() == 0)
				return "";
			return loc.first().innerText().strip();
		} catch (Exception e) {
			return "";
		}
	}

	/** 相対URLを絶対URLに変換する */
	static String toAbsoluteUrl(String href, String baseUrl) {
		if (href.startsWith("http"))
			return href;
		try {
			return java.net.URI.create(baseUrl).resolve(href).toString();
		} catch (Exception e) {
			return href;
		}
	}
}
