/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.zrc.util

import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.zrc.model.generated.Zaak
import java.util.UUID

fun Zaak.isOpen() = archiefnominatie == null

fun Zaak.isOpgeschort() = opschorting != null && opschorting.getIndicatie()

fun Zaak.isEerderOpgeschort() = opschorting?.eerdereOpschorting == true

fun Zaak.isVerlengd() = verlenging?.getDuur() != null

fun Zaak.isHoofdzaak() = !deelzaken.isNullOrEmpty()

fun Zaak.isDeelzaak() = hoofdzaak != null

fun Zaak.isLinkedTo(otherZaakUuid: UUID) =
    otherZaakUuid == hoofdzaak?.extractUuid() ||
        deelzaken.orEmpty().any { it.extractUuid() == otherZaakUuid } ||
        gerelateerdeZaken.orEmpty().any { it.url?.extractUuid() == otherZaakUuid }
