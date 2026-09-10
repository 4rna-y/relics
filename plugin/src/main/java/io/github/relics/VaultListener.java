package io.github.relics;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/**
 * 異次元チェスト: 設置を止め、右クリックで倉庫を開き、閉じたら保存する。
 *
 * <p>誰が持っていても開くのは開いた人の倉庫。死んで落としても、拾った人には自分の倉庫が開くだけ。
 */
public final class VaultListener implements Listener {

    private final RelicsPlugin plugin;
    private final VaultStore store;

    public VaultListener(RelicsPlugin plugin, VaultStore store) {
        this.plugin = plugin;
        this.store = store;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (RelicItems.is(event.getItemInHand(), RelicItems.DIMENSIONAL_CHEST)) {
            event.setCancelled(true);
            event.getPlayer().sendActionBar(plugin.message("<red>異次元チェストは置けない。右クリックで開く"));
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        ItemStack item = event.getItem();
        if (!RelicItems.is(item, RelicItems.DIMENSIONAL_CHEST)) {
            return;
        }
        Player player = event.getPlayer();
        if (event.getHand() == EquipmentSlot.OFF_HAND
                && RelicItems.is(player.getInventory().getItemInMainHand(), RelicItems.DIMENSIONAL_CHEST)) {
            event.setCancelled(true);
            return;
        }
        event.setUseItemInHand(Event.Result.DENY);
        event.setUseInteractedBlock(Event.Result.DENY);
        event.setCancelled(true);
        open(player);
    }

    public void open(Player player) {
        int size = plugin.vaultSize();
        VaultMenu menu = new VaultMenu(player.getUniqueId(), size,
                MiniMessage.miniMessage().deserialize(plugin.vaultTitle()), store.load(player, size));
        player.openInventory(menu.getInventory());
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof VaultMenu menu && event.getPlayer() instanceof Player player) {
            if (player.getUniqueId().equals(menu.owner())) {
                store.save(player, menu.getInventory().getContents());
            }
        }
    }

    /** 停止時: 開いている倉庫を保存して閉じる。 */
    public void closeAll() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (player.getOpenInventory().getTopInventory().getHolder() instanceof VaultMenu menu
                    && player.getUniqueId().equals(menu.owner())) {
                store.save(player, menu.getInventory().getContents());
                player.closeInventory();
            }
        }
    }
}
