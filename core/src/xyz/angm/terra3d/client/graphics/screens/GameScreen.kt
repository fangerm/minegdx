/*
 * Developed by Felix Ang. (felix.ang@pm.me).
 * Last modified on 7/5/19 4:00 PM.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.terra3d.client.graphics.screens

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.utils.PerformanceCounter
import com.badlogic.gdx.utils.viewport.FitViewport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import ktx.ashley.allOf
import ktx.ashley.exclude
import ktx.ashley.get
import xyz.angm.terra3d.client.Terra3D
import xyz.angm.terra3d.client.ecs.components.LocalPlayerComponent
import xyz.angm.terra3d.client.ecs.components.render.PlayerRenderComponent
import xyz.angm.terra3d.client.ecs.systems.*
import xyz.angm.terra3d.client.graphics.panels.Panel
import xyz.angm.terra3d.client.graphics.panels.PanelStack
import xyz.angm.terra3d.client.graphics.panels.game.GameplayOverlay
import xyz.angm.terra3d.client.graphics.panels.menu.MessagePanel
import xyz.angm.terra3d.client.graphics.registerBlockChangeListener
import xyz.angm.terra3d.client.graphics.render.Renderer
import xyz.angm.terra3d.client.networking.Client
import xyz.angm.terra3d.client.networking.LocalServer
import xyz.angm.terra3d.client.resources.I18N
import xyz.angm.terra3d.client.resources.ResourceManager
import xyz.angm.terra3d.client.world.World
import xyz.angm.terra3d.common.IntVector3
import xyz.angm.terra3d.common.ecs.EntityData
import xyz.angm.terra3d.common.ecs.components.IgnoreSyncFlag
import xyz.angm.terra3d.common.ecs.components.NetworkSyncComponent
import xyz.angm.terra3d.common.ecs.components.specific.PlayerComponent
import xyz.angm.terra3d.common.ecs.dayTime
import xyz.angm.terra3d.common.ecs.playerM
import xyz.angm.terra3d.common.ecs.position
import xyz.angm.terra3d.common.ecs.systems.DayTimeSystem
import xyz.angm.terra3d.common.ecs.systems.NetworkSystem
import xyz.angm.terra3d.common.ecs.systems.RemoveSystem
import xyz.angm.terra3d.common.networking.BlockUpdate
import xyz.angm.terra3d.common.networking.ChatMessagePacket
import xyz.angm.terra3d.common.networking.InitPacket
import xyz.angm.terra3d.common.schedule

/** The game screen. Active during gameplay. Uses 2 panels; 1 for hotbar and a stack for the other panels required by the Screen interface.
 *
 * This screen is mainly a bag of other objects that make up game state;
 * and should not have any other responsibility other than initializing them and
 * setting up their interactions that drive the game.
 * The only other responsibility of this class is putting together all graphics sources and drawing them.
 *
 * The screen is initialized by [Terra3D], which means that it's only created after a server connection
 * was established and the initial [InitPacket] was received, and the world around the player was meshed.
 *
 * @param client The client for communicating with the server
 * @property world The world
 * @property cam The player's camera
 *
 * @property engine The ECS engine used
 * @property player The player controlled by this game instance
 * @property playerInputSystem The input system active in the ECS
 * @property playerInventory A simpler way of getting the players inventory
 *
 * @property gameplayPanel This panel is always active; responsible for drawing hotbar and other elements */
