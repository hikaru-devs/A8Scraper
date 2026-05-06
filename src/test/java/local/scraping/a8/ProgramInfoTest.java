package local.scraping.a8;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ProgramInfo のユニットテスト
 *
 * ProgramInfo は依存クラスを持たないデータモデルのため @InjectMocks / @Mock は使用しない。
 * コンストラクタのロジック（null→空文字変換・空白トリム）と toString を検証する。
 */
@ExtendWith(MockitoExtension.class)
class ProgramInfoTest {

    // ---- フィールド宣言（値なし） ----
    ProgramInfo sut;        // 全フィールドに有効値を持つ標準インスタンス
    ProgramInfo nullSut;    // 全フィールドが null のインスタンス
    ProgramInfo paddedSut;  // 全フィールドに前後空白を含むインスタンス

    @BeforeEach
    void setUp() {
        sut = new ProgramInfo(
                "健康", "株式会社サプリA", "毎日サプリ定期購入プログラム",
                "https://example.com/supri", "新規1000円", "12.5", "80%");
        nullSut = new ProgramInfo(
                null, null, null, null, null, null, null);
        paddedSut = new ProgramInfo(
                "  健康  ", "  株式会社サプリA  ", "  毎日サプリ  ",
                "  https://example.com  ", "  新規1000円  ", "  12.5  ", "  80%  ");
    }

    // ================================================================
    // 正常系（一番太い）
    // ================================================================

    /**
     * 全フィールドに有効値を渡したとき、7項目が正しく格納されることを確認する。
     *
     * <p>テスト対象: {@link ProgramInfo#ProgramInfo}</p>
     * <p>観点: 一番太い正常系。全フィールドを個別にアサートする。</p>
     */
    @Test
    void programInfoTest001() {
        assertThat(sut.category).isEqualTo("健康");
        assertThat(sut.advertiserName).isEqualTo("株式会社サプリA");
        assertThat(sut.programTitle).isEqualTo("毎日サプリ定期購入プログラム");
        assertThat(sut.siteUrl).isEqualTo("https://example.com/supri");
        assertThat(sut.reward).isEqualTo("新規1000円");
        assertThat(sut.epc).isEqualTo("12.5");
        assertThat(sut.confirmRate).isEqualTo("80%");
    }

    // ================================================================
    // 正常系バリエーション
    // ================================================================

    /**
     * null を渡したとき、全フィールドが空文字に変換されることを確認する。
     *
     * <p>テスト対象: {@link ProgramInfo#ProgramInfo}</p>
     * <p>観点: 正常系バリエーション。nvl ヘルパーの null→空文字変換を検証する。</p>
     */
    @Test
    void programInfoTest002() {
        assertThat(nullSut.category).isEmpty();
        assertThat(nullSut.advertiserName).isEmpty();
        assertThat(nullSut.programTitle).isEmpty();
        assertThat(nullSut.siteUrl).isEmpty();
        assertThat(nullSut.reward).isEmpty();
        assertThat(nullSut.epc).isEmpty();
        assertThat(nullSut.confirmRate).isEmpty();
    }

    /**
     * 前後に空白を含む値を渡したとき、全フィールドがトリムされることを確認する。
     *
     * <p>テスト対象: {@link ProgramInfo#ProgramInfo}</p>
     * <p>観点: 正常系バリエーション。strip() によるトリム処理を全フィールドで検証する。</p>
     */
    @Test
    void programInfoTest003() {
        assertThat(paddedSut.category).isEqualTo("健康");
        assertThat(paddedSut.advertiserName).isEqualTo("株式会社サプリA");
        assertThat(paddedSut.programTitle).isEqualTo("毎日サプリ");
        assertThat(paddedSut.siteUrl).isEqualTo("https://example.com");
        assertThat(paddedSut.reward).isEqualTo("新規1000円");
        assertThat(paddedSut.epc).isEqualTo("12.5");
        assertThat(paddedSut.confirmRate).isEqualTo("80%");
    }

    /**
     * toString() が「[カテゴリ] プログラムタイトル / 成果報酬」形式で返ることを確認する。
     *
     * <p>テスト対象: {@link ProgramInfo#toString}</p>
     * <p>観点: 正常系バリエーション。フォーマット文字列の出力内容を完全一致で検証する。</p>
     */
    @Test
    void programInfoTest004() {
        assertThat(sut.toString()).isEqualTo("[健康] 毎日サプリ定期購入プログラム / 新規1000円");
    }

    // ================================================================
    // 異常系
    // ================================================================

    /**
     * 全フィールドが null（空文字変換済み）でも toString() が例外なく返ることを確認する。
     *
     * <p>テスト対象: {@link ProgramInfo#toString}</p>
     * <p>観点: 異常系。String.format に空文字が入る場合、"[]  / "（スペース2つ）になる仕様を確認する。</p>
     */
    @Test
    void programInfoTest005() {
        // "[%s] %s / %s" に空文字が入るため "] " と " /" の間にスペースが2つ入る
        assertThat(nullSut.toString()).isEqualTo("[]  / ");
    }
}
