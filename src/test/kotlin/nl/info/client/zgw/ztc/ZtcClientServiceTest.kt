/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.ztc

import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.date.shouldBeAfter
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import net.atos.client.zgw.shared.util.ZGWClientHeadersFactory
import nl.info.client.zgw.ztc.model.createZaakType
import nl.info.zac.configuratie.ConfiguratieService
import java.net.URI
import java.time.ZonedDateTime
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

class ZtcClientServiceTest : BehaviorSpec({
    val ztcClient = mockk<ZtcClient>()
    val zgwClientHeadersFactory = mockk<ZGWClientHeadersFactory>()
    val configuratieService = mockk<ConfiguratieService>()
    val ztcClientService = ZtcClientService(
        ztcClient = ztcClient,
        zgwClientHeadersFactory = zgwClientHeadersFactory,
        configuratieService = configuratieService,
    )
    val initialUUID = UUID.randomUUID()
    val expectedZaakType = createZaakType()
    val testStartDateTime = ZonedDateTime.now()
    lateinit var initialDateTime: ZonedDateTime

    beforeEach {
        checkUnnecessaryStub()
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

        When("reading lots of zaak types") {
            (1..101).forEach {
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
                        this?.missCount() shouldBe 102
                        this?.evictionCount() shouldBe 82
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
