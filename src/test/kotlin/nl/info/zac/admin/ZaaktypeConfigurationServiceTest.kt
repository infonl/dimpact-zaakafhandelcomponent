/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.admin

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.maps.shouldContainKey
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
import nl.info.zac.admin.model.ZaakbeeindigReden
import nl.info.zac.admin.model.ZaaktypeConfiguration
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
                val zaaktypeConfiguration = createZaaktypeConfiguration("fakeNietOntvankelijkResultaattype")
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
                val zaaktypeConfiguration = createZaaktypeConfiguration("fakeNietOntvankelijkResultaattype")
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

    context("finding current configuration and querying collections") {
        given("a configuration found by omschrijving") {
            val zaaktypeConfigurationService = ZaaktypeConfigurationService(zaaktypeConfigurationRepository)
            val config = ZaaktypeConfiguration().apply {
                zaaktypeUuid = UUID.randomUUID()
                zaaktypeOmschrijving = "fakeOmschrijving"
                isSmartDocumentsEnabled = true
            }
            every {
                zaaktypeConfigurationRepository.findCurrentByZaaktypeOmschrijving("fakeOmschrijving")
            } returns config

            `when`("finding current configuration by omschrijving") {
                val found = zaaktypeConfigurationService.findCurrentConfiguration("fakeOmschrijving")

                then("it returns the matching configuration") {
                    found shouldBe config
                }
            }
        }

        given("configurations listed by productaanvraagtype") {
            val zaaktypeConfigurationService = ZaaktypeConfigurationService(zaaktypeConfigurationRepository)
            val config = ZaaktypeConfiguration().apply {
                zaaktypeUuid = UUID.randomUUID()
                zaaktypeOmschrijving = "fakeOmschrijving"
            }
            every {
                zaaktypeConfigurationRepository.listCurrentByProductaanvraagtype("fakeProductaanvraagtype")
            } returns listOf(config)

            `when`("listing by productaanvraagtype") {
                val list = zaaktypeConfigurationService.listCurrentConfigurationsByProductaanvraagtype("fakeProductaanvraagtype")

                then("it returns the list from repository") {
                    list shouldContainExactly listOf(config)
                }
            }
        }

        given("configurations listed by bound engine") {
            val zaaktypeConfigurationService = ZaaktypeConfigurationService(zaaktypeConfigurationRepository)
            val config = ZaaktypeConfiguration().apply {
                zaaktypeUuid = UUID.randomUUID()
                zaaktypeOmschrijving = "fakeOmschrijving"
            }
            every {
                zaaktypeConfigurationRepository.listBoundTo(ProcessEngine.BPMN)
            } returns listOf(config)

            `when`("listing configurations bound to engine") {
                val list = zaaktypeConfigurationService.listConfigurationsBoundTo(ProcessEngine.BPMN)

                then("it returns the list from repository") {
                    list shouldContainExactly listOf(config)
                }
            }
        }
    }

    context("smart documents status and termination reasons") {
        given("a configuration with smart documents enabled") {
            val zaaktypeConfigurationService = ZaaktypeConfigurationService(zaaktypeConfigurationRepository)
            val configUuid = UUID.randomUUID()
            val nonExistingUuid = UUID.randomUUID()
            val config = ZaaktypeConfiguration().apply {
                zaaktypeUuid = configUuid
                isSmartDocumentsEnabled = true
            }
            every { zaaktypeConfigurationRepository.findByZaaktypeUuid(configUuid) } returns config
            every { zaaktypeConfigurationRepository.findByZaaktypeUuid(nonExistingUuid) } returns null

            `when`("checking isSmartDocumentsEnabled for existing and non-existing configuration") {
                val isEnabled = zaaktypeConfigurationService.isSmartDocumentsEnabled(configUuid)
                val isDisabled = zaaktypeConfigurationService.isSmartDocumentsEnabled(nonExistingUuid)

                then("it returns true for existing enabled config and false for non-existing") {
                    isEnabled shouldBe true
                    isDisabled shouldBe false
                }
            }
        }

        given("zaakbeeindig redenen in repository") {
            val zaaktypeConfigurationService = ZaaktypeConfigurationService(zaaktypeConfigurationRepository)
            val reden = ZaakbeeindigReden().apply {
                id = 1L
                naam = "fakeReden"
            }
            every { zaaktypeConfigurationRepository.listZaakbeeindigRedenen() } returns listOf(reden)

            `when`("listing zaakbeeindig redenen") {
                val reasons = zaaktypeConfigurationService.listZaakbeeindigRedenen()

                then("it returns reasons from repository") {
                    reasons shouldContainExactly listOf(reden)
                }
            }
        }
    }

    context("cache clearing and statistics") {
        given("a ZaaktypeConfigurationService instance") {
            val zaaktypeConfigurationService = ZaaktypeConfigurationService(zaaktypeConfigurationRepository)

            `when`("clearing managed cache") {
                val message = zaaktypeConfigurationService.clearManagedCache()

                then("it returns the cleared message") {
                    message shouldContain Caching.ZAC_ZAAKTYPECMMNCONFIGURATION_MANAGED
                }
            }

            `when`("inspecting cache statistics and estimated sizes") {
                val stats = zaaktypeConfigurationService.cacheStatistics()
                val sizes = zaaktypeConfigurationService.estimatedCacheSizes()

                then("caches are reported") {
                    stats.keys shouldHaveSize 2
                    sizes.keys shouldHaveSize 2
                    stats shouldContainKey "UUID -> ZaaktypeConfiguration"
                    sizes shouldContainKey "UUID -> ZaaktypeConfiguration"
                }
            }
        }
    }
})
