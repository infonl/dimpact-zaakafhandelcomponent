package net.atos.client.zgw.ztc

import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.core.test.TestCase
import io.kotest.matchers.date.shouldBeAfter
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.shouldBe
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import net.atos.client.zgw.ztc.model.createZaakType
import net.atos.zac.configuratie.ConfiguratieService
import java.time.ZonedDateTime
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

@MockKExtension.CheckUnnecessaryStub
class ZTCClientServiceTest : BehaviorSpec() {
    private val ztcClient = mockk<ZTCClient>()
    private val configuratieService = mockk<ConfiguratieService>()

    @InjectMockKs
    lateinit var ztcClientService: ZTCClientService

    private val initialUUID = UUID.randomUUID()
    private val expectedZaakType = createZaakType()

    override suspend fun beforeTest(testCase: TestCase) {
        every { configuratieService.readZgwApiClientMpRestUrl() } returns "url"
        every { ztcClient.zaaktypeRead(initialUUID) } returns expectedZaakType
        MockKAnnotations.init(this)
    }

    private val testStartDateTime = ZonedDateTime.now()
    private lateinit var initialDateTime: ZonedDateTime

    init {
        Given("ZTC client service") {
            When("reading cache time for the first time") {
                initialDateTime = ztcClientService.readCacheTime()

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
                val dateTime = ztcClientService.readCacheTime()

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
                    every { ztcClient.zaaktypeRead(generatedUUID) } returns createZaakType(uuid = generatedUUID)
                    ztcClientService.readZaaktype(generatedUUID)
                }

                Then("cache starts evicting") {
                    eventually(5.seconds) {
                        with(ztcClientService.cacheStatistics()["ZTC UUID -> ZaakType"]) {
                            this?.hitCount() shouldBe 0
                            this?.missCount() shouldBe 102
                            this?.evictionCount() shouldBe 2
                        }
                    }
                }
            }
        }

        Given("ZTC client service time cache was cleared") {
            ztcClientService.clearCacheTime()

            When("reading the cache time") {
                val cacheDateTime = ztcClientService.readCacheTime()

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
    }
}
