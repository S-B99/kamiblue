package me.zeroeightsix.kami.event.events

import me.zeroeightsix.kami.event.Cancellable
import me.zeroeightsix.kami.event.Event
import net.minecraft.entity.Entity

class ClientPlayerAttackEvent(val entity: Entity) : Event, Cancellable()