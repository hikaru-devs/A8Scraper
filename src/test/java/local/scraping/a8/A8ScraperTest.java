package local.scraping.a8;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.LoadState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * A8Scraper のユニットテスト
 *
 * A8Scraper は static メソッドのみのユーティリティクラスのため @InjectMocks は使用しない。
 * ページ操作は Playwright の Page / Locator をモック化して検証する。
 *
 * coverage: excluded
 *   - scrapeAllCategories / collectAllDetailUrls / goToNextPage（ページネーション全体）
 *     → 実ブラウザが必要な統合テスト対象。単体では検証不可。
 */
@ExtendWith(MockitoExtension.class)
class A8ScraperTest {

    /** goToNextPage で使用する次ページボタンのセレクタ（コードと完全一致が必要） */
    private static final String NEXT_PAGE_SELECTOR =
            "a:has-text('次へ'), a:has-text('次ページ'), a:has-text('次のページ'), " +
            ".pager a[rel='next'], .pagination a:last-child";

    // ---- Playwright モック ----
    @Mock Page page;

    // ---- scrapeDetailPage 用 Locator モック ----
    @Mock Locator companyLocator;
    @Mock Locator pgNameLocator;
    @Mock Locator siteUrlLocator;
    @Mock Locator rewardLocator;
    @Mock Locator epcAndConfirmLocator;
    @Mock Locator epcLocator;
    @Mock Locator confirmLocator;

    // ---- getCategoryFromPage チェーン用 Locator モック ----
    @Mock Locator statusDlLocator;
    @Mock Locator filteredStatusLocator;
    @Mock Locator ddLocator;
    @Mock Locator ddFirstLocator;

    // ---- scrapeCategoryByName 用 Locator モック ----
    @Mock Locator aLocator;
    @Mock Locator filteredCategoryLocator;
    @Mock Locator categoryFirstLocator;
    @Mock Locator detailLinkLocator;
    @Mock Locator detailLinkItemLocator;
    @Mock Locator nextPageLocator;

    // ---- テストデータ ----
    String detailUrl;

    @BeforeEach
    void setUp() {
        detailUrl = "https://pub.a8.net/a8v2/media/joinPrograms/detail.do?insIds=s00000000404014";
    }

    // ================================================================
    // scrapeDetailPage — 一番太い正常系
    // ================================================================

    /**
     * 全セレクタが正常に値を返すとき、7項目が全て抽出されることを確認する。
     *
     * <p>テスト対象: {@link A8Scraper#scrapeDetailPage}</p>
     * <p>観点: 一番太い正常系。getText / getHref / getTextByIndex の全パスを通す。</p>
     */
    @Test
    void a8ScraperTest001() throws InterruptedException {
        when(page.locator(".company")).thenReturn(companyLocator);
        when(companyLocator.count()).thenReturn(1);
        when(companyLocator.first()).thenReturn(companyLocator);
        when(companyLocator.innerText()).thenReturn("株式会社サプリA");

        when(page.locator(".pgName")).thenReturn(pgNameLocator);
        when(pgNameLocator.count()).thenReturn(1);
        when(pgNameLocator.first()).thenReturn(pgNameLocator);
        when(pgNameLocator.innerText()).thenReturn("毎日サプリ定期購入プログラム");

        when(page.locator(".allianceBtnBox a.subBtn:first-child")).thenReturn(siteUrlLocator);
        when(siteUrlLocator.count()).thenReturn(1);
        when(siteUrlLocator.first()).thenReturn(siteUrlLocator);
        when(siteUrlLocator.getAttribute("href")).thenReturn("https://example.com/supri");

        when(page.locator(".amountBox dl.base:first-child dd .bold")).thenReturn(rewardLocator);
        when(rewardLocator.count()).thenReturn(1);
        when(rewardLocator.first()).thenReturn(rewardLocator);
        when(rewardLocator.innerText()).thenReturn("新規1000円");

        when(page.locator(".amountBox dl.base.other dd")).thenReturn(epcAndConfirmLocator);
        when(epcAndConfirmLocator.count()).thenReturn(2);
        when(epcAndConfirmLocator.nth(0)).thenReturn(epcLocator);
        when(epcAndConfirmLocator.nth(1)).thenReturn(confirmLocator);
        when(epcLocator.innerText()).thenReturn("12.5");
        when(confirmLocator.innerText()).thenReturn("80%");

        ProgramInfo result = A8Scraper.scrapeDetailPage(page, detailUrl, "健康");

        assertThat(result.category).isEqualTo("健康");
        assertThat(result.advertiserName).isEqualTo("株式会社サプリA");
        assertThat(result.programTitle).isEqualTo("毎日サプリ定期購入プログラム");
        assertThat(result.siteUrl).isEqualTo("https://example.com/supri");
        assertThat(result.reward).isEqualTo("新規1000円");
        assertThat(result.epc).isEqualTo("12.5");
        assertThat(result.confirmRate).isEqualTo("80%");
    }

