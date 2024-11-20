/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.shared.model

class Paging(val page: Int, val maxResults: Int) {
    val firstResult: Int
        get() = page * maxResults
}
