/*
 * SPDX-FileCopyrightText: 2021 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.shared.exception

import nl.info.client.zgw.shared.model.ZgwError

/**
 * Exception thrown when an error occurred in the ZGW APIs.
 */
class ZgwErrorException(val zgwError: ZgwError) : RuntimeException() {

    override val message: String
        get() = "%s [%d %s] %s (%s %s)".format(
            zgwError.title,
            zgwError.status,
            zgwError.code,
            zgwError.detail,
            zgwError.type,
            zgwError.instance
        )
}
