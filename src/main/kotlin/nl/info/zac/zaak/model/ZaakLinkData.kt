/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.zaak.model

import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.zrc.model.generated.Zaak
import nl.info.client.zgw.zrc.util.isDeelzaak
import nl.info.client.zgw.zrc.util.isHoofdzaak
import nl.info.client.zgw.zrc.util.isOpen
import nl.info.zac.policy.output.ZaakRechten
import java.util.UUID

data class ZaakLinkData(
    val isOpen: Boolean,
    val isHoofdzaak: Boolean,
    val isDeelzaak: Boolean,
    val zaaktypeUUID: UUID,
    val lezen: Boolean,
    val koppelen: Boolean
)

fun Zaak.toZaakLinkData(rechten: ZaakRechten) = ZaakLinkData(
    isOpen = this.isOpen(),
    isHoofdzaak = this.isHoofdzaak(),
    isDeelzaak = this.isDeelzaak(),
    zaaktypeUUID = this.zaaktype.extractUuid(),
    lezen = rechten.lezen,
    koppelen = rechten.koppelen
)

private fun allowGerelateerd(from: ZaakLinkData, to: ZaakLinkData) =
    from.koppelen && to.lezen

private fun allowHoofdAndDeelzaak(hoofdzaak: ZaakLinkData, deelzaak: ZaakLinkData) =
    hoofdzaak.koppelen && deelzaak.koppelen && hoofdzaak.isOpen == deelzaak.isOpen

// a hoofdzaak CANNOT be a deelzaak, no tree-like hierarchy is allowed
private fun canBeHoofdzaak(zaak: ZaakLinkData): Boolean = !zaak.isDeelzaak

// a deelzaak CANNOT be a deelzaak to multiple hoofdzaken, and CANNOT be a hoofdzaak itself
private fun canBeNewDeelzaak(zaak: ZaakLinkData): Boolean = !zaak.isDeelzaak && !zaak.isHoofdzaak

fun ZaakLinkData.canBeRelatedTo(to: ZaakLinkData): Boolean =
    allowGerelateerd(this, to)

fun ZaakLinkData.canBeHoofdzaakFor(
    deelzaak: ZaakLinkData,
    allowedDeelzaaktypes: Set<UUID>
): Boolean =
    allowHoofdAndDeelzaak(this, deelzaak) &&
            canBeHoofdzaak(this) &&
            canBeNewDeelzaak(deelzaak) &&
            allowedDeelzaaktypes.contains(deelzaak.zaaktypeUUID)

fun ZaakLinkData.canBeUnlinkedFromDeelzaak(deelzaak: ZaakLinkData) =
    allowHoofdAndDeelzaak(this, deelzaak)

fun ZaakLinkData.canBeUnlinkedFromRelatedZaak(to: ZaakLinkData) =
    allowGerelateerd(this, to)