    // ================================================================
    // scrapeDetailPage — 正常系バリエーション
    // ================================================================

    /**
     * categoryName が空文字のとき、getCategoryFromPage でページ内から取得されることを確認する。
     *
     * <p>テスト対象: {@link A8Scraper#scrapeDetailPage}</p>
     * <p>観点: 正常系バリエーション。getCategoryFromPage のロケーターチェーンを検証する。</p>
     */
    @Test
    void a8ScraperTest002() throws InterruptedException {
        when(page.locator(".status dl.base")).thenReturn(statusDlLocator);
        when(statusDlLocator.filter(any(Locator.FilterOptions.class))).thenReturn(filteredStatusLocator);
        when(filteredStatusLocator.locator("dd")).thenReturn(ddLocator);
        when(ddLocator.first()).thenReturn(ddFirstLocator);
        when(ddFirstLocator.innerText()).thenReturn("健康");

        ProgramInfo result = A8Scraper.scrapeDetailPage(page, detailUrl, "");

        assertThat(result.category).isEqualTo("健康");
    }

    /**
     * EPC・確定率のロケーター件数が不足しているとき、両フィールドが空文字になることを確認する。
     *
     * <p>テスト対象: {@link A8Scraper#scrapeDetailPage}</p>
     * <p>観点: 正常系バリエーション。getTextByIndex の count <= index 分岐を検証する。</p>
     */
    @Test
    void a8ScraperTest003() throws InterruptedException {
        when(page.locator(".amountBox dl.base.other dd")).thenReturn(epcAndConfirmLocator);
        when(epcAndConfirmLocator.count()).thenReturn(0);

        ProgramInfo result = A8Scraper.scrapeDetailPage(page, detailUrl, "健康");

        assertThat(result.epc).isEmpty();
        assertThat(result.confirmRate).isEmpty();
    }

    /**
     * href 属性が null のとき、siteUrl が空文字になることを確認する。
     *
     * <p>テスト対象: {@link A8Scraper#scrapeDetailPage}</p>
     * <p>観点: 正常系バリエーション。getHref の null ガード分岐を検証する。</p>
     */
    @Test
    void a8ScraperTest004() throws InterruptedException {
        when(page.locator(".allianceBtnBox a.subBtn:first-child")).thenReturn(siteUrlLocator);
        when(siteUrlLocator.count()).thenReturn(1);
        when(siteUrlLocator.first()).thenReturn(siteUrlLocator);
        when(siteUrlLocator.getAttribute("href")).thenReturn(null);

        ProgramInfo result = A8Scraper.scrapeDetailPage(page, detailUrl, "健康");

        assertThat(result.siteUrl).isEmpty();
    }

    // ================================================================
    // scrapeDetailPage — 異常系
    // ================================================================

