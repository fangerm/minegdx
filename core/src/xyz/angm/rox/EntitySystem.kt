package xyz.angm.rox

abstract class EntitySystem {

    lateinit var engine: Engine

    abstract fun update(delta: Float)
}