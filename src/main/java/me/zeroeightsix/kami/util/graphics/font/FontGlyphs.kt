package me.zeroeightsix.kami.util.graphics.font

import me.zeroeightsix.kami.KamiMod
import me.zeroeightsix.kami.util.Wrapper
import me.zeroeightsix.kami.util.math.MathUtils
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.texture.DynamicTexture
import net.minecraft.client.renderer.texture.TextureUtil
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL12.*
import org.lwjgl.opengl.GL14.*
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.nio.ByteBuffer
import kotlin.math.max
import kotlin.math.min

/**
 * Adapted from Bobjob's edited version of Slick's TrueTypeFont.
 * http://forum.lwjgl.org/index.php?topic=2951
 *
 * License: http://slick.ninjacave.com/license/
 *
 * A TrueType font implementation originally for Slick, edited for Bobjob's Engine
 *
 * @original author James Chambers (Jimmy)
 * @original author Jeremy Adams (elias4444)
 * @original author Kevin Glass (kevglass)
 * @original author Peter Korzuszek (genail)
 *
 * @new version edited by David Aaron Muhar (bobjob)
 */
class FontGlyphs(val style: TextProperties.Style, private val font: Font, private val fallbackFont: Font) {

    /** HashMap for storing all the glyph chunks, each chunk contains 256 glyphs mapping to characters */
    private val chunkMap = HashMap<Int, GlyphChunk>()

    /** Font height */
    val fontHeight: Int

    init {
        // Loads the basic 256 characters on init
        fontHeight = loadGlyphChunk(0)?.let { glyphChunk ->
            chunkMap[0] = glyphChunk
            glyphChunk.charInfoArray.maxBy { it.height }?.height ?: 32
        } ?: 32
    }

    /** @return CharInfo of [char] */
    fun getCharInfo(char: Char): CharInfo {
        val charInt = char.toInt()
        val chunk = charInt shr 8
        val chunkStart = chunk shl 8
        return getChunk(chunk).charInfoArray[charInt - chunkStart]
    }

    /** @return the chunk the [char] is in */
    fun getChunk(char: Char) = getChunk(char.toInt() shr 8)

    /** @return the chunk */
    fun getChunk(chunk: Int): GlyphChunk = chunkMap.getOrPut(chunk) {
        // Try to load the glyph chunk if absent
        // Return an empty char if failed to load glyph
        // And let it fix itself next time
        loadGlyphChunk(chunk) ?: return chunkMap[0]!!
    }

    private fun loadGlyphChunk(chunk: Int): GlyphChunk? {
        return try {
            val chunkStart = chunk shl 8
            val bufferedImage = BufferedImage(TEXTURE_WIDTH, MAX_TEXTURE_HEIGHT, BufferedImage.TYPE_INT_ARGB)
            val graphics2D = bufferedImage.graphics as Graphics2D
            graphics2D.background = Color(0, 0, 0, 0)

            var rowHeight = 0
            var positionX = 1
            var positionY = 1

            val charInfoArray = Array(256) {
                val char = (chunkStart + it).toChar() // Plus 1 because char starts at 1 not 0
                val charImage = getCharImage(char) // Get image for this character

                if (positionX + charImage.width >= TEXTURE_WIDTH) { // Move to the next line if we reach the texture width
                    positionX = 1
                    positionY += rowHeight
                    rowHeight = 0
                }

                val charInfo = CharInfo(positionX, positionY, charImage.width, charImage.height) // Create char info for this character
                rowHeight = max(charImage.height, rowHeight) // Update row height
                graphics2D.drawImage(charImage, positionX, positionY, null) // Draw it here

                positionX += charImage.width + 2 // Move right for next character
                charInfo // Return the char info to lambda
            }

            val textureHeight = min(MathUtils.ceilToPOT(positionY + rowHeight), MAX_TEXTURE_HEIGHT)
            val textureImage = BufferedImage(TEXTURE_WIDTH, textureHeight, BufferedImage.TYPE_INT_ARGB)
            (textureImage.graphics as Graphics2D).drawImage(bufferedImage, 0, 0, null)

            val textureID = createTexture(textureImage) ?: throw Exception()
            GlyphChunk(chunk, textureID, textureHeight, charInfoArray)
        } catch (e: Exception) {
            KamiMod.log.error("Failed to load glyph chunk $chunk.")
            e.printStackTrace()
            null
        }
    }

