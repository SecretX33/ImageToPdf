package com.github.secretx33.imagetopdf.exception

class QuitApplicationException(
    errorMessage: String? = null,
    val exitCode: Int,
) : Throwable(message = errorMessage)
