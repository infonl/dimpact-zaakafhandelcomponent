/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.zrc.util

import nl.info.client.zgw.zrc.model.generated.Zaak
import org.apache.commons.lang3.StringUtils

fun Zaak.isOpen() = archiefnominatie == null

fun Zaak.isOpgeschort() = opschorting != null && opschorting.getIndicatie()

/**
* We use opschorting reden field here as this is an easy way to know if a zaak was suspended in the past.
 * Another approach would be to parse the zaak history/audit log, but that requires a lot of custom logic.
 *
 * A new flag to handle this "suspended in the past" case was requested with:
 * https://github.com/open-zaak/open-zaak/issues/1920
**/
fun Zaak.isEerderOpgeschort() = opschorting != null && StringUtils.isNotEmpty(opschorting.getReden())

fun Zaak.isVerlengd() = verlenging != null && verlenging.getDuur() != null

fun Zaak.isHoofdzaak() = deelzaken.isNotEmpty()

fun Zaak.isDeelzaak() = hoofdzaak != null