    /**
     * テキストロケーターの件数が 0 件のとき、対象フィールドが空文字になることを確認する。
     *
     * <p>テスト対象: {@link A8Scraper#scrapeDetailPage}</p>
     * <p>観点: 異常系。getText の count == 0 分岐（マッチなし）を検証する。</p>
     */
    @Test
    void a8ScraperTest005() throws InterruptedException {
        when(page.locator(".company")).thenReturn(companyLocator);
        when(companyLocator.count()).thenReturn(0);

        ProgramInfo result = A8Scraper.scrapeDetailPage(page, detailUrl, "健康");

        assertThat(result.advertiserName).isEmpty();
    }

    /**
     * innerText() 呼び出しで例外が発生しても、対象フィールドが空文字になることを確認する。
     *
     * <p>テスト対象: {@link A8Scraper#scrapeDetailPage}</p>
     * <p>観点: 異常系。getText の例外ハンドリング（try-catch → ""）を検証する。</p>
     */
    @Test
    void a8ScraperTest006() throws InterruptedException {
        when(page.locator(".company")).thenReturn(companyLocator);
        when(companyLocator.count()).thenReturn(1);
        when(companyLocator.first()).thenReturn(companyLocator);
        when(companyLocator.innerText()).thenThrow(new RuntimeException("playwright error"));

        ProgramInfo result = A8Scraper.scrapeDetailPage(page, detailUrl, "健康");

        assertThat(result.advertiserName).isEmpty();
    }

    /**
     * categoryName が空文字でページ内取得も失敗したとき、category が空文字になることを確認する。
     *
     * <p>テスト対象: {@link A8Scraper#scrapeDetailPage}</p>
     * <p>観点: 異常系。getCategoryFromPage の例外ハンドリング（try-catch → ""）を検証する。</p>
     */
    @Test
    void a8ScraperTest007() throws InterruptedException {
        when(page.locator(".status dl.base")).thenThrow(new RuntimeException("locator error"));

        ProgramInfo result = A8Scraper.scrapeDetailPage(page, detailUrl, "");

        assertThat(result.category).isEmpty();
    }

    // ================================================================
    // login — 一番太い正常系
    // ================================================================

    /**
     * 正常な認証情報を渡したとき、navigate / fill / click / waitForLoadState が正しい引数で呼ばれることを確認する。
     *
     * <p>テスト対象: {@link A8Scraper#login}</p>
     * <p>観点: 一番太い正常系。Mockito verify でページ操作の呼び出しシーケンスを検証する。</p>
     */
    @Test
    void a8ScraperTest008() {
        when(page.url()).thenReturn("https://pub.a8.net/a8v2/media/memberAction.do");

        A8Scraper.login(page, "testuser", "testpass");

        verify(page).navigate("https://www.a8.net/");
        verify(page).fill("input[name='login']", "testuser");
        verify(page).fill("input[name='passwd']", "testpass");
        verify(page).click("input[name='login_as_btn']");
        verify(page).waitForLoadState(LoadState.NETWORKIDLE);
    }

    // ================================================================
    // scrapeCategoryByName — 一番太い正常系
    // ================================================================

