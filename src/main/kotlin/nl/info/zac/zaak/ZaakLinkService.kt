/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.zaak

import nl.info.zac.zaak.model.ZaakLinkData
import java.util.UUID

private fun allowGerelateerd(from: ZaakLinkData, to: ZaakLinkData) =
    from.koppelen && to.lezen

private fun allowHoofdAndDeelzaak(hoofdzaak: ZaakLinkData, deelzaak: ZaakLinkData) =
    hoofdzaak.koppelen && deelzaak.koppelen && hoofdzaak.isOpen == deelzaak.isOpen

// a hoofdzaak CANNOT be a deelzaak, no tree-like hierarchy is allowed
private fun canBeHoofdzaak(zaak: ZaakLinkData): Boolean = !zaak.isDeelzaak

// a deelzaak CANNOT be a deelzaak to multiple hoofdzaken, and CANNOT be a hoofdzaak itself
private fun canBeNewDeelzaak(zaak: ZaakLinkData): Boolean = !zaak.isDeelzaak && !zaak.isHoofdzaak

fun canBeRelated(from: ZaakLinkData, to: ZaakLinkData): Boolean =
    allowGerelateerd(from, to)

fun canBeHoofdAndDeelzaak(
    hoofdzaak: ZaakLinkData,
    deelzaak: ZaakLinkData,
    allowedDeelzaaktypes: Set<UUID>
): Boolean =
    allowHoofdAndDeelzaak(hoofdzaak, deelzaak) &&
    canBeHoofdzaak(hoofdzaak) &&
    canBeNewDeelzaak(deelzaak) &&
    allowedDeelzaaktypes.contains(deelzaak.zaaktypeUUID)

fun hoofdAndDeelzaakCanBeOntkoppeld(hoofdzaak: ZaakLinkData, deelzaak: ZaakLinkData) =
    allowHoofdAndDeelzaak(hoofdzaak, deelzaak)

fun relatedZakenCanBeOntkoppeld(from: ZaakLinkData, to: ZaakLinkData) =
    allowGerelateerd(from, to)
