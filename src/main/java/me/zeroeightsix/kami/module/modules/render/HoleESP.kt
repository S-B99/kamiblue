package me.zeroeightsix.kami.module.modules.render

import me.zeroeightsix.kami.KamiMod
import me.zeroeightsix.kami.event.events.RenderEvent
import me.zeroeightsix.kami.module.Module
import me.zeroeightsix.kami.module.modules.combat.CrystalAura
import me.zeroeightsix.kami.setting.Settings
import me.zeroeightsix.kami.util.ColourHolder
import me.zeroeightsix.kami.util.ESPRenderer
import net.minecraft.init.Blocks
import net.minecraft.util.math.BlockPos
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.ceil

/**
 * Created 16 November 2019 by hub
 * Updated by dominikaaaa on 15/12/19
 */
@Module.Info(
        name = "HoleESP",
        category = Module.Category.RENDER,
        description = "Show safe holes for crystal pvp"
)
class HoleESP : Module() {
    private val surroundOffset = arrayOf(
            BlockPos(0, -1, 0),  // down
            BlockPos(0, 0, -1),  // north
            BlockPos(1, 0, 0),  // east
            BlockPos(0, 0, 1),  // south
            BlockPos(-1, 0, 0) // west
    )
    private val renderDistance = register(Settings.floatBuilder("RenderDistance").withValue(8.0f).withRange(0.0f, 32.0f).build())
    private val filled = register(Settings.b("Filled", true))
    private val outline = register(Settings.b("Outline", true))
    private val r1 = register(Settings.integerBuilder("Red(Obby)").withMinimum(0).withValue(208).withMaximum(255).withVisibility { obbySettings() }.build())
    private val g1 = register(Settings.integerBuilder("Green(Obby)").withMinimum(0).withValue(144).withMaximum(255).withVisibility { obbySettings() }.build())
    private val b1 = register(Settings.integerBuilder("Blue(Obby)").withMinimum(0).withValue(255).withMaximum(255).withVisibility { obbySettings() }.build())
    private val r2 = register(Settings.integerBuilder("Red(Bedrock)").withMinimum(0).withValue(144).withMaximum(255).withVisibility { bedrockSettings() }.build())
    private val g2 = register(Settings.integerBuilder("Green(Bedrock)").withMinimum(0).withValue(144).withMaximum(255).withVisibility { bedrockSettings() }.build())
    private val b2 = register(Settings.integerBuilder("Blue(Bedrock)").withMinimum(0).withValue(255).withMaximum(255).withVisibility { bedrockSettings() }.build())
    private val aFilled = register(Settings.integerBuilder("FilledAlpha").withMinimum(0).withValue(31).withMaximum(255).withVisibility { filled.value }.build())
    private val aOutline = register(Settings.integerBuilder("OutlineAlpha").withMinimum(0).withValue(127).withMaximum(255).withVisibility { outline.value }.build())
    private val renderModeSetting = register(Settings.e<RenderMode>("RenderMode", RenderMode.BLOCK))
    private val renderBlocksSetting = register(Settings.e<RenderBlocks>("Render", RenderBlocks.BOTH))

    private var safeHoles = ConcurrentHashMap<BlockPos, ColourHolder>()

    private enum class RenderMode {
        DOWN, BLOCK
    }

    private enum class RenderBlocks {
        OBBY, BEDROCK, BOTH
    }

    private fun obbySettings(): Boolean {
        return renderBlocksSetting.value == RenderBlocks.OBBY || renderBlocksSetting.value == RenderBlocks.BOTH
    }

    private fun bedrockSettings(): Boolean {
        return renderBlocksSetting.value == RenderBlocks.BEDROCK || renderBlocksSetting.value == RenderBlocks.BOTH
    }

    override fun onUpdate() {
        safeHoles.clear()
        val range = ceil(renderDistance.value).toInt()
        val crystalAura = KamiMod.MODULE_MANAGER.getModuleT(CrystalAura::class.java)
        val blockPosList = crystalAura.getSphere(CrystalAura.getPlayerPos(), range.toFloat(), range, false, true, 0)
        for (pos in blockPosList) {
            if (mc.world.getBlockState(pos).block != Blocks.AIR// block gotta be air
                    || mc.world.getBlockState(pos.up()).block != Blocks.AIR // block 1 above gotta be air
                    || mc.world.getBlockState(pos.up().up()).block != Blocks.AIR) continue // block 2 above gotta be air

            var isSafe = true
            var isBedrock = true
            for (offset in surroundOffset) {
                val block = mc.world.getBlockState(pos.add(offset)).block
                if (block !== Blocks.BEDROCK && block !== Blocks.OBSIDIAN && block !== Blocks.ENDER_CHEST && block !== Blocks.ANVIL) {
                    isSafe = false
                    break
                }
                if (block !== Blocks.BEDROCK) {
                    isBedrock = false
                }
            }

            if (isSafe) {
                if (!isBedrock && obbySettings()) {
                    safeHoles[pos] = ColourHolder(r1.value, g1.value, b1.value)
                }
                if (isBedrock && bedrockSettings()) {
                    safeHoles[pos] = ColourHolder(r2.value, g2.value, b2.value)
                }
            }
        }
    }

    override fun onWorldRender(event: RenderEvent) {
        if (mc.player == null || safeHoles.isEmpty()) return
        val renderer = ESPRenderer(event.partialTicks)
        renderer.aFilled = if (filled.value) aFilled.value else 0
        renderer.aOutline = if (outline.value) aOutline.value else 0
        for ((pos, colour) in safeHoles) {
            renderer.add(pos, colour)
        }
        renderer.render()
    }
}