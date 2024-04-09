package net.atos.zac.app.util

import jakarta.ws.rs.BadRequestException

// we subclass from a JAX-RS exception so that we can use our generic exception handling mechanism
class InputValidationFailedException(validationErrors: String) :
    BadRequestException("Validation failed, causes: $validationErrors")
