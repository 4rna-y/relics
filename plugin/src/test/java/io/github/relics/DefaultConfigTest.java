package io.github.relics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 同梱 config.yml の既定値と、アイテムの印。 */
class DefaultConfigTest {

    private final YamlConfiguration config = loadBundledConfig();

    @Test
    @DisplayName("既定値はコード側と一致する")
    void defaults() {
        assertTrue(config.getBoolean("enabled"));
        assertEquals(RelicsPlugin.DEFAULT_MESSAGE_PREFIX, config.getString("message-prefix"));
        assertEquals("[Relics] ", PlainTextComponentSerializer.plainText().serialize(
                MiniMessage.miniMessage().deserialize(config.getString("message-prefix", ""))));
        assertEquals(RelicsPlugin.DEFAULT_VAULT_SIZE, config.getInt("vault.size"));
        assertEquals(RelicsPlugin.DEFAULT_VAULT_TITLE, config.getString("vault.title"));
        assertEquals(RelicsPlugin.DEFAULT_SNIPER_DAMAGE, config.getDouble("sniper.damage"), 1e-9);
        assertEquals(RelicsPlugin.DEFAULT_SNIPER_RANGE, config.getDouble("sniper.range"), 1e-9);
        assertEquals(RelicsPlugin.DEFAULT_SNIPER_COOLDOWN_TICKS, config.getInt("sniper.cooldown-ticks"));
        assertEquals(RelicsPlugin.DEFAULT_SNIPER_SHOTS, config.getInt("sniper.shots"));
        assertFalse(config.getBoolean("sniper.hit-players"));
    }

    @Test
    @DisplayName("土台は RaidEvent の CustomItems と同じ (エンダーチェスト・望遠鏡)、ゾンビは一撃")
    void items() {
        assertEquals(Material.ENDER_CHEST, RelicItems.baseOf(RelicItems.DIMENSIONAL_CHEST).orElseThrow());
        assertEquals(Material.SPYGLASS, RelicItems.baseOf(RelicItems.SNIPER_RIFLE).orElseThrow());
        assertTrue(RelicItems.baseOf("mace").isEmpty());
        assertTrue(RelicsPlugin.DEFAULT_SNIPER_DAMAGE >= 20, "ゾンビ (20 HP) を一撃にする");
        assertEquals("relics:item", RelicItems.ITEM.toString());
        assertEquals(List.of("覗いている間に左クリックで発射", "弾: アメジストの欠片 1 個", "残り 199 発"), RelicItems.sniperLore(199));
    }

    static YamlConfiguration loadBundledConfig() {
        InputStream stream = DefaultConfigTest.class.getResourceAsStream("/config.yml");
        assertNotNull(stream, "config.yml が同梱されていない");
        return YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
    }
}
