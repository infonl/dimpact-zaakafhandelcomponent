/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.documentcreation.model

import java.net.URI

data class DocumentCreationAttendedResponse(
    val redirectUrl: URI? = null,
    val message: String? = null
)
