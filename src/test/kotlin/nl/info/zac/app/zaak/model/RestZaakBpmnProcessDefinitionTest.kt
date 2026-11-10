/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.zaak.model

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.test.org.flowable.engine.repository.createProcessDefinition

class RestZaakBpmnProcessDefinitionTest : BehaviorSpec({
    context("Converting a BPMN process definition to a REST zaak BPMN process definition") {
        given("a process definition with a name") {
            val processDefinition = createProcessDefinition(
                name = "fakeProcessDefinitionName",
                key = "fakeProcessDefinitionKey",
                version = 2
            )

            `when`("it is converted") {
                val restZaakBpmnProcessDefinition = processDefinition.toRestZaakBpmnProcessDefinition()

                then("the key, name and version of the process definition are returned") {
                    restZaakBpmnProcessDefinition shouldBe RestZaakBpmnProcessDefinition(
                        processDefinitionKey = "fakeProcessDefinitionKey",
                        processDefinitionName = "fakeProcessDefinitionName",
                        processDefinitionVersion = 2
                    )
                }
            }
        }

        given("a process definition without a name, deployed before the upload check existed") {
            val processDefinition = createProcessDefinition(name = null, key = "fakeProcessDefinitionKey")

            `when`("it is converted") {
                val restZaakBpmnProcessDefinition = processDefinition.toRestZaakBpmnProcessDefinition()

                then("the key of the process definition is returned as its name, so that a zaak on it can be opened") {
                    restZaakBpmnProcessDefinition.processDefinitionName shouldBe "fakeProcessDefinitionKey"
                }
            }
        }
    }
})
