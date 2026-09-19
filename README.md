# Relics

高難易度の報酬として出る特別なアイテムの**振る舞い**を持つ Paper プラグイン。3 種とも
[`raid_event`](../raid_event) のクレートの目玉枠が作る (`CustomItems`): 異次元チェストは L8 のマイナー/マジカル
(`custom: dimensional_chest`)、スナイパーライフルは L7/L8 のソルジャー (`sniper_rifle`)、
爆裂弓は L6/L7 のソルジャー (`explosive_bow`)。このプラグインが居ないと、それらは名前の付いた飾りになる。

| モジュール | 中身 |
| --- | --- |
| `plugin/` | Paper プラグイン `Relics` |
| `smoke/` | 実サーバー + ヘッドレスクライアントでの通し確認 |

開発環境は [`buftasks`](../buftasks) と同じ (JDK 25 / Gradle 9 / `flake.nix`)。mstore は使わない。

## アイテムの印

土台アイテム + PDC `relics:item` (= id) + 名前。`RelicItems` が作り、`RelicItems#idOf` で見分ける。
RaidEvent の `CustomItems` と土台・印を揃えてあるので、変えるときは両方を直すこと。

| id | 土台 | 名前 |
| --- | --- | --- |
| `dimensional_chest` | エンダーチェスト | 異次元チェスト |
| `sniper_rifle` | 望遠鏡 | スナイパーライフル (PDC `relics:shots` に発射数) |
| `explosive_bow` | 弓 | 爆裂弓 (放った矢に PDC `relics:explosive_arrow` の印が付く) |

## 異次元チェスト

- **設置できない** (`BlockPlaceEvent` を取り消す)。右クリックで 54 マスの倉庫「異次元チェスト」が開く。
- 中身は**開いた人の PDC** (`relics:vault`、`ItemStack#serializeItemsAsBytes`)。誰が持っていても開くのは持ち主の倉庫で、
  死んで落としても拾った人には自分の倉庫が開くだけ。
- PDC は `world/playerdata` に入るので、wiah がワールドを消せば倉庫も消える。mstore に置くとリセットを越えて物が残り、
  ハードコアの前提が崩れるので置かない。
- 閉じたときに保存。停止時は開いている倉庫を保存して閉じる。空なら鍵ごと消す。

## スナイパーライフル

- **覗いている間 (望遠鏡を使用中) に左クリック**で発射。右クリックは覗く操作そのものなので使えない。
  実装は `PlayerArmSwingEvent`: 利き手で腕を振ったとき、使用中のアイテムがライフルなら撃つ。
- 弾はアメジストの欠片 1 個。無ければ撃たない。発射後 1 秒 (`sniper.cooldown-ticks`) は撃てない (望遠鏡のクールタイム)。
- 視線の先 128 m (`sniper.range`) を直線で判定 (ブロックに遮られる)。当たった生き物に 24 ダメージ (`sniper.damage`。ゾンビ 20 HP を一撃)。
  攻撃者はプレイヤーなので、討伐の扱い (Buftasks のタスクなど) は剣で倒したのと同じ。
- プレイヤーには当たらない (`sniper.hit-players: false`。協力サーバーでの誤射防止)。
- 軌跡のパーティクルと音。アクションバーに残弾。**200 発 (`sniper.shots`) で壊れる**。

## 爆裂弓

- 放った矢が**敵対モブ (`Enemy`) に命中**すると、命中地点で爆発する (`explosive-bow.power`、既定 2.0。クリーパーが 3.0)。
- 爆発は**ブロックを壊さず、火も点けない**。ただし**近くの仲間もプレイヤー自身も巻き込む**。
- 敵対モブ以外 (プレイヤー・村人・動物) とブロックに当たったときは爆発しない。
- 追加の対価は無く、弓はバニラの耐久値で減る。修繕も効く。
- 撃った時点で矢に印を付ける (命中したときには弓が手から離れているかもしれないため)。爆発ダメージの帰属は射手なので、
  討伐の扱い (Buftasks のタスクなど) は自分で倒したのと同じ。
- Modifier の「地雷系」を選んでいる人がこの弓を撃つと、**爆発は 2 回重なる** (向こうの対価の満腹度も 1 減る)。
  relics は他プラグインを知らないままにしてある。

## コマンド

`/relics` — 権限 `relics.admin` (既定: OP)

- `give <player> <dimensional_chest|sniper_rifle|explosive_bow> [amount]` — 渡す (動作確認用。本来はレイドの報酬)
- `status` — 版と設定

## 設定 (`plugins/Relics/config.yml`)

| キー | 既定値 | 説明 |
| --- | --- | --- |
| `enabled` | `true` | `false` なら何もしない |
| `message-prefix` | `[Relics] ` | 返答の接頭辞 (MiniMessage) |
| `vault.size` / `title` | `54` / `異次元チェスト` | 倉庫の大きさ (9 の倍数、最大 54) と題 |
| `sniper.damage` / `range` / `cooldown-ticks` / `shots` / `hit-players` | `24` / `128` / `20` / `200` / `false` | スナイパーライフル |
| `explosive-bow.power` | `2.0` | 爆裂弓の爆発の強さ |

## 開発

```console
$ nix develop
$ gradle :plugin:test      # 既定値と印
$ gradle :plugin:build
$ smoke/run.sh             # 26.1 の使い捨てサーバー + mineflayer で、倉庫の出し入れ・設置拒否・射撃・爆発を一周
```

通し確認は `Modifier/e2e` が落とした 26.1 の Paper と node_modules を借りる。

## 本番への配置

Plugman 管理: リリース資産は `Relics.jar`、本番の `install.plugman` に `github:4rna-y/relics` を足す。
