/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.zaak.model

enum class ZaakNotLinkableReason {
    ALREADY_GERELATEERD,
    AFGEHANDELD,
    OPEN,
    IS_DEELZAAK,
    ALREADY_DEELZAAK,
    HAS_DEELZAKEN,
    ZAAKTYPE_DOES_NOT_ALLOW_DEELZAAK,
    NOT_AUTHORISED_TO_KOPPELEN,
    ZAAKTYPE_DOES_NOT_ALLOW_INFORMATIEOBJECTTYPE
}
