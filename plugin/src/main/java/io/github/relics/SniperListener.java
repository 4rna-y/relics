package io.github.relics;

import java.util.function.Predicate;

import io.papermc.paper.event.player.PlayerArmSwingEvent;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

/**
 * スナイパーライフル: 覗いている間 (望遠鏡を使用中) に腕を振る = 左クリックで発射。
 *
 * <p>右クリックは覗く操作そのものなので使えない。腕を振ったときに、利き手の使用中アイテムがライフルなら撃つ。
 * 弾はアメジストの欠片 1 個。視線の先を直線で判定し (ブロックに遮られる)、当たった生き物にダメージ。
 * 攻撃者はプレイヤーにするので、討伐の扱い (Buftasks のタスクなど) は剣で倒したのと同じ。
 */
public final class SniperListener implements Listener {

    private final RelicsPlugin plugin;

    public SniperListener(RelicsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onSwing(PlayerArmSwingEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Player player = event.getPlayer();
        if (!player.isHandRaised()) {
            return;
        }
        ItemStack rifle = player.getInventory().getItemInMainHand();
        if (!RelicItems.is(rifle, RelicItems.SNIPER_RIFLE) || !RelicItems.is(player.getActiveItem(), RelicItems.SNIPER_RIFLE)) {
            return;
        }
        fire(player, rifle);
    }

    void fire(Player player, ItemStack rifle) {
        if (player.getCooldown(Material.SPYGLASS) > 0) {
            return;
        }
        ItemStack shard = new ItemStack(Material.AMETHYST_SHARD, 1);
        if (!player.getInventory().containsAtLeast(shard, 1)) {
            player.sendActionBar(plugin.message("<red>弾が無い (アメジストの欠片)"));
            player.playSound(player, Sound.BLOCK_DISPENSER_FAIL, SoundCategory.PLAYERS, 0.8f, 1.2f);
            player.setCooldown(Material.SPYGLASS, 10);
            return;
        }
        player.getInventory().removeItem(shard);
        player.setCooldown(Material.SPYGLASS, plugin.sniperCooldownTicks());

        Location eye = player.getEyeLocation();
        Vector direction = eye.getDirection().normalize();
        double range = plugin.sniperRange();
        boolean hitPlayers = plugin.sniperHitsPlayers();
        Predicate<Entity> target = entity -> entity != player && entity instanceof LivingEntity
                && (hitPlayers || !(entity instanceof Player));
        RayTraceResult hit = player.getWorld().rayTrace(eye, direction, range, FluidCollisionMode.NEVER, true, 0.3, target);
        double distance = hit == null ? range : hit.getHitPosition().distance(eye.toVector());

        // 軌跡と音
        for (double d = 1.0; d < distance; d += 1.0) {
            Location point = eye.clone().add(direction.clone().multiply(d));
            player.getWorld().spawnParticle(Particle.END_ROD, point, 1, 0, 0, 0, 0);
        }
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_BLAST, SoundCategory.PLAYERS, 1.0f, 0.5f);
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_CROSSBOW_SHOOT, SoundCategory.PLAYERS, 1.0f, 0.6f);

        if (hit != null && hit.getHitEntity() instanceof LivingEntity victim) {
            victim.damage(plugin.sniperDamage(), player);
            player.getWorld().spawnParticle(Particle.CRIT, hit.getHitPosition().toLocation(player.getWorld()), 12, 0.2, 0.2, 0.2, 0.1);
            player.playSound(player, Sound.ENTITY_ARROW_HIT_PLAYER, SoundCategory.PLAYERS, 0.8f, 1.0f);
        }

        int shots = RelicItems.shots(rifle) + 1;
        int max = plugin.sniperShots();
        if (shots >= max) {
            player.getInventory().setItemInMainHand(null);
            player.playSound(player, Sound.ENTITY_ITEM_BREAK, SoundCategory.PLAYERS, 1.0f, 0.8f);
            player.sendMessage(plugin.message("<red>スナイパーライフルが壊れた (" + max + " 発)。"));
            return;
        }
        RelicItems.setShots(rifle, shots, max);
        player.sendActionBar(plugin.message("<gray>残り " + (max - shots) + " 発"));
    }
}
