/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.admin.model

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import nl.info.zac.admin.model.ZaakbeeindigReden
import nl.info.zac.admin.model.ZaaktypeBpmnConfiguration
import nl.info.zac.admin.model.ZaaktypeCompletionParameters
import java.util.UUID

class ZaaktypeConfigurationTest : BehaviorSpec({
    Context("mapCompletionParameters") {
        Given("A previous zaaktype configuration with completion parameters") {
            val previousConfig = ZaaktypeBpmnConfiguration().apply {
                zaaktypeUuid = UUID.randomUUID()
                zaaktypeOmschrijving = "Previous Type"

                zaaktypeCompletionParameters = mutableSetOf(
                    ZaaktypeCompletionParameters().apply {
                        id = 1L
                        zaakbeeindigReden = ZaakbeeindigReden().apply {
                            id = 100L
                            naam = "Reason 1"
                        }
                        resultaattype = UUID.randomUUID()
                    },
                    ZaaktypeCompletionParameters().apply {
                        id = 2L
                        zaakbeeindigReden = ZaakbeeindigReden().apply {
                            id = 200L
                            naam = "Reason 2"
                        }
                        resultaattype = UUID.randomUUID()
                    }
                )
            }

            When("completion parameters are mapped to a new configuration with the same zaaktypeUuid") {
                val newConfig = ZaaktypeBpmnConfiguration().apply {
                    zaaktypeUuid = previousConfig.zaaktypeUuid
                    zaaktypeOmschrijving = "Updated Type"
                }

                previousConfig.mapCompletionParameters(previousConfig, newConfig)

                Then("the completion parameters should be copied with the same IDs") {
                    newConfig.zaaktypeCompletionParameters shouldNotBe null
                    newConfig.zaaktypeCompletionParameters?.size shouldBe 2

                    val parameters = newConfig.zaaktypeCompletionParameters!!.toList()
                    parameters[0].id shouldBe 1L
                    parameters[0].zaaktypeConfiguration shouldBe newConfig
                    parameters[0].zaakbeeindigReden.id shouldBe 100L

                    parameters[1].id shouldBe 2L
                    parameters[1].zaaktypeConfiguration shouldBe newConfig
                    parameters[1].zaakbeeindigReden.id shouldBe 200L
                }
            }

            When("completion parameters are mapped to a configuration with a different zaaktypeUuid") {
                val newConfig = ZaaktypeBpmnConfiguration().apply {
                    zaaktypeUuid = UUID.randomUUID()
                    zaaktypeOmschrijving = "New Type"
                }

                previousConfig.mapCompletionParameters(previousConfig, newConfig)

                Then("the completion parameters should be copied with null IDs for new entity creation") {
                    newConfig.zaaktypeCompletionParameters shouldNotBe null
                    newConfig.zaaktypeCompletionParameters?.size shouldBe 2

                    newConfig.zaaktypeCompletionParameters?.forEach { param ->
                        param.id shouldBe null
                        param.zaaktypeConfiguration shouldBe newConfig
                    }
                }
            }
        }

        Given("A previous configuration with no completion parameters") {
            val previousConfig = ZaaktypeBpmnConfiguration().apply {
                zaaktypeUuid = UUID.randomUUID()
                zaaktypeOmschrijving = "Previous Type"
                zaaktypeCompletionParameters = null
            }

            When("completion parameters are mapped to a new configuration") {
                val newConfig = ZaaktypeBpmnConfiguration().apply {
                    zaaktypeUuid = UUID.randomUUID()
                    zaaktypeOmschrijving = "New Type"
                }

                previousConfig.mapCompletionParameters(previousConfig, newConfig)

                Then("the new configuration should have an empty set of completion parameters") {
                    newConfig.zaaktypeCompletionParameters shouldNotBe null
                    newConfig.zaaktypeCompletionParameters?.size shouldBe 0
                }
            }
        }

        Given("A previous configuration with empty completion parameters") {
            val previousConfig = ZaaktypeBpmnConfiguration().apply {
                zaaktypeUuid = UUID.randomUUID()
                zaaktypeOmschrijving = "Previous Type"
                zaaktypeCompletionParameters = mutableSetOf()
            }

            When("completion parameters are mapped to a new configuration") {
                val newConfig = ZaaktypeBpmnConfiguration().apply {
                    zaaktypeUuid = UUID.randomUUID()
                    zaaktypeOmschrijving = "New Type"
                }

                previousConfig.mapCompletionParameters(previousConfig, newConfig)

                Then("the new configuration should have an empty set of completion parameters") {
                    newConfig.zaaktypeCompletionParameters shouldNotBe null
                    newConfig.zaaktypeCompletionParameters?.size shouldBe 0
                }
            }
        }
    }
})
