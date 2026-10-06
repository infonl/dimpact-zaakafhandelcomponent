/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.admin

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.checkUnnecessaryStub
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import nl.info.client.zgw.shared.cache.Caching
import nl.info.zac.admin.exception.ZaaktypeConfigurationNotFoundException
import nl.info.zac.admin.model.ProcessEngine
import nl.info.zac.admin.model.ZaaktypeDeadlineWarningWindows
import nl.info.zac.admin.model.createZaaktypeConfigurationsUnderTest
import java.util.UUID

class ZaaktypeConfigurationServiceTest : BehaviorSpec({
    val zaaktypeConfigurationRepository = mockk<ZaaktypeConfigurationRepository>()

    afterEach {
        checkUnnecessaryStub()
        clearMocks(zaaktypeConfigurationRepository)
    }

    createZaaktypeConfigurationsUnderTest().forEach { (configurationType, createZaaktypeConfiguration) ->
        context("finding the configuration of a zaaktype bound to $configurationType") {
            given("a stored $configurationType configuration") {
                val zaaktypeConfigurationService = ZaaktypeConfigurationService(zaaktypeConfigurationRepository)
                val zaaktypeConfiguration = createZaaktypeConfiguration(UUID.randomUUID())
                every {
                    zaaktypeConfigurationRepository.findByZaaktypeUuid(zaaktypeConfiguration.zaaktypeUuid)
                } returns zaaktypeConfiguration

                `when`("the configuration is found twice") {
                    val firstResult = zaaktypeConfigurationService.findConfiguration(zaaktypeConfiguration.zaaktypeUuid)
                    val secondResult = zaaktypeConfigurationService.findConfiguration(zaaktypeConfiguration.zaaktypeUuid)

                    then("the configuration is returned and read from the database only once") {
                        firstResult shouldBe zaaktypeConfiguration
                        secondResult shouldBe zaaktypeConfiguration
                        verify(exactly = 1) {
                            zaaktypeConfigurationRepository.findByZaaktypeUuid(zaaktypeConfiguration.zaaktypeUuid)
                        }
                    }
                }
            }

            given("a cached $configurationType configuration that is evicted") {
                val zaaktypeConfigurationService = ZaaktypeConfigurationService(zaaktypeConfigurationRepository)
                val zaaktypeConfiguration = createZaaktypeConfiguration(UUID.randomUUID())
                every {
                    zaaktypeConfigurationRepository.findByZaaktypeUuid(zaaktypeConfiguration.zaaktypeUuid)
                } returns zaaktypeConfiguration
                zaaktypeConfigurationService.findConfiguration(zaaktypeConfiguration.zaaktypeUuid)

                `when`("the configuration is found again") {
                    zaaktypeConfigurationService.evict(zaaktypeConfiguration.zaaktypeUuid)
                    zaaktypeConfigurationService.findConfiguration(zaaktypeConfiguration.zaaktypeUuid)

                    then("it is read from the database again") {
                        verify(exactly = 2) {
                            zaaktypeConfigurationRepository.findByZaaktypeUuid(zaaktypeConfiguration.zaaktypeUuid)
                        }
                    }
                }
            }
        }
    }

    context("finding the configuration of a zaaktype without a configuration") {
        given("a zaaktype UUID without a configuration") {
            val zaaktypeConfigurationService = ZaaktypeConfigurationService(zaaktypeConfigurationRepository)
            val zaaktypeUuid = UUID.randomUUID()
            every { zaaktypeConfigurationRepository.findByZaaktypeUuid(zaaktypeUuid) } returns null

            `when`("the configuration is found") {
                val zaaktypeConfiguration = zaaktypeConfigurationService.findConfiguration(zaaktypeUuid)

                then("null is returned") {
                    zaaktypeConfiguration.shouldBeNull()
                }
            }

            `when`("the configuration is read") {
                val exception = shouldThrow<ZaaktypeConfigurationNotFoundException> {
                    zaaktypeConfigurationService.readConfiguration(zaaktypeUuid)
                }

                then("an exception names the zaaktype UUID") {
                    exception.message shouldContain zaaktypeUuid.toString()
                }
            }
        }
    }

    context("listing the deadline warning windows") {
        given("a zaaktype configuration with a deadline warning window") {
            val zaaktypeConfigurationService = ZaaktypeConfigurationService(zaaktypeConfigurationRepository)
            val deadlineWarningWindows = listOf(
                ZaaktypeDeadlineWarningWindows(
                    zaaktypeUuid = UUID.randomUUID(),
                    einddatumGeplandWaarschuwing = 3,
                    uiterlijkeEinddatumAfdoeningWaarschuwing = null
                )
            )
            every { zaaktypeConfigurationRepository.listDeadlineWarningWindows() } returns deadlineWarningWindows

            `when`("the windows are listed, the list cache is cleared, and the windows are listed again") {
                zaaktypeConfigurationService.listDeadlineWarningWindows()
                zaaktypeConfigurationService.listDeadlineWarningWindows()
                val clearedMessage = zaaktypeConfigurationService.clearListCache()
                val windows = zaaktypeConfigurationService.listDeadlineWarningWindows()

                then("the windows are read from the database once per filled cache") {
                    windows shouldBe deadlineWarningWindows
                    clearedMessage shouldContain Caching.ZAC_ZAAKTYPECMMNCONFIGURATION
                    verify(exactly = 2) { zaaktypeConfigurationRepository.listDeadlineWarningWindows() }
                }
            }
        }
    }

    context("listing the definition keys of an engine") {
        given("BPMN configurations with definition keys") {
            val zaaktypeConfigurationService = ZaaktypeConfigurationService(zaaktypeConfigurationRepository)
            every {
                zaaktypeConfigurationRepository.listDistinctDefinitionKeys(ProcessEngine.BPMN)
            } returns listOf("fakeProcessDefinitionKey1", "fakeProcessDefinitionKey2")

            `when`("the BPMN definition keys are listed") {
                val definitionKeys = zaaktypeConfigurationService.listDefinitionKeysBoundTo(ProcessEngine.BPMN)

                then("the distinct keys are returned") {
                    definitionKeys shouldBe listOf("fakeProcessDefinitionKey1", "fakeProcessDefinitionKey2")
                }
            }
        }
    }
})
