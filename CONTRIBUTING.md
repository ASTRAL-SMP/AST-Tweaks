# Contributing Guide

AST-Tweaks へのコントリビューションを歓迎します！

## 開発環境のセットアップ

### 前提条件

- JDK 25（1.19.4 / 1.20.1 も同じ JDK で `--release 17` としてビルドします）
- Git

### 手順

```bash
git clone https://github.com/ASTRAL-SMP/AST-Tweaks.git
cd AST-Tweaks
./gradlew chiseledBuild          # 全バージョンをビルド（jar はルートの build/libs/ に集まる）
./gradlew :1.19.4:build          # 1 バージョンだけビルド
./gradlew :1.19.4:runClient      # そのバージョンでクライアントを起動（run/<version>/ を使う）
```

IDE で開く場合は、IntelliJ IDEA を推奨します。Gradle プロジェクトとしてインポートしてください。

## マルチバージョン構成（Stonecutter）

1 つのソースから、[Stonecutter](https://stonecutter.kikugie.dev/) で 1.19.4 / 1.20.1 / 26.1.2 / 26.2 / 26.3 の jar をビルドしています。

| ファイル | 内容 |
|----------|------|
| `settings.gradle.kts` | ビルドする Minecraft バージョンの一覧と `vcsVersion` |
| `stonecutter.properties.toml` | `mod.version` と、バージョンごとの依存（Fabric API・Mod Menu・Cloth Config など）のバージョン |
| `stonecutter.gradle.kts` | アクティブなバージョンと、バージョン間で名前だけ違うシンボルの置換ルール |
| `build.gradle.kts` | 各バージョン共通のビルド設定（Mixin 設定ファイルの組み立てもここ） |

### バージョンごとに違うコードを書く

API が違う箇所は `//? if` コメントで分岐します。条件に合わないほうは Stonecutter が自動でコメントアウトします。

```java
//? if >=26.1 {
newApiCall();
//?} else {
/*oldApiCall();
*///?}
```

- 名前が変わっただけのクラスやメソッドは、`stonecutter.gradle.kts` の `replacements` に置換ルールを足すほうが簡単です
- 共通化できる差分は `util/`（`McClient`、`KeyInput`、`gui/Gfx`、`gui/CompatScreen` など）のヘルパーにまとめています
- 特定のバージョンだけの機能（Void Trade は 1.20.1 以前、Nvidium Chunk Fix は 1.19.4 のみなど）は、`FeatureManager` や `ConfigScreen` の登録部分も分岐させます

### アクティブなバージョン

`src/` に展開されているのは「アクティブなバージョン」のコードです。IDE の Gradle タスク `Set active project to <version>` で切り替えられます。
**コミットする前に `Reset active project` で `vcsVersion`（`settings.gradle.kts`）に戻してください。** 戻さずにコミットすると、別のバージョン用に書き換わったコードが入ってしまいます。

## コーディング規約

- **言語:** Java 17 の範囲で書く（1.19.4 / 1.20.1 は Java 17 向けにビルドするため）
- **コメント:** 日本語
- **エラーメッセージ:** 英語
- **インデント:** スペース4つ
- **命名規則:** Java 標準（camelCase / PascalCase）

## 機能の追加

新しい機能を追加する場合は、以下の構成に従ってください。

1. `src/main/java/com/astral/asttweaks/feature/<featurename>/` にパッケージを作成
2. `Feature` インターフェースを実装したクラスを作成
3. `FeatureManager` に登録
4. `ModConfig` に設定項目を追加
5. `ConfigScreen` に設定UIを追加
6. `en_us.json` / `ja_jp.json` に翻訳キーを追加
7. `README.md` の機能一覧と、`.github/ISSUE_TEMPLATE/bug_report.yml` の「関連する機能」に追加

## 更新履歴（CHANGELOG.md）

ユーザーから見て変わることがある PR では、[CHANGELOG.md](CHANGELOG.md) の `## [Unreleased]` に 1 行ずつ追記してください。

- 「追加 / 変更 / 修正 / 削除」の小見出しに分けて書く
- 内部のクラス名ではなく、プレイヤーから見た変化を書く（例: 「Auto Restock がオフハンドへ補充できなかった問題を修正」）
- 特定の Minecraft バージョンだけの変更なら、そのことも書く
- CI やリファクタリングだけの変更は書かなくて構いません

## リリース手順

1. `stonecutter.properties.toml` の `mod.version` を上げる
2. `CHANGELOG.md` の `## [Unreleased]` の内容を `## [x.y.z] - YYYY-MM-DD` の見出しに移し、空の `## [Unreleased]` を残す。末尾のリンク定義にも新しいバージョンを足す
3. PR をマージしたあと、`main` で `vx.y.z` タグを打って push する

タグを push すると `.github/workflows/release.yml` が全バージョンの jar をビルドして 1 つのリリースにまとめます。
リリースノートの先頭には CHANGELOG.md の該当バージョンの内容が入り、その下に GitHub が自動生成する PR 一覧が続きます。
タグと `mod.version` が一致しないとリリースは失敗します。

## ブランチ戦略

- `main` — リリースブランチ（直接プッシュ禁止）
- 機能追加: `feature/<機能名>`
- バグ修正: `fix/<説明>`

## コミットメッセージ

[Conventional Commits](https://www.conventionalcommits.org/) に従ってください。

```
feat: 新機能の説明
fix: バグ修正の説明
refactor: リファクタリングの説明
docs: ドキュメント変更
chore: ビルド・設定関連の変更
```

## Pull Request

1. フォークしてブランチを作成
2. 変更を実装
3. 全バージョンのビルドが通ることを確認 (`./gradlew chiseledBuild`)
4. PR を作成し、変更内容を説明

### PR チェックリスト

- [ ] `./gradlew chiseledBuild` が成功する（全バージョン）
- [ ] アクティブなバージョンを `vcsVersion` に戻してからコミットした
- [ ] Mixin を追加した場合、`asttweaks.mixins.json` に登録済み（バージョンによって有無が変わるものは `build.gradle.kts` の `mixinProps` に追加）
- [ ] 翻訳キーを `en_us.json` と `ja_jp.json` の両方に追加済み
- [ ] ユーザーから見える変更なら `CHANGELOG.md` の `Unreleased` に追記済み
- [ ] 既存機能に影響がないことを確認済み

## Issue

バグ報告は [Bug Report テンプレート](https://github.com/ASTRAL-SMP/AST-Tweaks/issues/new?template=bug_report.yml) を使用してください。

機能要望は Issue で自由に提案してください。

## ライセンス

コントリビューションは [Apache License 2.0](LICENSE) の下で提供されます。
