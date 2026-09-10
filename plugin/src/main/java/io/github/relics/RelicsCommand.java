package io.github.relics;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/** {@code /relics give <player> <dimensional_chest|sniper_rifle> [amount]} と {@code /relics status}。管理者向け。 */
public final class RelicsCommand implements BasicCommand {

    private static final List<String> ITEMS = List.of(RelicItems.DIMENSIONAL_CHEST, RelicItems.SNIPER_RIFLE);

    private final RelicsPlugin plugin;

    public RelicsCommand(RelicsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String permission() {
        return "relics.admin";
    }

    @Override
    public void execute(@NotNull CommandSourceStack source, String @NotNull [] args) {
        CommandSender sender = source.getSender();
        String sub = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "give" -> give(sender, args);
            case "status" -> {
                sender.sendMessage(plugin.message("<gray>Relics " + plugin.getPluginMeta().getVersion()
                        + " / 倉庫 " + plugin.vaultSize() + " マス / スナイパー " + plugin.sniperDamage() + " ダメージ・"
                        + plugin.sniperRange() + " m・" + plugin.sniperShots() + " 発"));
            }
            default -> sender.sendMessage(plugin.message("<red>使い方: /relics give <player> <dimensional_chest|sniper_rifle> [amount] | status"));
        }
    }

    private void give(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(plugin.message("<red>使い方: /relics give <player> <dimensional_chest|sniper_rifle> [amount]"));
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(plugin.message("<red>オンラインに居ない: " + args[1]));
            return;
        }
        String id = args[2].toLowerCase(Locale.ROOT);
        if (!ITEMS.contains(id)) {
            sender.sendMessage(plugin.message("<red>知らないアイテム: " + args[2] + " (" + ITEMS + ")"));
            return;
        }
        int amount = 1;
        if (args.length >= 4) {
            try {
                amount = Math.max(1, Integer.parseInt(args[3]));
            } catch (NumberFormatException e) {
                sender.sendMessage(plugin.message("<red>個数が整数ではない: " + args[3]));
                return;
            }
        }
        ItemStack item = RelicItems.create(id, amount, plugin.sniperShots());
        target.getInventory().addItem(item).values()
                .forEach(rest -> target.getWorld().dropItemNaturally(target.getLocation(), rest));
        sender.sendMessage(plugin.message("<green>" + target.getName() + " に " + id + " ×" + amount + " を渡した。"));
    }

    @Override
    public @NotNull Collection<String> suggest(@NotNull CommandSourceStack source, String @NotNull [] args) {
        if (args.length <= 1) {
            String prefix = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);
            return List.of("give", "status").stream().filter(s -> s.startsWith(prefix)).toList();
        }
        if (args[0].equalsIgnoreCase("give")) {
            if (args.length == 2) {
                return Bukkit.getOnlinePlayers().stream().map(Player::getName)
                        .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT))).toList();
            }
            if (args.length == 3) {
                return ITEMS.stream().filter(id -> id.startsWith(args[2].toLowerCase(Locale.ROOT))).toList();
            }
        }
        return List.of();
    }
}
