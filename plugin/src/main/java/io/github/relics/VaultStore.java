package io.github.relics;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

/**
 * 倉庫の中身の保存先 (プレイヤーの PDC、バイト列)。
 *
 * <p>PDC は {@code world/playerdata} に入るので、wiah がワールドを消せば倉庫も消える。
 * mstore に置くとリセットを越えて物が残り、ハードコアの前提が崩れるので置かない。
 */
public final class VaultStore {

    static final NamespacedKey VAULT = new NamespacedKey("relics", "vault");

    public ItemStack[] load(Player player, int size) {
        byte[] bytes = player.getPersistentDataContainer().get(VAULT, PersistentDataType.BYTE_ARRAY);
        ItemStack[] contents = new ItemStack[size];
        if (bytes == null || bytes.length == 0) {
            return contents;
        }
        ItemStack[] stored = ItemStack.deserializeItemsFromBytes(bytes);
        for (int i = 0; i < stored.length && i < size; i++) {
            contents[i] = stored[i];
        }
        return contents;
    }

    public void save(Player player, ItemStack[] contents) {
        boolean empty = true;
        for (ItemStack item : contents) {
            if (item != null && !item.getType().isAir()) {
                empty = false;
                break;
            }
        }
        if (empty) {
            player.getPersistentDataContainer().remove(VAULT);
            return;
        }
        player.getPersistentDataContainer().set(VAULT, PersistentDataType.BYTE_ARRAY, ItemStack.serializeItemsAsBytes(contents));
    }
}
