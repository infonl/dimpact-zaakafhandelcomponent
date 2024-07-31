/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.documentcreation.model

import java.net.URI

class DocumentCreationResponse {
    val redirectUrl: URI?

    val message: String?

    constructor(redirectUrl: URI) {
        this.redirectUrl = redirectUrl
        message = null
    }

    constructor(message: String) {
        this.message = message
        redirectUrl = null
    }
}
