# A8Scraper

A8.netのアフィリエイト案件情報を自動収集し、Excelファイルに出力するJavaツールです。

## 解決する問題

A8.netの案件情報を手動で確認・記録する作業は時間がかかります。  
本ツールはその収集・整形を自動化し、Excel形式で出力します。

## 技術スタック

| 技術 | 用途 |
|------|------|
| Java 11 | メイン言語 |
| Playwright | ブラウザ自動操作・スクレイピング |
| Apache POI | Excel出力 |
| JUnit5 / Mockito / AssertJ | テスト |
| Maven | ビルド管理 |

## アーキテクチャ

A8.net（ブラウザ自動操作）  
↓  
Playwright でデータ収集  
↓  
Java でデータ整形・集計  
↓  
Apache POI で Excel出力  

## セットアップ

```bash
git clone https://github.com/hikaru-devs/A8Scraper.git
cd A8Scraper
mvn compile
```

## 実行

```bash
mvn exec:java -Dexec.mainClass="local.scraping.A8ScraperMain"
```

## テスト

```bash
mvn test
```

## 出力例

<img width="1914" height="898" alt="スクリーンショット 2026-05-09 223058" src="https://github.com/user-attachments/assets/35a5db9c-b55e-4464-8333-4721a23f9074" />
