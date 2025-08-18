package nl.info.client.pabc.exception

class PabcRuntimeException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)
