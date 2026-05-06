# A8Scraper — Claude Code ルールブック

## プロジェクト概要

A8.net のアフィリエイトプログラム情報をスクレイピングして Excel に出力する Java アプリケーション。

- **メイン言語**: Java
- **パッケージ**: `local.scraping.a8`
- **構成**: `A8Scraper`, `A8ScraperMain`, `ExcelWriter`, `ProgramInfo`

---

※ ユニットテスト記述基準はグローバル CLAUDE.md（`~/.claude/CLAUDE.md`）に定義済み。以下はこのプロジェクト固有の補足。

## ユニットテスト記述基準（プロジェクト固有補足）

### 使用フレームワーク

- **JUnit 5**（`@Test`, `@ExtendWith`）
- **Mockito**（`@InjectMocks`, `@Mock`, `@ExtendWith(MockitoExtension.class)`）
- **AssertJ**（`assertThat`）— アサーションは **必ず AssertJ を使うこと**。`assertEquals` 等 JUnit 標準アサーションは使わない

### カバレッジ

- **全テストクラスのカバレッジ 100% を目指す**（行・分岐とも）
- カバー不可能な箇所は `// coverage: excluded` コメントを付けて理由を明記

### クラス構成

```java
@ExtendWith(MockitoExtension.class)
class FooTest {

    @Mock
    DependencyClass dependency;   // 依存クラスは必ずモック化

    @InjectMocks
    Foo sut;                      // テスト対象は @InjectMocks、変数名は sut に統一

    // テストで使うオブジェクトはフィールドで宣言し（値は入れない）
    ProgramInfo expectedProgram;
    List<String> inputIds;

    @BeforeEach
    void setUp() {
        // @BeforeEach には「全テストで共通して使う純粋なデータ」だけを置く
        // new によるオブジェクト生成・プリミティブ値の初期化など
        expectedProgram = new ProgramInfo(...);
        inputIds = Arrays.asList("s00000000001", "s00000000002");

        // when(...) によるスタブ定義は @BeforeEach に書かない
        // → 使われないスタブが UnnecessaryStubbingException を引き起こすため
    }
```

- テスト対象変数名は `sut`（System Under Test）に統一
- 依存クラスはすべて `@Mock` でモック化し、実装に依存しない
- テストで使うオブジェクトは**フィールドで宣言**し、**`new` と値の設定は `@BeforeEach` 内**で行う
- テストメソッド内で `new` しない
- **`when(...)` によるスタブ定義は各テストメソッド内に書く**（`@BeforeEach` には書かない）

### テストメソッドの順序

同一クラス内で以下の順に並べること：

1. **一番太い正常系**（主要ユースケース・最も典型的なハッピーパス）
2. **正常系バリエーション**（境界値・別パターン）
3. **異常系**（例外・エラー・null・空など）

### メソッド名ルール

**形式：テストクラス名（先頭小文字）＋通番3桁**

```
{testClassName}{001}   // 001, 002, ... 009, 010, 011, ...
```

例（クラス `A8ScraperTest` の場合）：

```java
a8ScraperTest001()   // 一番太い正常系
a8ScraperTest002()   // 正常系バリエーション
a8ScraperTest003()   // 異常系
```

- 通番は同一クラス内で連番とし、途中に欠番を作らない
- テストの意図・目的は Javadoc に記述する

### テストメソッドの Javadoc

各テストメソッドには Javadoc を付けること。最低でも以下を記載する：

- **1行目**：テストの概要（何を確認するか）
- **テスト対象**：`対象クラス#対象メソッド` の形式
- **観点**：正常系／異常系の種別と確認ポイント

```java
/**
 * 全フィールドに有効値を渡したとき、7項目が正しく格納されることを確認する。
 *
 * <p>テスト対象: {@link ProgramInfo#ProgramInfo}</p>
 * <p>観点: 一番太い正常系。全フィールドを個別にアサートする。</p>
 */
@Test
void programInfoTest001() { ... }
```

### AssertJ アサーションパターン（必須）

```java
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// 禁止: JUnit 標準アサーション
assertEquals("expected", actual);
assertNull(result);

// 必須: AssertJ
assertThat(actual).isEqualTo("expected");
assertThat(result).isNull();
assertThat(list).hasSize(3).contains("item");

// 例外アサーション
// メッセージが確定しているときは完全一致（hasMessage）を使うこと
assertThatThrownBy(() -> sut.method())
    .isInstanceOf(RuntimeException.class)
    .hasMessage("Unexpected error occurred");

// メッセージに動的な値が含まれる場合のみ部分一致を許容
assertThatThrownBy(() -> sut.method())
    .isInstanceOf(RuntimeException.class)
    .hasMessageContaining("error");
```

### ArgumentCaptor

モックに渡された引数の内容を取り出して検証する場合に使用する。

```java
@Captor
ArgumentCaptor<SomeRequest> requestCaptor;

@Test
void someTest001() {
    sut.doSomething(inputData);

    verify(dependency).send(requestCaptor.capture());
    SomeRequest captured = requestCaptor.getValue();

    assertThat(captured.getTitle()).isEqualTo("期待するタイトル");
    assertThat(captured.getAmount()).isEqualTo(1000);
}
```

- `@Captor` フィールドはフィールドで宣言（MockitoExtension が自動初期化）
- 複数回呼ばれた場合は `requestCaptor.getAllValues()` でリスト取得

### Mockito の基本パターン

```java
@Test
void someTest001() {
    // スタブはテストメソッド冒頭に、そのテストで必要な分だけ定義する
    when(dependency.fetch(anyString())).thenReturn("結果");

    // 例外スタブ
    when(dependency.fetch("bad")).thenThrow(new RuntimeException("error"));

    // 呼び出し検証
    verify(dependency, times(1)).fetch("expected-arg");
    verify(dependency, never()).fetch("unexpected-arg");
}
```

### やってはいけないこと

- `@InjectMocks` なしで `new` でテスト対象を生成しない
- 実際のネットワーク・ファイルシステム・DB にアクセスするテストを書かない（統合テストに分離する）
- `Thread.sleep` を直接テスト内に書かない（モック化する）
- `assertEquals` / `assertTrue` 等 JUnit 標準アサーションを使わない（AssertJ に統一）
- **`@BeforeEach` に `when(...)` スタブを書かない**（`UnnecessaryStubbingException` の原因）
