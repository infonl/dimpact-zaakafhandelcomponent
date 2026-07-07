/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.zaak

import nl.info.zac.zaak.model.ZaakKoppelenData
import java.util.UUID


private fun allowFromZaak(from: ZaakKoppelenData): Boolean = from.koppelen

private fun allowToZaak(to: ZaakKoppelenData): Boolean = to.lezen

private fun allowHoofdzaak(zaak: ZaakKoppelenData) = zaak.koppelen

private fun allowDeelzaak(zaak: ZaakKoppelenData) = zaak.koppelen

// a hoofdzaak CANNOT be a deelzaak, no tree-like hierarchy is allowed
private fun canBeHoofdzaak(zaak: ZaakKoppelenData): Boolean = !zaak.isDeelzaak

// a deelzaak CANNOT be a deelzaak to multiple hoofdzaken, and CANNOT be a hoofdzaak itself
private fun canBeNewDeelzaak(zaak: ZaakKoppelenData): Boolean = !zaak.isDeelzaak && !zaak.isHoofdzaak

fun canBeRelated(from: ZaakKoppelenData, to: ZaakKoppelenData): Boolean = allowFromZaak(from) && allowToZaak(to)

fun canBeHoofdAndDeelzaak(
    hoofdzaak: ZaakKoppelenData,
    deelzaak: ZaakKoppelenData,
    allowedDeelzaaktypes: Set<UUID>
): Boolean =
    allowHoofdzaak(hoofdzaak) &&
    allowDeelzaak(deelzaak) &&
    canBeHoofdzaak(hoofdzaak) &&
    canBeNewDeelzaak(deelzaak) &&
    hoofdzaak.isOpen == deelzaak.isOpen &&
    allowedDeelzaaktypes.contains(deelzaak.zaaktypeUUID)

fun hoofdAndDeelzaakCanBeOntkoppeld(hoofdzaak: ZaakKoppelenData, deelzaak: ZaakKoppelenData) =
    allowHoofdzaak(hoofdzaak) && allowDeelzaak(deelzaak)

fun relatedZakenCanBeOntkoppeld(from: ZaakKoppelenData, to: ZaakKoppelenData) = allowFromZaak(from) && allowToZaak(to)
