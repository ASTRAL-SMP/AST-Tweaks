# 更新履歴

AST-Tweaks の各バージョンで何が変わったかをまとめています。
各 jar は [Releases](https://github.com/ASTRAL-SMP/AST-Tweaks/releases) からダウンロードできます。

書き方は [Keep a Changelog](https://keepachangelog.com/ja/1.1.0/) に沿っています。
変更は「追加 / 変更 / 修正 / 削除」に分けて、新しいバージョンを上に書きます。
リリースするときは `## [x.y.z] - 日付` の見出しで書いておくと、その内容が GitHub のリリースノートの先頭に入ります。

## [Unreleased]

<!-- 次のリリースに入る変更をここに書く。リリース時に見出しを ## [x.y.z] - YYYY-MM-DD に変える -->

## [1.3.0] - 2026-11-03

**1 つのバージョンで Minecraft 1.19.4 / 1.20.1 / 26.1.2 / 26.2 / 26.3 に対応しました。**
同じリリースに、Minecraft バージョンごとの jar（`ast-tweaks-1.3.0+mc1.19.4.jar` など）が並びます。

### 追加

- Minecraft **1.20.1**、**26.2**、**26.3** に対応
- 1.19.4 版（1.1.x）と 26.1.2 版（1.2.0+mc26.1.2）を 1 つのバージョンに統合

### 変更

- アップデートチェッカーが、使っている Minecraft バージョン向けの jar だけを更新対象にするようにした（jar 名の `+mc1.19.4` などで判別）
- バージョンごとに使える機能を整理
  - Void Trade は 1.19.4 / 1.20.1 のみ（26.1 以降は取引画面が距離で閉じられるため使えない）
  - Nvidium Chunk Fix と Sodium の流体描画互換は 1.19.4 のみ
- ビルド構成を [Stonecutter](https://stonecutter.kikugie.dev/) に移行し、1 つのソースから全バージョンの jar を作るようにした（開発者向け）

> 1.1.30（アップデート確認を Minecraft バージョン別のリリースに対応）は単独ではリリースせず、このバージョンに含めています。

## [1.2.0+mc26.1.2] - 2026-11-01

Minecraft **26.1.2** 向けの最初のリリースです（26.1.2 専用。1.19.4 版は 1.1.29 のまま）。

### 追加

- Minecraft 26.1.2 に対応（Java 25 が必要）

### 変更

- Villager Link をデバッグ購読と Tweakeroo / TweakerMore の同期データで作り直した
- 溶岩ハイライトを 26.1 の流体描画に合わせて作り直した

### 削除

- Void Trade（26.1 では取引画面が距離で閉じられるため）
- Nvidium Chunk Fix（Nvidium 0.1.x 専用のため）

## [1.1.29] - 2026-09-30

### 追加

- **Nvidium Chunk Fix**: Nvidium（特に Bobby 併用時）で地形が 128×64×128 ブロック単位で抜けたまま戻らない問題を修正
  - 256 セクションが全部埋まったリージョンが描画されない Nvidium 0.1.x の不具合を修正（ネザー天井の層などで起きやすい。こちらが主な原因）
  - VRAM の上限で破棄された地形を再描画するようにした
  - 読み込んだばかりの近くの地形が優先的に破棄される問題と、破棄候補が無いときのクラッシュを修正
  - F3 画面の右側に処理状況を表示

### 変更

- 設定画面に「描画修正」タブを作り、World Border Fix と Nvidium Chunk Fix をまとめた

> 1.1.28 は単独ではリリースせず、このバージョンに含めています。

## [1.1.27] - 2026-09-28

### 変更

- Void Trade: 取引ステップの対象の既定を「お気に入り（すべて）」にした（Item Scroller 導入時）
- Void Trade: お気に入りのうち、材料不足・ロック・インベントリ満杯で 1 回も取引できなかったものをチャットで知らせるようにした

## [1.1.26] - 2026-09-28

### 追加

- **Void Trade**: 座標付きのステップ（移動・取引画面を開く・アンロード待ち・取引・画面を閉じる・待機・コマンドなど）を順に実行してボイドトレードを自動化
  - Item Scroller のお気に入り取引に対応
  - 前の周の取引が村人に残っていたら（ボイド不成立）止めて知らせる
  - 手順をクリップボードで共有したり、名前を付けてプリセットとして保存したりできる

## [1.1.25] - 2026-07-23

### 修正

- Litematica のドラッグ&ドロップで、開いているサブフォルダではなく常に schematics のルートに取り込まれていた問題を修正
  - Load / Manage / Save のどのブラウザ画面でもドロップできるようにした
  - schematics フォルダ内のファイルをドロップしたときも、表示中のフォルダにコピーするようにした

## [1.1.24] - 2026-06-11

### 追加

- **World Border Fix**: ワールドボーダー付近で壁が透明になり X-ray のように見える問題と、遠い座標で地形が正しく読み込まれない問題への対策。対象エリアにいる間だけ Nvidium を一時的に無効にして Sodium 標準の描画に切り替える（Nvidium 未導入なら何もしない）
- **Litematica 死サンゴ代用**: スケマティックが死んだサンゴを要求していて手持ちに無いとき、生きたサンゴで Easy Place・設置制限・ピックブロックを使えるようにした

## [1.1.23] - 2026-06-02

### 変更

- Auto Repair: 防具は装備したまま修繕する方式に変更（経験値ボトルをオフハンドから投げる）
- Syncer Tune: 検出チャンク半径の既定値を 4 → 8 に変更

## [1.1.22] - 2026-06-02

### 追加

- **Syncer Tune**: 周りのブロックエンティティ数に応じて、TweakerMore の Server Data Syncer の問い合わせ間隔・件数を自動で切り替える
- Auto Repair: 装備中の防具も修繕できるようにした

## [1.1.21] - 2026-05-29

### 追加

- Auto Restock: シュルカーボックスなどのコンテナから補充するとき、先頭のスロットから取るか末尾のスロットから取るかを選べるようにした

## [1.1.20] - 2026-05-23

### 追加

- 起動時に、アップデートで残った古い AST-Tweaks の jar を自動で削除するようにした

## [1.1.19] - 2026-05-20

### 修正

- Auto Restock がチェストやシュルカーボックスからオフハンドへ補充できなかった問題を修正

## [1.1.18] - 2026-05-17

### 変更

- Silk Touch Switch の対象候補にサンゴ系のブロックを追加

## [1.1.17] - 2026-05-16

### 変更

- Portal Protect: ポータルを壊しうる向きの設置だけを止めるようにした。ポータルの前後（薄い側）にはレールなどを置ける

## [1.1.16] - 2026-05-16

### 追加

- **Portal Protect**: ネザーポータルを壊してしまう位置へのブロック設置を防ぐ

## [1.1.15] - 2026-05-14

### 追加

- Auto Restock: 補充元として使わない保護スロットを設定できるようにした

### 修正

- インベントリソートの名前順で、シュルカーボックスが中身のアイテム名で並んでしまう問題を修正

## [1.1.14] - 2026-05-12

### 追加

- **Pick Protect**: ピックブロックで指定したホットバースロットが上書きされないようにする（保護されていない近くのスロットへ振り替える）

## [1.1.13] - 2026-05-11

### 追加

- 設定画面のキーバインド入力中に ESC を押すと割り当てを解除できるようにした

### 変更

- Auto Repair: 経験値ボトルの投げすぎを抑え、修繕が終わったらすぐ次のアイテムに持ち替えるようにした

## [1.1.12] - 2026-05-06

### 修正

- コンテナのソートがシュルカーボックスを考慮して並ぶよう修正

## [1.1.11] - 2026-05-01

### 追加

- **Litematica Schematic Drop**: Litematica の schematic 画面に `.litematic` / `.schematic` / `.schem` / `.nbt` ファイルをドラッグ&ドロップして読み込めるようにした

## [1.1.10] - 2026-04-29

### 変更

- Villager Link: 村人の実際のデータ（Brain）から職業ブロックを特定するようにした。データが届かない環境では従来の推定にフォールバックする

## [1.1.9] - 2026-04-26

### 追加

- 新しいバージョンがあるとき、タイトル画面にアップデート案内のポップアップを出すようにした。「アップデート」で新しい jar を mods フォルダにダウンロードする

## [1.1.8] - 2026-04-25

### 追加

- **Villager Link**: 範囲内の村人と職業ブロックを線で結んで表示。村人を見ると職業ブロックの座標を表示する

## [1.1.7] - 2026-04-23

### 追加

- Auto Restock: 開いたチェストやシュルカーボックスなどのコンテナからも補充できるようにした（TweakerMore の AutoCollectMaterial より優先するかを選べる）

## [1.1.6] - 2026-04-22

### 追加

- **Auto Restock**: 指定したスロットのアイテムが目標の個数を下回ったら、インベントリの他のスロットから補充する

## [1.1.5] - 2026-04-21

### 追加

- Auto Move に斜め 4 方向を追加

## [1.1.4] - 2026-04-20

### 修正

- Auto Drop の「インベントリを開いた時」モードがチェストなどのコンテナ画面で動かなかった問題を修正

## [1.1.3] - 2026-04-19

### 追加

- Auto Drop: 発動方式（実行キー押下 / インベントリを開いた時）を選べるようにした
- キーバインドにマウスボタンを割り当てられるようにした

## [1.1.2] - 2026-04-19

### 追加

- **Auto Drop**: 保護スロット・除外アイテム以外のアイテムをまとめてドロップする（防具は常に保護）。作業台やチェストなど任意のコンテナ画面で使える

> 1.1.0 / 1.1.1 は単独ではリリースせず、このバージョンに含めています。

## [1.0.9] - 2026-04-14

### 修正

- jar に同梱するライセンス表記を Apache License 2.0 に修正

## [1.0.8] - 2026-03-19

### 追加

- **Mouse Sensitivity**: キーでマウス感度を設定値に切り替え、もう一度押すと元に戻す

## [1.0.7] - 2026-03-16

### 修正

- Auto Repair: 修繕後に元のオフハンドのアイテム（トーテムなど）が戻らない問題を修正
- Auto Eat: 食事が途中で終わったとき（画面を開いた・タイムアウトなど）にホットバーのスロットが戻らない問題を修正

## [1.0.6] - 2026-03-14

### 追加

- インベントリソートに「合計数順」モードを追加

### 修正

- Auto Move が freecam 中にカメラを動かしてしまう問題を修正（freecam 中はプレイヤー本体だけが動く）

## [1.0.5] - 2026-03-14

### 追加

- Auto Move に前後左右の方向別キーバインドを追加
- Mod Menu の詳細画面に Twitter (X) と YouTube のリンクを表示

### 修正

- Auto Eat で食事がキャンセルされたり、ブロックを置いてしまったりする問題を修正

## [1.0.4] - 2026-03-12

### 修正

- キーバインドを変更しても設定画面の保存ボタンが押せない問題を修正

## [1.0.3] - 2026-03-12

### 追加

- 設定画面に「一般」タブを追加し、Auto Totem / Notepad / Auto Move / Update Checker をまとめた
- `L + K` で設定画面を直接開けるようにした

### 変更

- キーバインドを Minecraft のキー設定ではなく、AST-Tweaks の設定画面で管理するようにした
- Silk Touch Switch の対象ブロック候補を、シルクタッチの意味があるブロックだけに絞った

## [1.0.2] - 2026-03-12

### 変更

- Auto Eat が使用キーを押し続ける方式ではなく、アイテムを直接使う方式で食事するようにした（誤作動を防ぐ）

## [1.0.1] - 2026-03-05

最初の公開リリースです。

### 追加

- Scoreboard Helper / Auto Eat / Auto Move / Auto Totem / Auto Repair / Entity Culling / Lava Highlight / Inventory Sort / Mass Grindstone / Bone Meal Filter / Notepad / Update Checker
- **Silk Touch Switch**: 指定したブロックを壊すとき、Tweakeroo のツール切り替えをシルクタッチ付きのツールに置き換える

[Unreleased]: https://github.com/ASTRAL-SMP/AST-Tweaks/compare/v1.3.0...HEAD
[1.3.0]: https://github.com/ASTRAL-SMP/AST-Tweaks/releases/tag/v1.3.0
[1.2.0+mc26.1.2]: https://github.com/ASTRAL-SMP/AST-Tweaks/releases/tag/v1.2.0%2Bmc26.1.2
[1.1.29]: https://github.com/ASTRAL-SMP/AST-Tweaks/releases/tag/v1.1.29
[1.1.27]: https://github.com/ASTRAL-SMP/AST-Tweaks/releases/tag/v1.1.27
[1.1.26]: https://github.com/ASTRAL-SMP/AST-Tweaks/releases/tag/v1.1.26
[1.1.25]: https://github.com/ASTRAL-SMP/AST-Tweaks/releases/tag/v1.1.25
[1.1.24]: https://github.com/ASTRAL-SMP/AST-Tweaks/releases/tag/v1.1.24
[1.1.23]: https://github.com/ASTRAL-SMP/AST-Tweaks/releases/tag/v1.1.23
[1.1.22]: https://github.com/ASTRAL-SMP/AST-Tweaks/releases/tag/v1.1.22
[1.1.21]: https://github.com/ASTRAL-SMP/AST-Tweaks/releases/tag/v1.1.21
[1.1.20]: https://github.com/ASTRAL-SMP/AST-Tweaks/releases/tag/v1.1.20
[1.1.19]: https://github.com/ASTRAL-SMP/AST-Tweaks/releases/tag/v1.1.19
[1.1.18]: https://github.com/ASTRAL-SMP/AST-Tweaks/releases/tag/v1.1.18
[1.1.17]: https://github.com/ASTRAL-SMP/AST-Tweaks/releases/tag/v1.1.17
[1.1.16]: https://github.com/ASTRAL-SMP/AST-Tweaks/releases/tag/v1.1.16
[1.1.15]: https://github.com/ASTRAL-SMP/AST-Tweaks/releases/tag/v1.1.15
[1.1.14]: https://github.com/ASTRAL-SMP/AST-Tweaks/releases/tag/v1.1.14
[1.1.13]: https://github.com/ASTRAL-SMP/AST-Tweaks/releases/tag/v1.1.13
[1.1.12]: https://github.com/ASTRAL-SMP/AST-Tweaks/releases/tag/v1.1.12
[1.1.11]: https://github.com/ASTRAL-SMP/AST-Tweaks/releases/tag/v1.1.11
[1.1.10]: https://github.com/ASTRAL-SMP/AST-Tweaks/releases/tag/v1.1.10
[1.1.9]: https://github.com/ASTRAL-SMP/AST-Tweaks/releases/tag/v1.1.9
[1.1.8]: https://github.com/ASTRAL-SMP/AST-Tweaks/releases/tag/v1.1.8
[1.1.7]: https://github.com/ASTRAL-SMP/AST-Tweaks/releases/tag/v1.1.7
[1.1.6]: https://github.com/ASTRAL-SMP/AST-Tweaks/releases/tag/v1.1.6
[1.1.5]: https://github.com/ASTRAL-SMP/AST-Tweaks/releases/tag/v1.1.5
[1.1.4]: https://github.com/ASTRAL-SMP/AST-Tweaks/releases/tag/v1.1.4
[1.1.3]: https://github.com/ASTRAL-SMP/AST-Tweaks/releases/tag/v1.1.3
[1.1.2]: https://github.com/ASTRAL-SMP/AST-Tweaks/releases/tag/v1.1.2
[1.0.9]: https://github.com/ASTRAL-SMP/AST-Tweaks/releases/tag/v1.0.9
[1.0.8]: https://github.com/ASTRAL-SMP/AST-Tweaks/releases/tag/v1.0.8
[1.0.7]: https://github.com/ASTRAL-SMP/AST-Tweaks/releases/tag/v1.0.7
[1.0.6]: https://github.com/ASTRAL-SMP/AST-Tweaks/releases/tag/v1.0.6
[1.0.5]: https://github.com/ASTRAL-SMP/AST-Tweaks/releases/tag/v1.0.5
[1.0.4]: https://github.com/ASTRAL-SMP/AST-Tweaks/releases/tag/v1.0.4
[1.0.3]: https://github.com/ASTRAL-SMP/AST-Tweaks/compare/v1.0.2...v1.0.3
[1.0.2]: https://github.com/ASTRAL-SMP/AST-Tweaks/releases/tag/v1.0.2
[1.0.1]: https://github.com/ASTRAL-SMP/AST-Tweaks/releases/tag/v1.0.1
