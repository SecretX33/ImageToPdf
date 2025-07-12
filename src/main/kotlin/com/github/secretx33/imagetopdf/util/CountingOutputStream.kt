package com.github.secretx33.imagetopdf.util

import java.io.OutputStream

class CountingOutputStream : OutputStream() {
    private var count = 0L

    val byteCount: Long get() = count

    override fun write(b: Int) {
        count++
    }

    override fun write(b: ByteArray) {
        count += b.size
    }

    override fun write(b: ByteArray, off: Int, len: Int) {
        count += len
    }

    fun reset() {
        count = 0L
    }
}