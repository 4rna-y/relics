'use strict';
// Relics の通し確認: 異次元チェストの出し入れと設置拒否、スナイパーライフルの射撃。観測を JSON で流す。
const mineflayer = require('mineflayer');
const { Vec3 } = require('vec3');

const PORT = Number(process.argv[2] || 25597);
const emit = (event, fields = {}) => process.stdout.write(JSON.stringify({ event, ...fields }) + '\n');
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

const bot = mineflayer.createBot({ host: '127.0.0.1', port: PORT, username: 'Tester', auth: 'offline', version: '26.1' });
const chat = [];
const actionBars = [];
bot.on('message', (m, position) => { const s = m.toString(); if (position === 'game_info') actionBars.push(s); else { chat.push(s); emit('chat', { text: s }); } });
bot.on('actionBar', (m) => { actionBars.push(m.toString()); });
bot.on('kicked', (r) => { emit('kicked', { reason: JSON.stringify(r).slice(0, 300) }); process.exit(2); });
bot.on('error', (e) => { emit('error', { message: String(e) }); });

const openWindow = (action, timeoutMs = 5000) => new Promise((resolve) => {
  const timer = setTimeout(() => { bot.removeListener('windowOpen', onOpen); resolve(null); }, timeoutMs);
  const onOpen = (win) => { clearTimeout(timer); resolve(win); };
  bot.once('windowOpen', onOpen);
  action();
});
const count = (name) => bot.inventory.items().filter((i) => i.name === name).reduce((n, i) => n + i.count, 0);
const waitFor = async (pred, timeoutMs) => { const t = Date.now(); while (Date.now() - t < timeoutMs) { if (pred()) return true; await sleep(100); } return false; };

bot.once('spawn', async () => {
  try {
    emit('spawned', { pos: bot.entity.position });
    // 道具が届くのを待つ
    await waitFor(() => count('ender_chest') > 0 && count('spyglass') > 0 && count('amethyst_shard') > 0, 20000);
    emit('items', { chest: count('ender_chest'), rifle: count('spyglass'), shards: count('amethyst_shard') });

    // ---- 異次元チェスト: 開く → アメジストを入れる → 閉じる → 開き直す
    const chest = bot.inventory.items().find((i) => i.name === 'ender_chest');
    await bot.equip(chest, 'hand');
    let win = await openWindow(() => bot.activateItem());
    emit('vault_open', { opened: !!win, title: win && JSON.stringify(win.title).slice(0, 80), size: win && win.inventoryStart });
    if (win) {
      const shardSlot = win.slots.findIndex((s, i) => s && i >= win.inventoryStart && s.name === 'amethyst_shard');
      emit('vault_shard_slot', { slot: shardSlot });
      // 4 個だけ移す: 右クリックで半分取り、0 に置く
      await bot.clickWindow(shardSlot, 1, 0);
      await sleep(300);
      await bot.clickWindow(0, 0, 0);
      await sleep(500);
      emit('vault_put', { slot0: win.slots[0] && win.slots[0].name + 'x' + win.slots[0].count, shardsLeft: count('amethyst_shard') });
      bot.closeWindow(win);
      await sleep(800);
      win = await openWindow(() => bot.activateItem());
      emit('vault_reopen', { opened: !!win, slot0: win && win.slots[0] && win.slots[0].name + 'x' + win.slots[0].count });
      if (win) bot.closeWindow(win);
      await sleep(500);
    }
    // 設置できないこと: 足元の隣に置こうとする
    const feet = bot.entity.position.floored();
    const target = feet.offset(1, 0, 0);
    const ground = bot.blockAt(target.offset(0, -1, 0));
    await bot.equip(chest, 'hand');
    await bot.lookAt(target.offset(0.5, 0.5, 0.5), true);
    let placeError = null;
    await bot.placeBlock(ground, new Vec3(0, 1, 0)).catch((e) => { placeError = String(e).slice(0, 80); });
    await sleep(800);
    if (bot.currentWindow) bot.closeWindow(bot.currentWindow);
    emit('place_attempt', { blockAfter: bot.blockAt(target) && bot.blockAt(target).name, error: placeError, chestStillHeld: count('ender_chest') });

    // ---- スナイパーライフル: 8 m 先にゾンビを湧かせてもらい、覗いて撃つ
    const zombieAt = feet.offset(8, 0, 0);
    emit('summon_here', { cmd: `summon zombie ${zombieAt.x + 0.5} ${zombieAt.y} ${zombieAt.z + 0.5} {NoAI:1b,PersistenceRequired:1b}` });
    const spawned = await waitFor(() => Object.values(bot.entities).some((e) => e.name === 'zombie'), 15000);
    const zombie = Object.values(bot.entities).find((e) => e.name === 'zombie');
    emit('zombie', { spawned, pos: zombie && zombie.position });
    const rifle = bot.inventory.items().find((i) => i.name === 'spyglass');
    await bot.equip(rifle, 'hand');
    if (zombie) await bot.lookAt(zombie.position.offset(0, 1.2, 0), true);
    await sleep(300);
    const shardsBefore = count('amethyst_shard');
    bot.activateItem();
    await sleep(400);
    bot.swingArm('right');
    await sleep(1500);
    bot.deactivateItem();
    const alive = Object.values(bot.entities).some((e) => e.name === 'zombie');
    emit('shot', { zombieAliveAfter: alive, shardsBefore, shardsAfter: count('amethyst_shard'), actionBars: actionBars.slice(-3) });
    // クールタイム中の連射は 1 発しか減らない
    bot.activateItem();
    await sleep(300);
    bot.swingArm('right');
    await sleep(100);
    bot.swingArm('right');
    await sleep(1200);
    bot.deactivateItem();
    emit('double_swing', { shardsAfter: count('amethyst_shard'), actionBars: actionBars.slice(-2) });
    emit('done');
  } catch (e) {
    emit('scenario_error', { message: String(e && e.stack || e) });
  } finally {
    await sleep(500);
    bot.quit();
    setTimeout(() => process.exit(0), 1000);
  }
});
