package me.zeroeightsix.kami.command.commands

import io.netty.buffer.Unpooled
import me.zeroeightsix.kami.command.ClientCommand
import me.zeroeightsix.kami.util.Wrapper
import me.zeroeightsix.kami.util.text.MessageSendHelper
import me.zeroeightsix.kami.util.text.MessageSendHelper.sendChatMessage
import net.minecraft.item.ItemWritableBook
import net.minecraft.nbt.NBTTagList
import net.minecraft.nbt.NBTTagString
import net.minecraft.network.PacketBuffer
import net.minecraft.network.play.client.CPacketCustomPayload
import org.kamiblue.commons.extension.max

object SignBookCommand : ClientCommand(
    name = "signbook",
    alias = arrayOf("sign"),
    description = "Colored book names. &f#n&7 for a new line and &f&&7 for colour codes"
) {
    init {
        string("title") { titleArg ->
            executeSafe {
                val item = mc.player.inventory.getCurrentItem()

                if (item.item is ItemWritableBook) {
                    val title = titleArg.value
                        .replace("&", 0x00A7.toString())
                        .replace("#n", "\n")
                        .replace("null", "")
                        .max(31)

                    val pages = NBTTagList()
                    val bookData = item.tagCompound // have to save this
                    pages.appendTag(NBTTagString(""))

                    if (item.hasTagCompound()) {
                        bookData?.let { item.tagCompound = it }
                        item.tagCompound!!.setTag("title", NBTTagString(title))
                        item.tagCompound!!.setTag("author", NBTTagString(Wrapper.player!!.name))
                    } else {
                        item.setTagInfo("pages", pages)
                        item.setTagInfo("title", NBTTagString(title))
                        item.setTagInfo("author", NBTTagString(Wrapper.player!!.name))
                    }

                    val buf = PacketBuffer(Unpooled.buffer())
                    buf.writeItemStack(item)

                    Wrapper.player!!.connection.sendPacket(CPacketCustomPayload("MC|BSign", buf))
                    sendChatMessage("Signed book with title: [&7$title&f]")
                } else {
                    MessageSendHelper.sendErrorMessage("You're not holding a writable book!")
                }
            }
        }
    }
}