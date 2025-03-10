/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.brp.exception

class BrpRuntimeException(message: String, cause: Throwable?) : RuntimeException(message, cause) {
    constructor(message: String) : this(message, null)
}
