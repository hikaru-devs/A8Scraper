package local.scraping.a8;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;

/**
 * A8.net スクレイパー エントリーポイント
 *
 * 実行前に確認すること:
 *   1. {@code src/main/resources/a8-scraper.properties}（{@code a8-scraper.properties.example} をコピーして作成）
 *   2. 任意キー {@code a8.output.dir}（未設定時は実行ディレクトリ直下の output フォルダ）
 */
public class A8ScraperMain {

	private static final String CONFIG_RESOURCE = "/a8-scraper.properties";

	private static Properties loadProperties() throws IOException {
		InputStream raw = A8ScraperMain.class.getResourceAsStream(CONFIG_RESOURCE);
		if (raw == null) {
			throw new IllegalStateException(
					"a8-scraper.properties がクラスパスにありません。"
							+ " src/main/resources/a8-scraper.properties.example を"
							+ " src/main/resources/a8-scraper.properties にコピーし、a8.username / a8.password を設定してください。");
		}
		try (InputStreamReader reader = new InputStreamReader(raw, StandardCharsets.UTF_8)) {
			Properties p = new Properties();
			p.load(reader);
			return p;
		}
	}

	private static String propRequired(Properties p, String key) {
		String v = p.getProperty(key);
		if (v == null || v.isBlank())
			throw new IllegalStateException(
					"a8-scraper.properties に " + key + " が設定されていません（値が空です）。");
		return v.strip();
	}

	private static String resolveOutputDir(Properties p) {
		String v = p.getProperty("a8.output.dir");
		if (v != null && !v.isBlank())
			return v.strip();
		return Paths.get(System.getProperty("user.dir"), "output").toAbsolutePath().normalize().toString();
	}

	/**
	 * true: 最初の1カテゴリ・最大3件だけ取得するお試しモード
	 * 動作確認が取れたら false に変更して全件実行する
	 */
	private static final boolean TRIAL_MODE = false;
	private static final int TRIAL_MAX = 3;

	public static void main(String[] args) throws InterruptedException, IOException {

		Properties config = loadProperties();
		String username = propRequired(config, "a8.username");
		String password = propRequired(config, "a8.password");
		String outputDir = resolveOutputDir(config);

		System.out.println("===== A8.net スクレイパー開始 =====");
		String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm"));

		Paths.get(outputDir).toFile().mkdirs();

		try (Playwright playwright = Playwright.create()) {
			Browser browser = playwright.chromium().launch(
					new BrowserType.LaunchOptions()
							.setHeadless(false)
							.setSlowMo(200));
			BrowserContext context = browser.newContext(
					new Browser.NewContextOptions()
							.setViewportSize(1280, 900));
			Page page = context.newPage();

			// ---- ログイン ----
			A8Scraper.login(page, username, password);

			// ---- カテゴリごとにスクレイピング → 中間保存 ----
			List<ProgramInfo> allPrograms = new ArrayList<>();

			for (String categoryName : A8Scraper.TARGET_CATEGORIES) {
				System.out.printf("%n=== [%s] 処理開始 ===%n", categoryName);
				if (TRIAL_MODE)
					System.out.println("  ※ TRIAL_MODE: 最初の1カテゴリのみ実行します");

				try {
					int limit = TRIAL_MODE ? TRIAL_MAX : Integer.MAX_VALUE;
					List<ProgramInfo> categoryPrograms = A8Scraper.scrapeCategoryByName(page, categoryName, limit);

					allPrograms.addAll(categoryPrograms);

					// カテゴリ単位で中間保存（途中で止まっても損失最小化）
					String categoryFile = Paths.get(outputDir, "A8_" + categoryName + "_" + timestamp + ".xlsx").toString();
					ExcelWriter.write(categoryPrograms, categoryFile);
					System.out.printf("  保存完了: %s (%d件)%n", categoryFile, categoryPrograms.size());

				} catch (Exception e) {
					System.err.println("  カテゴリ処理エラー [" + categoryName + "]: " + e.getMessage());
				}

				if (TRIAL_MODE)
					break; // 1カテゴリで終了
			}

			// ---- 全カテゴリ統合ファイルを出力 ----
			String allFile = Paths.get(outputDir, "A8_全カテゴリ_" + timestamp + ".xlsx").toString();
			ExcelWriter.write(allPrograms, allFile);

			System.out.println("\n===== 完了 =====");
			System.out.println("合計取得件数: " + allPrograms.size() + "件");
			System.out.println("出力先: " + outputDir);

			browser.close();
		}
	}
}
