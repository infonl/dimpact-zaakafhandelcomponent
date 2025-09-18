/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.admin.model

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.zac.admin.model.createHumanTaskReferentieTabel

class HumanTaskReferentieTabelTest : BehaviorSpec({

    Given("Two equal objects") {
        val humanTaskReferentieTabel1 = createHumanTaskReferentieTabel()
        val humanTaskReferentieTabel2 = createHumanTaskReferentieTabel()

        When("The values of the two objects are compared") {
            val equalityResult = humanTaskReferentieTabel1 == humanTaskReferentieTabel2
            Then("The objects should be considered equal") {
                equalityResult shouldBe true
            }
        }
    }
})
