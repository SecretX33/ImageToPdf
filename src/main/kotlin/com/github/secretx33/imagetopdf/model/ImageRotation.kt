package com.github.secretx33.imagetopdf.model

import com.github.secretx33.imagetopdf.convert.ORIENTATION_MIRROR_HORIZONTAL_AND_ROTATE_270
import com.github.secretx33.imagetopdf.convert.ORIENTATION_MIRROR_HORIZONTAL_AND_ROTATE_90
import com.github.secretx33.imagetopdf.convert.ORIENTATION_ROTATE_180
import com.github.secretx33.imagetopdf.convert.ORIENTATION_ROTATE_270
import com.github.secretx33.imagetopdf.convert.ORIENTATION_ROTATE_90

enum class ImageRotation(val degrees: Double) {
    NONE(0.0),
    ROTATE_90(90.0),
    ROTATE_180(180.0),
    ROTATE_270(270.0);

    companion object {
        fun from(orientation: Int): ImageRotation = when (orientation) {
            ORIENTATION_ROTATE_90, ORIENTATION_MIRROR_HORIZONTAL_AND_ROTATE_90 -> ROTATE_90
            ORIENTATION_ROTATE_180 -> ROTATE_180
            ORIENTATION_ROTATE_270, ORIENTATION_MIRROR_HORIZONTAL_AND_ROTATE_270  -> ROTATE_270
            else -> NONE
        }
    }
}