package xyz.angm.rox

abstract class IteratingSystem(private val family: Family) : EntitySystem() {

    override fun update(delta: Float) {
        for (entity in engine[family]) {
            process(entity, delta)
        }
    }

    abstract fun process(entity: Entity, delta: Float)
}