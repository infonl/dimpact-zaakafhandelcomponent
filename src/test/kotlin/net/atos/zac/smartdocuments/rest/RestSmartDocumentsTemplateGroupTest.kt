/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.smartdocuments.rest

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import java.util.UUID

class RestSmartDocumentsTemplateGroupTest : BehaviorSpec({

    Given("a REST request") {
        val expectedInformatieobjectTypeUUID = UUID.randomUUID()
        val restTemplateRequest = setOf(
            createRestMappedSmartDocumentsTemplateGroup(
                name = "root",
                groups = setOf(
                    createRestMappedSmartDocumentsTemplateGroup(
                        name = "group 1",
                        templates = setOf(
                            createRestMappedSmartDocumentsTemplate(
                                name = "group 1 template 1",
                                informatieObjectTypeUUID = expectedInformatieobjectTypeUUID
                            ),
                            createRestMappedSmartDocumentsTemplate(
                                name = "group 1 template 2",
                                informatieObjectTypeUUID = expectedInformatieobjectTypeUUID
                            )
                        ),
                        groups = emptySet()
                    ),
                    createRestMappedSmartDocumentsTemplateGroup(
                        name = "group 2",
                        templates = setOf(
                            createRestMappedSmartDocumentsTemplate(
                                name = "group 2 template 1",
                                informatieObjectTypeUUID = expectedInformatieobjectTypeUUID
                            ),
                            createRestMappedSmartDocumentsTemplate(
                                name = "group 2 template 2",
                                informatieObjectTypeUUID = expectedInformatieobjectTypeUUID
                            )
                        ),
                        groups = emptySet()
                    )
                ),
                templates = setOf(
                    createRestMappedSmartDocumentsTemplate(
                        name = "root template 1",
                        informatieObjectTypeUUID = expectedInformatieobjectTypeUUID
                    ),
                    createRestMappedSmartDocumentsTemplate(
                        name = "root template 2",
                        informatieObjectTypeUUID = expectedInformatieobjectTypeUUID
                    )
                )
            )
        )

        When("convert to string representation is requested") {
            val stringSet = restTemplateRequest.toStringRepresentation()

            Then("it produces a correct set of strings") {
                stringSet.size shouldBe 9

                with(restTemplateRequest.first()) {
                    val rootId = id
                    val rootTemplate1Id = templates!!.first().id
                    val rootTemplate2Id = templates!!.last().id
                    val group1Id = groups!!.first().id
                    val group2Id = groups!!.last().id
                    val group1Template1Id = groups!!.first().templates!!.first().id
                    val group1Template2Id = groups!!.first().templates!!.last().id
                    val group2Template1Id = groups!!.last().templates!!.first().id
                    val group2Template2Id = groups!!.last().templates!!.last().id

                    stringSet shouldContainAll setOf(
                        "group.$rootId.root",
                        "group.$rootId.root.template.$rootTemplate1Id.root template 1",
                        "group.$rootId.root.template.$rootTemplate2Id.root template 2",
                        "group.$rootId.root.group.$group1Id.group 1",
                        "group.$rootId.root.group.$group1Id.group 1.template.$group1Template1Id.group 1 template 1",
                        "group.$rootId.root.group.$group1Id.group 1.template.$group1Template2Id.group 1 template 2",
                        "group.$rootId.root.group.$group2Id.group 2",
                        "group.$rootId.root.group.$group2Id.group 2.template.$group2Template1Id.group 2 template 1",
                        "group.$rootId.root.group.$group2Id.group 2.template.$group2Template2Id.group 2 template 2"
                    )
                }
            }
        }
    }
})
