/*
 * SPDX-FileCopyrightText: 2024 Lifely
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
import nl.info.client.zgw.ztc.model.createCatalogus
import nl.info.client.zgw.ztc.model.createCatalogusListParameters
import nl.info.client.zgw.ztc.model.createZaakType
import java.net.URI
import java.time.ZonedDateTime
import java.util.Optional
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

    beforeEach {
        checkUnnecessaryStub()
    }

    Given("An existing catalogus for the given parameters") {
        val catalogusListParameters = createCatalogusListParameters()
        val expectedCatalogus = createCatalogus()
        every { ztcClient.catalogusList(catalogusListParameters).singleResult } returns Optional.of(expectedCatalogus)

        When("readCatalogus is called") {
            val result = ztcClientService.readCatalogus(catalogusListParameters)

            Then("it should return the expected catalogus") {
                result shouldBe expectedCatalogus
            }
        }
    }

    Given("No catalog for the given parameters") {
        val catalogusListParameters = createCatalogusListParameters()
        every { ztcClient.catalogusList(catalogusListParameters).singleResult } returns Optional.empty()

        When("readCatalogus is called") {
            val exception = shouldThrow<CatalogusNotFoundException> {
                ztcClientService.readCatalogus(catalogusListParameters)
            }

            Then("it should throw an exception") {
                exception.message shouldBe
                    "No catalogus found for catalogus list parameters " +
                    "'CatalogusListParameters(domein=null, domeinIn=null, rsin=null, rsinIn=null, page=null)'."
            }
        }
    }

    Given("ZTC client service") {

        When("reading cache time for the first time") {
            initialDateTime = ztcClientService.resetCacheTimeToNow()

            Then("it should return time after test was started") {
                initialDateTime shouldBeAfter testStartDateTime
            }

            Then("it should generate the value and store it in the cache") {
                with(ztcClientService.cacheStatistics()["ZTC Time"]) {
                    this?.hitCount() shouldBe 0
                    this?.missCount() shouldBe 1
                }
            }
        }

        When("reading cache time for the second time") {
            val dateTime = ztcClientService.resetCacheTimeToNow()

            Then("it should cached the same time") {
                dateTime shouldBeEqual initialDateTime
            }

            Then("it should fetch the value from the cache") {
                with(ztcClientService.cacheStatistics()["ZTC Time"]) {
                    this?.hitCount() shouldBe 1
                    this?.missCount() shouldBe 1
                }
            }
        }

        When("reading zaak type") {
            every { ztcClient.zaaktypeRead(initialUUID) } returns expectedZaakType
            val zaakType = ztcClientService.readZaaktype(initialUUID)

            Then("it should return valid zaaktype") {
                zaakType shouldBe expectedZaakType
            }

            Then("it should generate the value and store it in the cache") {
                with(ztcClientService.cacheStatistics()["ZTC UUID -> ZaakType"]) {
                    this?.hitCount() shouldBe 0
                    this?.missCount() shouldBe 1
                }
            }
        }

        When("reading more zaak types than the cache can hold") {
            (1..MAX_CACHE_SIZE + 1).forEach { _ ->
                val generatedUUID = UUID.randomUUID()
                every {
                    ztcClient.zaaktypeRead(generatedUUID)
                } returns createZaakType(uri = URI("https://example.com/zaaktype/$generatedUUID"))
                ztcClientService.readZaaktype(generatedUUID)
            }

            Then("cache starts evicting") {
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

    Given("ZTC client service time cache was cleared") {
        ztcClientService.clearCacheTime()

        When("reading the cache time") {
            val cacheDateTime = ztcClientService.resetCacheTimeToNow()

            Then("time should be updated") {
                cacheDateTime shouldBeAfter initialDateTime
            }

            Then("cache statistics should be ok") {
                with(ztcClientService.cacheStatistics()["ZTC Time"]) {
                    this?.hitCount() shouldBe 1
                    this?.missCount() shouldBe 2
                }
            }
        }
    }
})
