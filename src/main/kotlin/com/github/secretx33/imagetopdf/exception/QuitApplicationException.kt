package com.github.secretx33.imagetopdf.exception

class QuitApplicationException(
    errorMessage: String? = null,
    val exitCode: Int = 0,
) : Throwable(message = errorMessage)
