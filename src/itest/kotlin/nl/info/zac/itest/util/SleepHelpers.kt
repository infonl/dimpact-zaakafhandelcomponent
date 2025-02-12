/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.itest.util

const val ONE_SECOND_IN_MILLIS = 1000L

/**
 * Simple sleep function for integration tests that need to wait for an external service to finish processing.
 *
 * For example, when we need to wait for a status to be set in OpenZaak before we can continue with some tests
 * because OpenZaak does not allow setting multiple statuses for one zaak
 * within the same timeframe of one second.
 * If we do not wait in these cases we get a 400 response from OpenZaak with:
 * "rest_framework.exceptions.ValidationError: {'non_field_errors':
 * [ErrorDetail(string='De velden zaak, datum_status_gezet moeten een unieke set zijn.', code='unique')]}"
 *
 * Related OpenZaak issue: (https://github.com/open-zaak/open-zaak/issues/1639)
 */
fun sleep(seconds: Long) {
    Thread.sleep(seconds * ONE_SECOND_IN_MILLIS)
}
