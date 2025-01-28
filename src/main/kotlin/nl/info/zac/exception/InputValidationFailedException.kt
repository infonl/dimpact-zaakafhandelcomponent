package nl.info.zac.exception

import jakarta.ws.rs.BadRequestException

/**
 * Custom exception for input validation failures.
 * We subclass from the JAX-RS [jakarta.ws.rs.BadRequestException] class so that we can use our generic exception handling mechanism
 */
class InputValidationFailedException(message: String) : BadRequestException(message) {
    constructor(errorCode: ErrorCode) : this(errorCode.value)
}
