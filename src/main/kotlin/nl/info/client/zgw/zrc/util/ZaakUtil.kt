/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.zrc.util

import nl.info.client.zgw.zrc.model.generated.Zaak

fun isOpgeschort(zaak: Zaak) = zaak.getOpschorting() != null && zaak.getOpschorting().getIndicatie()

fun isOpen(zaak: Zaak) = zaak.getArchiefnominatie() == null

fun isVerlengd(zaak: Zaak) = zaak.getVerlenging() != null && zaak.getVerlenging().getDuur() != null
