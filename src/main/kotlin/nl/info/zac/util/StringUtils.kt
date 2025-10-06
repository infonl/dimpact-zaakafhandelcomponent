/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.util

import java.nio.charset.StandardCharsets

fun String.isPureAscii(): Boolean = StandardCharsets.US_ASCII.newEncoder().canEncode(this)
