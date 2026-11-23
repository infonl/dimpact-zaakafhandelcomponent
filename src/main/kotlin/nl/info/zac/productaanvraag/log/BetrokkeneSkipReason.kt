/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.productaanvraag.log

enum class BetrokkeneSkipReason {
    BRP_NOT_ENABLED,
    KVK_NOT_ENABLED,
    NO_BSN_OR_KVK_NUMBER,
    ROLE_NOT_SUPPORTED,
    MULTIPLE_INITIATORS,
    INVALID_KVK_NUMBER
}
