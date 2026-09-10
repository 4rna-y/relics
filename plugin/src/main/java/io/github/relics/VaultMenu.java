package io.github.relics;

import java.util.UUID;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/** 異次元チェストの画面。持ち主の UUID を覚えていて、閉じたときにその人の PDC へ保存する。 */
public final class VaultMenu implements InventoryHolder {

    private final UUID owner;
    private final Inventory inventory;

    public VaultMenu(UUID owner, int size, Component title, ItemStack[] contents) {
        this.owner = owner;
        this.inventory = Bukkit.createInventory(this, size, title);
        for (int i = 0; i < contents.length && i < size; i++) {
            inventory.setItem(i, contents[i]);
        }
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    public UUID owner() {
        return owner;
    }
}
