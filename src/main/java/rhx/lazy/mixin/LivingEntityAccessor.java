package rhx.lazy.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LivingEntity.class)
public interface LivingEntityAccessor {
    @Invoker("dropEquipment")
    void lazy$dropEquipment();

    @Invoker("dropCustomDeathLoot")
    void lazy$dropCustomDeathLoot(ServerLevel level, DamageSource damageSource, boolean recentlyHit);
}
