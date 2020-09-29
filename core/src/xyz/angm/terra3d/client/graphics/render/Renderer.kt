/*
 * Developed as part of the Terra3D project.
 * This file was last modified at 9/29/20, 7:31 PM.
 * Copyright 2020, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.terra3d.client.graphics.render

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.badlogic.gdx.graphics.g3d.utils.DefaultShaderProvider
import com.badlogic.gdx.utils.Disposable
import ktx.assets.file
import xyz.angm.rox.Entity
import xyz.angm.rox.Family.Companion.allOf
import xyz.angm.terra3d.client.ecs.components.FOV
import xyz.angm.terra3d.client.ecs.components.render.ModelRenderComponent
import xyz.angm.terra3d.client.graphics.screens.GameScreen
import xyz.angm.terra3d.client.graphics.screens.WORLD_HEIGHT
import xyz.angm.terra3d.client.graphics.screens.WORLD_WIDTH
import xyz.angm.terra3d.common.ecs.dayTime
import xyz.angm.terra3d.common.ecs.modelRender
import xyz.angm.terra3d.common.ecs.playerRender

/** Responsible for 3D rendering the game. */
class Renderer(private val screen: GameScreen, private val dayTimeEntity: Entity) : Disposable {

    val cam = PerspectiveCamera(FOV, WORLD_WIDTH, WORLD_HEIGHT)
    private val env = Environment()
    private val modelBatch = ModelBatch(DefaultShaderProvider(file("shader/vertex.glsl"), file("shader/fragment.glsl")))
    private val renderableEntities = allOf(ModelRenderComponent::class)

    init {
        cam.near = 0.15f
        cam.far = 300f
        cam.update()
    }

    fun render() {
        env.preRender(this, dayTimeEntity[dayTime].time)
        modelBatch.begin(cam)
        env.render(modelBatch)
        renderWorld(modelBatch, cam, env.gdxEnv)
        modelBatch.end()
    }

    /** Renders all entities and the world, using the given objects.
     * The batch should already have begin() called, end() is NOT called. */
    internal fun renderWorld(batch: ModelBatch, cam: Camera, environment: com.badlogic.gdx.graphics.g3d.Environment?) {
        screen.client.lock()
        screen.world.render(batch, cam, environment)
        screen.player[playerRender].render(batch, environment)
        screen.engine[renderableEntities].forEach { it[modelRender].render(batch, environment) }
        screen.client.unlock()
    }

    override fun dispose() {
        env.dispose()
        modelBatch.dispose()
    }
}