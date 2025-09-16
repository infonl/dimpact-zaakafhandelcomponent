package net.atos.zac.admin.model

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.zac.admin.model.createHumanTaskParameters
import nl.info.zac.admin.model.createHumanTaskReferentieTabel

class HumanTaskParametersTest : BehaviorSpec({

    Given("Two HumanTaskParameters objects with the same values") {
        val humanTaskParameters1 = createHumanTaskParameters(referenceTables = listOf(createHumanTaskReferentieTabel()))
        val humanTaskParameters2 = createHumanTaskParameters(referenceTables = listOf(createHumanTaskReferentieTabel()))

        When("The values of the two objects are compared") {
            val equalityResult = humanTaskParameters1 == humanTaskParameters2

            Then("The objects should be considered equal") {
                equalityResult shouldBe true
            }
        }
    }
})
