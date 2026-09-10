package io.github.relics;

import java.util.List;

import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.plugin.java.JavaPlugin;

/** Relics プラグインの入口。 */
public final class RelicsPlugin extends JavaPlugin {

    public static final String DEFAULT_MESSAGE_PREFIX = "<gray>[<light_purple>Relics<gray>]</gray> ";
    public static final int DEFAULT_VAULT_SIZE = 54;
    public static final String DEFAULT_VAULT_TITLE = "異次元チェスト";
    public static final double DEFAULT_SNIPER_DAMAGE = 24;
    public static final double DEFAULT_SNIPER_RANGE = 128;
    public static final int DEFAULT_SNIPER_COOLDOWN_TICKS = 20;
    public static final int DEFAULT_SNIPER_SHOTS = 200;

    private VaultListener vault;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        if (!getConfig().getBoolean("enabled", true)) {
            getSLF4JLogger().warn("config.yml で enabled: false になっているため、何も行いません。");
            return;
        }
        this.vault = new VaultListener(this, new VaultStore());
        getServer().getPluginManager().registerEvents(vault, this);
        getServer().getPluginManager().registerEvents(new SniperListener(this), this);
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event ->
                event.registrar().register("relics", "特別なアイテムの配布と確認", List.of(), new RelicsCommand(this)));
        getSLF4JLogger().info("倉庫 {} マス / スナイパー {} ダメージ・{} m・{} 発", vaultSize(), sniperDamage(), sniperRange(), sniperShots());
    }

    @Override
    public void onDisable() {
        if (vault != null) {
            vault.closeAll();
        }
    }

    public int vaultSize() {
        int size = getConfig().getInt("vault.size", DEFAULT_VAULT_SIZE);
        size = Math.clamp(size, 9, 54);
        return size - size % 9;
    }

    public String vaultTitle() {
        return getConfig().getString("vault.title", DEFAULT_VAULT_TITLE);
    }

    public double sniperDamage() {
        return getConfig().getDouble("sniper.damage", DEFAULT_SNIPER_DAMAGE);
    }

    public double sniperRange() {
        return getConfig().getDouble("sniper.range", DEFAULT_SNIPER_RANGE);
    }

    public int sniperCooldownTicks() {
        return Math.max(1, getConfig().getInt("sniper.cooldown-ticks", DEFAULT_SNIPER_COOLDOWN_TICKS));
    }

    public int sniperShots() {
        return Math.max(1, getConfig().getInt("sniper.shots", DEFAULT_SNIPER_SHOTS));
    }

    public boolean sniperHitsPlayers() {
        return getConfig().getBoolean("sniper.hit-players", false);
    }

    public Component message(String miniMessage) {
        String prefix = getConfig().getString("message-prefix", DEFAULT_MESSAGE_PREFIX);
        return MiniMessage.miniMessage().deserialize(prefix + miniMessage);
    }
}
