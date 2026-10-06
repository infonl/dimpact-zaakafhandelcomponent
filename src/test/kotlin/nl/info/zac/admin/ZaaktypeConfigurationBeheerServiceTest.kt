/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.admin

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import jakarta.validation.ConstraintViolationException
import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.model.createZaakType
import nl.info.zac.admin.model.ZaaktypeConfiguration
import nl.info.zac.admin.model.createZaaktypeConfigurationsUnderTest
import nl.info.zac.exception.ErrorCode.ERROR_CODE_PRODUCTAANVRAAGTYPE_ALREADY_IN_USE
import nl.info.zac.exception.InputValidationFailedException
import nl.info.zac.smartdocuments.SmartDocumentsTemplatesService
import java.net.URI
import java.time.ZonedDateTime
import java.util.UUID

class ZaaktypeConfigurationBeheerServiceTest : BehaviorSpec({
    val zaaktypeConfigurationRepository = mockk<ZaaktypeConfigurationRepository>()
    val zaaktypeConfigurationService = mockk<ZaaktypeConfigurationService>()
    val ztcClientService = mockk<ZtcClientService>()
    val smartDocumentsTemplatesService = mockk<SmartDocumentsTemplatesService>()
    val zaaktypeHelperService = mockk<ZaaktypeHelperService>()
    val zaaktypeConfigurationBeheerService = ZaaktypeConfigurationBeheerService(
        zaaktypeConfigurationRepository = zaaktypeConfigurationRepository,
        zaaktypeConfigurationService = zaaktypeConfigurationService,
        ztcClientService = ztcClientService,
        smartDocumentsTemplatesService = smartDocumentsTemplatesService,
        zaaktypeHelperService = zaaktypeHelperService
    )

    afterEach {
        checkUnnecessaryStub()
    }

    fun clearZtcCachesJustRuns() {
        every { ztcClientService.clearZaaktypeCache() } returns "fakeCleared"
        every { ztcClientService.clearRoltypeCache() } returns "fakeCleared"
        every { ztcClientService.clearResultaattypeCache() } returns "fakeCleared"
        every { ztcClientService.clearStatustypeCache() } returns "fakeCleared"
        every { ztcClientService.clearEigenschapCache() } returns "fakeCleared"
    }

    createZaaktypeConfigurationsUnderTest().forEach { (configurationType, createZaaktypeConfiguration) ->
        context("storing a $configurationType configuration") {
            given("a valid $configurationType configuration for a zaaktype version that already has a configuration") {
                val zaaktypeConfiguration = createZaaktypeConfiguration(UUID.randomUUID()).apply {
                    id = 999L
                    groepID = "fakeGroup"
                }
                val storedZaaktypeConfiguration = createZaaktypeConfiguration(UUID.randomUUID()).apply { id = 42L }
                every {
                    zaaktypeConfigurationRepository.findByZaaktypeUuid(zaaktypeConfiguration.zaaktypeUuid)
                } returns storedZaaktypeConfiguration
                every { zaaktypeConfigurationRepository.store(zaaktypeConfiguration) } returns zaaktypeConfiguration
                every { zaaktypeConfigurationService.evict(zaaktypeConfiguration.zaaktypeUuid) } just runs

                `when`("it is stored with an id that does not exist") {
                    zaaktypeConfigurationBeheerService.storeConfiguration(zaaktypeConfiguration)

                    then("the existing configuration of the zaaktype version is updated and the cache is evicted") {
                        zaaktypeConfiguration.id shouldBe 42L
                        verify(exactly = 1) {
                            zaaktypeConfigurationRepository.store(zaaktypeConfiguration)
                            zaaktypeConfigurationService.evict(zaaktypeConfiguration.zaaktypeUuid)
                        }
                    }
                }
            }

            given("a $configurationType configuration without groep") {
                val zaaktypeConfiguration = createZaaktypeConfiguration(UUID.randomUUID()).apply { groepID = null }

                `when`("it is stored") {
                    shouldThrow<ConstraintViolationException> {
                        zaaktypeConfigurationBeheerService.storeConfiguration(zaaktypeConfiguration)
                    }

                    then("nothing is stored") {
                        verify(exactly = 0) { zaaktypeConfigurationRepository.store(zaaktypeConfiguration) }
                    }
                }
            }
        }

        context("handling a notification for a new version of a $configurationType zaaktype") {
            given("a $configurationType configuration of the previous version of the zaaktype") {
                val previousZaaktypeConfiguration = createZaaktypeConfiguration(UUID.randomUUID())
                val newZaaktypeUuid = UUID.randomUUID()
                val newZaaktype = createZaakType(
                    uri = URI("https://example.com/zaaktypes/$newZaaktypeUuid"),
                    omschrijving = previousZaaktypeConfiguration.zaaktypeOmschrijving
                )
                val newZaaktypeConfiguration = slot<ZaaktypeConfiguration>()
                clearZtcCachesJustRuns()
                every { ztcClientService.readZaaktype(newZaaktype.url) } returns newZaaktype
                every { zaaktypeConfigurationRepository.findByZaaktypeUuid(newZaaktypeUuid) } returns null
                every {
                    zaaktypeConfigurationRepository.findCurrentByZaaktypeOmschrijving(newZaaktype.omschrijving)
                } returns previousZaaktypeConfiguration
                every {
                    zaaktypeHelperService.copyConfigurationData(previousZaaktypeConfiguration, any(), newZaaktype)
                } answers {
                    secondArg<ZaaktypeConfiguration>().apply {
                        groepID = "fakeCopiedGroup"
                        creatiedatum = ZonedDateTime.now()
                    }
                }
                every { zaaktypeConfigurationRepository.store(capture(newZaaktypeConfiguration)) } answers { firstArg() }
                every { zaaktypeConfigurationService.evict(newZaaktypeUuid) } just runs
                every {
                    smartDocumentsTemplatesService.copySmartDocumentsTemplateMappings(
                        previousZaaktypeConfiguration.zaaktypeUuid,
                        newZaaktypeUuid
                    )
                } just runs

                `when`("the notification is handled") {
                    zaaktypeConfigurationBeheerService.updateZaaktypeConfiguration(newZaaktype.url)

                    then("a configuration for the new version is created from the previous one and stored") {
                        with(newZaaktypeConfiguration.captured) {
                            zaaktypeUuid shouldBe newZaaktypeUuid
                            zaaktypeOmschrijving shouldBe newZaaktype.omschrijving
                            groepID shouldBe "fakeCopiedGroup"
                        }
                    }

                    and("the SmartDocuments template mappings are copied onto the new version") {
                        verify(exactly = 1) {
                            smartDocumentsTemplatesService.copySmartDocumentsTemplateMappings(
                                previousZaaktypeConfiguration.zaaktypeUuid,
                                newZaaktypeUuid
                            )
                        }
                    }
                }
            }

            given("an existing $configurationType configuration of the zaaktype version itself") {
                val existingZaaktypeConfiguration = createZaaktypeConfiguration(UUID.randomUUID()).apply {
                    groepID = "fakeGroup"
                    zaaktypeOmschrijving = "fakeExistingZaaktype$configurationType"
                }
                val zaaktype = createZaakType(
                    uri = URI("https://example.com/zaaktypes/${existingZaaktypeConfiguration.zaaktypeUuid}"),
                    omschrijving = existingZaaktypeConfiguration.zaaktypeOmschrijving
                )
                clearZtcCachesJustRuns()
                every { ztcClientService.readZaaktype(zaaktype.url) } returns zaaktype
                every {
                    zaaktypeConfigurationRepository.findByZaaktypeUuid(existingZaaktypeConfiguration.zaaktypeUuid)
                } returns existingZaaktypeConfiguration
                every {
                    zaaktypeHelperService.updateZaakbeeindigGegevens(existingZaaktypeConfiguration, zaaktype)
                } just runs
                every {
                    zaaktypeConfigurationRepository.store(existingZaaktypeConfiguration)
                } returns existingZaaktypeConfiguration
                every { zaaktypeConfigurationService.evict(existingZaaktypeConfiguration.zaaktypeUuid) } just runs

                `when`("a notification for that zaaktype version is handled") {
                    zaaktypeConfigurationBeheerService.updateZaaktypeConfiguration(zaaktype.url)

                    then("only that configuration is updated, and no SmartDocuments template mappings are copied") {
                        verify(exactly = 1) {
                            zaaktypeHelperService.updateZaakbeeindigGegevens(existingZaaktypeConfiguration, zaaktype)
                            zaaktypeConfigurationRepository.store(existingZaaktypeConfiguration)
                        }
                        verify(exactly = 0) {
                            zaaktypeConfigurationRepository.findCurrentByZaaktypeOmschrijving(
                                existingZaaktypeConfiguration.zaaktypeOmschrijving
                            )
                            smartDocumentsTemplatesService.copySmartDocumentsTemplateMappings(
                                any(),
                                existingZaaktypeConfiguration.zaaktypeUuid
                            )
                        }
                    }
                }
            }
        }

        context("checking the productaanvraagtype of a $configurationType configuration") {
            given("a productaanvraagtype that the current configuration of another zaaktype uses") {
                val otherZaaktypeConfiguration = createZaaktypeConfiguration(UUID.randomUUID()).apply {
                    zaaktypeOmschrijving = "fakeOtherZaaktype"
                }
                every {
                    zaaktypeConfigurationService.listCurrentConfigurationsByProductaanvraagtype("fakeProductaanvraagtype")
                } returns listOf(otherZaaktypeConfiguration)

                `when`("a configuration of a zaaktype with another omschrijving uses it") {
                    val exception = shouldThrow<InputValidationFailedException> {
                        zaaktypeConfigurationBeheerService.checkProductaanvraagtypeIsNotInUse(
                            productaanvraagtype = "fakeProductaanvraagtype",
                            zaaktypeOmschrijving = "fakeZaaktype"
                        )
                    }

                    then("it is rejected as already in use") {
                        exception.errorCode shouldBe ERROR_CODE_PRODUCTAANVRAAGTYPE_ALREADY_IN_USE
                    }
                }
            }

            given("a productaanvraagtype that the current configuration of the same zaaktype uses") {
                val previousVersionConfiguration = createZaaktypeConfiguration(UUID.randomUUID()).apply {
                    zaaktypeOmschrijving = "fakeZaaktype"
                }
                every {
                    zaaktypeConfigurationService.listCurrentConfigurationsByProductaanvraagtype("fakeProductaanvraagtype")
                } returns listOf(previousVersionConfiguration)

                `when`("a configuration of a new version of that zaaktype uses it") {
                    then("it is accepted") {
                        shouldNotThrowAny {
                            zaaktypeConfigurationBeheerService.checkProductaanvraagtypeIsNotInUse(
                                productaanvraagtype = "fakeProductaanvraagtype",
                                zaaktypeOmschrijving = "fakeZaaktype"
                            )
                        }
                    }
                }
            }
        }
    }

    context("handling a zaaktype notification") {
        given("a concept zaaktype") {
            val zaaktype = createZaakType(concept = true)
            clearZtcCachesJustRuns()
            every { ztcClientService.readZaaktype(zaaktype.url) } returns zaaktype

            `when`("the notification is handled") {
                zaaktypeConfigurationBeheerService.updateZaaktypeConfiguration(zaaktype.url)

                then("no configuration is read") {
                    verify(exactly = 0) {
                        zaaktypeConfigurationRepository.findByZaaktypeUuid(zaaktype.url.extractUuid())
                    }
                }
            }
        }

        given("a zaaktype whose omschrijving has no configuration") {
            val zaaktypeUuid = UUID.randomUUID()
            val zaaktype = createZaakType(
                uri = URI("https://example.com/zaaktypes/$zaaktypeUuid"),
                omschrijving = "fakeZaaktypeWithoutConfiguration"
            )
            clearZtcCachesJustRuns()
            every { ztcClientService.readZaaktype(zaaktype.url) } returns zaaktype
            every { zaaktypeConfigurationRepository.findByZaaktypeUuid(zaaktypeUuid) } returns null
            every { zaaktypeConfigurationRepository.findCurrentByZaaktypeOmschrijving(zaaktype.omschrijving) } returns null

            `when`("the notification is handled") {
                zaaktypeConfigurationBeheerService.updateZaaktypeConfiguration(zaaktype.url)

                then("no configuration is created") {
                    verify(exactly = 0) {
                        zaaktypeConfigurationRepository.store(match { it.zaaktypeUuid == zaaktypeUuid })
                    }
                }
            }
        }
    }

    context("fetching and finding stored configuration") {
        given("a stored configuration") {
            val zaaktypeUuid = UUID.randomUUID()
            val stored = ZaaktypeConfiguration().apply { this.zaaktypeUuid = zaaktypeUuid }
            every { ztcClientService.resetCacheTimeToNow() } returns ZonedDateTime.now()
            every { zaaktypeConfigurationRepository.findByZaaktypeUuid(zaaktypeUuid) } returns stored

            `when`("fetching the configuration") {
                val fetched = zaaktypeConfigurationBeheerService.fetchConfiguration(zaaktypeUuid)

                then("it returns the stored configuration and resets the cache time") {
                    fetched shouldBe stored
                    verify(exactly = 1) { ztcClientService.resetCacheTimeToNow() }
                }
            }

            `when`("finding stored configuration directly") {
                val found = zaaktypeConfigurationBeheerService.findStoredConfiguration(zaaktypeUuid)

                then("it returns the stored configuration without resetting cache time") {
                    found shouldBe stored
                }
            }
        }

        given("no stored configuration") {
            val zaaktypeUuid = UUID.randomUUID()
            every { ztcClientService.resetCacheTimeToNow() } returns ZonedDateTime.now()
            every { zaaktypeConfigurationRepository.findByZaaktypeUuid(zaaktypeUuid) } returns null

            `when`("fetching configuration") {
                val fetched = zaaktypeConfigurationBeheerService.fetchConfiguration(zaaktypeUuid)

                then("a new configuration with the given UUID is returned") {
                    fetched.zaaktypeUuid shouldBe zaaktypeUuid
                    fetched.id shouldBe null
                }
            }
        }
    }

    context("upserting existing configuration without servicenorm") {
        given("an existing configuration with deadline warning and updated zaaktype without servicenorm") {
            val zaaktypeUuid = UUID.randomUUID()
            val existingConfig = ZaaktypeConfiguration().apply {
                this.zaaktypeUuid = zaaktypeUuid
                zaaktypeOmschrijving = "fakeOriginalOmschrijving"
                groepID = "fakeGroup"
                einddatumGeplandWaarschuwing = 5
                creatiedatum = ZonedDateTime.now()
            }
            val updatedZaaktype = createZaakType(
                uri = URI("https://example.com/zaaktypes/$zaaktypeUuid"),
                omschrijving = "fakeUpdatedOmschrijving",
                servicenorm = null
            )
            every { zaaktypeConfigurationRepository.findByZaaktypeUuid(zaaktypeUuid) } returns existingConfig
            every { zaaktypeHelperService.updateZaakbeeindigGegevens(existingConfig, updatedZaaktype) } just runs
            every { zaaktypeConfigurationRepository.store(existingConfig) } returns existingConfig
            every { zaaktypeConfigurationService.evict(zaaktypeUuid) } just runs

            `when`("upsertConfiguration is called") {
                zaaktypeConfigurationBeheerService.upsertConfiguration(updatedZaaktype)

                then("einddatumGeplandWaarschuwing is cleared and omschrijving updated") {
                    existingConfig.zaaktypeOmschrijving shouldBe "fakeUpdatedOmschrijving"
                    existingConfig.einddatumGeplandWaarschuwing shouldBe null
                    verify(exactly = 1) { zaaktypeConfigurationRepository.store(existingConfig) }
                }
            }
        }
    }
})
