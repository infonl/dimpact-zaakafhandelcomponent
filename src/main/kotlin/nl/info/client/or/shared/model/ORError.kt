/*
 * SPDX-FileCopyrightText: 2021 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.or.shared.model

import java.net.URI

open class ORError {
    var code: String? = null
    var title: String? = null
    var status: Int = 0
    var detail: String? = null
    var instance: URI? = null
}
