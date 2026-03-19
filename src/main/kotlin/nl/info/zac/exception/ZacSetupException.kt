package nl.info.zac.exception

/**
 * Indicates a problem in the setup of ZAC.
 * This should typically be solved by functional administrators of ZAC.
 * It should not be treated as a server error.
 */
open class ZacSetupException(override val message: String, open val errorCode: ErrorCode) : RuntimeException(message)
