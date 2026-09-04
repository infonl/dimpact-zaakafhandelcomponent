/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.smartdocuments.rest

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import java.util.UUID

class RestSmartDocumentsTemplateGroupTest : BehaviorSpec({

    given("a REST request") {
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

        `when`("convert to string representation is requested") {
            val stringSet = restTemplateRequest.toStringRepresentation()

            then("it produces a correct set of strings") {
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

    given("a live SmartDocuments template tree with a nested group and template") {
        val templateId = UUID.randomUUID().toString()
        val nestedGroupId = UUID.randomUUID().toString()
        val liveTemplateGroups = setOf(
            createRestSmartDocumentsTemplateGroup(
                name = "root",
                groups = setOf(
                    createRestSmartDocumentsTemplateGroup(
                        id = nestedGroupId,
                        name = "nested group (renamed)",
                        groups = emptySet(),
                        templates = setOf(
                            createRestSmartDocumentsTemplate(id = templateId, name = "nested template (renamed)")
                        )
                    )
                ),
                templates = emptySet()
            )
        )

        `when`("finding the nested group by id") {
            val found = liveTemplateGroups.findGroupById(nestedGroupId)

            then("it is found regardless of nesting depth") {
                found?.name shouldBe "nested group (renamed)"
            }
        }

        `when`("finding a group id that no longer exists") {
            val found = liveTemplateGroups.findGroupById("no such group id")

            then("nothing is found") {
                found.shouldBeNull()
            }
        }

        `when`("finding the nested template by id") {
            val found = liveTemplateGroups.findTemplateById(templateId)

            then("it is found regardless of nesting depth") {
                found?.name shouldBe "nested template (renamed)"
            }
        }

        `when`("finding a template id that no longer exists") {
            val found = liveTemplateGroups.findTemplateById("no such template id")

            then("nothing is found") {
                found.shouldBeNull()
            }
        }
    }

    given("a live SmartDocuments group whose groups and templates fields are null, not an empty set") {
        val liveTemplateGroups = setOf(
            RestSmartDocumentsTemplateGroup(
                id = UUID.randomUUID().toString(),
                name = "leaf group",
                groups = null,
                templates = null
            )
        )

        `when`("finding a group id against it") {
            val found = liveTemplateGroups.findGroupById("no such group id")

            then("nothing is found, without failing on the null groups field") {
                found.shouldBeNull()
            }
        }

        `when`("finding a template id against it") {
            val found = liveTemplateGroups.findTemplateById("no such template id")

            then("nothing is found, without failing on the null templates field") {
                found.shouldBeNull()
            }
        }
    }

    given("a persisted template mapping and a live tree with a rename and a deletion") {
        val informatieObjectTypeUUID = UUID.randomUUID()
        val renamedTemplateId = UUID.randomUUID().toString()
        val deletedTemplateId = UUID.randomUUID().toString()
        val renamedGroupId = UUID.randomUUID().toString()
        val deletedGroupId = UUID.randomUUID().toString()

        val persistedMapping = setOf(
            createRestMappedSmartDocumentsTemplateGroup(
                id = renamedGroupId,
                name = "old group name",
                groups = emptySet(),
                templates = setOf(
                    createRestMappedSmartDocumentsTemplate(
                        id = renamedTemplateId,
                        name = "old template name",
                        informatieObjectTypeUUID = informatieObjectTypeUUID
                    ),
                    createRestMappedSmartDocumentsTemplate(
                        id = deletedTemplateId,
                        name = "deleted template",
                        informatieObjectTypeUUID = UUID.randomUUID()
                    )
                )
            ),
            createRestMappedSmartDocumentsTemplateGroup(
                id = deletedGroupId,
                name = "deleted group",
                groups = emptySet(),
                templates = emptySet()
            )
        )
        val liveTemplateGroups = setOf(
            createRestSmartDocumentsTemplateGroup(
                id = renamedGroupId,
                name = "new group name",
                groups = emptySet(),
                templates = setOf(
                    createRestSmartDocumentsTemplate(id = renamedTemplateId, name = "new template name")
                )
            )
        )

        `when`("current names are resolved against the live tree") {
            val resolvedMapping = persistedMapping.resolveCurrentNames(liveTemplateGroups)

            then("a renamed group keeps its id and gets its current name") {
                resolvedMapping.first { it.id == renamedGroupId }.name shouldBe "new group name"
            }

            then("a renamed template keeps its persisted informatieobjecttype and gets its current name") {
                val renamedTemplate = resolvedMapping.first { it.id == renamedGroupId }
                    .templates!!.first { it.id == renamedTemplateId }

                renamedTemplate.name shouldBe "new template name"
                renamedTemplate.informatieObjectTypeUUID shouldBe informatieObjectTypeUUID
            }

            then("a deleted template is omitted while its sibling remains") {
                resolvedMapping.first { it.id == renamedGroupId }.templates!!.map { it.id } shouldBe
                    listOf(renamedTemplateId)
            }

            then("a deleted group is omitted entirely") {
                resolvedMapping.map { it.id } shouldBe listOf(renamedGroupId)
            }
        }
    }

    given("a persisted template mapping where a template moved to a different group in SmartDocuments") {
        val groupAId = UUID.randomUUID().toString()
        val groupBId = UUID.randomUUID().toString()
        val movedTemplateId = UUID.randomUUID().toString()
        val informatieObjectTypeUUID = UUID.randomUUID()

        val persistedMapping = setOf(
            createRestMappedSmartDocumentsTemplateGroup(
                id = groupAId,
                name = "group A",
                groups = emptySet(),
                templates = setOf(
                    createRestMappedSmartDocumentsTemplate(
                        id = movedTemplateId,
                        name = "old name under group A",
                        informatieObjectTypeUUID = informatieObjectTypeUUID
                    )
                )
            )
        )
        // The template still exists live, but SmartDocuments moved it out of group A into group B
        val liveTemplateGroups = setOf(
            createRestSmartDocumentsTemplateGroup(
                id = groupAId,
                name = "group A",
                groups = emptySet(),
                templates = emptySet()
            ),
            createRestSmartDocumentsTemplateGroup(
                id = groupBId,
                name = "group B",
                groups = emptySet(),
                templates = setOf(
                    createRestSmartDocumentsTemplate(id = movedTemplateId, name = "new name under group B")
                )
            )
        )

        `when`("current names are resolved against the live tree") {
            val resolvedMapping = persistedMapping.resolveCurrentNames(liveTemplateGroups)

            then("the moved template is omitted from its old group instead of kept there under a refreshed name") {
                resolvedMapping.first { it.id == groupAId }.templates!!.shouldBeEmpty()
            }
        }
    }

    given("a persisted group with no subgroups or templates recorded") {
        val groupId = UUID.randomUUID().toString()
        val persistedMapping = setOf(
            createRestMappedSmartDocumentsTemplateGroup(id = groupId, name = "old name")
        )
        val liveTemplateGroups = setOf(
            createRestSmartDocumentsTemplateGroup(
                id = groupId,
                name = "new name",
                groups = emptySet(),
                templates = emptySet()
            )
        )

        `when`("current names are resolved against the live tree") {
            val resolvedMapping = persistedMapping.resolveCurrentNames(liveTemplateGroups)

            then("the group is resolved with its current name, and null groups/templates stay null") {
                val resolvedGroup = resolvedMapping.first { it.id == groupId }
                resolvedGroup.name shouldBe "new name"
                resolvedGroup.groups.shouldBeNull()
                resolvedGroup.templates.shouldBeNull()
            }
        }
    }
})
