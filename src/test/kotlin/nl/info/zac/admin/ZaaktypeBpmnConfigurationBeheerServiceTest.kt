/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.admin

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainAllInAnyOrder
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import jakarta.persistence.EntityManager
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.Order
import jakarta.persistence.criteria.Path
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.model.createResultaatType
import nl.info.client.zgw.ztc.model.createZaakType
import nl.info.client.zgw.ztc.model.generated.ZaakType
import nl.info.zac.admin.model.ZaaktypeBpmnConfiguration
import nl.info.zac.admin.model.ZaaktypeBpmnConfiguration.Companion.BPMN_PROCESS_DEFINITION_KEY
import nl.info.zac.admin.model.ZaaktypeConfiguration.Companion.CREATIEDATUM_VARIABLE_NAME
import nl.info.zac.admin.model.ZaaktypeConfiguration.Companion.PRODUCTAANVRAAGTYPE_VARIABLE_NAME
import nl.info.zac.admin.model.ZaaktypeConfiguration.Companion.ZAAKTYPE_OMSCHRIJVING_VARIABLE_NAME
import nl.info.zac.admin.model.ZaaktypeConfiguration.Companion.ZAAKTYPE_UUID_VARIABLE_NAME
import nl.info.zac.admin.model.createBetrokkeneKoppelingen
import nl.info.zac.admin.model.createZaakbeeindigReden
import nl.info.zac.admin.model.createZaaktypeBpmnConfiguration
import nl.info.zac.admin.model.createZaaktypeBrpParameters
import nl.info.zac.admin.model.createZaaktypeCompletionParameters
import nl.info.zac.smartdocuments.SmartDocumentsTemplatesService
import java.net.URI
import java.util.UUID
import kotlin.jvm.optionals.getOrNull

