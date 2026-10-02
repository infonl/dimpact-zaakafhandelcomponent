/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.zaak.model

import java.util.UUID

@Suppress("LongParameterList")
fun createZaakLinkData(
    isOpen: Boolean = true,
    isHoofdzaak: Boolean = false,
    isDeelzaak: Boolean = false,
    zaaktypeUUID: UUID = UUID.randomUUID(),
    lezen: Boolean = true,
    koppelen: Boolean = true
) = ZaakLinkData(
    isOpen = isOpen,
    isHoofdzaak = isHoofdzaak,
    isDeelzaak = isDeelzaak,
    zaaktypeUUID = zaaktypeUUID,
    canLezen = lezen,
    canKoppelen = koppelen
)
