/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.zrc.util

import nl.info.client.zgw.zrc.model.generated.Zaak
import org.apache.commons.lang3.StringUtils

const val OMSCHRIJVING_MAX_LENGTH = 80

const val TOELICHTING_MAX_LENGTH = 1000

fun Zaak.isOpen() = archiefnominatie == null

fun Zaak.isOpgeschort() = opschorting != null && opschorting.getIndicatie()

/**
* We use opschorting reden field here as this is an easy way to know if a zaak was suspended in the past.
 * Another approach would be to parse the zaak history/audit log, but that requires a lot of custom logic.
 *
 * A new flag to handle this "suspended in the past" case was requested with:
 * https://github.com/open-zaak/open-zaak/issues/1920
**/
fun Zaak.isEerderOpgeschort() = opschorting?.getReden()?.isNotEmpty() == true

fun Zaak.isVerlengd() = verlenging?.getDuur() != null

fun Zaak.isHoofdzaak() = !deelzaken.isNullOrEmpty()

fun Zaak.isDeelzaak() = hoofdzaak != null
