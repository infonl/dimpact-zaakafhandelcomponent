/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.flowable.bpmn

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.test.org.flowable.engine.repository.createProcessDefinition

class ProcessDefinitionExtensionsTest : BehaviorSpec({
    context("Reading the name or key of a process definition") {
        given("a process definition with a name") {
            val processDefinition = createProcessDefinition(name = "fakeName", key = "fakeKey")

            `when`("the name or key is read") {
                val nameOrKey = processDefinition.nameOrKey()

                then("the name is returned") {
                    nameOrKey shouldBe "fakeName"
                }
            }
        }

        given("a process definition deployed before the upload check, without a name") {
            val processDefinition = createProcessDefinition(name = null, key = "fakeKey")

            `when`("the name or key is read") {
                val nameOrKey = processDefinition.nameOrKey()

                then("the key is returned") {
                    nameOrKey shouldBe "fakeKey"
                }
            }
        }

        given("a process definition deployed before the upload check, with a blank name") {
            val processDefinition = createProcessDefinition(name = " ", key = "fakeKey")

            `when`("the name or key is read") {
                val nameOrKey = processDefinition.nameOrKey()

                then("the key is returned instead of an empty label") {
                    nameOrKey shouldBe "fakeKey"
                }
            }
        }
    }
})
