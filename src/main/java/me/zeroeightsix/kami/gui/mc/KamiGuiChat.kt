package me.zeroeightsix.kami.gui.mc

import me.zeroeightsix.kami.command.CommandManager
import me.zeroeightsix.kami.gui.kami.theme.kami.KamiGuiColors
import me.zeroeightsix.kami.mixin.extension.historyBuffer
import me.zeroeightsix.kami.mixin.extension.sentHistoryCursor
import me.zeroeightsix.kami.util.color.ColorHolder
import me.zeroeightsix.kami.util.graphics.GlStateUtils
import me.zeroeightsix.kami.util.graphics.RenderUtils2D
import me.zeroeightsix.kami.util.graphics.VertexHelper
import me.zeroeightsix.kami.util.math.Vec2d
import net.minecraft.client.gui.GuiChat
import org.kamiblue.command.AbstractArg
import org.kamiblue.commons.extension.stream
import org.lwjgl.input.Keyboard
import java.util.*
import kotlin.math.min

class KamiGuiChat(
    startStringIn: String,
    historyBufferIn: String? = null,
    sentHistoryCursorIn: Int? = null
) : GuiChat(startStringIn) {

    init {
        historyBufferIn?.let { historyBuffer = it }
        sentHistoryCursorIn?.let { sentHistoryCursor = it }
    }

    private var predictString = ""
    private var currentArg: AbstractArg<*>? = null

    override fun keyTyped(typedChar: Char, keyCode: Int) {
        if (guiChatKeyTyped(typedChar, keyCode)) return

        if (!inputField.text.startsWith(CommandManager.prefix.value)) {
            displayNormalChatGUI()
            return
        }

        if (keyCode == Keyboard.KEY_TAB && predictString.isNotBlank()) {
            inputField.text += "$predictString "
            predictString = ""
        } else {
            calcCommandPredict()
        }
    }

    private fun guiChatKeyTyped(typedChar: Char, keyCode: Int): Boolean {
        return if (keyCode == 1) {
            mc.displayGuiScreen(null)
            true
        } else if (keyCode != Keyboard.KEY_RETURN && keyCode != Keyboard.KEY_NUMPADENTER) {
            val chatGUI = mc.ingameGUI.chatGUI
            when (keyCode) {
                Keyboard.KEY_UP -> getSentHistory(-1)
                Keyboard.KEY_DOWN -> getSentHistory(1)
                Keyboard.KEY_PRIOR -> chatGUI.scroll(chatGUI.lineCount - 1)
                Keyboard.KEY_NEXT -> chatGUI.scroll(-chatGUI.lineCount + 1)
                else -> inputField.textboxKeyTyped(typedChar, keyCode)
            }
            false
        } else {
            val string = inputField.text.trim { it <= ' ' }
            if (string.isNotEmpty()) {
                sendChatMessage(string)
            }
            mc.displayGuiScreen(null)
            true
        }
    }

    private fun displayNormalChatGUI() {
        GuiChat(inputField.text).apply {
            historyBuffer = this@KamiGuiChat.historyBuffer
            sentHistoryCursor = this@KamiGuiChat.sentHistoryCursor
        }.also {
            mc.displayGuiScreen(it)
        }
    }

    private fun calcCommandPredict() {
        predictString = ""
        val string = inputField.text.removePrefix(CommandManager.prefix.value)
        val args = kotlin.runCatching { CommandManager.parseArguments(string) }.getOrNull() ?: return
        var argCount = args.size
        val inputName = args[0]

        if (argCount == 1) {
            CommandManager.getCommands()
                .stream()
                .flatMap { it.allNames.stream() }
                .filter { it.length >= inputName.length && it.startsWith(inputName) }
                .sorted()
                .findFirst()
                .orElse(null)
                ?.let { predictString = it.substring(min(inputName.length, it.length)) }

            return
        } else if (string.endsWith(' ') && string[min(string.length - 2, 0)] != ' ') {
            argCount += 1
        }
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        // Draw rect background
        drawRect(2, height - 14, width - 2, height - 2, Integer.MIN_VALUE)

        // Draw predict string
        if (predictString.isNotBlank()) {
            val posX = fontRenderer.getStringWidth(inputField.text) + inputField.x
            val posY = inputField.y
            fontRenderer.drawStringWithShadow(predictString, posX.toFloat(), posY.toFloat(), 0x666666)
        }

        // Draw normal string
        inputField.drawTextBox()

        // Draw outline around input field
        val vertexHelper = VertexHelper(GlStateUtils.useVbo())
        val pos1 = Vec2d(inputField.x - 2.0, inputField.y - 2.0)
        val pos2 = pos1.add(inputField.width.toDouble(), inputField.height.toDouble())
        RenderUtils2D.drawRectOutline(vertexHelper, pos1, pos2, 1.5f, ColorHolder(KamiGuiColors.GuiC.windowOutline.color))
    }

}