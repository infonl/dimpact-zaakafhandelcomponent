/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.klant.model

import java.util.UUID

data class KlantcontactContactDetails(
    val klantcontactUuid: UUID,
    val contactDetails: ContactDetails
)
