package io.github.relics;

import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Enemy;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Trident;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.projectiles.ProjectileSource;

/**
 * 爆裂弓: 放った矢が敵対モブに命中すると、命中地点で爆発する。
 *
 * <p>爆発はブロックを壊さず、火も点けない (Modifier の「地雷系」と同じ)。矢が当たっても爆発しないのは
 * 敵対モブ以外 (プレイヤー・村人・動物) とブロック。追加の対価は無く、弓はバニラの耐久値で減る。
 *
 * <p>撃った時点で矢に印を付けるのは、命中したときには弓が手から離れている (持ち替え・死亡) かもしれないため。
 * 印は矢そのものの PDC に入れる。
 *
 * <p>Modifier の「地雷系」を選んでいるプレイヤーがこの弓を撃つと、爆発は 2 回重なる (向こうの対価の満腹度も減る)。
 * relics は他プラグインを知らないままにしてある。
 */
public final class ExplosiveBowListener implements Listener {

    /** 爆発で火を点けるか。 */
    public static final boolean SET_FIRE = false;
    /** 爆発でブロックを壊すか。死 = ワールド消滅のサーバーなので壊さない。 */
    public static final boolean BREAK_BLOCKS = false;

    private final RelicsPlugin plugin;

    public ExplosiveBowListener(RelicsPlugin plugin) {
        this.plugin = plugin;
    }

    /** 爆裂弓から放たれた矢に印を付ける。 */
    @EventHandler(ignoreCancelled = true)
    public void onShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        if (!RelicItems.is(event.getBow(), RelicItems.EXPLOSIVE_BOW)) {
            return;
        }
        if (!(event.getProjectile() instanceof AbstractArrow arrow) || arrow instanceof Trident) {
            return;
        }
        arrow.getPersistentDataContainer().set(RelicItems.EXPLOSIVE_ARROW, PersistentDataType.BOOLEAN, true);
    }

    /** 印の付いた矢が敵対モブに当たったら爆発させる。 */
    @EventHandler(ignoreCancelled = true)
    public void onHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof AbstractArrow arrow) || arrow instanceof Trident) {
            return;
        }
        if (!Boolean.TRUE.equals(arrow.getPersistentDataContainer()
                .get(RelicItems.EXPLOSIVE_ARROW, PersistentDataType.BOOLEAN))) {
            return;
        }
        if (!(event.getHitEntity() instanceof Enemy target)) {
            return;
        }
        // source に射手を渡すことで、爆発ダメージの帰属も射手になる (討伐の扱いが剣と揃う)
        ProjectileSource shooter = arrow.getShooter();
        Entity source = shooter instanceof Entity entity ? entity : null;
        target.getWorld().createExplosion(source, target.getLocation(),
                plugin.explosiveBowPower(), SET_FIRE, BREAK_BLOCKS);
    }
}
