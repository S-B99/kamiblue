package me.zeroeightsix.kami.manager.mangers

import me.zero.alpine.listener.EventHook
import me.zero.alpine.listener.Listener
import me.zeroeightsix.kami.event.events.OnUpdateWalkingPlayerEvent
import me.zeroeightsix.kami.manager.Manager
import me.zeroeightsix.kami.module.Module
import net.minecraft.util.math.Vec2f
import net.minecraft.util.math.Vec3d
import java.util.*

/**
 * @author Xiaro
 *
 * Created by Xiaro on 27/08/20
 */
object PlayerPacketManager : Manager() {
    /** TreeMap for all packets to be sent, sorted by their callers' priority */
    private val packetList = TreeMap<Module, PlayerPacket>(compareByDescending { it.modulePriority })

    private val onUpdateWalkingPlayerListener = Listener(EventHook { event: OnUpdateWalkingPlayerEvent ->
        if (packetList.isEmpty()) return@EventHook
        packetList.values.first().apply(event)
    })

    /**
     * Adds a packet to the packet list
     *
     * @param packet Packet to be add
     */
    fun addPacket(caller: Module, packet: PlayerPacket) {
        if (packet.isEmpty()) return
        packetList[caller] = packet
    }

    /**
     * Used for PlayerPacketManager. All constructor parameters are optional.
     * They are null by default. null values would not be used for modifying
     * the packet
     */
    class PlayerPacket(
            val sprinting: Boolean? = null,
            val sneaking: Boolean? = null,
            val onGround: Boolean? = null,
            val pos: Vec3d? = null,
            val rotation: Vec2f? = null
    ) {

        /**
         * Checks whether this packet contains values
         *
         * @return True if all values in this packet is null
         */
        fun isEmpty(): Boolean {
            return sprinting == null && sneaking == null && onGround == null && pos == null && rotation == null
        }

        /**
         * Apply this packet on an [event]
         *
         * @param event Event to apply on
         */
        fun apply(event: OnUpdateWalkingPlayerEvent) {
            if (this.isEmpty()) return
            this.sprinting?.let { event.sprinting = it }
            this.sneaking?.let { event.sneaking = it }
            this.onGround?.let { event.onGround = it }
            this.pos?.let { event.pos = it }
            this.rotation?.let { event.rotation = it }
        }

    }
}