/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.admin

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldBeSameSizeAs
import io.kotest.matchers.collections.shouldContainOnly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import jakarta.persistence.EntityManager
import jakarta.persistence.TypedQuery
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.Expression
import jakarta.persistence.criteria.Order
import jakarta.persistence.criteria.Path
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import jakarta.persistence.criteria.Subquery
import net.atos.zac.admin.ZaaktypeCmmnConfigurationService
import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.model.createResultaatType
import nl.info.client.zgw.ztc.model.createZaakType
import nl.info.zac.admin.exception.ZaaktypeInUseException
import nl.info.zac.admin.model.ZaaktypeCmmnBetrokkeneParameters
import nl.info.zac.admin.model.ZaaktypeCmmnConfiguration
import nl.info.zac.admin.model.createZaaktypeCmmnConfiguration
import nl.info.zac.flowable.bpmn.model.createZaaktypeBpmnConfiguration
import nl.info.zac.smartdocuments.SmartDocumentsTemplatesService
import nl.info.zac.smartdocuments.rest.RestMappedSmartDocumentsTemplateGroup
import java.net.URI
import java.time.ZonedDateTime
import java.util.Date
import java.util.UUID

class ZaaktypeCmmnConfigurationBeheerServiceTest : BehaviorSpec({
    val entityManager = mockk<EntityManager>()
    val ztcClientService = mockk<ZtcClientService>()
    val criteriaBuilder = mockk<CriteriaBuilder>()
    val zaaktypeCmmnConfigurationCriteriaQuery = mockk<CriteriaQuery<ZaaktypeCmmnConfiguration>>()
    val dateSubquery = mockk<Subquery<Date>>()
    val zaaktypeCmmnConfigurationTypedQuery = mockk<TypedQuery<ZaaktypeCmmnConfiguration>>()
    val zaaktypeCmmnConfigurationRoot = mockk<Root<ZaaktypeCmmnConfiguration>>()
    val path = mockk<Path<Any>>()
    val pathString = mockk<Path<String>>()
    val predicate = mockk<Predicate>()
    val order = mockk<Order>()
    val expressionString = mockk<Expression<String>>()
    val zaaktypeCmmnConfigurationService = mockk<ZaaktypeCmmnConfigurationService>()
    val smartDocumentsTemplatesService = mockk<SmartDocumentsTemplatesService>()
    val zaaktypeBpmnConfigurationService = mockk<ZaaktypeBpmnConfigurationService>()

    val zaaktypeCmmnConfigurationBeheerService = ZaaktypeCmmnConfigurationBeheerService(
        entityManager = entityManager,
        ztcClientService = ztcClientService,
        zaaktypeCmmnConfigurationService = zaaktypeCmmnConfigurationService,
        smartDocumentsTemplatesService = smartDocumentsTemplatesService,
        zaaktypeBpmnConfigurationService = zaaktypeBpmnConfigurationService
    )

    beforeEach {
        checkUnnecessaryStub()
    }

    Given("One zaaktypeCmmnConfiguration for a given zaaktype UUID") {
        val zaaktypeCmmnConfiguration = createZaaktypeCmmnConfiguration()
        val now = ZonedDateTime.now()
        every { ztcClientService.resetCacheTimeToNow() } returns now
        every { entityManager.criteriaBuilder } returns criteriaBuilder
        every {
            criteriaBuilder.createQuery(ZaaktypeCmmnConfiguration::class.java)
        } returns zaaktypeCmmnConfigurationCriteriaQuery
        every {
            zaaktypeCmmnConfigurationCriteriaQuery.from(ZaaktypeCmmnConfiguration::class.java)
        } returns zaaktypeCmmnConfigurationRoot
        every {
            zaaktypeCmmnConfigurationCriteriaQuery.select(zaaktypeCmmnConfigurationRoot)
        } returns zaaktypeCmmnConfigurationCriteriaQuery
        every { zaaktypeCmmnConfigurationRoot.get<Any>("zaakTypeUUID") } returns path
        every { criteriaBuilder.equal(path, zaaktypeCmmnConfiguration.zaakTypeUUID) } returns predicate
        every { zaaktypeCmmnConfigurationCriteriaQuery.where(predicate) } returns zaaktypeCmmnConfigurationCriteriaQuery
        every {
            entityManager.createQuery(zaaktypeCmmnConfigurationCriteriaQuery).setMaxResults(1).resultList
        } returns listOf(zaaktypeCmmnConfiguration)

        When("the zaaktypeCmmnConfiguration are retrieved based on the zaaktypeUUID") {
            val returnedZaaktypeCmmnConfiguration = zaaktypeCmmnConfigurationBeheerService.fetchZaaktypeCmmnConfiguration(
                zaaktypeCmmnConfiguration.zaakTypeUUID!!
            )

            Then("the zaaktypeCmmnConfiguration should be returned") {
                with(returnedZaaktypeCmmnConfiguration) {
                    zaakTypeUUID shouldBe zaaktypeCmmnConfiguration.zaakTypeUUID
                }
            }
        }
    }
    Given("Two zaaktypeCmmnConfigurations") {
        val zaaktypeCmmnConfigurations = listOf(
            createZaaktypeCmmnConfiguration(),
            createZaaktypeCmmnConfiguration()
        )
        every { entityManager.criteriaBuilder } returns criteriaBuilder
        every {
            criteriaBuilder.createQuery(ZaaktypeCmmnConfiguration::class.java)
        } returns zaaktypeCmmnConfigurationCriteriaQuery
        every {
            zaaktypeCmmnConfigurationCriteriaQuery.from(ZaaktypeCmmnConfiguration::class.java)
        } returns zaaktypeCmmnConfigurationRoot
        every {
            zaaktypeCmmnConfigurationCriteriaQuery.select(zaaktypeCmmnConfigurationRoot)
        } returns zaaktypeCmmnConfigurationCriteriaQuery
        every {
            entityManager.createQuery(zaaktypeCmmnConfigurationCriteriaQuery).resultList
        } returns zaaktypeCmmnConfigurations
        every { zaaktypeCmmnConfigurationRoot.get<Any>("id") } returns path
        every { criteriaBuilder.desc(path) } returns order
        every { zaaktypeCmmnConfigurationCriteriaQuery.orderBy(order) } returns zaaktypeCmmnConfigurationCriteriaQuery

        When("the zaaktypeCmmnConfiguration are retrieved based on the zaaktypeUUID") {
            val returnedZaaktypeCmmnConfiguration = zaaktypeCmmnConfigurationBeheerService.listZaaktypeCmmnConfiguration()

            Then("both zaaktypeCmmnConfiguration should be returned") {
                returnedZaaktypeCmmnConfiguration.size shouldBe 2
                returnedZaaktypeCmmnConfiguration.forEachIndexed { index, returnedZaakafhandelparameter ->
                    with(returnedZaakafhandelparameter) {
                        zaakTypeUUID shouldBe zaaktypeCmmnConfigurations[index].zaakTypeUUID
                        id shouldBe zaaktypeCmmnConfigurations[index].id
                    }
                }
            }
        }
    }
    Given("Two active zaaktypeCmmnConfiguration for a given productaanvraagType") {
        val productaanvraagType = "fakeProductaanvraagType"
        val zaaktypeCmmnConfigurationList = listOf(
            createZaaktypeCmmnConfiguration(),
            createZaaktypeCmmnConfiguration()
        )
        with(entityManager) {
            every { createQuery(zaaktypeCmmnConfigurationCriteriaQuery) } returns zaaktypeCmmnConfigurationTypedQuery
            every { getCriteriaBuilder() } returns criteriaBuilder
        }
        with(criteriaBuilder) {
            every { createQuery(ZaaktypeCmmnConfiguration::class.java) } returns zaaktypeCmmnConfigurationCriteriaQuery
            every { greatest<String>(any()) } returns expressionString
            every { equal(pathString, pathString) } returns predicate
            every { equal(pathString, productaanvraagType) } returns predicate
            every { equal(pathString, dateSubquery) } returns predicate
        }
        every { criteriaBuilder.and(predicate, predicate) } returns predicate
        with(zaaktypeCmmnConfigurationCriteriaQuery) {
            every { from(ZaaktypeCmmnConfiguration::class.java) } returns zaaktypeCmmnConfigurationRoot
            every { subquery(Date::class.java) } returns dateSubquery
            every { select(zaaktypeCmmnConfigurationRoot) } returns zaaktypeCmmnConfigurationCriteriaQuery
            every { where(predicate) } returns zaaktypeCmmnConfigurationCriteriaQuery
        }
        with(dateSubquery) {
            every { from(ZaaktypeCmmnConfiguration::class.java) } returns mockk {
                every { get<String>("creatiedatum") } returns pathString
                every { get<String>("zaaktypeOmschrijving") } returns pathString
            }
            every { select(any()) } returns dateSubquery
            every { where(predicate) } returns dateSubquery
        }
        with(zaaktypeCmmnConfigurationRoot) {
            every { get<String>("zaaktypeOmschrijving") } returns pathString
            every { zaaktypeCmmnConfigurationRoot.get<String>("productaanvraagtype") } returns pathString
            every { zaaktypeCmmnConfigurationRoot.get<String>("creatiedatum") } returns pathString
        }
        every { zaaktypeCmmnConfigurationTypedQuery.resultList } returns zaaktypeCmmnConfigurationList

        When("the active zaaktypeCmmnConfiguration are retrieved for the given productaanvraagType") {
            val returnedZaaktypeCmmnConfiguration =
                zaaktypeCmmnConfigurationBeheerService.findActiveZaaktypeCmmnConfigurationByProductaanvraagtype(
                    productaanvraagType
                )

            Then("two zaaktypeCmmnConfiguration should be returned") {
                returnedZaaktypeCmmnConfiguration.size shouldBe 2
                returnedZaaktypeCmmnConfiguration.map { productaanvraagType } shouldContainOnly listOf(productaanvraagType)
            }
        }
    }

    Given("A new zaaktype was created and no previous version exists") {
        val zaaktypeUUID = UUID.randomUUID()
        val zaaktypeUri = URI("https://example.com/zaaktypes/$zaaktypeUUID")
        val zaakType = createZaakType(uri = zaaktypeUri, servicenorm = "P30D", concept = false)

        every { zaaktypeCmmnConfigurationService.clearListCache() } returns "Cache cleared"

        // ZtcClientService mocking
        every { ztcClientService.clearZaaktypeCache() } returns "Cache cleared"
        every { ztcClientService.readZaaktype(zaaktypeUri) } returns zaakType

        every {
            zaaktypeBpmnConfigurationService.findConfigurationByZaaktypeUuid(zaaktypeUUID)
        } returns null

        // Relaxed entity manager mocking; criteria queries and persisting
        val criteriaQuery = mockk<CriteriaQuery<ZaaktypeCmmnConfiguration>>(relaxed = true)
        every { entityManager.criteriaBuilder } returns mockk(relaxed = true) {
            every { createQuery(ZaaktypeCmmnConfiguration::class.java) } returns criteriaQuery
        }
        val slotPersistZaaktypeCmmnConfiguration = slot<ZaaktypeCmmnConfiguration>()

        every {
            entityManager.persist(capture(slotPersistZaaktypeCmmnConfiguration))
        } answers { ZaaktypeCmmnConfiguration() }

        every { entityManager.createQuery(criteriaQuery) } returns mockk {
            every { setMaxResults(1) } returns this
            every { resultList } returns emptyList()
        }

        When("Publishing a new zaaktype") {
            zaaktypeCmmnConfigurationBeheerService.upsertZaaktypeCmmnConfiguration(zaaktypeUri)

            Then("The new zaak type is stored") {
                verify {
                    entityManager.persist(any<ZaaktypeCmmnConfiguration>())
                }
            }
        }
    }

    Given("A zaaktype that has been updated") {
        val zaaktypeUUID = UUID.randomUUID()
        val zaaktypeUri = URI("https://example.com/zaaktypes/$zaaktypeUUID")
        val zaakType = createZaakType(uri = zaaktypeUri, servicenorm = "P30D", concept = false)

        every { zaaktypeCmmnConfigurationService.clearListCache() } returns "Cache cleared"

        every {
            zaaktypeBpmnConfigurationService.findConfigurationByZaaktypeUuid(zaaktypeUUID)
        } returns null

        // ZtcClientService mocking
        every { ztcClientService.clearZaaktypeCache() } returns "Cache cleared"
        every { ztcClientService.readZaaktype(zaaktypeUri) } returns zaakType
        every { ztcClientService.readResultaattype(any<URI>()) } returns createResultaatType()
        every { ztcClientService.readResultaattype(any<UUID>()) } returns createResultaatType()

        // Relaxed entity manager mocking; criteria queries and persisting
        val criteriaQuery = mockk<CriteriaQuery<ZaaktypeCmmnConfiguration>>(relaxed = true)
        every { entityManager.criteriaBuilder } returns mockk(relaxed = true) {
            every { createQuery(ZaaktypeCmmnConfiguration::class.java) } returns criteriaQuery
        }
        val zaakafhandelParamentersId = 100L
        val originalZaaktypeCmmnConfiguration = createZaaktypeCmmnConfiguration(
            id = zaakafhandelParamentersId,
            zaaktypeUUID = zaaktypeUUID,
        )

        val betrokkeneKoppelingen = ZaaktypeCmmnBetrokkeneParameters().apply {
            brpKoppelen = true
            kvkKoppelen = false
            zaaktypeCmmnConfiguration = originalZaaktypeCmmnConfiguration
        }

        originalZaaktypeCmmnConfiguration.zaaktypeCmmnBetrokkeneParameters = betrokkeneKoppelingen

        val slotPersistZaaktypeCmmnConfiguration = slot<ZaaktypeCmmnConfiguration>()

        When("Processing the updated zaaktype") {
            every { entityManager.createQuery(criteriaQuery) } returns mockk {
                every { setMaxResults(1) } returns this
                every { resultList } returns listOf(originalZaaktypeCmmnConfiguration)
            }

            every {
                entityManager.merge(capture(slotPersistZaaktypeCmmnConfiguration))
            } answers { ZaaktypeCmmnConfiguration() }

            zaaktypeCmmnConfigurationBeheerService.upsertZaaktypeCmmnConfiguration(zaaktypeUri)

            Then("The related zaaktypeCmmnConfiguration is stored through the entity manager") {
                slotPersistZaaktypeCmmnConfiguration.isCaptured shouldBe true
                verify {
                    entityManager.merge(any<ZaaktypeCmmnConfiguration>())
                }
            }

            And("The zaaktype values have been copied into the zaakparameters") {
                with(slotPersistZaaktypeCmmnConfiguration.captured) {
                    id shouldBe zaakafhandelParamentersId
                    zaakTypeUUID shouldBe zaakType.url.extractUuid()
                    zaaktypeOmschrijving shouldBe zaakType.omschrijving
                }
            }
        }

        When("Publishing a new zaaktype") {
            every {
                entityManager.persist(capture(slotPersistZaaktypeCmmnConfiguration))
            } answers { ZaaktypeCmmnConfiguration() }

            every { entityManager.createQuery(criteriaQuery) } returns mockk {
                every { setMaxResults(1) } returns this
                every { resultList } returns emptyList() andThen listOf(originalZaaktypeCmmnConfiguration)
            }

            val template = RestMappedSmartDocumentsTemplateGroup(
                id = "test",
                name = "test",
                groups = null,
                templates = null
            )

            every { smartDocumentsTemplatesService.getTemplatesMapping(any<UUID>()) } answers { setOf(template) }

            every {
                smartDocumentsTemplatesService.storeTemplatesMapping(
                    any<Set<RestMappedSmartDocumentsTemplateGroup>>(),
                    any<UUID>()
                )
            } returns mockk {}

            zaaktypeCmmnConfigurationBeheerService.upsertZaaktypeCmmnConfiguration(zaaktypeUri)

            Then("The zaaktype simple values have been copied from the original") {
                with(slotPersistZaaktypeCmmnConfiguration.captured) {
                    zaakTypeUUID shouldBe zaakType.url.extractUuid()
                    zaaktypeOmschrijving shouldBe zaakType.omschrijving
                    caseDefinitionID shouldBe originalZaaktypeCmmnConfiguration.caseDefinitionID
                    groepID shouldBe originalZaaktypeCmmnConfiguration.groepID
                    gebruikersnaamMedewerker shouldBe originalZaaktypeCmmnConfiguration.gebruikersnaamMedewerker
                    einddatumGeplandWaarschuwing shouldBe originalZaaktypeCmmnConfiguration.einddatumGeplandWaarschuwing
                    uiterlijkeEinddatumAfdoeningWaarschuwing shouldBe originalZaaktypeCmmnConfiguration
                        .uiterlijkeEinddatumAfdoeningWaarschuwing
                    intakeMail shouldBe originalZaaktypeCmmnConfiguration.intakeMail
                    afrondenMail shouldBe originalZaaktypeCmmnConfiguration.afrondenMail
                    productaanvraagtype shouldBe originalZaaktypeCmmnConfiguration.productaanvraagtype
                    domein shouldBe originalZaaktypeCmmnConfiguration.domein
                    smartDocumentsIngeschakeld shouldBe originalZaaktypeCmmnConfiguration.smartDocumentsIngeschakeld
                }
            }

            And("The human task parameters should have been cloned") {
                slotPersistZaaktypeCmmnConfiguration.captured.getHumanTaskParametersCollection().let {
                    it shouldBeSameSizeAs originalZaaktypeCmmnConfiguration.getHumanTaskParametersCollection()
                    it zip originalZaaktypeCmmnConfiguration.getHumanTaskParametersCollection()
                }.forEach { (new, original) ->
                    new.id shouldNotBe original.id
                    new.zaaktypeCmmnConfiguration shouldNotBe original.zaaktypeCmmnConfiguration
                    new.zaaktypeCmmnConfiguration shouldBe slotPersistZaaktypeCmmnConfiguration.captured
                    new.groepID shouldNotBe original.groepID
                    new.actief shouldBe original.actief
                    new.doorlooptijd shouldBe original.doorlooptijd
                    new.getFormulierDefinitieID() shouldBe original.getFormulierDefinitieID()
                    new.doorlooptijd shouldBe original.doorlooptijd
                    new.planItemDefinitionID shouldBe original.planItemDefinitionID
                }
            }

            And("The user event listener parameters should get copied") {
                slotPersistZaaktypeCmmnConfiguration.captured.getUserEventListenerParametersCollection().let {
                    it shouldBeSameSizeAs originalZaaktypeCmmnConfiguration.getUserEventListenerParametersCollection()
                    it zip originalZaaktypeCmmnConfiguration.getUserEventListenerParametersCollection()
                }.forEach { (new, original) ->
                    new.id shouldNotBe original.id
                    new.zaaktypeCmmnConfiguration shouldNotBe original.zaaktypeCmmnConfiguration
                    new.zaaktypeCmmnConfiguration shouldBe slotPersistZaaktypeCmmnConfiguration.captured
                    new.planItemDefinitionID shouldBe original.planItemDefinitionID
                    new.toelichting shouldBe original.toelichting
                }
            }

            And("The zaakbeindiggegevens should get copied") {
                slotPersistZaaktypeCmmnConfiguration.captured.getZaakbeeindigParameters().let {
                    it shouldBeSameSizeAs originalZaaktypeCmmnConfiguration.getZaakbeeindigParameters()
                    it zip originalZaaktypeCmmnConfiguration.getZaakbeeindigParameters()
                }.forEach { (new, original) ->
                    new.id shouldNotBe original.id
                    new.zaaktypeCmmnConfiguration shouldNotBe original.zaaktypeCmmnConfiguration
                    new.zaaktypeCmmnConfiguration shouldBe slotPersistZaaktypeCmmnConfiguration.captured
                    new.resultaattype shouldBe original.resultaattype
                    new.zaakbeeindigReden shouldBe original.zaakbeeindigReden
                }
            }

            And("The mailtemplate koppelingen should get copied") {
                slotPersistZaaktypeCmmnConfiguration.captured.getMailtemplateKoppelingen().let {
                    it shouldBeSameSizeAs originalZaaktypeCmmnConfiguration.getMailtemplateKoppelingen()
                    it zip originalZaaktypeCmmnConfiguration.getMailtemplateKoppelingen()
                }.forEach { (new, original) ->
                    new.id shouldBe null
                    new.zaaktypeCmmnConfiguration shouldNotBe original.zaaktypeCmmnConfiguration
                    new.zaaktypeCmmnConfiguration shouldBe slotPersistZaaktypeCmmnConfiguration.captured
                    new.mailTemplate shouldBe original.mailTemplate
                }
            }

            And("The afzenders should get copied") {
                slotPersistZaaktypeCmmnConfiguration.captured.getZaakAfzenders().let {
                    it shouldBeSameSizeAs originalZaaktypeCmmnConfiguration.getZaakAfzenders()
                    it zip originalZaaktypeCmmnConfiguration.getZaakAfzenders()
                }.forEach { (new, original) ->
                    new.id shouldBe null
                    new.zaaktypeCmmnConfiguration shouldNotBe original.zaaktypeCmmnConfiguration
                    new.zaaktypeCmmnConfiguration shouldBe slotPersistZaaktypeCmmnConfiguration.captured
                    new.defaultMail shouldBe original.defaultMail
                    new.mail shouldBe original.mail
                    new.replyTo shouldBe original.replyTo
                }
            }

            And("The betrokkene koppelingen should get copied") {
                slotPersistZaaktypeCmmnConfiguration.captured.getBetrokkeneParameters().let {
                    it.brpKoppelen shouldBe originalZaaktypeCmmnConfiguration.getBetrokkeneParameters().brpKoppelen
                    it.kvkKoppelen shouldBe originalZaaktypeCmmnConfiguration.getBetrokkeneParameters().kvkKoppelen
                }
            }

            And("The BRP doeleinden should get copied") {
                slotPersistZaaktypeCmmnConfiguration.captured.getBrpParameters().let {
                    it.zoekWaarde shouldBe originalZaaktypeCmmnConfiguration.getBrpParameters().zoekWaarde
                    it.raadpleegWaarde shouldBe originalZaaktypeCmmnConfiguration.getBrpParameters().raadpleegWaarde
                }
            }

            And("The new zaak type is stored") {
                verify {
                    entityManager.persist(any<ZaaktypeCmmnConfiguration>())
                }
            }

            And("Then updated with SmartDocuments mappings") {
                verify {
                    entityManager.merge(any<ZaaktypeCmmnConfiguration>())
                }
            }

            And("The automatic email confirmation should be copied") {
                slotPersistZaaktypeCmmnConfiguration.captured.zaaktypeCmmnEmailParameters.let {
                    it?.enabled shouldBe originalZaaktypeCmmnConfiguration.zaaktypeCmmnEmailParameters?.enabled
                    it?.templateName shouldBe originalZaaktypeCmmnConfiguration.zaaktypeCmmnEmailParameters?.templateName
                    it?.emailSender shouldBe originalZaaktypeCmmnConfiguration.zaaktypeCmmnEmailParameters?.emailSender
                    it?.emailReply shouldBe originalZaaktypeCmmnConfiguration.zaaktypeCmmnEmailParameters?.emailReply
                }
            }
        }
    }

    Given("A zaaktype with existing BPMN process mapping") {
        val zaaktypeCmmnConfiguration = createZaaktypeCmmnConfiguration()
        every {
            zaaktypeBpmnConfigurationService.findConfigurationByZaaktypeUuid(zaaktypeCmmnConfiguration.zaakTypeUUID!!)
        } returns createZaaktypeBpmnConfiguration()

        When("create a zaaktypeCmmnConfiguration is attempted") {
            val exception = shouldThrow<ZaaktypeInUseException> {
                zaaktypeCmmnConfigurationBeheerService.storeZaaktypeCmmnConfiguration(zaaktypeCmmnConfiguration)
            }

            Then("an exception should be thrown") {
                exception.message shouldContain zaaktypeCmmnConfiguration.zaaktypeOmschrijving
            }
        }
    }
})
