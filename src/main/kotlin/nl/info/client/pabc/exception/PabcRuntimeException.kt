/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.client.pabc.exception

class PabcRuntimeException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)
