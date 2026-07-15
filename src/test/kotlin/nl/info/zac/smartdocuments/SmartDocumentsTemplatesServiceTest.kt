/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.smartdocuments

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import jakarta.persistence.EntityManager
import jakarta.persistence.TypedQuery
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.Path
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import nl.info.client.smartdocuments.model.createsmartDocumentsTemplatesResponse
import nl.info.zac.admin.ZaaktypeConfigurationService
import nl.info.zac.admin.model.ZaaktypeConfiguration
import nl.info.zac.smartdocuments.exception.SmartDocumentsConfigurationException
import nl.info.zac.smartdocuments.templates.model.SmartDocumentsTemplate
import nl.info.zac.smartdocuments.templates.model.SmartDocumentsTemplateGroup
import java.util.UUID

class SmartDocumentsTemplatesServiceTest : BehaviorSpec({
    val entityManager = mockk<EntityManager>()
    val smartDocumentsService = mockk<SmartDocumentsService>()
    val zaaktypeConfigurationService = mockk<ZaaktypeConfigurationService>()
    val smartDocumentsTemplatesService = SmartDocumentsTemplatesService(
        entityManager = entityManager,
        smartDocumentsService = smartDocumentsService,
        zaaktypeConfigurationService = zaaktypeConfigurationService
    )

    afterEach {
        checkUnnecessaryStub()
    }

    given("SmartDocuments is enabled and contains a list of templates") {
        val smartDocumentsTemplatesResponse = createsmartDocumentsTemplatesResponse()
        every { smartDocumentsService.listTemplates() } returns smartDocumentsTemplatesResponse
        every { smartDocumentsService.isEnabled() } returns true

        `when`("a list of template is requested") {
            val restSmartDocumentsTemplateGroupSet = smartDocumentsTemplatesService.listTemplates()

            then("the template is returned") {
                restSmartDocumentsTemplateGroupSet.size shouldBe
                    smartDocumentsTemplatesResponse.documentsStructure.templatesStructure.templateGroups.size
            }
        }

        `when`("list template names for a first-level group is called") {
            val templateNames = smartDocumentsTemplatesService.listGroupTemplateNames(listOf("Dimpact"))

            then("it should return a list of template names") {
                templateNames shouldBe listOf("Aanvullende informatie nieuw", "Aanvullende informatie oud")
            }
        }

        `when`("list template names for a nested level group is called") {
            val templateNames = smartDocumentsTemplatesService.listGroupTemplateNames(
                listOf(
                    "Dimpact",
                    "fakeTemplateGroup1"
                )
            )

            then("it should return a list of template names") {
                templateNames shouldBe listOf("Data Test", "OpenZaakTest")
            }
        }

        `when`("list template names for a non-existent first-level group is called") {
            val exception = shouldThrow<IllegalArgumentException> {
                smartDocumentsTemplatesService.listGroupTemplateNames(listOf("no such group"))
            }

            then("it should return a list of template names") {
                exception.message shouldContain "no such group"
            }
        }

        `when`("list template names for a non-existent nested group is called") {
            val exception = shouldThrow<IllegalArgumentException> {
                smartDocumentsTemplatesService.listGroupTemplateNames(
                    listOf("Dimpact", "no such group")
                )
            }

            then("it should return a list of template names") {
                exception.message shouldContain "Dimpact, no such group"
            }
        }
    }

    given("A missing mapping") {
        val zaaktypeUUID = UUID.randomUUID()
        val zaakafhanderParametersId = 1L
        val templateGroupId = "template group id"
        val templateId = "template id"

        val criteriaBuilder = mockk<CriteriaBuilder>()
        val criteriaQuery = mockk<CriteriaQuery<UUID>>()
        val root = mockk<Root<SmartDocumentsTemplate>>()
        val namePath = mockk<Path<UUID>>()
        val zaaktypeConfigurationPath = mockk<Path<ZaaktypeConfiguration>>()
        val longPath = mockk<Path<Long>>()
        val stringPath = mockk<Path<String>>()
        val templateGroupPath = mockk<Path<SmartDocumentsTemplateGroup>>()
        val templatePath = mockk<Path<SmartDocumentsTemplate>>()
        val predicate = mockk<Predicate>()
        val zaaktypeConfiguration = mockk<ZaaktypeConfiguration>()
        val typedQuery = mockk<TypedQuery<UUID>>()

        every { entityManager.criteriaBuilder } returns criteriaBuilder
        every { entityManager.createQuery(criteriaQuery) } returns typedQuery

        every { criteriaBuilder.createQuery(UUID::class.java) } returns criteriaQuery
        every { criteriaBuilder.and(any<Predicate>(), any<Predicate>(), any<Predicate>()) } returns predicate
        every { criteriaBuilder.equal(longPath, zaakafhanderParametersId) } returns predicate
        every { criteriaBuilder.equal(stringPath, templateGroupId) } returns predicate
        every { criteriaBuilder.equal(templatePath, templateId) } returns predicate

        every { criteriaQuery.select(namePath) } returns criteriaQuery
        every { criteriaQuery.where(any<Predicate>()) } returns criteriaQuery
        every { criteriaQuery.from(SmartDocumentsTemplate::class.java) } returns root

        every { root.get<UUID>("informatieObjectTypeUUID") } returns namePath
        every { root.get<ZaaktypeConfiguration>("zaaktypeConfiguration") } returns zaaktypeConfigurationPath
        every { root.get<SmartDocumentsTemplateGroup>("templateGroup") } returns templateGroupPath
        every { root.get<SmartDocumentsTemplate>("smartDocumentsId") } returns templatePath

        every { zaaktypeConfigurationPath.get<Long>("id") } returns longPath
        every { templateGroupPath.get<String>("smartDocumentsId") } returns stringPath

        every {
            zaaktypeConfigurationService.readZaaktypeConfiguration(zaaktypeUUID)
        } returns zaaktypeConfiguration
        every { zaaktypeConfiguration.id } returns zaakafhanderParametersId

        every { typedQuery.setMaxResults(any<Int>()) } returns typedQuery
        every { typedQuery.resultList } returns emptyList()

        `when`("information object UUID is requested") {
            val exception = shouldThrow<SmartDocumentsConfigurationException> {
                smartDocumentsTemplatesService.getInformationObjectTypeUUID(
                    zaaktypeUUID,
                    templateGroupId,
                    templateId
                )
            }

            then("exception is thrown") {
                exception.message shouldContain templateGroupId
                exception.message shouldContain templateId
            }
        }
    }

    given("An existing mapping") {
        val zaaktypeUUID = UUID.randomUUID()
        val zaakafhanderParametersId = 1L
        val templateGroupId = "template group id"
        val templateId = "template id"
        val informationObjectTypeUUID = UUID.randomUUID()

        val criteriaBuilder = mockk<CriteriaBuilder>()
        val criteriaQuery = mockk<CriteriaQuery<UUID>>()
        val root = mockk<Root<SmartDocumentsTemplate>>()
        val namePath = mockk<Path<UUID>>()
        val zaaktypeConfigurationPath = mockk<Path<ZaaktypeConfiguration>>()
        val longPath = mockk<Path<Long>>()
        val stringPath = mockk<Path<String>>()
        val templateGroupPath = mockk<Path<SmartDocumentsTemplateGroup>>()
        val templatePath = mockk<Path<SmartDocumentsTemplate>>()
        val predicate = mockk<Predicate>()
        val zaaktypeConfiguration = mockk<ZaaktypeConfiguration>()
        val typedQuery = mockk<TypedQuery<UUID>>()

        every { entityManager.criteriaBuilder } returns criteriaBuilder
        every { entityManager.createQuery(criteriaQuery) } returns typedQuery

        every { criteriaBuilder.createQuery(UUID::class.java) } returns criteriaQuery
        every { criteriaBuilder.and(any<Predicate>(), any<Predicate>(), any<Predicate>()) } returns predicate
        every { criteriaBuilder.equal(longPath, zaakafhanderParametersId) } returns predicate
        every { criteriaBuilder.equal(stringPath, templateGroupId) } returns predicate
        every { criteriaBuilder.equal(templatePath, templateId) } returns predicate

        every { criteriaQuery.select(namePath) } returns criteriaQuery
        every { criteriaQuery.where(any<Predicate>()) } returns criteriaQuery
        every { criteriaQuery.from(SmartDocumentsTemplate::class.java) } returns root

        every { root.get<UUID>("informatieObjectTypeUUID") } returns namePath
        every { root.get<ZaaktypeConfiguration>("zaaktypeConfiguration") } returns zaaktypeConfigurationPath
        every { root.get<SmartDocumentsTemplateGroup>("templateGroup") } returns templateGroupPath
        every { root.get<SmartDocumentsTemplate>("smartDocumentsId") } returns templatePath

        every { zaaktypeConfigurationPath.get<Long>("id") } returns longPath
        every { templateGroupPath.get<String>("smartDocumentsId") } returns stringPath

        every {
            zaaktypeConfigurationService.readZaaktypeConfiguration(zaaktypeUUID)
        } returns zaaktypeConfiguration
        every { zaaktypeConfiguration.id } returns zaakafhanderParametersId

        every { typedQuery.setMaxResults(any<Int>()) } returns typedQuery
        every { typedQuery.resultList } returns listOf(informationObjectTypeUUID)

        `when`("information object UUID is requested") {
            val result = smartDocumentsTemplatesService.getInformationObjectTypeUUID(
                zaaktypeUUID,
                templateGroupId,
                templateId
            )

            then("the information object type UUID is returned") {
                result shouldBe informationObjectTypeUUID
            }
        }
    }

    given("A missing template group") {
        val templateGroupId = "123abc"

        val criteriaBuilder = mockk<CriteriaBuilder>()
        val criteriaQuery = mockk<CriteriaQuery<String>>()
        val root = mockk<Root<SmartDocumentsTemplateGroup>>()
        val stringPath = mockk<Path<String>>()
        val predicate = mockk<Predicate>()
        val typedQuery = mockk<TypedQuery<String>>()

        every { entityManager.criteriaBuilder } returns criteriaBuilder
        every { entityManager.createQuery(criteriaQuery) } returns typedQuery

        every { criteriaBuilder.createQuery(String::class.java) } returns criteriaQuery
        every { criteriaBuilder.equal(stringPath, templateGroupId) } returns predicate

        every { root.get<String>("name") } returns stringPath
        every { root.get<String>("smartDocumentsId") } returns stringPath

        every { criteriaQuery.select(stringPath) } returns criteriaQuery
        every { criteriaQuery.where(any<Predicate>()) } returns criteriaQuery
        every { criteriaQuery.from(SmartDocumentsTemplateGroup::class.java) } returns root

        every { typedQuery.setMaxResults(any<Int>()) } returns typedQuery
        every { typedQuery.resultList } returns emptyList()

        `when`("template group name query is started") {
            val exception = shouldThrow<SmartDocumentsConfigurationException> {
                smartDocumentsTemplatesService.getTemplateGroupName(templateGroupId)
            }

            then("exception is thrown") {
                exception.message shouldContain "123abc"
            }
        }
    }

    given("A missing template") {
        val templateId = "123abc"

        val criteriaBuilder = mockk<CriteriaBuilder>()
        val criteriaQuery = mockk<CriteriaQuery<String>>()
        val root = mockk<Root<SmartDocumentsTemplate>>()
        val stringPath = mockk<Path<String>>()
        val predicate = mockk<Predicate>()
        val typedQuery = mockk<TypedQuery<String>>()

        every { entityManager.criteriaBuilder } returns criteriaBuilder
        every { entityManager.createQuery(criteriaQuery) } returns typedQuery

        every { criteriaBuilder.createQuery(String::class.java) } returns criteriaQuery
        every { criteriaBuilder.equal(stringPath, templateId) } returns predicate

        every { root.get<String>("name") } returns stringPath
        every { root.get<String>("smartDocumentsId") } returns stringPath

        every { criteriaQuery.select(stringPath) } returns criteriaQuery
        every { criteriaQuery.where(any<Predicate>()) } returns criteriaQuery
        every { criteriaQuery.from(SmartDocumentsTemplate::class.java) } returns root

        every { typedQuery.setMaxResults(any<Int>()) } returns typedQuery
        every { typedQuery.resultList } returns emptyList()

        `when`("template name query is started") {
            val exception = shouldThrow<SmartDocumentsConfigurationException> {
                smartDocumentsTemplatesService.getTemplateName(templateId)
            }

            then("exception is thrown") {
                exception.message shouldContain "123abc"
            }
        }
    }

    given("No zaaktype configuration exists for the given UUID") {
        val unknownUUID = UUID.randomUUID()

        every { zaaktypeConfigurationService.readZaaktypeConfiguration(unknownUUID) } returns null

        `when`("store templates mapping is called") {
            val exception = shouldThrow<IllegalArgumentException> {
                smartDocumentsTemplatesService.storeTemplatesMapping(emptySet(), unknownUUID)
            }

            then("exception is thrown with the missing UUID") {
                exception.message shouldBe "No zaaktype configuration found for zaaktype UUID $unknownUUID"
            }
        }
    }

    given("SmartDocuments is enabled but no zaaktype configuration exists") {
        val zaaktypeUUID = UUID.randomUUID()
        every { smartDocumentsService.isEnabled() } returns true
        every { zaaktypeConfigurationService.readZaaktypeConfiguration(zaaktypeUUID) } returns null

        `when`("templates mapping is requested") {
            val mappings = smartDocumentsTemplatesService.getTemplatesMapping(zaaktypeUUID)

            then("it returns an empty set") {
                mappings shouldBe emptySet()
            }
        }
    }

    given("SmartDocuments is disabled") {
        every { smartDocumentsService.isEnabled() } returns false

        `when`("templates are listed") {
            val templates = smartDocumentsTemplatesService.listTemplates()

            then("it returns an empty set") {
                templates shouldBe emptySet()
            }
        }

        `when`("template names are listed") {
            val templateNames = smartDocumentsTemplatesService.listGroupTemplateNames(
                listOf("Dimpact")
            )

            then("it should return a list of template names") {
                templateNames shouldBe emptyList()
            }
        }

        `when`("mapping is listed") {
            val mappings = smartDocumentsTemplatesService.getTemplatesMapping(UUID.randomUUID())

            then("it returns an empty set") {
                mappings shouldBe emptySet()
            }
        }
    }
})
