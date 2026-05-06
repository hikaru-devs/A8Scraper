package local.scraping.a8;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.FontUnderline;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * スクレイピング結果をテンプレートと同じ列構成の .xlsx に書き出す
 *
 *   A: 番号
 *   B: ①カテゴリ
 *   C: ②会社
 *   D: ③プログラムタイトル
 *   E: ④広告URL
 *   F: ⑤成果報酬
 *   G: ⑥EPC
 *   H: ⑦確定率
 */
public class ExcelWriter {

	private static final String[] HEADERS = {
			"番号",
			"①カテゴリ",
			"②会社",
			"③プログラムタイトル",
			"④広告URL",
			"⑤成果報酬",
			"⑥EPC",
			"⑦確定率"
	};

	private static final int[] COL_WIDTHS = {
			6 * 256, // 番号
			20 * 256, // ①カテゴリ
			25 * 256, // ②会社
			55 * 256, // ③プログラムタイトル
			55 * 256, // ④広告URL
			30 * 256, // ⑤成果報酬
			10 * 256, // ⑥EPC
			10 * 256 // ⑦確定率
	};

	public static void write(List<ProgramInfo> programs, String outputPath) throws IOException {

		try (XSSFWorkbook workbook = new XSSFWorkbook()) {
			XSSFSheet sheet = workbook.createSheet("A8プログラム一覧");

			XSSFCellStyle headerStyle = buildHeaderStyle(workbook);
			XSSFCellStyle urlStyle = buildUrlStyle(workbook);
			XSSFCellStyle numStyle = buildNumStyle(workbook);

			// ---- ヘッダー行 ----
			XSSFRow headerRow = sheet.createRow(0);
			headerRow.setHeightInPoints(18);
			for (int i = 0; i < HEADERS.length; i++) {
				XSSFCell cell = headerRow.createCell(i);
				cell.setCellValue(HEADERS[i]);
				cell.setCellStyle(headerStyle);
			}

			// ---- データ行 ----
			for (int i = 0; i < programs.size(); i++) {
				ProgramInfo p = programs.get(i);
				XSSFRow row = sheet.createRow(i + 1);
				row.setHeightInPoints(15);

				// A: 番号
				XSSFCell numCell = row.createCell(0);
				numCell.setCellValue(i + 1);
				numCell.setCellStyle(numStyle);

				// B: ①カテゴリ
				row.createCell(1).setCellValue(p.category);

				// C: ②広告主名
				row.createCell(2).setCellValue(p.advertiserName);

				// D: ③プログラムタイトル
				row.createCell(3).setCellValue(p.programTitle);

				// E: ④掲載URL（URLスタイル）
				XSSFCell urlCell = row.createCell(4);
				urlCell.setCellValue(p.siteUrl);
				urlCell.setCellStyle(urlStyle);

				// F: ⑤成果報酬
				row.createCell(5).setCellValue(p.reward);

				// G: ⑥EPC
				row.createCell(6).setCellValue(p.epc);

				// H: ⑦確認率
				row.createCell(7).setCellValue(p.confirmRate);
			}

			// ---- 列幅 ----
			for (int i = 0; i < COL_WIDTHS.length; i++) {
				sheet.setColumnWidth(i, COL_WIDTHS[i]);
			}

			// ---- ヘッダー行固定 ----
			sheet.createFreezePane(0, 1);

			// ---- オートフィルター ----
			sheet.setAutoFilter(
					new CellRangeAddress(0, programs.size(), 0, HEADERS.length - 1));

			// ---- ファイル書き込み ----
			try (FileOutputStream fos = new FileOutputStream(outputPath)) {
				workbook.write(fos);
			}
		}

		System.out.printf("Excel出力完了: %s  (%d件)%n", outputPath, programs.size());
	}

	// ---- スタイルビルダー ----

	private static XSSFCellStyle buildHeaderStyle(XSSFWorkbook wb) {
		XSSFCellStyle style = wb.createCellStyle();
		XSSFFont font = wb.createFont();
		font.setBold(true);
		font.setColor(IndexedColors.WHITE.getIndex());
		style.setFont(font);
		style.setFillForegroundColor(
				new XSSFColor(new byte[] { (byte) 0x1F, (byte) 0x5C, (byte) 0x99 }, null));
		style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
		style.setAlignment(HorizontalAlignment.CENTER);
		style.setVerticalAlignment(VerticalAlignment.CENTER);
		setBorder(style);
		return style;
	}

	private static XSSFCellStyle buildUrlStyle(XSSFWorkbook wb) {
		XSSFCellStyle style = wb.createCellStyle();
		XSSFFont font = wb.createFont();
		font.setColor(IndexedColors.BLUE.getIndex());
		font.setUnderline(FontUnderline.SINGLE);
		style.setFont(font);
		return style;
	}

	private static XSSFCellStyle buildNumStyle(XSSFWorkbook wb) {
		XSSFCellStyle style = wb.createCellStyle();
		style.setAlignment(HorizontalAlignment.CENTER);
		return style;
	}

	private static void setBorder(XSSFCellStyle style) {
		style.setBorderBottom(BorderStyle.THIN);
		style.setBorderTop(BorderStyle.THIN);
		style.setBorderLeft(BorderStyle.THIN);
		style.setBorderRight(BorderStyle.THIN);
	}
}
