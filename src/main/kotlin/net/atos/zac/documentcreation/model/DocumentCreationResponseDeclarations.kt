/*
 * SPDX-FileCopyrightText: 2022 Atos,
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.documentcreation.model

import java.net.URI

data class DocumentCreationAttendedResponse(
    val redirectUrl: URI? = null,
    val message: String? = null
)

data class DocumentCreationUnattendedResponse(
    val message: String
    // will be extended with additional fields in future
)
