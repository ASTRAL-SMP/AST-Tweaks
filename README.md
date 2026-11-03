<div align="center">

<img src="src/main/resources/assets/asttweaks/icon.png" alt="AST-Tweaks" width="128">

# AST-Tweaks

**Minecraft Fabric クライアントサイドMod — 便利機能・自動化ツール集**

[![Minecraft](https://img.shields.io/badge/Minecraft-1.19.4_|_1.20.1_|_26.1_|_26.2_|_26.3-green?logo=mojang-studios)](https://www.minecraft.net/)
[![Fabric](https://img.shields.io/badge/Fabric-Loader-blue)](https://fabricmc.net/)
[![License](https://img.shields.io/badge/License-Apache_2.0-yellow)](LICENSE)
[![Java](https://img.shields.io/badge/Java-17_%2F_25-orange)](https://adoptium.net/)

</div>

---

## 概要

AST-Tweaks は、Minecraft 1.19.4 / 1.20.1 / 26.1 / 26.2 / 26.3 向けの Fabric クライアントサイド Mod です。  
サバイバルプレイを快適にする自動化機能やユーティリティを多数搭載しています。
バージョンごとに jar が分かれています（`ast-tweaks-<version>+mc<Minecraft バージョン>.jar`）。

## 機能一覧

| 機能 | 説明 |
|------|------|
| **Scoreboard Helper** | スコアボードのカスタム表示（位置・スケール・色・ページング対応） |
| **Auto Eat** | 空腹度が閾値以下になると自動で食事（ブラックリスト対応） |
| **Auto Move** | 前後左右の移動キーを自動で押し続ける（freecam対応） |
| **Auto Totem** | トーテム使用後に自動でオフハンドに補充 |
| **Auto Repair** | 経験値ボトルで修繕アイテムを高速自動修理（Tweakeroo スタイル） |
| **Entity Culling** | 特定エンティティの描画を無効化してFPSを改善 |
| **Lava Highlight** | 溶岩をカスタムカラーでハイライト表示（プリセット＆カスタムカラー対応） |
| **Inventory Sort** | インベントリの並べ替え（ID・名前・カテゴリ・スタック数・合計数） |
| **Mass Grindstone** | 砥石でエンチャントを一括除去（ホワイトリスト/ブラックリスト対応） |
| **Bone Meal Filter** | 骨粉の使用を特定ブロックのみに制限 |
| **Silk Touch Switch** | 対象ブロック破壊時にシルクタッチツールへ自動切替（Tweakeroo連携） |
| **Mouse Sensitivity** | マウス感度のワンキー切替トグル |
| **Notepad** | ゲーム内メモ帳 |
| **Update Checker** | Modrinth API 経由でアップデート通知 |

## インストール

### 前提Mod

- [Fabric Loader](https://fabricmc.net/)（1.19.4 / 1.20.1 は 0.14.21 以上、26.x は 0.18.4 以上）
- [Fabric API](https://modrinth.com/mod/fabric-api)（使う Minecraft バージョン向けのもの）
- Java 17 以上（26.x は Java 25 以上）

### 推奨Mod

- [Mod Menu](https://modrinth.com/mod/modmenu) — 設定画面へのアクセス
- [Cloth Config](https://modrinth.com/mod/cloth-config) — 設定UIライブラリ（JARに同梱済み）

### 互換性

- [Sodium](https://modrinth.com/mod/sodium) 0.4.10 — 流体レンダリング互換Mixin対応（1.19.4 のみ。それ以外のバージョンは Fabric API の流体描画 API 経由で動作）
- [Tweakeroo](https://www.curseforge.com/minecraft/mc-mods/tweakeroo) — Auto Repair / Silk Touch Switch 連携

### 手順

1. [Releases](https://github.com/ASTRAL-SMP/AST-Tweaks/releases) から使う Minecraft バージョンに合う `.jar`（`+mc1.19.4` など）をダウンロード
2. `.minecraft/mods/` フォルダに配置
3. Fabric Loader で Minecraft を起動

## 設定

Mod Menu がインストールされていれば、Mod一覧から設定画面を開けます。  
設定ファイルは `.minecraft/config/asttweaks.json` に保存されます。

### デフォルトキーバインド

| キー | 機能 |
|------|------|
| `O` | スコアボード表示切替 |
| `↑` / `↓` | スコアボードページ送り |
| `R` | インベントリソート実行 |
| `L + K` | 設定画面を開く |

その他のキーバインドは設定画面から自由にカスタマイズできます。

## ビルド

JDK 25 が必要です（1 つのソースから [Stonecutter](https://stonecutter.kikugie.dev/) で全バージョンをビルドします）。

```bash
git clone https://github.com/ASTRAL-SMP/AST-Tweaks.git
cd AST-Tweaks
./gradlew chiseledBuild          # 全バージョン
./gradlew :1.19.4:build          # 1 バージョンだけ
```

ビルド成果物は `build/libs/` に出力されます。

## ライセンス

[Apache License 2.0](LICENSE)

## リンク

- [Twitter (X) — Sim_999_256](https://x.com/Sim_999_256)
- [YouTube — ASTRAL-SMP](https://www.youtube.com/@ASTRAL-SMP)
