package nl.info.zac.exception

import jakarta.ws.rs.BadRequestException

/**
 * Custom exception for input validation failures.
 * We subclass from the JAX-RS [jakarta.ws.rs.BadRequestException] class so that we can use our generic exception handling mechanism
 * in [net.atos.zac.app.exception.RestExceptionMapper].
 */
open class InputValidationFailedException(
    val errorCode: ErrorCode? = null,
    message: String? = null
) : RuntimeException(message)
