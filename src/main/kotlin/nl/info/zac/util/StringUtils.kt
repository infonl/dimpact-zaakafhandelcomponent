package nl.info.zac.util

import java.nio.charset.StandardCharsets

fun String.isPureAscii(): Boolean = StandardCharsets.US_ASCII.newEncoder().canEncode(this)
