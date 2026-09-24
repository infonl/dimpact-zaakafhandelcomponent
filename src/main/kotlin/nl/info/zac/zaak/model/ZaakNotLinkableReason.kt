/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.zaak.model

enum class ZaakNotLinkableReason {
    AFGEHANDELD,
    OPEN,
    IS_DEELZAAK,
    ALREADY_DEELZAAK,
    HAS_DEELZAKEN,
    ZAAKTYPE_DOES_NOT_ALLOW_DEELZAAK,
    NOT_AUTHORISED_TO_KOPPELEN,
    NOT_AUTHORISED_TO_LEZEN,
    ZAAKTYPE_DOES_NOT_ALLOW_INFORMATIEOBJECTTYPE
}
