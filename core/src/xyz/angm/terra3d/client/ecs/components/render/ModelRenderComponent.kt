/*
 * Developed as part of the Terra3D project.
 * This file was last modified at 9/17/20, 7:39 PM.
 * Copyright 2020, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.terra3d.client.ecs.components.render

import com.badlogic.gdx.graphics.g3d.Environment
import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.badlogic.gdx.graphics.g3d.ModelInstance

/** A render component that uses a model to render itself.
 * @property model The model instance used in render calls. */
class ModelRenderComponent : RenderableComponent {
    lateinit var model: ModelInstance
    override fun render(batch: ModelBatch, environment: Environment?) = batch.render(model, environment)
}