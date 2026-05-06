package local.scraping.a8;

/**
 * A8.netプログラム1件分のデータモデル（テンプレート8列対応）
 *
 * テンプレートの列構成:
 *   番号 / ①カテゴリ / ②会社 / ③プログラムタイトル / ④広告URL / ⑤成果報酬 / ⑥EPC / ⑦確定率
 */
public class ProgramInfo {

	public final String category; // ①カテゴリ
	public final String advertiserName; // ②会社
	public final String programTitle; // ③プログラムタイトル
	public final String siteUrl; // ④広告URL
	public final String reward; // ⑤成果報酬
	public final String epc; // ⑥EPC
	public final String confirmRate; // ⑦確定率

	public ProgramInfo(String category, String advertiserName, String programTitle,
			String siteUrl, String reward, String epc, String confirmRate) {
		this.category = nvl(category);
		this.advertiserName = nvl(advertiserName);
		this.programTitle = nvl(programTitle);
		this.siteUrl = nvl(siteUrl);
		this.reward = nvl(reward);
		this.epc = nvl(epc);
		this.confirmRate = nvl(confirmRate);
	}

	private static String nvl(String s) {
		return s == null ? "" : s.strip();
	}

	@Override
	public String toString() {
		return String.format("[%s] %s / %s", category, programTitle, reward);
	}
}
