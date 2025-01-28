/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.admin

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldBeSameSizeAs
import io.kotest.matchers.collections.shouldContainOnly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
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
import net.atos.client.zgw.util.extractUuid
import net.atos.client.zgw.ztc.ZtcClientService
import net.atos.client.zgw.ztc.model.createResultaatType
import net.atos.client.zgw.ztc.model.createZaakType
import net.atos.zac.admin.model.ZaakafhandelParameters
import net.atos.zac.admin.model.createZaakafhandelParameters
import net.atos.zac.smartdocuments.SmartDocumentsTemplatesService
import net.atos.zac.smartdocuments.rest.RestMappedSmartDocumentsTemplateGroup
import java.net.URI
import java.time.ZonedDateTime
import java.util.Date
import java.util.UUID

class ZaakafhandelParameterBeheerServiceTest : BehaviorSpec({
    val entityManager = mockk<EntityManager>()
    val ztcClientService = mockk<ZtcClientService>()
    val criteriaBuilder = mockk<CriteriaBuilder>()
    val zaakafhandelparametersCriteriaQuery = mockk<CriteriaQuery<ZaakafhandelParameters>>()
    val dateSubquery = mockk<Subquery<Date>>()
    val zaakafhandelparametersTypedQuery = mockk<TypedQuery<ZaakafhandelParameters>>()
    val zaakafhandelparametersRoot = mockk<Root<ZaakafhandelParameters>>()
    val path = mockk<Path<Any>>()
    val pathString = mockk<Path<String>>()
    val predicate = mockk<Predicate>()
    val order = mockk<Order>()
    val expressionString = mockk<Expression<String>>()
    val zaakafhandelParameterService = mockk<ZaakafhandelParameterService>()
    val smartDocumentsTemplatesService = mockk<SmartDocumentsTemplatesService>()

    val zaakafhandelParameterBeheerService = ZaakafhandelParameterBeheerService(
        entityManager = entityManager,
        ztcClientService = ztcClientService,
        zaakafhandelParameterService = zaakafhandelParameterService,
        smartDocumentsTemplatesService = smartDocumentsTemplatesService
    )

    beforeEach {
        checkUnnecessaryStub()
    }

    Given("One zaakafhandelparameters for a given zaaktype UUID") {
        val zaakafhandelparameters = createZaakafhandelParameters()
        val now = ZonedDateTime.now()
        every { ztcClientService.resetCacheTimeToNow() } returns now
        every { entityManager.criteriaBuilder } returns criteriaBuilder
        every {
            criteriaBuilder.createQuery(ZaakafhandelParameters::class.java)
        } returns zaakafhandelparametersCriteriaQuery
        every {
            zaakafhandelparametersCriteriaQuery.from(ZaakafhandelParameters::class.java)
        } returns zaakafhandelparametersRoot
        every {
            zaakafhandelparametersCriteriaQuery.select(zaakafhandelparametersRoot)
        } returns zaakafhandelparametersCriteriaQuery
        every { zaakafhandelparametersRoot.get<Any>("zaakTypeUUID") } returns path
        every { criteriaBuilder.equal(path, zaakafhandelparameters.zaakTypeUUID) } returns predicate
        every { zaakafhandelparametersCriteriaQuery.where(predicate) } returns zaakafhandelparametersCriteriaQuery
        every {
            entityManager.createQuery(zaakafhandelparametersCriteriaQuery).setMaxResults(1).resultList
        } returns listOf(zaakafhandelparameters)

        When("the zaakafhandelparameters are retrieved based on the zaaktypeUUID") {
            val returnedZaakafhandelParameters = zaakafhandelParameterBeheerService.readZaakafhandelParameters(
                zaakafhandelparameters.zaakTypeUUID
            )

            Then("the zaakafhandelparameters should be returned") {
                with(returnedZaakafhandelParameters) {
                    zaakTypeUUID shouldBe zaakafhandelparameters.zaakTypeUUID
                }
            }
        }
    }
    Given("Two zaakafhandelparameters") {
        val zaakafhandelparameters = listOf(
            createZaakafhandelParameters(),
            createZaakafhandelParameters()
        )
        every { entityManager.criteriaBuilder } returns criteriaBuilder
        every {
            criteriaBuilder.createQuery(ZaakafhandelParameters::class.java)
        } returns zaakafhandelparametersCriteriaQuery
        every {
            zaakafhandelparametersCriteriaQuery.from(ZaakafhandelParameters::class.java)
        } returns zaakafhandelparametersRoot
        every {
            zaakafhandelparametersCriteriaQuery.select(zaakafhandelparametersRoot)
        } returns zaakafhandelparametersCriteriaQuery
        every {
            entityManager.createQuery(zaakafhandelparametersCriteriaQuery).resultList
        } returns zaakafhandelparameters
        every { zaakafhandelparametersRoot.get<Any>("id") } returns path
        every { criteriaBuilder.desc(path) } returns order
        every { zaakafhandelparametersCriteriaQuery.orderBy(order) } returns zaakafhandelparametersCriteriaQuery

        When("the zaakafhandelparameters are retrieved based on the zaaktypeUUID") {
            val returnedZaakafhandelParameters = zaakafhandelParameterBeheerService.listZaakafhandelParameters()

            Then("both zaakafhandelparameters should be returned") {
                returnedZaakafhandelParameters.size shouldBe 2
                returnedZaakafhandelParameters.forEachIndexed { index, returnedZaakafhandelparameter ->
                    with(returnedZaakafhandelparameter) {
                        zaakTypeUUID shouldBe zaakafhandelparameters[index].zaakTypeUUID
                        id shouldBe zaakafhandelparameters[index].id
                    }
                }
            }
        }
    }
    Given("Two active zaakafhandelparameters for a given productaanvraagType") {
        val productaanvraagType = "dummyProductaanvraagType"
        val zaakafhandelparametersList = listOf(
            createZaakafhandelParameters(),
            createZaakafhandelParameters()
        )
        with(entityManager) {
            every { createQuery(zaakafhandelparametersCriteriaQuery) } returns zaakafhandelparametersTypedQuery
            every { getCriteriaBuilder() } returns criteriaBuilder
        }
        with(criteriaBuilder) {
            every { createQuery(ZaakafhandelParameters::class.java) } returns zaakafhandelparametersCriteriaQuery
            every { greatest<String>(any()) } returns expressionString
            every { equal(pathString, pathString) } returns predicate
            every { equal(pathString, productaanvraagType) } returns predicate
            every { equal(pathString, dateSubquery) } returns predicate
        }
        every { criteriaBuilder.and(predicate, predicate) } returns predicate
        with(zaakafhandelparametersCriteriaQuery) {
            every { from(ZaakafhandelParameters::class.java) } returns zaakafhandelparametersRoot
            every { subquery(Date::class.java) } returns dateSubquery
            every { select(zaakafhandelparametersRoot) } returns zaakafhandelparametersCriteriaQuery
            every { where(predicate) } returns zaakafhandelparametersCriteriaQuery
        }
        with(dateSubquery) {
            every { from(ZaakafhandelParameters::class.java) } returns mockk {
                every { get<String>("creatiedatum") } returns pathString
                every { get<String>("zaaktypeOmschrijving") } returns pathString
            }
            every { select(any()) } returns dateSubquery
            every { where(predicate) } returns dateSubquery
        }
        with(zaakafhandelparametersRoot) {
            every { get<String>("zaaktypeOmschrijving") } returns pathString
            every { zaakafhandelparametersRoot.get<String>("productaanvraagtype") } returns pathString
            every { zaakafhandelparametersRoot.get<String>("creatiedatum") } returns pathString
        }
        every { zaakafhandelparametersTypedQuery.resultList } returns zaakafhandelparametersList

        When("the active zaakafhandelparameters are retrieved for the given productaanvraagType") {
            val returnedZaakafhandelParameters =
                zaakafhandelParameterBeheerService.findActiveZaakafhandelparametersByProductaanvraagtype(
                    productaanvraagType
                )

            Then("two zaakafhandelparameters should be returned") {
                returnedZaakafhandelParameters.size shouldBe 2
                returnedZaakafhandelParameters.map { productaanvraagType } shouldContainOnly listOf(productaanvraagType)
            }
        }
    }

    Given("A new zaaktype was created and no previous version exists") {
        val zaaktypeUUID = UUID.randomUUID()
        val zaaktypeUri = URI("https://example.com/zaaktypes/$zaaktypeUUID")
        val zaakType = createZaakType(uri = zaaktypeUri, servicenorm = "dummyServiceNorm", concept = false)

        every { zaakafhandelParameterService.clearListCache() } returns "Cache cleared"

        // ZtcClientService mocking
        every { ztcClientService.clearZaaktypeCache() } returns "Cache cleared"
        every { ztcClientService.readZaaktype(zaaktypeUri) } returns zaakType

        // Relaxed entity manager mocking; criteria queries and persisting
        val criteriaQuery = mockk<CriteriaQuery<ZaakafhandelParameters>>(relaxed = true)
        every { entityManager.criteriaBuilder } returns mockk(relaxed = true) {
            every { createQuery(ZaakafhandelParameters::class.java) } returns criteriaQuery
        }
        val slotPersistZaakafhandelParameters = slot<ZaakafhandelParameters>()

        every {
            entityManager.persist(capture(slotPersistZaakafhandelParameters))
        } answers { ZaakafhandelParameters() }

        every { entityManager.createQuery(criteriaQuery) } returns mockk {
            every { setMaxResults(1) } returns this
            every { resultList } returns emptyList()
        }

        When("Publishing a new zaaktype") {
            zaakafhandelParameterBeheerService.upsertZaakafhandelParameters(zaaktypeUri)

            Then("The new zaak type is stored") {
                verify {
                    entityManager.persist(any<ZaakafhandelParameters>())
                }
            }
        }
    }

    Given("A zaaktype that has been updated") {
        val zaaktypeUUID = UUID.randomUUID()
        val zaaktypeUri = URI("https://example.com/zaaktypes/$zaaktypeUUID")
        val zaakType = createZaakType(uri = zaaktypeUri, servicenorm = "dummyServiceNorm", concept = false)

        every { zaakafhandelParameterService.clearListCache() } returns "Cache cleared"

        // ZtcClientService mocking
        every { ztcClientService.clearZaaktypeCache() } returns "Cache cleared"
        every { ztcClientService.readZaaktype(zaaktypeUri) } returns zaakType
        every { ztcClientService.readResultaattype(any<URI>()) } returns createResultaatType()

        // Relaxed entity manager mocking; criteria queries and persisting
        val criteriaQuery = mockk<CriteriaQuery<ZaakafhandelParameters>>(relaxed = true)
        every { entityManager.criteriaBuilder } returns mockk(relaxed = true) {
            every { createQuery(ZaakafhandelParameters::class.java) } returns criteriaQuery
        }
        val zaakafhandelParamentersId = 100L
        val originalZaakafhandelParameters = createZaakafhandelParameters(
            id = zaakafhandelParamentersId,
            zaaktypeUUID = zaaktypeUUID,
        )

        val slotPersistZaakafhandelParameters = slot<ZaakafhandelParameters>()

        When("Processing the updated zaaktype") {
            every { entityManager.createQuery(criteriaQuery) } returns mockk {
                every { setMaxResults(1) } returns this
                every { resultList } returns listOf(originalZaakafhandelParameters)
            }

            every {
                entityManager.merge(capture(slotPersistZaakafhandelParameters))
            } answers { ZaakafhandelParameters() }

            zaakafhandelParameterBeheerService.upsertZaakafhandelParameters(zaaktypeUri)

            Then("The related zaakafhandelparameters is stored through the entity manager") {
                slotPersistZaakafhandelParameters.isCaptured shouldBe true
                verify {
                    entityManager.merge(any<ZaakafhandelParameters>())
                }
            }

            And("The zaaktype values have been copied into the zaakparameters") {
                with(slotPersistZaakafhandelParameters.captured) {
                    id shouldBe zaakafhandelParamentersId
                    zaakTypeUUID shouldBe zaakType.url.extractUuid()
                    zaaktypeOmschrijving shouldBe zaakType.omschrijving
                }
            }
        }

        When("Publishing a new zaaktype") {
            every {
                entityManager.persist(capture(slotPersistZaakafhandelParameters))
            } answers { ZaakafhandelParameters() }

            every { entityManager.createQuery(criteriaQuery) } returns mockk {
                every { setMaxResults(1) } returns this
                every { resultList } returns emptyList() andThen listOf(originalZaakafhandelParameters)
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

            zaakafhandelParameterBeheerService.upsertZaakafhandelParameters(zaaktypeUri)

            Then("The zaaktype simple values have been copied from the original") {
                with(slotPersistZaakafhandelParameters.captured) {
                    zaakTypeUUID shouldBe zaakType.url.extractUuid()
                    zaaktypeOmschrijving shouldBe zaakType.omschrijving
                    caseDefinitionID shouldBe originalZaakafhandelParameters.caseDefinitionID
                    groepID shouldBe originalZaakafhandelParameters.groepID
                    gebruikersnaamMedewerker shouldBe originalZaakafhandelParameters.gebruikersnaamMedewerker
                    einddatumGeplandWaarschuwing shouldBe originalZaakafhandelParameters.einddatumGeplandWaarschuwing
                    uiterlijkeEinddatumAfdoeningWaarschuwing shouldBe originalZaakafhandelParameters
                        .uiterlijkeEinddatumAfdoeningWaarschuwing
                    intakeMail shouldBe originalZaakafhandelParameters.intakeMail
                    afrondenMail shouldBe originalZaakafhandelParameters.afrondenMail
                    productaanvraagtype shouldBe originalZaakafhandelParameters.productaanvraagtype
                    domein shouldBe originalZaakafhandelParameters.domein
                    isSmartDocumentsIngeschakeld shouldBe originalZaakafhandelParameters.isSmartDocumentsIngeschakeld
                }
            }

            And("The human task parameters should have been cloned") {
                slotPersistZaakafhandelParameters.captured.humanTaskParametersCollection.let {
                    it shouldBeSameSizeAs originalZaakafhandelParameters.humanTaskParametersCollection
                    it zip originalZaakafhandelParameters.humanTaskParametersCollection
                }.forEach { (new, original) ->
                    new.id shouldNotBe original.id
                    new.zaakafhandelParameters shouldNotBe original.zaakafhandelParameters
                    new.zaakafhandelParameters shouldBe slotPersistZaakafhandelParameters.captured
                    new.groepID shouldNotBe original.groepID
                    new.isActief shouldBe original.isActief
                    new.doorlooptijd shouldBe original.doorlooptijd
                    new.formulierDefinitieID shouldBe original.formulierDefinitieID
                    new.doorlooptijd shouldBe original.doorlooptijd
                    new.planItemDefinitionID shouldBe original.planItemDefinitionID
                }
            }

            And("The user event listener parameters should get copied") {
                slotPersistZaakafhandelParameters.captured.userEventListenerParametersCollection.let {
                    it shouldBeSameSizeAs originalZaakafhandelParameters.userEventListenerParametersCollection
                    it zip originalZaakafhandelParameters.userEventListenerParametersCollection
                }.forEach { (new, original) ->
                    new.id shouldNotBe original.id
                    new.zaakafhandelParameters shouldNotBe original.zaakafhandelParameters
                    new.zaakafhandelParameters shouldBe slotPersistZaakafhandelParameters.captured
                    new.planItemDefinitionID shouldBe original.planItemDefinitionID
                    new.toelichting shouldBe original.toelichting
                }
            }

            And("The zaakbeindiggegevens should get copied") {
                slotPersistZaakafhandelParameters.captured.zaakbeeindigParameters.let {
                    it shouldBeSameSizeAs originalZaakafhandelParameters.zaakbeeindigParameters
                    it zip originalZaakafhandelParameters.zaakbeeindigParameters
                }.forEach { (new, original) ->
                    new.id shouldNotBe original.id
                    new.zaakafhandelParameters shouldNotBe original.zaakafhandelParameters
                    new.zaakafhandelParameters shouldBe slotPersistZaakafhandelParameters.captured
                    new.resultaattype shouldBe original.resultaattype
                    new.zaakbeeindigReden shouldBe original.zaakbeeindigReden
                }
            }

            And("The mailtemplate koppelingen should get copied") {
                slotPersistZaakafhandelParameters.captured.mailtemplateKoppelingen.let {
                    it shouldBeSameSizeAs originalZaakafhandelParameters.mailtemplateKoppelingen
                    it zip originalZaakafhandelParameters.mailtemplateKoppelingen
                }.forEach { (new, original) ->
                    new.id shouldNotBe original.id
                    new.zaakafhandelParameters shouldNotBe original.zaakafhandelParameters
                    new.zaakafhandelParameters shouldBe slotPersistZaakafhandelParameters.captured
                    new.mailTemplate shouldBe original.mailTemplate
                }
            }

            And("The afzenders should get copied") {
                slotPersistZaakafhandelParameters.captured.zaakAfzenders.let {
                    it shouldBeSameSizeAs originalZaakafhandelParameters.zaakAfzenders
                    it zip originalZaakafhandelParameters.zaakAfzenders
                }.forEach { (new, original) ->
                    new.id shouldNotBe original.id
                    new.zaakafhandelParameters shouldNotBe original.zaakafhandelParameters
                    new.zaakafhandelParameters shouldBe slotPersistZaakafhandelParameters.captured
                    new.isDefault shouldBe original.isDefault
                    new.mail shouldBe original.mail
                    new.replyTo shouldBe original.replyTo
                }
            }

            And("The new zaak type is stored") {
                verify {
                    entityManager.persist(any<ZaakafhandelParameters>())
                }
            }

            And("Then updated with SmartDocuments mappings") {
                verify {
                    entityManager.merge(any<ZaakafhandelParameters>())
                }
            }
        }
    }
})
