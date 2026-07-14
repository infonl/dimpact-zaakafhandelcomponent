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

    given("a TaskFunctionsDelegate") {
        `when`("getting the prefix") {
            val prefix = delegate.prefix()

            then("it should return 'taken'") {
                prefix shouldBe "taken"
            }
        }

        `when`("getting local names") {
            val names = delegate.localNames()

            then("it should contain 'groep' and 'behandelaar'") {
                names.shouldContainExactlyInAnyOrder("groep", "behandelaar")
            }
        }

        `when`("requesting functionMethod with correct prefix and local names") {
            val groepMethod = delegate.functionMethod("taken", "groep")
            val behandelaarMethod = delegate.functionMethod("taken", "behandelaar")

            then("it should return the correct methods") {
                groepMethod.shouldNotBeNull()
                groepMethod.name shouldBe "groep"
                behandelaarMethod.shouldNotBeNull()
                behandelaarMethod.name shouldBe "behandelaar"
            }
        }

        `when`("requesting functionMethod with incorrect prefix") {
            val method = delegate.functionMethod("wrong", "groep")

            then("it should return null") {
                method.shouldBeNull()
            }
        }

        `when`("requesting functionMethod with unknown local name") {
            val method = delegate.functionMethod("taken", "unknown")

            then("it should return null") {
                method.shouldBeNull()
            }
        }
    }
})
