/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.admin

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import jakarta.persistence.EntityManager
import jakarta.persistence.criteria.CriteriaQuery
import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.model.createZaakType
import nl.info.zac.admin.model.ZaaktypeConfiguration
import nl.info.zac.admin.model.createZaaktypeCmmnConfiguration
import nl.info.zac.flowable.bpmn.model.createZaaktypeBpmnConfiguration
import java.net.URI
import java.util.UUID

class ZaaktypeConfigurationServiceTest : BehaviorSpec({
    val zaaktypeUri = URI("https://example.com/zaaktype/${UUID.randomUUID()}")
    val cacheClearMessage = "ztc-zaaktype cache cleared"

    val entityManager = mockk<EntityManager>()
    val ztcClientService = mockk<ZtcClientService>()
    val zaaktypeCmmnConfigurationBeheerService = mockk<ZaaktypeCmmnConfigurationBeheerService>()
    val zaaktypeBpmnConfigurationBeheerService = mockk<ZaaktypeBpmnConfigurationBeheerService>()
    val zaaktypeConfigurationService = ZaaktypeConfigurationService(
        entityManager,
        ztcClientService,
        zaaktypeCmmnConfigurationBeheerService,
        zaaktypeBpmnConfigurationBeheerService
    )

    afterEach {
        checkUnnecessaryStub()
    }

    context("updating zaakafhandel parameters") {
        given("a concept zaaktype event") {
            val zaaktype = createZaakType(concept = true)

            every { ztcClientService.clearZaaktypeCache() } returns cacheClearMessage
            every { ztcClientService.readZaaktype(zaaktypeUri) } returns zaaktype

            `when`("updating zaakafhandel parameters") {
                zaaktypeConfigurationService.updateZaaktypeConfiguration(zaaktypeUri)

                then("no update is actually made") {
                    verify(exactly = 0) {
                        zaaktypeCmmnConfigurationBeheerService.upsertZaaktypeCmmnConfiguration(zaaktype)
                        zaaktypeBpmnConfigurationBeheerService.copyConfiguration(zaaktype)
                    }
                }
            }
        }

        given("a new version of zaaktype without existing zaakafhandelparameters") {
            val zaaktype = createZaakType()

            every { ztcClientService.clearZaaktypeCache() } returns cacheClearMessage
            every { ztcClientService.readZaaktype(zaaktypeUri) } returns zaaktype

            // Relaxed entity manager mocking; criteria queries and persisting
            val criteriaQuery = mockk<CriteriaQuery<ZaaktypeConfiguration>>(relaxed = true)
            every { entityManager.criteriaBuilder } returns mockk(relaxed = true) {
                every { createQuery(ZaaktypeConfiguration::class.java) } returns criteriaQuery
            }
            every { entityManager.createQuery(criteriaQuery) } returns mockk {
                every { setMaxResults(1) } returns this
                every { resultList } returns emptyList()
            }

            `when`("updating zaakafhandel parameters") {
                zaaktypeConfigurationService.updateZaaktypeConfiguration(zaaktypeUri)

                then("no update is actually made") {
                    verify(exactly = 0) {
                        zaaktypeCmmnConfigurationBeheerService.upsertZaaktypeCmmnConfiguration(zaaktype)
                        zaaktypeBpmnConfigurationBeheerService.copyConfiguration(zaaktype)
                    }
                }
            }
        }

        given("a new version of zaaktype with existing CMMN zaakafhandelparameters") {
            val zaaktype = createZaakType()
            val zaaktypeCmmnConfiguration = createZaaktypeCmmnConfiguration()

            every { ztcClientService.clearZaaktypeCache() } returns cacheClearMessage
            every { ztcClientService.readZaaktype(zaaktypeUri) } returns zaaktype

            // Relaxed entity manager mocking; criteria queries and persisting
            val criteriaQuery = mockk<CriteriaQuery<ZaaktypeConfiguration>>(relaxed = true)
            every { entityManager.criteriaBuilder } returns mockk(relaxed = true) {
                every { createQuery(ZaaktypeConfiguration::class.java) } returns criteriaQuery
            }
            every { entityManager.createQuery(criteriaQuery) } returns mockk {
                every { setMaxResults(1) } returns this
                every { resultList } returns listOf(zaaktypeCmmnConfiguration)
            }

            every { zaaktypeCmmnConfigurationBeheerService.upsertZaaktypeCmmnConfiguration(zaaktype) } just runs

            `when`("updating zaakafhandel parameters") {
                zaaktypeConfigurationService.updateZaaktypeConfiguration(zaaktypeUri)

                then("the correct updates are made") {
                    verify(exactly = 0) {
                        zaaktypeBpmnConfigurationBeheerService.copyConfiguration(zaaktype)
                    }
                    verify(exactly = 1) {
                        zaaktypeCmmnConfigurationBeheerService.upsertZaaktypeCmmnConfiguration(zaaktype)
                    }
                }
            }
        }

        given("a new version of zaaktype with existing BPMN zaakafhandelparameters") {
            val zaaktype = createZaakType()
            val zaaktypeBpmnConfiguration = createZaaktypeBpmnConfiguration()

            every { ztcClientService.clearZaaktypeCache() } returns cacheClearMessage
            every { ztcClientService.readZaaktype(zaaktypeUri) } returns zaaktype

            // Relaxed entity manager mocking; criteria queries and persisting
            val criteriaQuery = mockk<CriteriaQuery<ZaaktypeConfiguration>>(relaxed = true)
            every { entityManager.criteriaBuilder } returns mockk(relaxed = true) {
                every { createQuery(ZaaktypeConfiguration::class.java) } returns criteriaQuery
            }
            every { entityManager.createQuery(criteriaQuery) } returns mockk {
                every { setMaxResults(1) } returns this
                every { resultList } returns listOf(zaaktypeBpmnConfiguration)
            }

            every { zaaktypeBpmnConfigurationBeheerService.copyConfiguration(zaaktype) } just runs

            `when`("updating zaakafhandel parameters") {
                zaaktypeConfigurationService.updateZaaktypeConfiguration(zaaktypeUri)

                then("the correct updates are made") {
                    verify(exactly = 0) {
                        zaaktypeCmmnConfigurationBeheerService.upsertZaaktypeCmmnConfiguration(zaaktype)
                    }
                    verify(exactly = 1) {
                        zaaktypeBpmnConfigurationBeheerService.copyConfiguration(zaaktype)
                    }
                }
            }
        }
    }

    context("reading zaaktype configuration") {
        given("a configured zaaktype") {
            val zaaktype = createZaakType()
            val zaaktypeUUID = zaaktype.url.extractUuid()
            val zaaktypeBpmnConfiguration = createZaaktypeBpmnConfiguration()

            // Relaxed entity manager mocking; criteria queries and persisting
            val criteriaQuery = mockk<CriteriaQuery<ZaaktypeConfiguration>>(relaxed = true)
            every { entityManager.criteriaBuilder } returns mockk(relaxed = true) {
                every { createQuery(ZaaktypeConfiguration::class.java) } returns criteriaQuery
            }
            every { entityManager.createQuery(criteriaQuery) } returns mockk {
                every { setMaxResults(1) } returns this
                every { resultList } returns listOf(zaaktypeBpmnConfiguration)
            }

            `when`("reading zaaktype configuration") {
                val zaaktypeConfiguration = zaaktypeConfigurationService.readZaaktypeConfiguration(zaaktypeUUID)

                then("it is returned correctly") {
                    zaaktypeConfiguration shouldBe zaaktypeBpmnConfiguration
                }
            }
        }

        given("a zaaktype that has no configuration") {
            val zaaktypeUUID = UUID.randomUUID()

            // Relaxed entity manager mocking; criteria queries and persisting
            val criteriaQuery = mockk<CriteriaQuery<ZaaktypeConfiguration>>(relaxed = true)
            every { entityManager.criteriaBuilder } returns mockk(relaxed = true) {
                every { createQuery(ZaaktypeConfiguration::class.java) } returns criteriaQuery
            }
            every { entityManager.createQuery(criteriaQuery) } returns mockk {
                every { setMaxResults(1) } returns this
                every { resultList } returns emptyList()
            }

            `when`("reading zaaktype configuration") {
                val result = zaaktypeConfigurationService.readZaaktypeConfiguration(zaaktypeUUID)

                then("it returns null") {
                    result shouldBe null
                }
            }
        }
    }
})