    private fun getCharImage(char: Char): BufferedImage {
        val font = when {
            font.canDisplay(char.toInt()) -> font
            fallbackFont.canDisplay(char.toInt()) -> fallbackFont
            else -> font
        }

        // Create a temporary image to extract the character's size
        val tempGraphics2D = BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).graphics as Graphics2D
        tempGraphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        tempGraphics2D.font = font
        val fontMetrics = tempGraphics2D.fontMetrics
        tempGraphics2D.dispose()

        val charWidth = if (fontMetrics.charWidth(char) > 0) fontMetrics.charWidth(char) else 8
        val charHeight = if (fontMetrics.height > 0) fontMetrics.height else font.size

        // Create another image holding the character we are creating
        val charImage = BufferedImage(charWidth, charHeight, BufferedImage.TYPE_INT_ARGB)
        val graphics2D = charImage.graphics as Graphics2D

        graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        graphics2D.font = font
        graphics2D.color = Color.WHITE
        graphics2D.drawString(char.toString(), 0, fontMetrics.ascent)
        return charImage
    }

    private fun createTexture(bufferedImage: BufferedImage): Int? {
        return try {
            val dynamicTexture = DynamicTexture(bufferedImage)
            dynamicTexture.loadTexture(Wrapper.minecraft.getResourceManager())
            val textureId = dynamicTexture.glTextureId

            // Tells Gl that our texture isn't a repeating texture (edges are not connecting to each others)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)

            // Setup texture filters
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
            glHint(GL_GENERATE_MIPMAP_HINT, GL_NICEST)

            // Setup mipmap parameters
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_LOD, 0)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAX_LOD, 3)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_BASE_LEVEL, 0)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAX_LEVEL, 3)
            glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_LOD_BIAS, 0f)
            GlStateManager.bindTexture(textureId)

            // We only need 3 levels of mipmaps for 32 sized font
            // 0: 32 x 32, 1: 16 x 16, 2: 8 x 8, 3: 4 x 4
            for (mipmapLevel in 0..3) {
                // GL_ALPHA means that the texture is a grayscale texture (black & white and alpha only)
                glTexImage2D(GL_TEXTURE_2D, mipmapLevel, GL_ALPHA, bufferedImage.width shr mipmapLevel, bufferedImage.height shr mipmapLevel, 0, GL_ALPHA, GL_UNSIGNED_BYTE, null as ByteBuffer?)
            }

            glTexParameteri(GL_TEXTURE_2D, GL_GENERATE_MIPMAP, 1)
            TextureUtil.uploadTextureImageSub(textureId, bufferedImage, 0, 0, true, true)
            textureId
        } catch (e: Exception) {
            KamiMod.log.error("Failed to create font texture.")
            e.printStackTrace()
            null
        }
    }

    data class CharInfo(
            /** Character's stored x position  */
            val posX: Int,

            /** Character's stored y position  */
            val posY: Int,

            /** Character's width  */
            val width: Int,

            /** Character's height  */
            val height: Int
    )

    data class GlyphChunk(
            /** Id of this chunk */
            val chunk: Int,

            /** Texture id of the chunk texture */
            val textureId: Int,

            /** Texture height of this chunk texture */
            val textureHeight: Int,

            /** Array for all characters' info in this chunk */
            val charInfoArray: Array<CharInfo>
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as GlyphChunk

            if (chunk != other.chunk) return false
            if (textureId != other.textureId) return false
            if (!charInfoArray.contentEquals(other.charInfoArray)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = chunk
            result = 31 * result + textureId
            result = 31 * result + charInfoArray.contentHashCode()
            return result
        }
    }

    companion object {
        /** Default font texture width */
        const val TEXTURE_WIDTH = 1024

        /** Max font texture height */
        const val MAX_TEXTURE_HEIGHT = 1024
    }
}