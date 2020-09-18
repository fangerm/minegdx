package xyz.angm.rox

/** An extension of [EntitySystem] that allows implementing logic
 * per-entity instead of per-update.
 *
 * @param family The family of entities to act on.
 * @param priority [EntitySystem.priority] */
abstract class IteratingSystem(private val family: Family, priority: Int = 0) : EntitySystem(priority) {

    override fun update(delta: Float) {
        for (entity in engine[family]) {
            process(entity, delta)
        }
    }

    /** This function is called for every entity in [family] every update cycle.
     * @param entity The entity to process
     * @param delta Time since last call */
    abstract fun process(entity: Entity, delta: Float)
}