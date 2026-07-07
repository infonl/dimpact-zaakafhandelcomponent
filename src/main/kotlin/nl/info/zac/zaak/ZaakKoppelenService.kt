/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.zaak

import nl.info.zac.zaak.model.ZaakKoppelenData
import java.util.UUID

private fun allowGerelateerd(from: ZaakKoppelenData, to: ZaakKoppelenData) =
    from.koppelen && to.lezen

private fun allowHoofdAndDeelzaak(hoofdzaak: ZaakKoppelenData, deelzaak: ZaakKoppelenData) =
    hoofdzaak.koppelen && deelzaak.koppelen && hoofdzaak.isOpen == deelzaak.isOpen

// a hoofdzaak CANNOT be a deelzaak, no tree-like hierarchy is allowed
private fun canBeHoofdzaak(zaak: ZaakKoppelenData): Boolean = !zaak.isDeelzaak

// a deelzaak CANNOT be a deelzaak to multiple hoofdzaken, and CANNOT be a hoofdzaak itself
private fun canBeNewDeelzaak(zaak: ZaakKoppelenData): Boolean = !zaak.isDeelzaak && !zaak.isHoofdzaak

fun canBeRelated(from: ZaakKoppelenData, to: ZaakKoppelenData): Boolean =
    allowGerelateerd(from, to)

fun canBeHoofdAndDeelzaak(
    hoofdzaak: ZaakKoppelenData,
    deelzaak: ZaakKoppelenData,
    allowedDeelzaaktypes: Set<UUID>
): Boolean =
    allowHoofdAndDeelzaak(hoofdzaak, deelzaak) &&
    canBeHoofdzaak(hoofdzaak) &&
    canBeNewDeelzaak(deelzaak) &&
    allowedDeelzaaktypes.contains(deelzaak.zaaktypeUUID)

fun hoofdAndDeelzaakCanBeOntkoppeld(hoofdzaak: ZaakKoppelenData, deelzaak: ZaakKoppelenData) =
    allowHoofdAndDeelzaak(hoofdzaak, deelzaak)

fun relatedZakenCanBeOntkoppeld(from: ZaakKoppelenData, to: ZaakKoppelenData) =
    allowGerelateerd(from, to)
