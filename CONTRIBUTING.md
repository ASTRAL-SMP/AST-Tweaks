# Contributing Guide

AST-Tweaks へのコントリビューションを歓迎します！

## 開発環境のセットアップ

### 前提条件

- Java 17 (JDK)
- Git

### 手順

```bash
git clone https://github.com/ASTRAL-SMP/AST-Tweaks.git
cd AST-Tweaks
./gradlew genSources   # Minecraft ソースを生成
./gradlew build         # ビルド確認
```

IDE で開く場合は、IntelliJ IDEA を推奨します。Gradle プロジェクトとしてインポートしてください。

## コーディング規約

- **言語:** Java 17
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
3. ビルドが通ることを確認 (`./gradlew build`)
4. PR を作成し、変更内容を説明

### PR チェックリスト

- [ ] `./gradlew build` が成功する
- [ ] Mixin を追加した場合、`asttweaks.mixins.json` に登録済み
- [ ] 翻訳キーを `en_us.json` と `ja_jp.json` の両方に追加済み
- [ ] 既存機能に影響がないことを確認済み

## Issue

バグ報告は [Bug Report テンプレート](https://github.com/ASTRAL-SMP/AST-Tweaks/issues/new?template=bug_report.yml) を使用してください。

機能要望は Issue で自由に提案してください。

## ライセンス

コントリビューションは [MIT License](LICENSE) の下で提供されます。
