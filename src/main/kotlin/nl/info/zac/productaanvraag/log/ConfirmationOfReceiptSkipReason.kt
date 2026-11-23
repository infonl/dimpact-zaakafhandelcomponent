/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.productaanvraag.log

enum class ConfirmationOfReceiptSkipReason {
    NO_EMAIL_ADDRESS,
    NO_SENDER_CONFIGURED,
    TEMPLATE_NOT_FOUND,
    NO_TEMPLATE_CONFIGURED
}
