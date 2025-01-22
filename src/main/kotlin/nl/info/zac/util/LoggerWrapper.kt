package nl.info.zac.util

import java.util.logging.Level
import java.util.logging.Logger

/**
 * Simple wapper function around the Java Util Logger class to make it easier to unit test classes that use logging.
 */
fun log(logger: Logger, level: Level, message: String, throwable: Throwable) {
    logger.log(level, message, throwable)
}
