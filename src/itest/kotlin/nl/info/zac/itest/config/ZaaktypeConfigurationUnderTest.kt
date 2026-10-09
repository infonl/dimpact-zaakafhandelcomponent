/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest.config

import java.util.UUID

data class ZaaktypeConfigurationUnderTest(
    val configurationType: ZaaktypeConfigurationType,
    val zaaktypeUuid: UUID,
    val secondVersionZaaktypeUuid: UUID,
    val readConfiguration: () -> String,
    val storeConfiguration: (String) -> Unit,
    val nietOntvankelijkResultaattypeUuid: String,
    val nietOntvankelijkResultaattypeOmschrijving: String,
    val zaakbeeindigResultaattypeUuid: String,
    val secondVersionNietOntvankelijkResultaattypeUuid: String,
    val secondVersionZaakbeeindigResultaattypeUuid: String
)