class ZaaktypeBpmnConfigurationBeheerServiceTest : BehaviorSpec({
    val entityManager = mockk<EntityManager>()
    val criteriaBuilder = mockk<CriteriaBuilder>()
    val zaaktypeBpmnConfigurationCriteriaQuery = mockk<CriteriaQuery<ZaaktypeBpmnConfiguration>>()
    val zaaktypeBpmnConfigurationRoot = mockk<Root<ZaaktypeBpmnConfiguration>>()
    val predicate = mockk<Predicate>()
    val pathUuid = mockk<Path<UUID>>()
    val pathString = mockk<Path<String>>()
    val pathProductAanvraagType = mockk<Path<String>>()
    val pathCreatieDatum = mockk<Path<Any>>()
    val creatieDatumOrder = mockk<Order>()

    val smartDocumentsTemplatesService = mockk<SmartDocumentsTemplatesService>()
    val ztcClientService = mockk<ZtcClientService>()
    val zaaktypeBpmnConfigurationBeheerService = ZaaktypeBpmnConfigurationBeheerService(
        entityManager,
        smartDocumentsTemplatesService,
        ZaaktypeHelperService(ztcClientService)
    )

    afterEach {
        checkUnnecessaryStub()
    }

    fun resultaattypeUri() = URI("https://example.com/resultaattype/${UUID.randomUUID()}")

    given("Multiple zaaktypeBpmnConfigurations with two unique BPMN process definition keys") {
        val uniqueBpmnProcessDefinitionKeys = listOf("fakeBpmnProcessDefinitionKey", "fakeBpmnProcessDefinitionKey2")
        val stringCriteriaQuery = mockk<CriteriaQuery<String>>()
        val bpmnProcessDefinitionKeyPath = mockk<Path<String>>()
        every { entityManager.criteriaBuilder } returns criteriaBuilder
        every { criteriaBuilder.createQuery(String::class.java) } returns stringCriteriaQuery
        every {
            stringCriteriaQuery.from(ZaaktypeBpmnConfiguration::class.java)
        } returns zaaktypeBpmnConfigurationRoot
        every {
            zaaktypeBpmnConfigurationRoot.get<String>(BPMN_PROCESS_DEFINITION_KEY)
        } returns bpmnProcessDefinitionKeyPath
        every {
            stringCriteriaQuery.select(bpmnProcessDefinitionKeyPath)
        } returns stringCriteriaQuery
        every { stringCriteriaQuery.distinct(true) } returns stringCriteriaQuery
        every {
            entityManager.createQuery(stringCriteriaQuery).resultList
        } returns uniqueBpmnProcessDefinitionKeys

        `when`("Returning the unique BPMN process definition keys") {
            val keys = zaaktypeBpmnConfigurationBeheerService.findUniqueBpmnProcessDefinitionKeysFromZaaktypeConfigurations()

            then("Gives a list of two unique BPMN process definition keys") {
                keys shouldContainAllInAnyOrder uniqueBpmnProcessDefinitionKeys
            }
        }
    }

    given("One zaaktypeBpmnConfiguration for a given zaaktype omschrijving") {
        val zaaktypeBpmnConfiguration = createZaaktypeBpmnConfiguration()
        every { entityManager.criteriaBuilder } returns criteriaBuilder
        every {
            criteriaBuilder.createQuery(ZaaktypeBpmnConfiguration::class.java)
        } returns zaaktypeBpmnConfigurationCriteriaQuery
        every {
            zaaktypeBpmnConfigurationCriteriaQuery.from(ZaaktypeBpmnConfiguration::class.java)
        } returns zaaktypeBpmnConfigurationRoot
        every {
            zaaktypeBpmnConfigurationCriteriaQuery.select(zaaktypeBpmnConfigurationRoot)
        } returns zaaktypeBpmnConfigurationCriteriaQuery
        every { zaaktypeBpmnConfigurationRoot.get<String>(ZAAKTYPE_OMSCHRIJVING_VARIABLE_NAME) } returns pathString
        every { zaaktypeBpmnConfigurationRoot.get<Any>(CREATIEDATUM_VARIABLE_NAME) } returns pathCreatieDatum
        every { criteriaBuilder.equal(pathString, zaaktypeBpmnConfiguration.zaaktypeOmschrijving) } returns predicate
        every { zaaktypeBpmnConfigurationCriteriaQuery.where(predicate) } returns zaaktypeBpmnConfigurationCriteriaQuery
        every {
            entityManager.createQuery(zaaktypeBpmnConfigurationCriteriaQuery).setMaxResults(1).resultStream.findFirst()
                .getOrNull()
        } returns zaaktypeBpmnConfiguration
        every { criteriaBuilder.desc(pathCreatieDatum) } returns creatieDatumOrder
        every { zaaktypeBpmnConfigurationCriteriaQuery.orderBy(creatieDatumOrder) } returns zaaktypeBpmnConfigurationCriteriaQuery

        `when`("Finding the configuration by zaaktype omschrijving") {
            val foundConfiguration =
                zaaktypeBpmnConfigurationBeheerService.findConfiguration(zaaktypeBpmnConfiguration.zaaktypeOmschrijving)
            then("The correct configuration is returned") {
                foundConfiguration shouldBe zaaktypeBpmnConfiguration
            }
        }
    }

    context("Creating a zaaktype - BPMN process definition (no id)") {
        given("A zaaktype - BPMN process definition relation") {
            val zaaktypeBpmnProcessDefinition = createZaaktypeBpmnConfiguration(id = null)
            every { entityManager.persist(zaaktypeBpmnProcessDefinition) } just Runs
            every { entityManager.flush() } just Runs
            every {
                zaaktypeBpmnConfigurationBeheerService.findConfiguration(
                    zaaktypeBpmnProcessDefinition.zaaktypeUuid
                )
            } returns zaaktypeBpmnProcessDefinition

            `when`("the zaaktype BPMN process definition relation is created") {
                zaaktypeBpmnConfigurationBeheerService.storeConfiguration(zaaktypeBpmnProcessDefinition)

                then("the zaaktype BPMN process definition relation is persisted") {
                    verify(exactly = 1) {
                        entityManager.persist(zaaktypeBpmnProcessDefinition)
                        entityManager.flush()
                    }
                }
            }
        }
    }

    context("Updating a zaaktype - BPMN process definition (existing id)") {
        given("An existing zaaktype - BPMN process definition relation") {
            val zaaktypeBpmnProcessDefinition = createZaaktypeBpmnConfiguration()
            every {
                zaaktypeBpmnConfigurationBeheerService.findConfiguration(
                    zaaktypeBpmnProcessDefinition.zaaktypeUuid
                )
            } returns zaaktypeBpmnProcessDefinition
            every { entityManager.merge(zaaktypeBpmnProcessDefinition) } returns zaaktypeBpmnProcessDefinition

            `when`("the zaaktype BPMN process definition relation is created") {
                zaaktypeBpmnConfigurationBeheerService.storeConfiguration(zaaktypeBpmnProcessDefinition)

                then("the zaaktype BPMN process definition relation is persisted") {
                    verify(exactly = 1) {
                        entityManager.merge(zaaktypeBpmnProcessDefinition)
                    }
                }
            }
        }

        given("A 'pristine' zaaktype - BPMN process definition relation") {
            val cmmnId = 1L
            val zaaktypeBpmnProcessDefinition = createZaaktypeBpmnConfiguration(id = cmmnId)
            every {
                zaaktypeBpmnConfigurationBeheerService.findConfiguration(zaaktypeBpmnProcessDefinition.zaaktypeUuid)
            } returns null andThen zaaktypeBpmnProcessDefinition
            val zaaktypeBpmnConfigurationSlot = slot<ZaaktypeBpmnConfiguration>()
            every { entityManager.persist(capture(zaaktypeBpmnConfigurationSlot)) } just Runs
            every { entityManager.flush() } just Runs

            `when`("the zaaktype BPMN process definition relation is created") {
                zaaktypeBpmnConfigurationBeheerService.storeConfiguration(zaaktypeBpmnProcessDefinition)

                then("the zaaktype BPMN process definition relation is persisted") {
                    verify(exactly = 1) {
                        entityManager.persist(zaaktypeBpmnProcessDefinition)
                        entityManager.flush()
                    }
                }

                and("the ID was reset") {
                    zaaktypeBpmnConfigurationSlot.captured.id shouldBe null
                }
            }
        }
    }

    context("Deleting a zaaktype - BPMN process definition") {
        given("A stored zaaktype BPMN process definition relation") {
            val zaaktypeBpmnProcessDefinition = createZaaktypeBpmnConfiguration()
            every { entityManager.remove(zaaktypeBpmnProcessDefinition) } just Runs

            `when`("the zaaktype BPMN process definition relation is deleted") {
                zaaktypeBpmnConfigurationBeheerService.deleteConfiguration(zaaktypeBpmnProcessDefinition)

                then("the zaaktype BPMN process definition relation is removed") {
                    verify(exactly = 1) {
                        entityManager.remove(zaaktypeBpmnProcessDefinition)
                    }
                }
            }
        }
    }

    context("Finding a BPMN process definition by zaaktype UUID") {
        given("A valid zaaktype UUID with a corresponding BPMN process definition") {
            val zaaktypeUUID = UUID.randomUUID()
            val zaaktypeBpmnProcessDefinition = createZaaktypeBpmnConfiguration(zaaktypeUUID = zaaktypeUUID)
            every { entityManager.criteriaBuilder } returns criteriaBuilder
            every {
                criteriaBuilder.createQuery(ZaaktypeBpmnConfiguration::class.java)
            } returns zaaktypeBpmnConfigurationCriteriaQuery
            every {
                zaaktypeBpmnConfigurationCriteriaQuery.from(ZaaktypeBpmnConfiguration::class.java)
            } returns zaaktypeBpmnConfigurationRoot
            every {
                zaaktypeBpmnConfigurationCriteriaQuery.select(zaaktypeBpmnConfigurationRoot)
            } returns zaaktypeBpmnConfigurationCriteriaQuery
            every { zaaktypeBpmnConfigurationCriteriaQuery.where(predicate) } returns zaaktypeBpmnConfigurationCriteriaQuery
            every { criteriaBuilder.equal(pathUuid, zaaktypeUUID) } returns predicate
            every { zaaktypeBpmnConfigurationRoot.get<UUID>(ZAAKTYPE_UUID_VARIABLE_NAME) } returns pathUuid
            every { zaaktypeBpmnConfigurationRoot.get<Any>(CREATIEDATUM_VARIABLE_NAME) } returns pathCreatieDatum
            every { criteriaBuilder.desc(pathCreatieDatum) } returns creatieDatumOrder
            every { zaaktypeBpmnConfigurationCriteriaQuery.orderBy(creatieDatumOrder) } returns zaaktypeBpmnConfigurationCriteriaQuery
            every {
                entityManager.createQuery(zaaktypeBpmnConfigurationCriteriaQuery).setMaxResults(1).resultStream.findFirst().getOrNull()
            } returns zaaktypeBpmnProcessDefinition

            `when`("finding the BPMN process definition by zaaktype UUID") {
                val result =
                    zaaktypeBpmnConfigurationBeheerService.findConfiguration(zaaktypeUUID)

                then("the BPMN process definition is returned") {
                    result shouldBe zaaktypeBpmnProcessDefinition
                }
            }
        }
        given("A valid zaaktype UUID without a corresponding BPMN process definition") {
            val zaaktypeUUID = UUID.randomUUID()
            every { entityManager.criteriaBuilder } returns criteriaBuilder
            every {
                criteriaBuilder.createQuery(ZaaktypeBpmnConfiguration::class.java)
            } returns zaaktypeBpmnConfigurationCriteriaQuery
            every {
                zaaktypeBpmnConfigurationCriteriaQuery.from(ZaaktypeBpmnConfiguration::class.java)
            } returns zaaktypeBpmnConfigurationRoot
            every {
                zaaktypeBpmnConfigurationCriteriaQuery.select(zaaktypeBpmnConfigurationRoot)
            } returns zaaktypeBpmnConfigurationCriteriaQuery
            every { zaaktypeBpmnConfigurationCriteriaQuery.where(predicate) } returns zaaktypeBpmnConfigurationCriteriaQuery
            every { criteriaBuilder.equal(pathUuid, zaaktypeUUID) } returns predicate
            every { zaaktypeBpmnConfigurationRoot.get<UUID>(ZAAKTYPE_UUID_VARIABLE_NAME) } returns pathUuid
            every { zaaktypeBpmnConfigurationRoot.get<Any>(CREATIEDATUM_VARIABLE_NAME) } returns pathCreatieDatum
            every { criteriaBuilder.desc(pathCreatieDatum) } returns creatieDatumOrder
            every { zaaktypeBpmnConfigurationCriteriaQuery.orderBy(creatieDatumOrder) } returns zaaktypeBpmnConfigurationCriteriaQuery
            every {
                entityManager.createQuery(zaaktypeBpmnConfigurationCriteriaQuery).setMaxResults(1).resultStream.findFirst().getOrNull()
            } returns null

            `when`("finding the BPMN process definition by zaaktype UUID") {
                val result =
                    zaaktypeBpmnConfigurationBeheerService.findConfiguration(zaaktypeUUID)

                then("null is returned") {
                    result shouldBe null
                }
            }
        }
    }

    context("Finding a BPMN process definition by productaanvraagtype") {
        given("A productaanvraagtype with a corresponding BPMN process definition") {
            val productAanvraagType = "fakeProductaanvraagtype"
            val definition = createZaaktypeBpmnConfiguration()
            every { entityManager.criteriaBuilder } returns criteriaBuilder
            every {
                criteriaBuilder.createQuery(ZaaktypeBpmnConfiguration::class.java)
            } returns zaaktypeBpmnConfigurationCriteriaQuery
            every {
                zaaktypeBpmnConfigurationCriteriaQuery.from(ZaaktypeBpmnConfiguration::class.java)
            } returns zaaktypeBpmnConfigurationRoot
            every {
                zaaktypeBpmnConfigurationCriteriaQuery.select(zaaktypeBpmnConfigurationRoot)
            } returns zaaktypeBpmnConfigurationCriteriaQuery
            every { zaaktypeBpmnConfigurationCriteriaQuery.where(predicate) } returns zaaktypeBpmnConfigurationCriteriaQuery
            every { zaaktypeBpmnConfigurationRoot.get<String>(PRODUCTAANVRAAGTYPE_VARIABLE_NAME) } returns pathProductAanvraagType
            every { criteriaBuilder.equal(pathProductAanvraagType, productAanvraagType) } returns predicate
            every { zaaktypeBpmnConfigurationRoot.get<Any>(CREATIEDATUM_VARIABLE_NAME) } returns pathCreatieDatum
            every { criteriaBuilder.desc(pathCreatieDatum) } returns creatieDatumOrder
            every { zaaktypeBpmnConfigurationCriteriaQuery.orderBy(creatieDatumOrder) } returns zaaktypeBpmnConfigurationCriteriaQuery
            every {
                entityManager.createQuery(zaaktypeBpmnConfigurationCriteriaQuery).setMaxResults(1).resultStream.findFirst().getOrNull()
            } returns definition

            `when`("finding the BPMN process definition by productaanvraagtype") {
                val result =
                    zaaktypeBpmnConfigurationBeheerService.findConfigurationByProductAanvraagType(
                        productAanvraagType
                    )

                then("the BPMN process definition is returned") {
                    result shouldBe definition
                }
            }
        }

        given("A productaanvraagtype without a corresponding BPMN process definition") {
            val productAanvraagType = "notExistingProductaanvraagtype"
            every { entityManager.criteriaBuilder } returns criteriaBuilder
            every {
                criteriaBuilder.createQuery(ZaaktypeBpmnConfiguration::class.java)
            } returns zaaktypeBpmnConfigurationCriteriaQuery
            every {
                zaaktypeBpmnConfigurationCriteriaQuery.from(ZaaktypeBpmnConfiguration::class.java)
            } returns zaaktypeBpmnConfigurationRoot
            every {
                zaaktypeBpmnConfigurationCriteriaQuery.select(zaaktypeBpmnConfigurationRoot)
            } returns zaaktypeBpmnConfigurationCriteriaQuery
            every { zaaktypeBpmnConfigurationCriteriaQuery.where(predicate) } returns zaaktypeBpmnConfigurationCriteriaQuery
            every { zaaktypeBpmnConfigurationRoot.get<String>(PRODUCTAANVRAAGTYPE_VARIABLE_NAME) } returns pathProductAanvraagType
            every { criteriaBuilder.equal(pathProductAanvraagType, productAanvraagType) } returns predicate
            every { zaaktypeBpmnConfigurationRoot.get<Any>(CREATIEDATUM_VARIABLE_NAME) } returns pathCreatieDatum
            every { criteriaBuilder.desc(pathCreatieDatum) } returns creatieDatumOrder
            every { zaaktypeBpmnConfigurationCriteriaQuery.orderBy(creatieDatumOrder) } returns zaaktypeBpmnConfigurationCriteriaQuery
            every {
                entityManager.createQuery(zaaktypeBpmnConfigurationCriteriaQuery).setMaxResults(1).resultStream.findFirst().getOrNull()
            } returns null

            `when`("finding the BPMN process definition by productaanvraagtype") {
                val result = zaaktypeBpmnConfigurationBeheerService.findConfigurationByProductAanvraagType(
                    productAanvraagType
                )

                then("null is returned") {
                    result shouldBe null
                }
            }
        }
    }

    context("listing all BPMN definitions") {
        given("Listing all BPMN process definitions when definitions exist") {
            val definition1 = createZaaktypeBpmnConfiguration()
            val definition2 = createZaaktypeBpmnConfiguration()
            every { entityManager.criteriaBuilder } returns criteriaBuilder
            every {
                criteriaBuilder.createQuery(ZaaktypeBpmnConfiguration::class.java)
            } returns zaaktypeBpmnConfigurationCriteriaQuery
            every {
                zaaktypeBpmnConfigurationCriteriaQuery.from(ZaaktypeBpmnConfiguration::class.java)
            } returns zaaktypeBpmnConfigurationRoot
            every { entityManager.createQuery(zaaktypeBpmnConfigurationCriteriaQuery).resultList } returns listOf(definition1, definition2)

            `when`("listing BPMN process definitions") {
                val result = zaaktypeBpmnConfigurationBeheerService.listConfigurations()

                then("a list with all BPMN process definitions is returned") {
                    result shouldBe listOf(definition1, definition2)
                }
            }
        }

        given("Listing all BPMN process definitions when none exist") {
            every { entityManager.criteriaBuilder } returns criteriaBuilder
            every {
                criteriaBuilder.createQuery(ZaaktypeBpmnConfiguration::class.java)
            } returns zaaktypeBpmnConfigurationCriteriaQuery
            every {
                zaaktypeBpmnConfigurationCriteriaQuery.from(ZaaktypeBpmnConfiguration::class.java)
            } returns zaaktypeBpmnConfigurationRoot
            every { entityManager.createQuery(zaaktypeBpmnConfigurationCriteriaQuery).resultList } returns emptyList()

            `when`("listing BPMN process definitions") {
                val result = zaaktypeBpmnConfigurationBeheerService.listConfigurations()

                then("an empty list is returned") {
                    result shouldBe emptyList()
                }
            }
        }
    }

    context("upserting the configuration for a zaaktype") {
        fun stubConfigurationQueries(
            zaakType: ZaakType,
            results: List<ZaaktypeBpmnConfiguration?>,
            previousVersionIsLookedUp: Boolean = true
        ) {
            every { entityManager.criteriaBuilder } returns criteriaBuilder
            every {
                criteriaBuilder.createQuery(ZaaktypeBpmnConfiguration::class.java)
            } returns zaaktypeBpmnConfigurationCriteriaQuery
            every {
                zaaktypeBpmnConfigurationCriteriaQuery.from(ZaaktypeBpmnConfiguration::class.java)
            } returns zaaktypeBpmnConfigurationRoot
            every {
                zaaktypeBpmnConfigurationCriteriaQuery.select(zaaktypeBpmnConfigurationRoot)
            } returns zaaktypeBpmnConfigurationCriteriaQuery
            every { zaaktypeBpmnConfigurationCriteriaQuery.where(predicate) } returns zaaktypeBpmnConfigurationCriteriaQuery
            every { criteriaBuilder.equal(pathUuid, zaakType.url.extractUuid()) } returns predicate
            every { zaaktypeBpmnConfigurationRoot.get<UUID>(ZAAKTYPE_UUID_VARIABLE_NAME) } returns pathUuid
            if (previousVersionIsLookedUp) {
                every { criteriaBuilder.equal(pathString, zaakType.omschrijving) } returns predicate
                every {
                    zaaktypeBpmnConfigurationRoot.get<String>(ZAAKTYPE_OMSCHRIJVING_VARIABLE_NAME)
                } returns pathString
            }
            every { zaaktypeBpmnConfigurationRoot.get<Any>(CREATIEDATUM_VARIABLE_NAME) } returns pathCreatieDatum
            every { criteriaBuilder.desc(pathCreatieDatum) } returns creatieDatumOrder
            every { zaaktypeBpmnConfigurationCriteriaQuery.orderBy(creatieDatumOrder) } returns zaaktypeBpmnConfigurationCriteriaQuery
            every {
                entityManager.createQuery(zaaktypeBpmnConfigurationCriteriaQuery).setMaxResults(1).resultStream.findFirst().getOrNull()
            } returnsMany results
        }

        given("no configuration for the zaaktype nor for a previous version of it") {
            val zaakType = createZaakType()
            stubConfigurationQueries(zaakType, listOf(null, null))

            `when`("upserting the configuration") {
                zaaktypeBpmnConfigurationBeheerService.upsertConfiguration(zaakType)

                then("nothing is stored") {
                    verify(exactly = 0) { entityManager.persist(any<Any>()) }
                    verify(exactly = 0) { entityManager.merge(any<Any>()) }
                }
            }
        }

        given("a configuration for a previous version of the zaaktype whose resultaattypen are not the first ones of the new version") {
            val previousToegekendResultaattypeUuid = UUID.randomUUID()
            val previousNietOntvankelijkResultaattypeUuid = UUID.randomUUID()
            val previousVervallenResultaattypeUuid = UUID.randomUUID()
            val newVerlengdResultaattypeUri = resultaattypeUri()
            val newToegekendResultaattypeUri = resultaattypeUri()
            val newNietOntvankelijkResultaattypeUri = resultaattypeUri()
            val zaakType = createZaakType(
                resultTypes = listOf(
                    newVerlengdResultaattypeUri,
                    newToegekendResultaattypeUri,
                    newNietOntvankelijkResultaattypeUri
                )
            )
            val newZaaktypeUuid = zaakType.url.extractUuid()
            val previousConfiguration = createZaaktypeBpmnConfiguration(
                zaaktypeBrpParameters = createZaaktypeBrpParameters(raadpleegWaarde = "fakeRaadpleegWaarde"),
                zaaktypeBetrokkeneParameters = createBetrokkeneKoppelingen(brpKoppelen = false),
                nietOntvankelijkResultaattype = previousNietOntvankelijkResultaattypeUuid,
                zaaktypeCompletionParameters = setOf(
                    createZaaktypeCompletionParameters(
                        zaakbeeindigReden = createZaakbeeindigReden(name = "fakeZaakbeeindigReden"),
                        resultaattype = previousToegekendResultaattypeUuid
                    ),
                    createZaaktypeCompletionParameters(
                        id = 5678L,
                        zaakbeeindigReden = createZaakbeeindigReden(id = 5678L, name = "fakeVervallenReden"),
                        resultaattype = previousVervallenResultaattypeUuid
                    )
                ),
                groupId = "fakeGroupId",
                productaanvraagtype = "fakeProductaanvraagtype",
                bpmnProcessDefinitionKey = "fakeBpmnProcessDefinitionKey"
            )
            stubConfigurationQueries(zaakType, listOf(null, previousConfiguration, previousConfiguration))
            every { ztcClientService.readResultaattype(newVerlengdResultaattypeUri) } returns
                createResultaatType(url = newVerlengdResultaattypeUri, omschrijving = "Verlengd")
            every { ztcClientService.readResultaattype(newToegekendResultaattypeUri) } returns
                createResultaatType(url = newToegekendResultaattypeUri, omschrijving = "Toegekend")
            every { ztcClientService.readResultaattype(newNietOntvankelijkResultaattypeUri) } returns
                createResultaatType(url = newNietOntvankelijkResultaattypeUri, omschrijving = "Niet ontvankelijk")
            every { ztcClientService.readResultaattype(previousToegekendResultaattypeUuid) } returns
                createResultaatType(omschrijving = "Toegekend")
            every { ztcClientService.readResultaattype(previousNietOntvankelijkResultaattypeUuid) } returns
                createResultaatType(omschrijving = "Niet ontvankelijk")
            every { ztcClientService.readResultaattype(previousVervallenResultaattypeUuid) } returns
                createResultaatType(omschrijving = "Vervallen in de nieuwe versie")

            val configurationSlot = slot<ZaaktypeBpmnConfiguration>()
            every { entityManager.persist(capture(configurationSlot)) } just Runs
            every { entityManager.flush() } just Runs
            every { smartDocumentsTemplatesService.copySmartDocumentsTemplateMappings(any(), any()) } just Runs

            `when`("upserting the configuration") {
                zaaktypeBpmnConfigurationBeheerService.upsertConfiguration(zaakType)

                then("the resultaattypen are matched by omschrijving onto those of the new zaaktype") {
                    with(configurationSlot.captured) {
                        zaaktypeUuid shouldBe newZaaktypeUuid
                        nietOntvankelijkResultaattype shouldBe newNietOntvankelijkResultaattypeUri.extractUuid()
                        getZaakbeeindigParameters().map { it.resultaattype } shouldBe
                            listOf(newToegekendResultaattypeUri.extractUuid())
                    }
                }

                and("the configuration data shared with CMMN configurations is copied") {
                    with(configurationSlot.captured) {
                        bpmnProcessDefinitionKey shouldBe "fakeBpmnProcessDefinitionKey"
                        groepID shouldBe "fakeGroupId"
                        productaanvraagtype shouldBe "fakeProductaanvraagtype"
                        with(zaaktypeBetrokkeneParameters!!) {
                            kvkKoppelen shouldBe true
                            brpKoppelen shouldBe false
                        }
                        zaaktypeBrpParameters!!.raadpleegWaarde shouldBe "fakeRaadpleegWaarde"
                    }
                }

                and("the SmartDocuments template mappings are copied onto the new zaaktype version") {
                    verify(exactly = 1) {
                        smartDocumentsTemplatesService.copySmartDocumentsTemplateMappings(
                            previousConfiguration.zaaktypeUuid,
                            newZaaktypeUuid
                        )
                    }
                }
            }
        }

        given("an existing configuration for the zaaktype itself") {
            val previousToegekendResultaattypeUuid = UUID.randomUUID()
            val newToegekendResultaattypeUri = resultaattypeUri()
            val zaakType = createZaakType(resultTypes = listOf(newToegekendResultaattypeUri))
            val existingConfiguration = createZaaktypeBpmnConfiguration(
                zaaktypeUUID = zaakType.url.extractUuid(),
                nietOntvankelijkResultaattype = previousToegekendResultaattypeUuid
            )
            stubConfigurationQueries(
                zaakType,
                listOf(existingConfiguration, existingConfiguration),
                previousVersionIsLookedUp = false
            )
            every { ztcClientService.readResultaattype(newToegekendResultaattypeUri) } returns
                createResultaatType(url = newToegekendResultaattypeUri, omschrijving = "Toegekend")
            every { ztcClientService.readResultaattype(previousToegekendResultaattypeUuid) } returns
                createResultaatType(omschrijving = "Toegekend")

            val configurationSlot = slot<ZaaktypeBpmnConfiguration>()
            every { entityManager.merge(capture(configurationSlot)) } returns existingConfiguration

            `when`("upserting the configuration") {
                zaaktypeBpmnConfigurationBeheerService.upsertConfiguration(zaakType)

                then("its resultaattypen are remapped in place instead of a copy being made") {
                    verify(exactly = 0) { entityManager.persist(any<Any>()) }
                    configurationSlot.captured shouldBe existingConfiguration
                    existingConfiguration.nietOntvankelijkResultaattype shouldBe
                        newToegekendResultaattypeUri.extractUuid()
                }
            }
        }
    }
})
