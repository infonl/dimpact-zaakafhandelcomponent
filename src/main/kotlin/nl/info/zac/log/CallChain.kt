/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.log

import kotlin.reflect.KClass

private val ZAC_PACKAGE_PREFIXES = listOf("nl.info.", "net.atos.")
private const val WELD_PROXY_CLASS_NAME_MARKER = "\$Proxy\$"
private const val CALL_CHAIN_SEPARATOR = " <- "
private const val MAX_CALL_CHAIN_FRAMES = 10L

/**
 * Returns the ZAC methods on the current call stack that led to a call into [calledClass], innermost first.
 * Frames of [calledClass] itself and of Weld proxies are left out.
 * Walking the stack is relatively expensive, so only use this for log messages of rare events.
 */
fun zacCallChain(calledClass: KClass<*>): String {
    val calledClassName = calledClass.java.name
    return StackWalker.getInstance().walk { frames ->
        frames
            .skip(1)
            .filter { frame -> ZAC_PACKAGE_PREFIXES.any { frame.className.startsWith(it) } }
            .filter { frame -> !frame.className.isClassOrInnerClassOf(calledClassName) }
            .filter { frame -> !frame.className.contains(WELD_PROXY_CLASS_NAME_MARKER) }
            .limit(MAX_CALL_CHAIN_FRAMES)
            .map { frame -> "${frame.className.substringAfterLast('.')}.${frame.methodName}:${frame.lineNumber}" }
            .toList()
    }.joinToString(CALL_CHAIN_SEPARATOR)
}

private fun String.isClassOrInnerClassOf(className: String) = this == className || startsWith("$className$")
