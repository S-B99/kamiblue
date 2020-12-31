package me.zeroeightsix.kami.gui.hudgui.elements.combat

import me.zeroeightsix.kami.event.events.SafeTickEvent
import me.zeroeightsix.kami.gui.hudgui.HudElement
import me.zeroeightsix.kami.setting.GuiConfig.setting
import me.zeroeightsix.kami.util.InventoryUtils
import me.zeroeightsix.kami.util.color.ColorGradient
import me.zeroeightsix.kami.util.color.ColorHolder
import me.zeroeightsix.kami.util.graphics.GlStateUtils
import me.zeroeightsix.kami.util.graphics.RenderUtils2D
import me.zeroeightsix.kami.util.graphics.VertexHelper
import me.zeroeightsix.kami.util.graphics.font.FontRenderAdapter
import me.zeroeightsix.kami.util.graphics.font.HAlign
import me.zeroeightsix.kami.util.graphics.font.VAlign
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.init.Items
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.gameevent.TickEvent
import org.kamiblue.commons.utils.MathUtils
import org.kamiblue.event.listener.asyncListener
import kotlin.math.max

object Armor : HudElement(
    name = "Armor",
    category = Category.COMBAT,
    description = "Show the dura of armor and the count of them"
) {

    private val classic = setting("Classic", false)
    private val armorCount = setting("ArmorCount", true)

    override val maxWidth: Float
        get() = if (classic.value) {
            80.0f
        } else {
            stringWidth
        }

    override val maxHeight: Float
        get() = if (classic.value) {
            40.0f
        } else {
            80.0f
        }

    private var stringWidth = 120.0f

    private val armorItems = arrayOf(Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS)
    private val armorCounts = IntArray(4)
    private val duraColorGradient = ColorGradient(
        0f to ColorHolder(180, 20, 20),
        50f to ColorHolder(240, 220, 20),
        100f to ColorHolder(20, 232, 20)
    )

    init {
        asyncListener<SafeTickEvent> { event ->
            if (event.phase != TickEvent.Phase.END) return@asyncListener

            for ((index, item) in armorItems.withIndex()) {
                armorCounts[index] = InventoryUtils.countItemAll(item)
            }

            width = maxWidth
            height = maxHeight
        }
    }

    override fun renderHud(vertexHelper: VertexHelper) {
        super.renderHud(vertexHelper)
        val player = mc.player ?: return

        GlStateManager.pushMatrix()

        if (classic.value) {
            val itemY = if (dockingV != VAlign.TOP) (FontRenderAdapter.getFontHeight() + 4.0f).toInt() else 2
            val duraY = if (dockingV != VAlign.TOP) 2.0f else 22.0f

            for ((index, armor) in player.armorInventoryList.reversed().withIndex()) {
                drawItem(armor, index, 2, itemY)

                if (armor.isItemStackDamageable) {
                    val duraPercent = MathUtils.round((armor.maxDamage - armor.itemDamage) / armor.maxDamage.toFloat() * 100.0f, 1).toFloat()
                    val string = duraPercent.toInt().toString()
                    val width = FontRenderAdapter.getStringWidth(string)
                    val color = duraColorGradient.get(duraPercent)

                    FontRenderAdapter.drawString(string, 10 - width * 0.5f, duraY, color = color)
                }

                GlStateManager.translate(20.0f, 0.0f, 0.0f)
            }
        } else {
            val itemX = if (dockingH != HAlign.RIGHT) 2 else (stringWidth - 18).toInt()
            val duraY = 10.0f - FontRenderAdapter.getFontHeight() * 0.5f
            var maxWidth = 0.0f

            for ((index, armor) in player.armorInventoryList.reversed().withIndex()) {
                drawItem(armor, index, itemX, 2)

                if (armor.isItemStackDamageable) {
                    val dura = armor.maxDamage - armor.itemDamage
                    val duraPercent = MathUtils.round(dura / armor.maxDamage.toFloat() * 100.0f, 1).toFloat()

                    val string = "$dura/${armor.maxDamage}  ($duraPercent%)"
                    val duraWidth = FontRenderAdapter.getStringWidth(string)
                    val duraX = if (dockingH != HAlign.RIGHT) 22.0f else stringWidth - 22.0f - duraWidth
                    val color = duraColorGradient.get(duraPercent)
                    maxWidth = max(duraWidth, maxWidth)

                    FontRenderAdapter.drawString(string, duraX, duraY, color = color)
                }

                GlStateManager.translate(0.0f, 20.0f, 0.0f)
            }

            stringWidth = maxWidth + 24.0f
        }

        GlStateManager.popMatrix()
    }

    private fun drawItem(itemStack: ItemStack, index: Int, x: Int, y: Int) {
        RenderUtils2D.drawItem(itemStack, x, y, drawOverlay = false)
        if (armorCount.value) {
            val string = armorCounts[index].toString()
            val width = FontRenderAdapter.getStringWidth(string)
            val height = FontRenderAdapter.getFontHeight()

            GlStateUtils.depth(false)
            FontRenderAdapter.drawString(string, x + 16.0f -width, y + 16.0f - height)
            GlStateUtils.depth(true)
        }
    }
}