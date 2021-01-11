package me.zeroeightsix.kami.module.modules.chat

import me.zeroeightsix.kami.module.Module
import me.zeroeightsix.kami.setting.ModuleConfig.setting
import me.zeroeightsix.kami.util.TimeUtils
import me.zeroeightsix.kami.util.color.EnumTextColor
import me.zeroeightsix.kami.util.text.format
import net.minecraft.util.text.TextComponentString
import net.minecraftforge.client.event.ClientChatReceivedEvent
import org.kamiblue.commons.interfaces.DisplayEnum
import org.kamiblue.event.listener.listener

object ChatTimestamp : Module(
    category = Category.CHAT,
    showOnArray = false
) {
    private val color by setting(getTranslationKey("Color"), EnumTextColor.GRAY)
    private val separator by setting(getTranslationKey("Separator"), Separator.ARROWS)
    private val timeFormat by setting(getTranslationKey("TimeFormat"), TimeUtils.TimeFormat.HHMM)
    private val timeUnit by setting(getTranslationKey("TimeUnit"), TimeUtils.TimeUnit.H12)

    init {
        listener<ClientChatReceivedEvent> {
            if (mc.player == null) return@listener
            it.message = TextComponentString(formattedTime).appendSibling(it.message)
        }
    }

    val formattedTime: String
        get() = "${separator.left}${color format TimeUtils.getTime(timeFormat, timeUnit)}${separator.right} "

    @Suppress("unused")
    private enum class Separator(override val displayName: String, val left: String, val right: String) : DisplayEnum {
        ARROWS("< >", "<", ">"),
        SQUARE_BRACKETS("[ ]", "[", "]"),
        CURLY_BRACKETS("{ }", "{", "}"),
        ROUND_BRACKETS("( )", "(", ")"),
        NONE("None", "", "")
    }
}