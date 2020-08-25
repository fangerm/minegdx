package xyz.angm.terra3d.client.graphics.panels.menu.options

/** Options submenu for keybinds. *
class KeybindsPanel(private var screen: Screen) : Panel(screen) {

private lateinit var current: Pair<Int, PlayerAction>
private lateinit var table: Table

init {
focusedActor = scrollPane {
table = table {}

onKeyDown { keycode ->
if (keycode == Input.Keys.ESCAPE) {
screen.configuration.save()
screen.popPanel()
} else {
screen.configuration.keybinds.unregisterKeybind(current.first)
screen.configuration.keybinds.registerKeybind(keycode, current.second.type)
updateBinds()
}
}

setFillParent(true)
}
this + focusedActor
clearListeners()
updateBinds()
}

private fun updateBinds() {
table.clearChildren()

screen.configuration.keybinds.getAllSorted().forEach { action ->
val label = Label("${action.second.name}:", skin)
table.add(label).pad(20f)

val button = TextButton(Input.Keys.toString(action.first), skin)
table.add(button).height(Skin.textButtonHeight).width(Skin.textButtonWidth).pad(20f).row()

button.onClick { current = action }
}
}
}*/