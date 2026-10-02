/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.admin.model

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import nl.info.zac.admin.model.createHumanTaskParameters
import nl.info.zac.admin.model.createHumanTaskReferentieTabel

class HumanTaskParametersTest : BehaviorSpec({

    given("Two HumanTaskParameters objects with the same values") {
        val humanTaskParameters1 = createHumanTaskParameters(referenceTables = listOf(createHumanTaskReferentieTabel()))
        val humanTaskParameters2 = createHumanTaskParameters(referenceTables = listOf(createHumanTaskReferentieTabel()))

        `when`("The values of the two objects are compared") {
            val isEqual = humanTaskParameters1 == humanTaskParameters2

            then("The objects should be considered equal") {
                isEqual shouldBe true
            }
        }
    }

    given("Two HumanTaskParameters objects with the different values") {
        val humanTaskParameters1 = createHumanTaskParameters(referenceTables = listOf(createHumanTaskReferentieTabel()))
        val humanTaskParameters2 = createHumanTaskParameters(
            referenceTables = listOf(createHumanTaskReferentieTabel())
        ).apply {
            val tabel = getReferentieTabellen()[0].tabel.shouldNotBeNull()
            tabel.values[0].name = "different name"
        }

        `when`("The values of the two objects are compared") {
            val isEqual = humanTaskParameters1 == humanTaskParameters2

            then("The objects should be considered different") {
                isEqual shouldBe false
            }
        }
    }
})
