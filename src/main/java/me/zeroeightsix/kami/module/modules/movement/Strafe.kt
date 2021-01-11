package me.zeroeightsix.kami.module.modules.movement

import me.zeroeightsix.kami.event.SafeClientEvent
import me.zeroeightsix.kami.mixin.extension.tickLength
import me.zeroeightsix.kami.mixin.extension.timer
import me.zeroeightsix.kami.module.Module
import me.zeroeightsix.kami.setting.ModuleConfig.setting
import me.zeroeightsix.kami.util.BaritoneUtils
import me.zeroeightsix.kami.util.MovementUtils
import me.zeroeightsix.kami.util.MovementUtils.speed
import me.zeroeightsix.kami.util.threads.safeListener
import net.minecraft.client.settings.KeyBinding
import net.minecraftforge.fml.common.gameevent.TickEvent
import kotlin.math.cos
import kotlin.math.sin

object Strafe : Module(
    category = Category.MOVEMENT,
) {
    private val airSpeedBoost by setting(getTranslationKey("AirSpeedBoost"), true)
    private val timerBoost by setting(getTranslationKey("TimerBoost"), true)
    private val autoJump by setting(getTranslationKey("AutoJump"), true)
    private val onHolding by setting(getTranslationKey("OnHoldingSprint"), false)

    private var jumpTicks = 0

    /* If you skid this you omega gay */
    init {
        onDisable {
            reset()
        }

        safeListener<TickEvent.ClientTickEvent> {
            if (!shouldStrafe()) {
                reset()
                return@safeListener
            }
            MovementUtils.setSpeed(player.speed)
            if (airSpeedBoost) player.jumpMovementFactor = 0.029f
            if (timerBoost) mc.timer.tickLength = 45.87155914306640625f

            if (autoJump && player.onGround && jumpTicks <= 0) {
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindJump.keyCode, false)
                player.motionY = 0.41
                if (player.isSprinting) {
                    val yaw = MovementUtils.calcMoveYaw()
                    player.motionX -= sin(yaw) * 0.2
                    player.motionZ += cos(yaw) * 0.2
                }
                player.isAirBorne = true
                jumpTicks = 5
            }
            if (jumpTicks > 0) jumpTicks--
        }
    }

    private fun SafeClientEvent.shouldStrafe() = !BaritoneUtils.isPathing
        && !player.capabilities.isFlying
        && !player.isElytraFlying
        && (mc.gameSettings.keyBindSprint.isKeyDown || !onHolding)
        && (player.moveForward != 0f || player.moveStrafing != 0f)

    private fun reset() {
        mc.player?.jumpMovementFactor = 0.02F
        mc.timer.tickLength = 50F
        jumpTicks = 0
    }
}
