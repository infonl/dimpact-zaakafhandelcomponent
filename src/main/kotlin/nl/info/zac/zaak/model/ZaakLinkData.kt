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

fun ZaakLinkData.canBeRelatedTo(to: ZaakLinkData): Boolean =
    gerelateerdNotLinkableReason(to) == null

fun ZaakLinkData.canBeHoofdzaakFor(
    deelzaak: ZaakLinkData,
    allowedDeelzaaktypes: Set<UUID>
): Boolean =
    statusNotLinkableReason(deelzaak) == null &&
        hoofdzaakDeelzaakNotLinkableReason(deelzaak, allowedDeelzaaktypes) == null

fun ZaakLinkData.canBeUnlinkedFromDeelzaak(deelzaak: ZaakLinkData) =
    allowHoofdAndDeelzaak(this, deelzaak)

fun ZaakLinkData.canBeUnlinkedFromRelatedZaak(to: ZaakLinkData) =
    allowGerelateerd(this, to)

/**
 * The reason is worded from the perspective of the zaak the user is currently looking at (the receiver)
 * and the zaak they found (the parameter), so it cannot be expressed in terms of hoofdzaak and deelzaak.
 */
fun ZaakLinkData.statusNotLinkableReason(foundZaak: ZaakLinkData): ZaakNotLinkableReason? = when {
    isOpen && !foundZaak.isOpen -> ZaakNotLinkableReason.FOUND_ZAAK_AFGEHANDELD
    !isOpen && foundZaak.isOpen -> ZaakNotLinkableReason.FOUND_ZAAK_OPEN
    else -> null
}

fun ZaakLinkData.hoofdzaakDeelzaakNotLinkableReason(
    deelzaak: ZaakLinkData,
    allowedDeelzaaktypes: Set<UUID>
): ZaakNotLinkableReason? = when {
    isDeelzaak -> ZaakNotLinkableReason.FOUND_ZAAK_IS_DEELZAAK_SO_NO_HOOFDZAAK
    deelzaak.isDeelzaak -> ZaakNotLinkableReason.FOUND_ZAAK_ALREADY_DEELZAAK
    deelzaak.isHoofdzaak -> ZaakNotLinkableReason.FOUND_ZAAK_HAS_DEELZAKEN
    !allowedDeelzaaktypes.contains(deelzaak.zaaktypeUUID) -> ZaakNotLinkableReason.ZAAKTYPE_DOES_NOT_ALLOW_DEELZAAK
    !koppelen || !deelzaak.koppelen -> ZaakNotLinkableReason.NO_KOPPELEN_RIGHT
    else -> null
}

fun ZaakLinkData.gerelateerdNotLinkableReason(to: ZaakLinkData): ZaakNotLinkableReason? = when {
    !koppelen || !to.lezen -> ZaakNotLinkableReason.NO_LEZEN_RIGHT
    else -> null
}

private fun allowGerelateerd(from: ZaakLinkData, to: ZaakLinkData) =
    from.koppelen && to.lezen

private fun allowHoofdAndDeelzaak(hoofdzaak: ZaakLinkData, deelzaak: ZaakLinkData) =
    hoofdzaak.koppelen && deelzaak.koppelen && hoofdzaak.isOpen == deelzaak.isOpen
