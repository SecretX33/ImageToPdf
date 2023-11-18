package com.github.secretx33.imagetopdf.model

import com.github.secretx33.imagetopdf.convert.ORIENTATION_MIRROR_HORIZONTAL
import com.github.secretx33.imagetopdf.convert.ORIENTATION_MIRROR_HORIZONTAL_AND_ROTATE_270
import com.github.secretx33.imagetopdf.convert.ORIENTATION_MIRROR_HORIZONTAL_AND_ROTATE_90
import com.github.secretx33.imagetopdf.convert.ORIENTATION_MIRROR_VERTICAL

enum class ImageMirroring {
    NONE,
    HORIZONTAL,
    VERTICAL;

    companion object {
        fun from(mirroring: Int): ImageMirroring = when (mirroring) {
            ORIENTATION_MIRROR_VERTICAL -> VERTICAL
            ORIENTATION_MIRROR_HORIZONTAL,
            ORIENTATION_MIRROR_HORIZONTAL_AND_ROTATE_90,
            ORIENTATION_MIRROR_HORIZONTAL_AND_ROTATE_270 -> HORIZONTAL
            else -> NONE
        }
    }
}