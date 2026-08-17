package rhx.lazy.feature.simulation

import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.ServerLevelAccessor
import net.minecraft.world.level.entity.EntityTypeTest
import net.minecraft.world.phys.AABB
import java.util.function.Predicate

/**
 * Presents world state to spawn finalization without letting a simulated entity interact with
 * entities that actually exist in that world.
 *
 * Some mobs create jockeys or mounts from `finalizeSpawn`. In particular, a zombie can add its new
 * chicken mount directly through the supplied accessor even though the zombie itself is never
 * added. Capturing additions here keeps the whole spawn temporary. Hiding existing entities also
 * prevents the alternate zombie-jockey path from commandeering a chicken that was already nearby.
 */
internal class IsolatedServerLevelAccessor(
    private val level: ServerLevel,
) : ServerLevelAccessor by level {
    private val addedEntities = linkedSetOf<Entity>()

    override fun getLevel(): ServerLevel = level

    override fun addFreshEntity(entity: Entity): Boolean {
        addedEntities += entity
        return false
    }

    override fun addFreshEntityWithPassengers(entity: Entity) {
        entity.selfAndPassengers.forEach(::addFreshEntity)
    }

    override fun getEntities(
        except: Entity?,
        area: AABB,
        predicate: Predicate<in Entity>,
    ): List<Entity> = emptyList()

    override fun <T : Entity> getEntities(
        typeTest: EntityTypeTest<Entity, T>,
        area: AABB,
        predicate: Predicate<in T>,
    ): List<T> = emptyList()

    override fun players(): List<Player> = emptyList()

    /** Discards every captured or riding companion without running any death or loot logic. */
    fun discardCompanions(primary: Entity) {
        val companions = linkedSetOf<Entity>()
        primary.rootVehicle.selfAndPassengers.forEach(companions::add)
        companions += addedEntities
        companions.remove(primary)
        companions
            .filter { it in addedEntities || !it.isAddedToLevel }
            .forEach(Entity::discard)
        primary.stopRiding()
        primary.ejectPassengers()
        addedEntities.clear()
    }
}
