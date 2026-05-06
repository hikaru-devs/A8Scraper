package local.scraping.a8;

import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * ExcelWriter のユニットテスト
 *
 * ExcelWriter は static メソッドのみのユーティリティクラスであり、
 * インジェクション対象の依存クラスが存在しないため @InjectMocks / @Mock は使用しない。
 * その他のテスト基準（AssertJ・@BeforeEach・テスト順序）はすべて適用する。
 */
@ExtendWith(MockitoExtension.class)
class ExcelWriterTest {

    // ---- フィールド宣言（値なし） ----
    List<ProgramInfo> twoPrograms;
    ProgramInfo program1;
    ProgramInfo program2;
    String tempFilePath;

    @BeforeEach
    void setUp() throws IOException {
        program1 = new ProgramInfo(
                "健康", "株式会社サプリA", "毎日サプリ定期購入プログラム",
                "https://example.com/supri", "新規1000円", "12.5", "80%");
        program2 = new ProgramInfo(
                "美容", "株式会社スキンB", "スキンケア定期購入プログラム",
                "https://example.com/skin", "新規2000円", "8.3", "65%");
        twoPrograms = Arrays.asList(program1, program2);
        tempFilePath = Files.createTempFile("a8test_", ".xlsx").toString();
    }

    @AfterEach
    void tearDown() throws IOException {
        Files.deleteIfExists(Paths.get(tempFilePath));
    }

    // ================================================================
    // 正常系（一番太い）
    // ================================================================

    /**
     * 2件のデータを書き込んだとき、シート名・全8列のヘッダー・全データ行が正しく出力されることを確認する。
     *
     * <p>テスト対象: {@link ExcelWriter#write}</p>
     * <p>観点: 一番太い正常系。ヘッダー行の全列とデータ行の全列を完全検証する。</p>
     */
    @Test
    void excelWriterTest001() throws IOException {
        ExcelWriter.write(twoPrograms, tempFilePath);

        try (XSSFWorkbook workbook = new XSSFWorkbook(new FileInputStream(tempFilePath))) {
            XSSFSheet sheet = workbook.getSheet("A8プログラム一覧");

            assertThat(sheet).isNotNull();
            assertThat(sheet.getLastRowNum()).isEqualTo(2); // ヘッダー1行 + データ2行

            // ヘッダー行（行0）の全列を検証
            assertThat(sheet.getRow(0).getCell(0).getStringCellValue()).isEqualTo("番号");
            assertThat(sheet.getRow(0).getCell(1).getStringCellValue()).isEqualTo("①カテゴリ");
            assertThat(sheet.getRow(0).getCell(2).getStringCellValue()).isEqualTo("②会社");
            assertThat(sheet.getRow(0).getCell(3).getStringCellValue()).isEqualTo("③プログラムタイトル");
            assertThat(sheet.getRow(0).getCell(4).getStringCellValue()).isEqualTo("④広告URL");
            assertThat(sheet.getRow(0).getCell(5).getStringCellValue()).isEqualTo("⑤成果報酬");
            assertThat(sheet.getRow(0).getCell(6).getStringCellValue()).isEqualTo("⑥EPC");
            assertThat(sheet.getRow(0).getCell(7).getStringCellValue()).isEqualTo("⑦確定率");

            // データ行1（行1）の全列を検証
            assertThat(sheet.getRow(1).getCell(0).getNumericCellValue()).isEqualTo(1.0);
            assertThat(sheet.getRow(1).getCell(1).getStringCellValue()).isEqualTo(program1.category);
            assertThat(sheet.getRow(1).getCell(2).getStringCellValue()).isEqualTo(program1.advertiserName);
            assertThat(sheet.getRow(1).getCell(3).getStringCellValue()).isEqualTo(program1.programTitle);
            assertThat(sheet.getRow(1).getCell(4).getStringCellValue()).isEqualTo(program1.siteUrl);
            assertThat(sheet.getRow(1).getCell(5).getStringCellValue()).isEqualTo(program1.reward);
            assertThat(sheet.getRow(1).getCell(6).getStringCellValue()).isEqualTo(program1.epc);
            assertThat(sheet.getRow(1).getCell(7).getStringCellValue()).isEqualTo(program1.confirmRate);

            // データ行2（行2）の番号とカテゴリを検証
            assertThat(sheet.getRow(2).getCell(0).getNumericCellValue()).isEqualTo(2.0);
            assertThat(sheet.getRow(2).getCell(1).getStringCellValue()).isEqualTo(program2.category);
        }
    }

    // ================================================================
    // 正常系バリエーション
    // ================================================================

    /**
     * 空リストを渡したとき、ヘッダー行のみのシートが生成されることを確認する。
     *
     * <p>テスト対象: {@link ExcelWriter#write}</p>
     * <p>観点: 正常系バリエーション。データ0件の境界値を検証する。</p>
     */
    @Test
    void excelWriterTest002() throws IOException {
        ExcelWriter.write(Collections.emptyList(), tempFilePath);

        try (XSSFWorkbook workbook = new XSSFWorkbook(new FileInputStream(tempFilePath))) {
            XSSFSheet sheet = workbook.getSheet("A8プログラム一覧");

            assertThat(sheet.getLastRowNum()).isEqualTo(0); // ヘッダー行のみ
            assertThat(sheet.getRow(0).getCell(0).getStringCellValue()).isEqualTo("番号");
        }
    }

    /**
     * 1件のデータを書き込んだとき、番号列が 1 から始まることを確認する。
     *
     * <p>テスト対象: {@link ExcelWriter#write}</p>
     * <p>観点: 正常系バリエーション。行番号の採番が 1 始まりであることを検証する。</p>
     */
    @Test
    void excelWriterTest003() throws IOException {
        ExcelWriter.write(Arrays.asList(program1), tempFilePath);

        try (XSSFWorkbook workbook = new XSSFWorkbook(new FileInputStream(tempFilePath))) {
            XSSFSheet sheet = workbook.getSheet("A8プログラム一覧");

            assertThat(sheet.getLastRowNum()).isEqualTo(1);
            assertThat(sheet.getRow(1).getCell(0).getNumericCellValue()).isEqualTo(1.0);
        }
    }

    // ================================================================
    // 異常系
    // ================================================================

    /**
     * 存在しないディレクトリへのパスを渡したとき、IOException がスローされることを確認する。
     *
     * <p>テスト対象: {@link ExcelWriter#write}</p>
     * <p>観点: 異常系。書き込み先が不正な場合の例外ハンドリングを検証する。</p>
     */
    @Test
    void excelWriterTest004() {
        assertThatThrownBy(
                () -> ExcelWriter.write(twoPrograms, "C:/nonexistent_dir_a8test/output.xlsx"))
                .isInstanceOf(IOException.class);
    }
}
