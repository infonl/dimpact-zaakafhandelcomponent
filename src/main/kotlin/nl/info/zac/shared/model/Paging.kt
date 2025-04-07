/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.shared.model

data class Paging(val page: Int, val maxResults: Int) {
    fun getFirstResult() = page * maxResults
}
