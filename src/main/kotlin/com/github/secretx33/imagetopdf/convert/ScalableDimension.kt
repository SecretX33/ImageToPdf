package com.github.secretx33.imagetopdf.convert

import java.awt.Dimension
import kotlin.math.min

/**
 * Associates a new [Dimension] tuple.
 *
 * @param original The key for this key-value pairing.
 * @param modified The value for this key-value pairing.
 */
data class DimensionTuple(val original: Dimension, val modified: Dimension) {

    /**
     * Returns the ratio of the value width to the key width.
     *
     * @return A unit-less ratio between the value and key widths.
     */
    val widthRatio: Double
        get() = modified.getWidth() / original.getWidth()

    /**
     * Returns the ratio of the value height to the key height.
     *
     * @return A unit-less ratio between the value and key heights.
     */
    val heightRatio: Double
        get() = modified.getHeight() / original.getHeight()

}

/**
 * Delegates construction to the superclass.
 *
 * @param w The dimension's width
 * @param h The dimension's height
 */
class ScalableDimension(
    w: Int,
    h: Int,
    private val scale: Double = 1.0,
) : Dimension(w, h) {

    /**
     * Delegates construction to this class.
     *
     * @param w The width, cast to an integer
     * @param h The height, cast to an integer
     */
    @Suppress("unused")
    constructor(w: Double, h: Double, scale: Double = 1.0) : this(w = w.toInt(), h = h.toInt(), scale = scale)

    fun toScaledDimensionTuple(): DimensionTuple {
        val dimensions = ScalableDimension(width, height, scale)
        return DimensionTuple(dimensions, dimensions.scale())
    }

    /**
     * Scales the given source [Dimension] to the destination [Dimension], maintaining the aspect ratio with
     * respect to the best fit.
     *
     * @return The given source dimensions scaled to the destination dimensions, maintaining the aspect ratio.
     */
    fun scale(): Dimension {
        if (scale == 1.0) return this

        val srcWidth = getWidth()
        val srcHeight = getHeight()

        // Scale both dimensions with respect to the best fit ratio
        return ScalableDimension((srcWidth * scale).toInt(), (srcHeight * scale).toInt())
    }

    /**
     * Scales the given source [Dimension] to the destination
     * [Dimension], maintaining the aspect ratio with respect to
     * the best fit.
     *
     * @param desiredDimensions The desired image dimensions to scale.
     * @return The given source dimensions scaled to the destination dimensions,
     * maintaining the aspect ratio.
     */
    fun scale(desiredDimensions: Dimension): Dimension {
        val srcWidth = getWidth()
        val srcHeight = getHeight()

        // Determine the ratio that will have the best fit
        val ratio = min(desiredDimensions.getWidth() / srcWidth, desiredDimensions.getHeight() / srcHeight)

        // Scale both dimensions with respect to the best fit ratio
        return ScalableDimension((srcWidth * ratio * scale).toInt(), (srcHeight * ratio * scale).toInt())
    }

    override fun toString(): String = "(${getWidth()}, ${getHeight()})"
}
