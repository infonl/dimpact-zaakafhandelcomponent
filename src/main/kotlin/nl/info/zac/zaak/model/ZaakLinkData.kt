/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
@file:Suppress("TooManyFunctions")

package nl.info.zac.zaak.model

import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.zrc.model.generated.Zaak
import nl.info.client.zgw.zrc.util.isDeelzaak
import nl.info.client.zgw.zrc.util.isHoofdzaak
import nl.info.client.zgw.zrc.util.isLinkedTo
import nl.info.client.zgw.zrc.util.isOpen
import nl.info.client.zgw.ztc.model.generated.ZaakType
import nl.info.zac.app.zaak.model.RelatieType
import nl.info.zac.policy.output.ZaakRechten
import java.util.UUID

data class ZaakLinkData(
    val isOpen: Boolean,
    val isHoofdzaak: Boolean,
    val isDeelzaak: Boolean,
    val zaaktypeUUID: UUID,
    val canLezen: Boolean,
    val canKoppelen: Boolean
)

fun Zaak.toZaakLinkData(rechten: ZaakRechten) = ZaakLinkData(
    isOpen = this.isOpen(),
    isHoofdzaak = this.isHoofdzaak(),
    isDeelzaak = this.isDeelzaak(),
    zaaktypeUUID = this.zaaktype.extractUuid(),
    canLezen = rechten.canLezen,
    canKoppelen = rechten.canKoppelen
)

fun ZaakType.getDeelzaaktypenSet() = deelzaaktypen
    .orEmpty()
    .map { it.extractUuid() }
    .toSet()

fun Zaak.alreadyGerelateerdReason(foundZaakUuid: UUID) =
    ZaakNotLinkableReason.ALREADY_GERELATEERD.takeIf { isLinkedTo(foundZaakUuid) }

fun ZaakLinkData.canBeRelatedTo(to: ZaakLinkData): Boolean =
    to.canLezen && gerelateerdNotLinkableReason() == null

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
    isOpen && !foundZaak.isOpen -> ZaakNotLinkableReason.AFGEHANDELD
    !isOpen && foundZaak.isOpen -> ZaakNotLinkableReason.OPEN
    else -> null
}

fun ZaakLinkData.hoofdzaakDeelzaakNotLinkableReason(
    deelzaak: ZaakLinkData,
    allowedDeelzaaktypes: Set<UUID>
): ZaakNotLinkableReason? = when {
    isDeelzaak -> ZaakNotLinkableReason.IS_DEELZAAK
    deelzaak.isDeelzaak -> ZaakNotLinkableReason.ALREADY_DEELZAAK
    deelzaak.isHoofdzaak -> ZaakNotLinkableReason.HAS_DEELZAKEN
    !allowedDeelzaaktypes.contains(deelzaak.zaaktypeUUID) -> ZaakNotLinkableReason.ZAAKTYPE_DOES_NOT_ALLOW_DEELZAAK
    !canKoppelen || !deelzaak.canKoppelen -> ZaakNotLinkableReason.NOT_AUTHORISED_TO_KOPPELEN
    else -> null
}

/**
 * The reason why a new zaak of zaaktype [newZaaktypeUuid], which does not exist yet, cannot be linked to this zaak.
 * [relatieType] is the role of the new zaak, so [RelatieType.DEELZAAK] makes the new zaak a deelzaak of this zaak.
 * Only [RelatieType.GERELATEERD] and [RelatieType.DEELZAAK] can be used; see [canLinkNewZaakAs].
 */
fun ZaakLinkData.newZaakNotLinkableReason(
    relatieType: RelatieType,
    newZaaktypeUuid: UUID,
    allowedDeelzaaktypes: Set<UUID>
): ZaakNotLinkableReason? = when {
    !canLezen || !canKoppelen -> ZaakNotLinkableReason.NOT_AUTHORISED_TO_KOPPELEN
    !isOpen -> ZaakNotLinkableReason.AFGEHANDELD
    relatieType == RelatieType.DEELZAAK && isDeelzaak -> ZaakNotLinkableReason.IS_DEELZAAK
    relatieType == RelatieType.DEELZAAK && newZaaktypeUuid !in allowedDeelzaaktypes ->
        ZaakNotLinkableReason.ZAAKTYPE_DOES_NOT_ALLOW_DEELZAAK
    else -> null
}

fun canLinkNewZaakAs(relatieType: RelatieType) = relatieType == RelatieType.GERELATEERD ||
    relatieType == RelatieType.DEELZAAK

fun ZaakLinkData.gerelateerdNotLinkableReason(): ZaakNotLinkableReason? =
    ZaakNotLinkableReason.NOT_AUTHORISED_TO_KOPPELEN.takeIf { !canKoppelen }

private fun allowGerelateerd(from: ZaakLinkData, to: ZaakLinkData) =
    from.canKoppelen && to.canLezen

private fun allowHoofdAndDeelzaak(hoofdzaak: ZaakLinkData, deelzaak: ZaakLinkData) =
    hoofdzaak.canKoppelen && deelzaak.canKoppelen && hoofdzaak.isOpen == deelzaak.isOpen
