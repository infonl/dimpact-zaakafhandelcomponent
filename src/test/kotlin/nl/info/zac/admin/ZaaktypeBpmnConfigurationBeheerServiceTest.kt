/*
 * SPDX-FileCopyrightText: 2025, 2026 INFO.nl
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
import nl.info.client.zgw.ztc.model.createZaakType
import nl.info.zac.admin.model.ZaaktypeBpmnConfiguration
import nl.info.zac.admin.model.ZaaktypeBpmnConfiguration.Companion.BPMN_PROCESS_DEFINITION_KEY
import nl.info.zac.admin.model.ZaaktypeConfiguration.Companion.CREATIEDATUM_VARIABLE_NAME
import nl.info.zac.admin.model.ZaaktypeConfiguration.Companion.PRODUCTAANVRAAGTYPE_VARIABLE_NAME
import nl.info.zac.admin.model.ZaaktypeConfiguration.Companion.ZAAKTYPE_OMSCHRIJVING_VARIABLE_NAME
import nl.info.zac.admin.model.ZaaktypeConfiguration.Companion.ZAAKTYPE_UUID_VARIABLE_NAME
import nl.info.zac.admin.model.createBetrokkeneKoppelingen
import nl.info.zac.admin.model.createZaaktypeBpmnConfiguration
import nl.info.zac.admin.model.createZaaktypeBrpParameters
import nl.info.zac.smartdocuments.SmartDocumentsTemplatesService
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
    val zaaktypeBpmnConfigurationBeheerService = ZaaktypeBpmnConfigurationBeheerService(
        entityManager,
        smartDocumentsTemplatesService
    )

    afterEach {
        checkUnnecessaryStub()
    }

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

                And("the ID was reset") {
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

    context("copying configuration on a new zaaktype version") {
        given("no previous configuration") {
            val zaakType = createZaakType()

            every {
                zaaktypeBpmnConfigurationBeheerService.findConfiguration(zaakType.omschrijving)
            } returns null

            `when`("copying configuration") {
                zaaktypeBpmnConfigurationBeheerService.copyConfiguration(zaakType)

                then("no copying is done") {
                    verify(exactly = 0) { entityManager.persist(any<Any>()) }
                    verify(exactly = 0) { entityManager.merge(any<Any>()) }
                }
            }
        }

        given("existing previous configuration") {
            val zaakType = createZaakType()
            val newZaaktypeUuid = zaakType.url.extractUuid()
            val nietOntvankelijkResultaattype = UUID.randomUUID()

            val previousConfiguration = createZaaktypeBpmnConfiguration(
                zaaktypeBrpParameters = createZaaktypeBrpParameters(raadpleegWaarde = "fakeRaadpleegWaarde"),
                zaaktypeBetrokkeneParameters = createBetrokkeneKoppelingen(brpKoppelen = false),
                nietOntvankelijkResultaattype = nietOntvankelijkResultaattype,
                bpmnProcessDefinitionKey = "fakeBpmnProcessDefinitionKey"
            )
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
            every { criteriaBuilder.equal(pathString, zaakType.omschrijving) } returns predicate
            every { criteriaBuilder.equal(pathUuid, newZaaktypeUuid) } returns predicate
            every { zaaktypeBpmnConfigurationRoot.get<String>(ZAAKTYPE_OMSCHRIJVING_VARIABLE_NAME) } returns pathString
            every { zaaktypeBpmnConfigurationRoot.get<UUID>(ZAAKTYPE_UUID_VARIABLE_NAME) } returns pathUuid
            every { zaaktypeBpmnConfigurationRoot.get<Any>(CREATIEDATUM_VARIABLE_NAME) } returns pathCreatieDatum
            every { criteriaBuilder.desc(pathCreatieDatum) } returns creatieDatumOrder
            every { zaaktypeBpmnConfigurationCriteriaQuery.orderBy(creatieDatumOrder) } returns zaaktypeBpmnConfigurationCriteriaQuery
            every {
                entityManager.createQuery(zaaktypeBpmnConfigurationCriteriaQuery).setMaxResults(1).resultStream.findFirst().getOrNull()
            } returns previousConfiguration

            val configurationSlot = slot<ZaaktypeBpmnConfiguration>()
            val newConfiguration = createZaaktypeBpmnConfiguration()
            every {
                entityManager.merge(capture(configurationSlot))
            } returns newConfiguration
            every { smartDocumentsTemplatesService.copySmartDocumentsTemplateMappings(any(), any()) } just Runs

            `when`("copying configuration") {
                zaaktypeBpmnConfigurationBeheerService.copyConfiguration(zaakType)

                then("correct copy is stored") {
                    with(configurationSlot.captured) {
                        zaaktypeUuid shouldBe newZaaktypeUuid
                        with(zaaktypeBetrokkeneParameters!!) {
                            kvkKoppelen shouldBe true
                            brpKoppelen shouldBe false
                        }
                        with(zaaktypeBrpParameters!!) {
                            raadpleegWaarde shouldBe "fakeRaadpleegWaarde"
                        }
                        nietOntvankelijkResultaattype shouldBe nietOntvankelijkResultaattype
                    }
                }
            }
        }
    }
})
