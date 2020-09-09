package me.zeroeightsix.kami.module.modules.misc

import me.zero.alpine.listener.EventHandler
import me.zero.alpine.listener.EventHook
import me.zero.alpine.listener.Listener
import me.zeroeightsix.kami.module.Module
import me.zeroeightsix.kami.setting.Setting
import me.zeroeightsix.kami.setting.Settings
import me.zeroeightsix.kami.util.combat.CombatUtils
import net.minecraft.block.state.IBlockState
import net.minecraft.enchantment.EnchantmentHelper
import net.minecraft.init.Enchantments
import net.minecraftforge.event.entity.player.AttackEntityEvent
import net.minecraftforge.event.entity.player.PlayerInteractEvent.LeftClickBlock
import org.lwjgl.input.Mouse
import kotlin.math.pow

/**
 * Created by 086 on 2/10/2018.
 * Updated by dominikaaaa on 06/04/20
 */
@Module.Info(
        name = "AutoTool",
        description = "Automatically switch to the best tools when mining or attacking",
        category = Module.Category.MISC
)
class AutoTool : Module() {
    private val switchBack = register(Settings.b("SwitchBack", true))
    private val timeout = register(Settings.integerBuilder("Timeout").withRange(1, 100).withValue(20).withVisibility { switchBack.value }.build())
    private val preferWeapon = register(Settings.e<CombatUtils.PreferWeapon>("Prefer", CombatUtils.PreferWeapon.SWORD))

    private var shouldMoveBack = false
    private var lastSlot = 0
    private var lastChange = 0L

    init {
        switchBack.settingListener = Setting.SettingListeners { if (!switchBack.value) shouldMoveBack = false }
    }

    @EventHandler
    private val leftClickListener = Listener(EventHook { event: LeftClickBlock -> if (shouldMoveBack || !switchBack.value) equipBestTool(mc.world.getBlockState(event.pos)) })

    @EventHandler
    private val attackListener = Listener(EventHook { event: AttackEntityEvent? -> CombatUtils.equipBestWeapon(preferWeapon.value) })

    override fun onUpdate() {
        if (mc.currentScreen != null || !switchBack.value) return

        val mouse = Mouse.isButtonDown(0)
        if (mouse && !shouldMoveBack) {
            lastChange = System.currentTimeMillis()
            shouldMoveBack = true
            lastSlot = mc.player.inventory.currentItem
            mc.playerController.syncCurrentPlayItem()
        } else if (!mouse && shouldMoveBack && (lastChange + timeout.value * 10 < System.currentTimeMillis())) {
            shouldMoveBack = false
            mc.player.inventory.currentItem = lastSlot
            mc.playerController.syncCurrentPlayItem()
        }
    }

    private fun equipBestTool(blockState: IBlockState) {
        var bestSlot = -1
        var max = 0.0

        for (i in 0..8) {
            val stack = mc.player.inventory.getStackInSlot(i)
            if (stack.isEmpty) continue
            var speed = stack.getDestroySpeed(blockState)
            var eff: Int

            if (speed > 1) {
                speed += (if (EnchantmentHelper.getEnchantmentLevel(Enchantments.EFFICIENCY, stack).also { eff = it } > 0.0) eff.toDouble().pow(2.0) + 1 else 0.0).toFloat()
                if (speed > max) {
                    max = speed.toDouble()
                    bestSlot = i
                }
            }
        }
        if (bestSlot != -1) equip(bestSlot)
    }

    companion object {
        private fun equip(slot: Int) {
            mc.player.inventory.currentItem = slot
            mc.playerController.syncCurrentPlayItem()
        }
    }
}