package io.github.relics;

import java.util.List;
import java.util.Optional;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemLore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

/**
 * 特別なアイテムの印と作り方。土台アイテム + PDC {@code relics:item} + 名前。
 *
 * <p>RaidEvent の {@code CustomItems} が同じ印で作る。土台と印を変えるときは両方を直すこと。
 */
public final class RelicItems {

    public static final String DIMENSIONAL_CHEST = "dimensional_chest";
    public static final String SNIPER_RIFLE = "sniper_rifle";

    public static final NamespacedKey ITEM = new NamespacedKey("relics", "item");
    /** スナイパーライフルの発射数。 */
    public static final NamespacedKey SHOTS = new NamespacedKey("relics", "shots");

    private RelicItems() {
    }

    public static Optional<Material> baseOf(String id) {
        return switch (id) {
            case DIMENSIONAL_CHEST -> Optional.of(Material.ENDER_CHEST);
            case SNIPER_RIFLE -> Optional.of(Material.SPYGLASS);
            default -> Optional.empty();
        };
    }

    /** 持っているアイテムの id。印が無ければ空。 */
    public static Optional<String> idOf(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return Optional.empty();
        }
        return Optional.ofNullable(item.getPersistentDataContainer().get(ITEM, PersistentDataType.STRING));
    }

    public static boolean is(ItemStack item, String id) {
        return idOf(item).filter(id::equals).isPresent();
    }

    /** 印付きのアイテムを作る。サーバーが要る。 */
    public static ItemStack create(String id, int amount, int maxShots) {
        return switch (id) {
            case DIMENSIONAL_CHEST -> {
                ItemStack item = ItemStack.of(Material.ENDER_CHEST, amount);
                name(item, "異次元チェスト", NamedTextColor.LIGHT_PURPLE);
                lore(item, List.of("右クリックで自分だけの倉庫 (54 マス) を開く", "設置はできない。誰が持っても開くのは持ち主の倉庫"));
                item.setData(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
                item.editPersistentDataContainer(pdc -> pdc.set(ITEM, PersistentDataType.STRING, DIMENSIONAL_CHEST));
                yield item;
            }
            case SNIPER_RIFLE -> {
                ItemStack item = ItemStack.of(Material.SPYGLASS, 1);
                name(item, "スナイパーライフル", NamedTextColor.GOLD);
                lore(item, sniperLore(maxShots));
                item.editPersistentDataContainer(pdc -> {
                    pdc.set(ITEM, PersistentDataType.STRING, SNIPER_RIFLE);
                    pdc.set(SHOTS, PersistentDataType.INTEGER, 0);
                });
                yield item;
            }
            default -> throw new IllegalArgumentException("知らないアイテム: " + id);
        };
    }

    /** スナイパーライフルの説明。残弾を入れる。 */
    public static List<String> sniperLore(int remaining) {
        return List.of("覗いている間に左クリックで発射", "弾: アメジストの欠片 1 個", "残り " + remaining + " 発");
    }

    /** これまでの発射数。印が無ければ 0。 */
    public static int shots(ItemStack rifle) {
        Integer shots = rifle.getPersistentDataContainer().get(SHOTS, PersistentDataType.INTEGER);
        return shots == null ? 0 : shots;
    }

    /** 発射数を書き、説明の残弾を直す。 */
    public static void setShots(ItemStack rifle, int shots, int maxShots) {
        rifle.editPersistentDataContainer(pdc -> pdc.set(SHOTS, PersistentDataType.INTEGER, shots));
        lore(rifle, sniperLore(Math.max(0, maxShots - shots)));
    }

    private static void name(ItemStack item, String text, NamedTextColor color) {
        item.setData(DataComponentTypes.CUSTOM_NAME, Component.text(text, color).decoration(TextDecoration.ITALIC, false));
    }

    private static void lore(ItemStack item, List<String> lines) {
        item.setData(DataComponentTypes.LORE, ItemLore.lore(lines.stream()
                .map(line -> (Component) Component.text(line, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false))
                .toList()));
    }
}
