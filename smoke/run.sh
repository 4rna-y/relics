#!/usr/bin/env bash
#
# 通し確認: 使い捨ての 26.1 サーバーを立て、ヘッドレスクライアントで
# 異次元チェスト (開く → 入れる → 閉じる → 開き直す → 残っている、設置できない) と
# スナイパーライフル (覗いて左クリック → 8 m 先のゾンビが死ぬ、弾が減る) を一周する。
#
# 前提: ../plugin/build/libs/Relics-*.jar、../../Modifier/e2e/build/paper/paper-26.1*.jar、
#       ../../Modifier/e2e/bot/node_modules、JAVA_HOME (nix develop の中で実行)
#
set -euo pipefail
HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "$HERE/../.." && pwd)"
SERVER="$HERE/server"
PORT="${RELICS_SMOKE_PORT:-25597}"
FIFO="/tmp/relics-smoke-$$.fifo"

say() { printf '\033[36m==>\033[0m %s\n' "$*" >&2; }
die() { printf '\033[31mエラー:\033[0m %s\n' "$*" >&2; exit 1; }

JAR="$(ls -1 "$HERE"/../plugin/build/libs/Relics-*.jar 2>/dev/null | grep -v sources | sort -V | tail -1 || true)"
[ -n "$JAR" ] || die "Relics の jar が無い。gradle :plugin:build を先に"
PAPER="$(ls -1 "$ROOT"/Modifier/e2e/build/paper/paper-26.1*.jar 2>/dev/null | head -1 || true)"
[ -n "$PAPER" ] || die "26.1 の Paper が無い ($ROOT/Modifier/e2e/build/paper/)"
[ -d "$ROOT/Modifier/e2e/bot/node_modules" ] || die "mineflayer が無い"
[ -n "${JAVA_HOME:-}" ] || die "JAVA_HOME が無い。nix develop の中で実行すること"
export PATH="$JAVA_HOME/bin:$PATH"

rm -rf "$SERVER"
mkdir -p "$SERVER/plugins"
ln -s "$PAPER" "$SERVER/paper.jar"
for d in cache libraries versions; do
    [ -e "$ROOT/Modifier/e2e/build/server/$d" ] && ln -s "$ROOT/Modifier/e2e/build/server/$d" "$SERVER/$d"
done
echo "eula=true" > "$SERVER/eula.txt"
cat > "$SERVER/server.properties" <<PROPS
server-port=$PORT
online-mode=false
level-type=minecraft:flat
generate-structures=false
spawn-protection=0
view-distance=4
simulation-distance=4
spawn-monsters=false
gamemode=survival
difficulty=normal
enforce-secure-profile=false
motd=relics-smoke
PROPS
cp "$JAR" "$SERVER/plugins/"

cleanup() {
    [ -n "${PAPER_PID:-}" ] && kill -0 "$PAPER_PID" 2>/dev/null && { echo stop > "$FIFO"; sleep 5; kill "$PAPER_PID" 2>/dev/null || true; }
    [ -n "${HOLDER_PID:-}" ] && kill "$HOLDER_PID" 2>/dev/null || true
    rm -f "$FIFO"
}
trap cleanup EXIT

mkfifo "$FIFO"
sleep 100000 > "$FIFO" &
HOLDER_PID=$!
say "Paper 26.1 を $PORT 番で起動"
( cd "$SERVER" && java -Xms1G -Xmx2G -jar paper.jar nogui < "$FIFO" > server.log 2>&1 ) &
PAPER_PID=$!
for _ in $(seq 1 60); do
    grep -q "Done (" "$SERVER/server.log" 2>/dev/null && break
    kill -0 "$PAPER_PID" 2>/dev/null || die "Paper が落ちた。$SERVER/server.log を見ること"
    sleep 3
done
grep -q "Done (" "$SERVER/server.log" || die "Paper が起動しない"
grep -q "\[Relics\] 倉庫" "$SERVER/server.log" || die "Relics が起動していない"

say "ヘッドレスクライアントを参加させる"
export NODE_PATH="$ROOT/Modifier/e2e/bot/node_modules"
node "$HERE/bot.cjs" "$PORT" > "$SERVER/bot.out" 2>&1 &
BOT_PID=$!
for _ in $(seq 1 60); do
    grep -q "Tester joined the game" "$SERVER/server.log" && break
    sleep 0.5
done
sleep 2
echo "op Tester" > "$FIFO"
echo "relics give Tester dimensional_chest" > "$FIFO"
echo "relics give Tester sniper_rifle" > "$FIFO"
echo "give Tester amethyst_shard 8" > "$FIFO"
# ボットが位置を報告したらゾンビを湧かせる
for _ in $(seq 1 60); do
    grep -q '"event":"summon_here"' "$SERVER/bot.out" && break
    sleep 0.5
done
POS="$(grep '"event":"summon_here"' "$SERVER/bot.out" | head -1 | sed -E 's/.*"cmd":"([^"]*)".*/\1/')"
[ -n "$POS" ] && echo "$POS" > "$FIFO"
wait "$BOT_PID" || true

echo
echo "== 観測 (bot.out)"
grep -v '"event":"chat"' "$SERVER/bot.out"
echo
echo "== サーバーの Relics ログ"
grep "\[Relics\]\|Summoned\|zombie" "$SERVER/server.log" | tail -6 | cut -c1-160
echo
echo "== サーバーの例外"
if grep -q "Exception" "$SERVER/server.log"; then
    grep -n -A3 "Exception" "$SERVER/server.log" | head -40
else
    echo "(なし)"
fi
say "サーバーのログ: $SERVER/server.log"
