/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.ztc

import io.kotest.assertions.nondeterministic.eventually
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.date.shouldBeAfter
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import nl.info.client.zgw.ztc.ZtcClientService.Companion.MAX_CACHE_SIZE
import nl.info.client.zgw.ztc.exception.CatalogusNotFoundException
import nl.info.client.zgw.ztc.exception.RoltypeNotFoundException
import nl.info.client.zgw.ztc.model.createCatalogus
import nl.info.client.zgw.ztc.model.createCatalogusListParameters
import nl.info.client.zgw.ztc.model.createRolType
import nl.info.client.zgw.ztc.model.createZaakType
import nl.info.client.zgw.ztc.model.generated.OmschrijvingGeneriekEnum
import nl.info.client.zgw.ztc.model.RoltypeListGeneriekParameters
import nl.info.client.zgw.ztc.model.ZaaktypeListParameters
import nl.info.client.zgw.shared.model.Results
import nl.info.client.zgw.shared.ZgwApiService.Companion.ROLTYPE_OMSCHRIJVING_BEHANDELAAR
import java.net.URI
import java.time.ZonedDateTime
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

class ZtcClientServiceTest : BehaviorSpec({
    val ztcClient = mockk<ZtcClient>()
    val ztcClientService = ZtcClientService(
        ztcClient = ztcClient
    )
    val initialUUID = UUID.randomUUID()
    val expectedZaakType = createZaakType()
    val testStartDateTime = ZonedDateTime.now()
    lateinit var initialDateTime: ZonedDateTime

    afterEach {
        checkUnnecessaryStub()
    }

    given("An existing catalogus for the given parameters") {
        val catalogusListParameters = createCatalogusListParameters()
        val expectedCatalogus = createCatalogus()
        every { ztcClient.catalogusList(catalogusListParameters).singleResult } returns expectedCatalogus

        `when`("readCatalogus is called") {
            val result = ztcClientService.readCatalogus(catalogusListParameters)

            then("it should return the expected catalogus") {
                result shouldBe expectedCatalogus
            }
        }
    }

    given("No catalog for the given parameters") {
        val catalogusListParameters = createCatalogusListParameters()
        every { ztcClient.catalogusList(catalogusListParameters).singleResult } returns null

        `when`("readCatalogus is called") {
            val exception = shouldThrow<CatalogusNotFoundException> {
                ztcClientService.readCatalogus(catalogusListParameters)
            }

            then("it should throw an exception") {
                exception.message shouldBe
                    "No catalogus found for catalogus list parameters " +
                    "'CatalogusListParameters(domein=null, domeinIn=null, rsin=null, rsinIn=null, page=null)'."
            }
        }
    }

    given("ZTC client service") {

        `when`("reading cache time for the first time") {
            initialDateTime = ztcClientService.resetCacheTimeToNow()

            then("it should return time after test was started") {
                initialDateTime shouldBeAfter testStartDateTime
            }

            then("it should generate the value and store it in the cache") {
                with(ztcClientService.cacheStatistics()["ZTC Time"]) {
                    this?.hitCount() shouldBe 0
                    this?.missCount() shouldBe 1
                }
            }
        }

        `when`("reading cache time for the second time") {
            val dateTime = ztcClientService.resetCacheTimeToNow()

            then("it should cached the same time") {
                dateTime shouldBeEqual initialDateTime
            }

            then("it should fetch the value from the cache") {
                with(ztcClientService.cacheStatistics()["ZTC Time"]) {
                    this?.hitCount() shouldBe 1
                    this?.missCount() shouldBe 1
                }
            }
        }

        `when`("reading zaak type") {
            every { ztcClient.zaaktypeRead(initialUUID) } returns expectedZaakType
            val zaakType = ztcClientService.readZaaktype(initialUUID)

            then("it should return valid zaaktype") {
                zaakType shouldBe expectedZaakType
            }

            then("it should generate the value and store it in the cache") {
                with(ztcClientService.cacheStatistics()["ZTC UUID -> ZaakType"]) {
                    this?.hitCount() shouldBe 0
                    this?.missCount() shouldBe 1
                }
            }
        }

        `when`("reading more zaak types than the cache can hold") {
            (1..MAX_CACHE_SIZE + 1).forEach { _ ->
                val generatedUUID = UUID.randomUUID()
                every {
                    ztcClient.zaaktypeRead(generatedUUID)
                } returns createZaakType(uri = URI("https://example.com/zaaktype/$generatedUUID"))
                ztcClientService.readZaaktype(generatedUUID)
            }

            then("cache starts evicting") {
                eventually(5.seconds) {
                    with(ztcClientService.cacheStatistics()["ZTC UUID -> ZaakType"]) {
                        this?.hitCount() shouldBe 0
                        this?.missCount() shouldBe MAX_CACHE_SIZE + 2
                        this?.evictionCount() shouldBe 2
                    }
                }
            }
        }
    }

    given("ZTC client service time cache was cleared") {
        ztcClientService.clearCacheTime()

        `when`("reading the cache time") {
            val cacheDateTime = ztcClientService.resetCacheTimeToNow()

            then("time should be updated") {
                cacheDateTime shouldBeAfter initialDateTime
            }

            then("cache statistics should be ok") {
                with(ztcClientService.cacheStatistics()["ZTC Time"]) {
                    this?.hitCount() shouldBe 1
                    this?.missCount() shouldBe 2
                }
            }
        }
    }

    given("zaaktypes for a catalogus that are spread over multiple pages") {
        val catalogusUri = URI("https://example.com/catalogussen/${UUID.randomUUID()}")
        val firstPageZaakType = createZaakType()
        val secondPageZaakType = createZaakType()
        val nextPageUri = URI("https://example.com/zaaktypen?page=2")

        every {
            ztcClient.zaaktypeList(match<ZaaktypeListParameters> { it.getPage() == 1 })
        } returns Results(1, listOf(firstPageZaakType), nextPageUri, null)
        every {
            ztcClient.zaaktypeList(match<ZaaktypeListParameters> { it.getPage() == 2 })
        } returns Results(1, listOf(secondPageZaakType), null, null)

        `when`("listZaaktypen is called") {
            val zaaktypen = ztcClientService.listZaaktypen(catalogusUri)

            then("it should return the zaaktypes of all pages combined") {
                zaaktypen shouldBe listOf(firstPageZaakType, secondPageZaakType)
            }
        }
    }

    context("Reading roltype with omschrijving") {
        given("a zaaktype with a matching roltype") {
            val zaaktypeUri = URI("https://example.com/zaaktypes/${UUID.randomUUID()}")
            val matchingRolType = createRolType(
                omschrijving = ROLTYPE_OMSCHRIJVING_BEHANDELAAR,
                omschrijvingGeneriek = OmschrijvingGeneriekEnum.BEHANDELAAR,
                zaakTypeUri = zaaktypeUri
            )
            val otherRolType = createRolType(
                omschrijving = "Andere omschrijving",
                omschrijvingGeneriek = OmschrijvingGeneriekEnum.BEHANDELAAR,
                zaakTypeUri = zaaktypeUri
            )
            every {
                ztcClient.roltypeListGeneriek(any<RoltypeListGeneriekParameters>())
            } returns Results(listOf(matchingRolType, otherRolType), 2)

            `when`("readRoltype is called with the matching omschrijving") {
                val result = ztcClientService.readRoltype(
                    zaaktypeUri,
                    OmschrijvingGeneriekEnum.BEHANDELAAR,
                    "Behandelaar"
                )

                then("it should return the matching roltype") {
                    result shouldBe matchingRolType
                }
            }
        }

        given("a zaaktype without a matching roltype omschrijving") {
            val zaaktypeUri = URI("https://example.com/zaaktypes/${UUID.randomUUID()}")
            val nonMatchingRolType = createRolType(
                omschrijving = "Andere omschrijving",
                omschrijvingGeneriek = OmschrijvingGeneriekEnum.BEHANDELAAR,
                zaakTypeUri = zaaktypeUri
            )
            every {
                ztcClient.roltypeListGeneriek(any<RoltypeListGeneriekParameters>())
            } returns Results(listOf(nonMatchingRolType), 1)

            `when`("readRoltype is called with a non-matching omschrijving") {
                val roltypeNotFoundException = shouldThrow<RoltypeNotFoundException> {
                    ztcClientService.readRoltype(
                        zaaktypeUri,
                        OmschrijvingGeneriekEnum.BEHANDELAAR,
                        "Behandelaar"
                    )
                }

                then("it should throw RoltypeNotFoundException") {
                    roltypeNotFoundException.message shouldBe
                        "Roltype with aard 'behandelaar' and omschrijving 'Behandelaar' " +
                        "not found for zaaktype '$zaaktypeUri'"
                }
            }
        }

        given("a zaaktype with no roltypen at all") {
            val zaaktypeUri = URI("https://example.com/zaaktypes/${UUID.randomUUID()}")
            every {
                ztcClient.roltypeListGeneriek(any<RoltypeListGeneriekParameters>())
            } returns Results(emptyList(), 0)

            `when`("readRoltype is called") {
                val roltypeNotFoundException = shouldThrow<RoltypeNotFoundException> {
                    ztcClientService.readRoltype(
                        zaaktypeUri,
                        OmschrijvingGeneriekEnum.BEHANDELAAR,
                        "Behandelaar"
                    )
                }

                then("it should throw RoltypeNotFoundException") {
                    roltypeNotFoundException.message shouldBe
                        "Roltype with aard 'behandelaar' and omschrijving 'Behandelaar' " +
                        "not found for zaaktype '$zaaktypeUri'"
                }
            }
        }
    }
})
