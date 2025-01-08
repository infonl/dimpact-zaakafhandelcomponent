/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.itest.util

const val ONE_SECOND_IN_MILLIS = 1000L

fun sleep(seconds: Long) {
    Thread.sleep(seconds * ONE_SECOND_IN_MILLIS)
}
