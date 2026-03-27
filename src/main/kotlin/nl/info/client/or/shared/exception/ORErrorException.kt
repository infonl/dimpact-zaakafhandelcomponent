/*
 * SPDX-FileCopyrightText: 2021 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.or.shared.exception

import nl.info.client.or.shared.model.ORError

class ORErrorException(val orError: ORError) : RuntimeException() {
    override val message: String
        get() = "${orError.title} [${orError.status} ${orError.code}] ${orError.detail} (${orError.instance})"
}