    /**
     * カテゴリリンクをクリックして詳細URLを収集し、1件スクレイピングして返すことを確認する。
     *
     * <p>テスト対象: {@link A8Scraper#scrapeCategoryByName(Page, String, int)}</p>
     * <p>観点: 一番太い正常系。カテゴリクリック→URL収集→詳細スクレイピングの全パスを通す。</p>
     */
    @Test
    void a8ScraperTest009() throws InterruptedException {
        // カテゴリリンク（dispatchEvent 用）
        when(page.locator("a")).thenReturn(aLocator);
        when(aLocator.filter(any(Locator.FilterOptions.class))).thenReturn(filteredCategoryLocator);
        when(filteredCategoryLocator.count()).thenReturn(1);
        when(filteredCategoryLocator.first()).thenReturn(categoryFirstLocator);

        // 詳細URL収集（1件）
        when(page.locator("a[href*='joinPrograms/detail.do']")).thenReturn(detailLinkLocator);
        when(detailLinkLocator.all()).thenReturn(List.of(detailLinkItemLocator));
        when(detailLinkItemLocator.getAttribute("href")).thenReturn(
                "https://pub.a8.net/a8v2/media/joinPrograms/detail.do?insIds=s00000000404014");

        // 次ページなし
        when(page.locator(NEXT_PAGE_SELECTOR)).thenReturn(nextPageLocator);
        when(nextPageLocator.count()).thenReturn(0);

        // scrapeDetailPage 用スタブ
        when(page.locator(".company")).thenReturn(companyLocator);
        when(companyLocator.count()).thenReturn(1);
        when(companyLocator.first()).thenReturn(companyLocator);
        when(companyLocator.innerText()).thenReturn("株式会社サプリA");

        when(page.locator(".pgName")).thenReturn(pgNameLocator);
        when(pgNameLocator.count()).thenReturn(1);
        when(pgNameLocator.first()).thenReturn(pgNameLocator);
        when(pgNameLocator.innerText()).thenReturn("毎日サプリ定期購入プログラム");

        when(page.locator(".allianceBtnBox a.subBtn:first-child")).thenReturn(siteUrlLocator);
        when(siteUrlLocator.count()).thenReturn(0);
        when(page.locator(".amountBox dl.base:first-child dd .bold")).thenReturn(rewardLocator);
        when(rewardLocator.count()).thenReturn(0);
        when(page.locator(".amountBox dl.base.other dd")).thenReturn(epcAndConfirmLocator);
        when(epcAndConfirmLocator.count()).thenReturn(0);

        List<ProgramInfo> result = A8Scraper.scrapeCategoryByName(page, "総合通販", 1);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).category).isEqualTo("総合通販");
        assertThat(result.get(0).advertiserName).isEqualTo("株式会社サプリA");
        assertThat(result.get(0).programTitle).isEqualTo("毎日サプリ定期購入プログラム");
    }

    // ================================================================
    // scrapeCategoryByName — 正常系バリエーション
    // ================================================================

    /**
     * カテゴリリンクが 0 件のとき、空リストを返すことを確認する。
     *
     * <p>テスト対象: {@link A8Scraper#scrapeCategoryByName(Page, String, int)}</p>
     * <p>観点: 正常系バリエーション。カテゴリリンク未発見時の早期リターンを検証する。</p>
     */
    @Test
    void a8ScraperTest010() throws InterruptedException {
        when(page.locator("a")).thenReturn(aLocator);
        when(aLocator.filter(any(Locator.FilterOptions.class))).thenReturn(filteredCategoryLocator);
        when(filteredCategoryLocator.count()).thenReturn(0);

        List<ProgramInfo> result = A8Scraper.scrapeCategoryByName(page, "総合通販", 10);

        assertThat(result).isEmpty();
    }

    /**
     * カテゴリページに詳細URLが 0 件のとき、空リストを返すことを確認する。
     *
     * <p>テスト対象: {@link A8Scraper#scrapeCategoryByName(Page, String, int)}</p>
     * <p>観点: 正常系バリエーション。詳細URL未取得時の動作を検証する。</p>
     */
    @Test
    void a8ScraperTest011() throws InterruptedException {
        when(page.locator("a")).thenReturn(aLocator);
        when(aLocator.filter(any(Locator.FilterOptions.class))).thenReturn(filteredCategoryLocator);
        when(filteredCategoryLocator.count()).thenReturn(1);
        when(filteredCategoryLocator.first()).thenReturn(categoryFirstLocator);

        when(page.locator("a[href*='joinPrograms/detail.do']")).thenReturn(detailLinkLocator);
        when(detailLinkLocator.all()).thenReturn(List.of());

        when(page.locator(NEXT_PAGE_SELECTOR)).thenReturn(nextPageLocator);
        when(nextPageLocator.count()).thenReturn(0);

        List<ProgramInfo> result = A8Scraper.scrapeCategoryByName(page, "総合通販", 10);

        assertThat(result).isEmpty();
    }

    /**
     * maxItems で URL 収集件数が制限され、結果リストが maxItems 以下になることを確認する。
     *
     * <p>テスト対象: {@link A8Scraper#scrapeCategoryByName(Page, String, int)}</p>
     * <p>観点: 正常系バリエーション。maxItems が詳細URL収集の上限として機能することを検証する。</p>
     */
    @Test
    void a8ScraperTest012() throws InterruptedException {
        when(page.locator("a")).thenReturn(aLocator);
        when(aLocator.filter(any(Locator.FilterOptions.class))).thenReturn(filteredCategoryLocator);
        when(filteredCategoryLocator.count()).thenReturn(1);
        when(filteredCategoryLocator.first()).thenReturn(categoryFirstLocator);

        // 2件のリンクを返すが maxItems=1 なので1件に制限される
        when(page.locator("a[href*='joinPrograms/detail.do']")).thenReturn(detailLinkLocator);
        when(detailLinkLocator.all()).thenReturn(List.of(detailLinkItemLocator, detailLinkItemLocator));
        when(detailLinkItemLocator.getAttribute("href"))
                .thenReturn("https://example.com/detail.do?insIds=s001")
                .thenReturn("https://example.com/detail.do?insIds=s002");

        when(page.locator(NEXT_PAGE_SELECTOR)).thenReturn(nextPageLocator);
        when(nextPageLocator.count()).thenReturn(0);

        when(page.locator(".company")).thenReturn(companyLocator);
        when(companyLocator.count()).thenReturn(0);
        when(page.locator(".pgName")).thenReturn(pgNameLocator);
        when(pgNameLocator.count()).thenReturn(0);
        when(page.locator(".allianceBtnBox a.subBtn:first-child")).thenReturn(siteUrlLocator);
        when(siteUrlLocator.count()).thenReturn(0);
        when(page.locator(".amountBox dl.base:first-child dd .bold")).thenReturn(rewardLocator);
        when(rewardLocator.count()).thenReturn(0);
        when(page.locator(".amountBox dl.base.other dd")).thenReturn(epcAndConfirmLocator);
        when(epcAndConfirmLocator.count()).thenReturn(0);

        List<ProgramInfo> result = A8Scraper.scrapeCategoryByName(page, "総合通販", 1);

        assertThat(result).hasSize(1);
    }

    // ================================================================
    // toAbsoluteUrl — 一番太い正常系
    // ================================================================

    /**
     * http で始まる絶対 URL はそのまま返すことを確認する。
     *
     * <p>テスト対象: {@link A8Scraper#toAbsoluteUrl}</p>
     * <p>観点: 一番太い正常系。絶対 URL のパススルーを検証する。</p>
     */
    @Test
    void a8ScraperTest013() {
        String result = A8Scraper.toAbsoluteUrl("https://example.com/page", "https://base.com/");

        assertThat(result).isEqualTo("https://example.com/page");
    }

    // ================================================================
    // toAbsoluteUrl — 正常系バリエーション
    // ================================================================

    /**
     * 相対 URL を baseURL を使って絶対 URL に変換することを確認する。
     *
     * <p>テスト対象: {@link A8Scraper#toAbsoluteUrl}</p>
     * <p>観点: 正常系バリエーション。URI.create.resolve による相対 URL 解決を検証する。</p>
     */
    @Test
    void a8ScraperTest014() {
        String result = A8Scraper.toAbsoluteUrl(
                "/a8v2/media/searchAction/category.do",
                "https://pub.a8.net/a8v2/media/searchAction.do");

        assertThat(result).isEqualTo("https://pub.a8.net/a8v2/media/searchAction/category.do");
    }

    // ================================================================
    // toAbsoluteUrl — 異常系
    // ================================================================

    /**
     * baseURL が不正な文字列のとき、href をそのまま返すことを確認する。
     *
     * <p>テスト対象: {@link A8Scraper#toAbsoluteUrl}</p>
     * <p>観点: 異常系。URI.create 例外時のフォールバック（href をそのまま返す）を検証する。</p>
     */
    @Test
    void a8ScraperTest015() {
        String result = A8Scraper.toAbsoluteUrl("/relative/path", ":::invalid:::");

        assertThat(result).isEqualTo("/relative/path");
    }
}
