/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.klant.model

import java.util.UUID

data class ProductaanvraagSpecificContactDetails(
    val klantcontactUuid: UUID?,
    val email: String?,
    val phone: String?
)
