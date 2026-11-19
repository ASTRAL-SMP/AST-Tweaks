<div align="center">

<img src="src/main/resources/assets/asttweaks/icon.png" alt="AST-Tweaks" width="128">

# AST-Tweaks

**Minecraft Fabric クライアントサイドMod — 便利機能・自動化ツール集**

[![Minecraft](https://img.shields.io/badge/Minecraft-1.19.4_|_1.20.1_|_26.1.2_|_26.2_|_26.3-green?logo=mojang-studios)](#対応バージョン)
[![Fabric](https://img.shields.io/badge/Fabric-Loader-blue)](https://fabricmc.net/)
[![Release](https://img.shields.io/github/v/release/ASTRAL-SMP/AST-Tweaks?label=Release)](https://github.com/ASTRAL-SMP/AST-Tweaks/releases/latest)
[![License](https://img.shields.io/badge/License-Apache_2.0-yellow)](LICENSE)
[![Java](https://img.shields.io/badge/Java-17_%2F_25-orange)](https://adoptium.net/)

</div>

---

## 概要

AST-Tweaks は、Minecraft **1.19.4 / 1.20.1 / 26.1.2 / 26.2 / 26.3** 向けの Fabric クライアントサイド Mod です。  
サバイバルプレイを快適にする自動化機能やユーティリティを多数搭載しています。

> [!NOTE]
> **v1.3.0 で Minecraft 1.20.1 / 26.2 / 26.3 に対応しました。**
> 1 つのリリースに Minecraft バージョンごとの jar が並んでいるので、使うバージョンに合うものを選んでください。
> 各バージョンの変更内容は [CHANGELOG.md](CHANGELOG.md) にまとめています。

## 対応バージョン

| Minecraft | ダウンロードする jar | Fabric Loader | Java |
|-----------|----------------------|---------------|------|
| 1.19.4 | `ast-tweaks-<version>+mc1.19.4.jar` | 0.14.21 以上 | 17 以上 |
| 1.20.1 | `ast-tweaks-<version>+mc1.20.1.jar` | 0.14.21 以上 | 17 以上 |
| 26.1.2 | `ast-tweaks-<version>+mc26.1.2.jar` | 0.18.4 以上 | 25 以上 |
| 26.2 | `ast-tweaks-<version>+mc26.2.jar` | 0.18.4 以上 | 25 以上 |
| 26.3 | `ast-tweaks-<version>+mc26.3.jar` | 0.18.4 以上 | 25 以上 |

一部の機能は特定のバージョンだけで使えます（[機能一覧](#機能一覧)の「対応」列を参照）。

## 機能一覧

「対応」列が空欄の機能は、全バージョンで使えます。

### 自動化

| 機能 | 説明 | 対応 |
|------|------|------|
| **Auto Eat** | 空腹度がしきい値を下回ると自動で食事（ブラックリスト対応） | |
| **Auto Move** | 前後左右・斜めの 8 方向に自動で歩き続ける（freecam 対応） | |
| **Auto Totem** | トーテムを使ったあと、自動でオフハンドに補充 | |
| **Auto Repair** | 経験値ボトルで修繕付きのアイテムや装備中の防具を高速修理（Tweakeroo スタイル） | |
| **Auto Restock** | 指定したスロットを、インベントリや開いたコンテナから目標の個数まで自動補充（オフハンド・保護スロット対応） | |
| **Auto Drop** | 保護スロット・除外アイテム以外をまとめてドロップ（実行キー / 画面を開いたときに発動） | |
| **Void Trade** | 座標付きのワークフローでボイドトレードを自動化（Item Scroller のお気に入り対応、プリセット保存・共有） | 1.19.4 / 1.20.1 |

### インベントリ・アイテム

| 機能 | 説明 | 対応 |
|------|------|------|
| **Inventory Sort** | インベントリやコンテナを並べ替え（ID・名前・カテゴリ・スタック数・合計数、除外スロット対応） | |
| **Mass Grindstone** | 砥石でエンチャントを一括除去（ホワイトリスト / ブラックリスト対応） | |
| **Silk Touch Switch** | 対象ブロックを壊すとき、シルクタッチ付きのツールへ自動で持ち替え（Tweakeroo 連携） | |
| **Pick Protect** | ピックブロックで指定したホットバースロットが上書きされないよう保護 | |

### 建築・設置

| 機能 | 説明 | 対応 |
|------|------|------|
| **Bone Meal Filter** | 骨粉を使えるブロックを指定したものだけに制限 | |
| **Portal Protect** | ネザーポータルを壊してしまう向きへのブロック設置を防止 | |
| **Litematica Schematic Drop** | Litematica の schematic 画面に `.litematic` / `.schematic` / `.schem` / `.nbt` をドラッグ&ドロップして読み込み | Litematica 導入時 |
| **Litematica 死サンゴ代用** | スケマティックが死サンゴを要求しているとき、手持ちの生サンゴで代用して設置 | Litematica 導入時 |

### 表示・描画

| 機能 | 説明 | 対応 |
|------|------|------|
| **Scoreboard Helper** | スコアボードのカスタム表示（位置・スケール・色・ページング対応） | |
| **Entity Culling** | 指定したエンティティやアイテムの描画を止めて FPS を改善 | |
| **Lava Highlight** | 溶岩を好きな色でハイライト表示（プリセット＆カスタムカラー対応） | |
| **Villager Link** | 村人と職業ブロックを線で結んで表示 | |
| **World Border Fix** | ワールドボーダー付近や遠い座標での Nvidium の描画バグを回避 | Nvidium 導入時 |
| **Nvidium Chunk Fix** | Nvidium 0.1.x で地形が抜け落ちたまま戻らない問題を修正（Bobby 併用時に多い） | 1.19.4 |

### ユーティリティ

| 機能 | 説明 | 対応 |
|------|------|------|
| **Mouse Sensitivity** | キー 1 つでマウス感度を切り替え | |
| **Notepad** | ゲーム内メモ帳 | |
| **Syncer Tune** | 周りのブロックエンティティ数に応じて TweakerMore の Server Data Syncer の設定を自動調整 | TweakerMore 導入時 |
| **Update Checker** | GitHub Releases から新しいバージョンを確認し、タイトル画面でお知らせ。使っている Minecraft バージョン向けの jar をそのままダウンロードできる | |

## インストール

### 必須

- [Fabric Loader](https://fabricmc.net/)（バージョンは[対応バージョン](#対応バージョン)を参照）
- [Fabric API](https://modrinth.com/mod/fabric-api)（使う Minecraft バージョン向けのもの）

### 推奨

- [Mod Menu](https://modrinth.com/mod/modmenu) — Mod 一覧から設定画面を開ける
- [Cloth Config](https://modrinth.com/mod/cloth-config) — 設定 UI ライブラリ（jar に同梱済みなので別途入れなくても動きます）

### 連携・互換

| Mod | 内容 |
|-----|------|
| [Tweakeroo](https://modrinth.com/mod/tweakeroo) | Auto Repair / Silk Touch Switch と連携。Villager Link で同期データを利用 |
| [TweakerMore](https://modrinth.com/mod/tweakermore) | Syncer Tune で設定を自動調整。Villager Link / Auto Restock でも連携 |
| [Litematica](https://modrinth.com/mod/litematica) | Schematic のドラッグ&ドロップ、死サンゴ代用 |
| [Item Scroller](https://modrinth.com/mod/item-scroller) | Void Trade でお気に入りの取引を使う |
| [Sodium](https://modrinth.com/mod/sodium) | 1.19.4 では Sodium 0.4.10 向けの流体描画互換 Mixin で Lava Highlight が動作。他のバージョンは Fabric API の流体描画 API 経由で動作 |
| [Nvidium](https://modrinth.com/mod/nvidium) | World Border Fix / Nvidium Chunk Fix（1.19.4、Nvidium 0.1.12〜0.1.15）の対象 |

### 手順

1. [Releases](https://github.com/ASTRAL-SMP/AST-Tweaks/releases/latest) から、使う Minecraft バージョンに合う jar（ファイル名の `+mc1.19.4` などで判別）をダウンロード
2. `.minecraft/mods/` フォルダに配置
3. Fabric Loader で Minecraft を起動

一度入れておけば、新しいバージョンが出たときに Update Checker がタイトル画面で知らせてくれます。

## 設定

Mod Menu がインストールされていれば、Mod 一覧から設定画面を開けます（`L + K` でも開けます）。  
設定ファイルは `.minecraft/config/asttweaks.json` に保存されます。

### デフォルトキーバインド

| キー | 機能 |
|------|------|
| `O` | スコアボード表示切替 |
| `↑` / `↓` | スコアボードページ送り |
| `R` | インベントリソート実行 |
| `L + K` | 設定画面を開く |

その他のキーバインド（各機能の ON/OFF など）は、設定画面から割り当てられます。マウスボタンも使えます。キー入力中に `ESC` を押すと割り当てを解除できます。

## 更新履歴

各バージョンの変更内容は [CHANGELOG.md](CHANGELOG.md) を、jar のダウンロードは [Releases](https://github.com/ASTRAL-SMP/AST-Tweaks/releases) を見てください。

## ビルド

JDK 25 が必要です（1 つのソースから [Stonecutter](https://stonecutter.kikugie.dev/) で全バージョンをビルドします）。

```bash
git clone https://github.com/ASTRAL-SMP/AST-Tweaks.git
cd AST-Tweaks
./gradlew chiseledBuild          # 全バージョン
./gradlew :1.19.4:build          # 1 バージョンだけ
```

`chiseledBuild` の成果物はルートの `build/libs/` に集められます。開発の詳しい手順は [CONTRIBUTING.md](CONTRIBUTING.md) を見てください。

## ライセンス

[Apache License 2.0](LICENSE)

## リンク

- [Modrinth — AST-Tweaks](https://modrinth.com/mod/ast-tweaks)
- [Twitter (X) — Sim_999_256](https://x.com/Sim_999_256)
- [YouTube — ASTRAL-SMP](https://www.youtube.com/@ASTRAL-SMP)
