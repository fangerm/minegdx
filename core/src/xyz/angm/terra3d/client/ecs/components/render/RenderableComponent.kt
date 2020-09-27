/*
 * Developed as part of the Terra3D project.
 * This file was last modified at 9/17/20, 9:58 PM.
 * Copyright 2020, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.terra3d.client.ecs.components.render

import com.badlogic.gdx.graphics.g3d.Environment
import com.badlogic.gdx.graphics.g3d.ModelBatch
import xyz.angm.rox.Component

/** An interface for a component that can render itself into the 3D game world.
 * Note that all components that implement this interface should NOT be transferred over network when serializing an entity containing it.  */
interface RenderableComponent : Component {

    /** Called when the component should render itself.
     * @param batch A model batch with begin() already called.
     * @param environment The environment that should be used. */
    fun render(batch: ModelBatch, environment: Environment?)
}