class GameScreen(
    private val game: Terra3D,
    val client: Client,
    val world: World,
    val player: Entity,
    entities: Array<Entity>
) : ScreenAdapter(), Screen {

    private val coScope = CoroutineScope(Dispatchers.Default)
    val bench = PerformanceCounter("render")

    // 3D Graphics
    val inputHandler = PlayerInputHandler(this)
    private val renderer = Renderer(this, entities.find { it[dayTime] != null }!!)
    val cam get() = renderer.cam

    // Entities
    val engine = Engine()
    val playerInputSystem: PlayerInputSystem get() = engine.getSystem(PlayerInputSystem::class.java)
    val playerRenderSystem = PlayerRenderSystem(this)
    val playerInventory get() = player[playerM]!!.inventory
    private val players = allOf(PlayerComponent::class).get()

    // 2D Graphics
    private val stage = Stage(FitViewport(WORLD_WIDTH, WORLD_HEIGHT))
    private val uiPanels = PanelStack()
    val gameplayPanel = GameplayOverlay(this)

    val entitiesLoaded get() = engine.entities.size()
    val systemsActive get() = engine.systems.size()
    val onlinePlayers get() = engine.getEntitiesFor(players).map { it[playerM]!!.name }

    init {
        initSystems()
        engine.addEntity(player)
        entities.forEach { engine.addEntity(it) }

        initState()
        initRender()
    }

    override fun render(delta: Float) {
        // Uncomment this and the stop call at the end to enable performance profiling.
        // startBench(delta)

        client.lock()
        world.update()
        engine.update(delta)
        client.unlock()

        renderer.render()
        stage.act()
        stage.draw()

        // bench.stop()
    }

    @Suppress("unused")
    private fun startBench(delta: Float) {
        bench.tick(delta)
        bench.start()
    }

    override fun pushPanel(panel: Panel) {
        if (uiPanels.panelsInStack == 0) { // First UI panel, switch gameplay > ui for input
            inputHandler.beforeUnregister()
            Gdx.input.inputProcessor = stage
            Gdx.input.isCursorCatched = false
            Gdx.input.setCursorPosition(stage.viewport.screenWidth / 2, (stage.viewport.screenY + stage.viewport.screenHeight) / 2)
        }
        uiPanels.pushPanel(panel)
    }

    override fun popPanel() {
        if (uiPanels.panelsInStack == 1) { // About to pop last panel, return to gameplay
            inputHandler.beforeRegister()
            Gdx.input.inputProcessor = inputHandler
            Gdx.input.isCursorCatched = true
        }
        uiPanels.popPanel()
    }

    fun popAllPanels() {
        while (uiPanels.panelsInStack != 0) popPanel()
    }

    /** Called when the game can no longer continue (disconnect; player quit; etc.)
     * Returns to the menu screen.
     * @param message The message to display. Defaults to no message which will return to menu screen immediately. */
    fun returnToMenu(message: String? = null) {
        client.send(EntityData.from(player)) // Make sure the player is up-to-date on the server
        client.disconnectListener = {} // Prevent it from showing the 'disconnected' message when it shouldn't
        game.screen = MenuScreen(game)
        dispose()
        (game.screen as Screen).pushPanel(MessagePanel(game.screen as Screen, message ?: return) {
            (game.screen as Screen).popPanel()
        })
    }

    // Initialize all ECS systems
    private fun initSystems() {
        addLocalPlayerComponents()
        engine.addSystem(PlayerSystem(this, player))

        val physicsSystem = PlayerPhysicsSystem(world::blockExists, player)
        engine.addSystem(physicsSystem)
        client.addListener {
            if (it is BlockUpdate) Gdx.app.postRunnable(physicsSystem::blockChanged)
        }

        engine.addSystem(PlayerInputSystem(this, player, engine.getSystem(PlayerPhysicsSystem::class.java), inputHandler))
        engine.addSystem(RemoveSystem())
        val netSystem = NetworkSystem(client::send)
        val renderSystem = RenderSystem()
        engine.addSystem(renderSystem)
        engine.addEntityListener(exclude(LocalPlayerComponent::class).get(), 1, renderSystem)
        engine.addSystem(netSystem)
        engine.addEntityListener(allOf(NetworkSyncComponent::class).get(), netSystem)
        engine.addSystem(DayTimeSystem())
        engine.addSystem(playerRenderSystem)
    }

    // Initialize everything not render-related
    private fun initState() {
        // Network
        val netSystem = engine.getSystem(NetworkSystem::class.java)
        client.addListener { if (it is EntityData) netSystem.receive(it) }

        client.disconnectListener = { Gdx.app.postRunnable { returnToMenu(I18N["disconnected-from-server"]) } }
        client.send(ChatMessagePacket("[CYAN]${player[playerM]!!.name}[LIGHT_GRAY] ${I18N["joined-game"]}"))

        // Input
        Gdx.input.inputProcessor = inputHandler
        Gdx.input.isCursorCatched = true

        // Chunk loading
        schedule(2000, 1000, coScope) {
            Gdx.app.postRunnable { world.updateLoadedChunks(IntVector3(player[position]!!)) }
        }
    }

    // Adds local components to the player entity.
    private fun addLocalPlayerComponents() {
        player.add(LocalPlayerComponent())
        player.add(PlayerRenderComponent())
        player.add(IgnoreSyncFlag())
    }

    // Initialize all rendering components
    private fun initRender() {
        // Initialize model cache to ensure loading of required models
        ResourceManager.models.init()

        // 2D / Stage
        stage.addActor(gameplayPanel)
        stage.addActor(uiPanels)
        stage.addActor(playerRenderSystem.getActor())

        // Sound
        registerBlockChangeListener(client, world)
    }

    override fun resize(width: Int, height: Int) = stage.viewport.update(width, height, true)

    /** hide is called when the screen is no longer active, at which point this type of screen becomes dereferenced and needs to be disposed. */
    override fun hide() = dispose()

    override fun dispose() {
        LocalServer.stop()
        client.close()
        coScope.cancel()
        renderer.dispose()
        gameplayPanel.dispose()
        uiPanels.dispose()
        engine.getSystem(PlayerPhysicsSystem::class.java).dispose()
    }
}