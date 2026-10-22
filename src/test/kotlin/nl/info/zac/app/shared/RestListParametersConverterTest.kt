/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.shared

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.zac.shared.model.ListParameters
import nl.info.zac.shared.model.Paging
import nl.info.zac.shared.model.SorteerRichting
import nl.info.zac.shared.model.Sorting

class RestListParametersConverterTest : BehaviorSpec({
    context("Applying the common REST list parameters") {
        given("REST list parameters with a sort field, order, page and maximum number of results") {
            val restListParameters = RestListParameters().apply {
                sort = "fakeSortField"
                order = "desc"
                page = 2
                maxResults = 25
            }

            `when`("they are applied to list parameters") {
                val listParameters = restListParameters.applyCommonParametersTo(ListParameters())

                then("the sorting and paging are set") {
                    listParameters.sorting shouldBe Sorting("fakeSortField", SorteerRichting.DESCENDING)
                    listParameters.paging shouldBe Paging(2, 25)
                }
            }
        }

        given("REST list parameters with a blank sort field") {
            val restListParameters = RestListParameters().apply {
                sort = " "
                page = 1
                maxResults = 10
            }

            `when`("they are applied to list parameters") {
                val listParameters = restListParameters.applyCommonParametersTo(ListParameters())

                then("no sorting is set, but the paging is") {
                    listParameters.sorting shouldBe null
                    listParameters.paging shouldBe Paging(1, 10)
                }
            }
        }
    }
})
