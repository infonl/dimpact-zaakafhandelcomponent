/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.flowable.bpmn.function

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

class TaskFunctionsDelegateTest : BehaviorSpec({
    val delegate = TaskFunctionsDelegate()

    Given("a TaskFunctionsDelegate") {
        When("getting the prefix") {
            val prefix = delegate.prefix()

            Then("it should return 'taken'") {
                prefix shouldBe "taken"
            }
        }

        When("getting local names") {
            val names = delegate.localNames()

            Then("it should contain 'groep' and 'behandelaar'") {
                names.shouldContainExactlyInAnyOrder("groep", "behandelaar")
            }
        }

        When("requesting functionMethod with correct prefix and local names") {
            val groepMethod = delegate.functionMethod("taken", "groep")
            val behandelaarMethod = delegate.functionMethod("taken", "behandelaar")

            Then("it should return the correct methods") {
                groepMethod.shouldNotBeNull()
                groepMethod.name shouldBe "groep"
                behandelaarMethod.shouldNotBeNull()
                behandelaarMethod.name shouldBe "behandelaar"
            }
        }

        When("requesting functionMethod with incorrect prefix") {
            val method = delegate.functionMethod("wrong", "groep")

            Then("it should return null") {
                method.shouldBeNull()
            }
        }

        When("requesting functionMethod with unknown local name") {
            val method = delegate.functionMethod("taken", "unknown")

            Then("it should return null") {
                method.shouldBeNull()
            }
        }
    }
})
