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
import io.kotest.matchers.longs.shouldBeGreaterThan
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import nl.info.client.zgw.shared.ZgwApiService.Companion.ROLTYPE_OMSCHRIJVING_BEHANDELAAR
import nl.info.client.zgw.shared.model.Results
import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.ztc.exception.CatalogusNotFoundException
import nl.info.client.zgw.ztc.exception.RoltypeNotFoundException
import nl.info.client.zgw.ztc.model.RoltypeListGeneriekParameters
import nl.info.client.zgw.ztc.model.StatustypeListParameters
import nl.info.client.zgw.ztc.model.ZaaktypeListParameters
import nl.info.client.zgw.ztc.model.createBesluitType
import nl.info.client.zgw.ztc.model.createCatalogus
import nl.info.client.zgw.ztc.model.createCatalogusListParameters
import nl.info.client.zgw.ztc.model.createEigenschap
import nl.info.client.zgw.ztc.model.createInformatieObjectType
import nl.info.client.zgw.ztc.model.createResultaatType
import nl.info.client.zgw.ztc.model.createRolType
import nl.info.client.zgw.ztc.model.createStatusType
import nl.info.client.zgw.ztc.model.createZaakType
import nl.info.client.zgw.ztc.model.createZaakTypeInformatieObjectType
import nl.info.client.zgw.ztc.model.generated.OmschrijvingGeneriekEnum
import java.net.URI
import java.time.ZonedDateTime
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

class ZtcClientServiceTest : BehaviorSpec({
    val cacheMaxSize = 100L
    val cacheExpirationSeconds = 86_400L
    fun createZtcClientService(ztcClient: ZtcClient) = ZtcClientService(
        ztcClient = ztcClient,
        cacheMaxSize = cacheMaxSize,
        cacheExpirationSeconds = cacheExpirationSeconds
    )
    val ztcClient = mockk<ZtcClient>()
    val ztcClientService = createZtcClientService(ztcClient)
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
            (1..cacheMaxSize + 1).forEach { _ ->
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
                        this?.missCount() shouldBe cacheMaxSize + 2
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
        } returns Results(countValue = 1, resultsValue = listOf(firstPageZaakType), nextValue = nextPageUri, previousValue = null)
        every {
            ztcClient.zaaktypeList(match<ZaaktypeListParameters> { it.getPage() == 2 })
        } returns Results(countValue = 1, resultsValue = listOf(secondPageZaakType), nextValue = null, previousValue = null)

        `when`("listZaaktypen is called") {
            val zaaktypen = ztcClientService.listZaaktypen(catalogusUri)

            then("it should return the zaaktypes of all pages combined") {
                zaaktypen shouldBe listOf(firstPageZaakType, secondPageZaakType)
            }
        }
    }

    given("zaaktypes for a catalogus that have been listed") {
        val catalogusUri = URI("https://example.com/catalogussen/${UUID.randomUUID()}")
        val firstZaakType = createZaakType()
        val secondZaakType = createZaakType()
        every {
            ztcClient.zaaktypeList(match<ZaaktypeListParameters> { it.getCatalogus() == catalogusUri })
        } returns Results(
            countValue = 2,
            resultsValue = listOf(firstZaakType, secondZaakType),
            nextValue = null,
            previousValue = null
        )
        ztcClientService.listZaaktypen(catalogusUri)

        `when`("readZaaktype is called for each listed zaaktype") {
            val firstReadZaakType = ztcClientService.readZaaktype(firstZaakType.url.extractUuid())
            val secondReadZaakType = ztcClientService.readZaaktype(secondZaakType.url.extractUuid())

            then("it should return the listed zaaktypes from the cache without reading them from the ZTC") {
                firstReadZaakType shouldBe firstZaakType
                secondReadZaakType shouldBe secondZaakType
                verify(exactly = 0) { ztcClient.zaaktypeRead(any()) }
            }
        }
    }

    context("Refreshing the caches") {
        fun ZtcClient.stubEmptyTypeListsExceptZaaktypenAndStatustypen() {
            every { resultaattypeList(any()) } returns Results(emptyList(), 0)
            every { besluittypeList(any()) } returns Results(emptyList(), 0)
            every { informatieobjecttypeList(any()) } returns Results(emptyList(), 0)
            every { zaaktypeinformatieobjecttypeList(any()) } returns Results(emptyList(), 0)
            every { eigenschapList(any()) } returns Results(emptyList(), 0)
            every { roltypeList(any()) } returns Results(emptyList(), 0)
        }

        given(
            """a catalogus with a zaaktype with related types, a zaaktype without related types,
            and a statustype of a zaaktype in another catalogus"""
        ) {
            val refreshedZtcClient = mockk<ZtcClient>()
            val refreshedZtcClientService = createZtcClientService(refreshedZtcClient)
            val catalogusUri = URI("https://example.com/catalogussen/${UUID.randomUUID()}")
            val zaakType = createZaakType()
            val zaakTypeWithoutRelatedTypes = createZaakType()
            val statusType = createStatusType().apply { zaaktype = zaakType.url }
            val statusTypeOfOtherCatalogus = createStatusType().apply {
                zaaktype = URI("https://example.com/zaaktypes/${UUID.randomUUID()}")
            }
            val resultaatType = createResultaatType().apply { zaaktype = zaakType.url }
            val besluitType = createBesluitType(zaaktypen = listOf(zaakType.url))
            val informatieObjectType = createInformatieObjectType()
            val zaakTypeInformatieObjectType = createZaakTypeInformatieObjectType(
                zaaktype = zaakType.url,
                informatieobjecttype = informatieObjectType.url
            )
            val eigenschap = createEigenschap(zaaktype = zaakType.url)
            val rolType = createRolType(zaakTypeUri = zaakType.url)
            every {
                refreshedZtcClient.zaaktypeList(any())
            } returns Results(listOf(zaakType, zaakTypeWithoutRelatedTypes), 2)
            every {
                refreshedZtcClient.statustypeList(any())
            } returns Results(listOf(statusType, statusTypeOfOtherCatalogus), 2)
            every { refreshedZtcClient.resultaattypeList(any()) } returns Results(listOf(resultaatType), 1)
            every { refreshedZtcClient.besluittypeList(any()) } returns Results(listOf(besluitType), 1)
            every {
                refreshedZtcClient.informatieobjecttypeList(any())
            } returns Results(listOf(informatieObjectType), 1)
            every {
                refreshedZtcClient.zaaktypeinformatieobjecttypeList(any())
            } returns Results(listOf(zaakTypeInformatieObjectType), 1)
            every { refreshedZtcClient.eigenschapList(any()) } returns Results(listOf(eigenschap), 1)
            every { refreshedZtcClient.roltypeList(any()) } returns Results(listOf(rolType), 1)

            `when`("the caches are refreshed") {
                refreshedZtcClientService.refreshCaches(catalogusUri)

                then("the zaaktypes and their related types are read from the caches") {
                    with(refreshedZtcClientService) {
                        listZaaktypen(catalogusUri) shouldBe listOf(zaakType, zaakTypeWithoutRelatedTypes)
                        readZaaktype(zaakType.url) shouldBe zaakType
                        readStatustypen(zaakType.url) shouldBe listOf(statusType)
                        readStatustype(statusType.url) shouldBe statusType
                        readResultaattypen(zaakType.url) shouldBe listOf(resultaatType)
                        readResultaattype(resultaatType.url) shouldBe resultaatType
                        readBesluittypen(zaakType.url) shouldBe listOf(besluitType)
                        readBesluittype(besluitType.url) shouldBe besluitType
                        readZaaktypeInformatieobjecttypen(zaakType.url) shouldBe listOf(zaakTypeInformatieObjectType)
                        readInformatieobjecttypen(zaakType.url) shouldBe listOf(informatieObjectType)
                        readInformatieobjecttype(informatieObjectType.url) shouldBe informatieObjectType
                        readEigenschappen(zaakType.url) shouldBe listOf(eigenschap)
                        listRoltypen(zaakType.url) shouldBe listOf(rolType)
                        readRoltype(rolType.url.extractUuid()) shouldBe rolType
                    }
                    verify(exactly = 1) {
                        refreshedZtcClient.zaaktypeList(any())
                        refreshedZtcClient.statustypeList(any())
                        refreshedZtcClient.resultaattypeList(any())
                        refreshedZtcClient.besluittypeList(any())
                        refreshedZtcClient.informatieobjecttypeList(any())
                        refreshedZtcClient.zaaktypeinformatieobjecttypeList(any())
                        refreshedZtcClient.eigenschapList(any())
                        refreshedZtcClient.roltypeList(any())
                    }
                }

                and("the zaaktype without related types has empty lists in the caches") {
                    with(refreshedZtcClientService) {
                        readStatustypen(zaakTypeWithoutRelatedTypes.url) shouldBe emptyList()
                        readResultaattypen(zaakTypeWithoutRelatedTypes.url) shouldBe emptyList()
                        readBesluittypen(zaakTypeWithoutRelatedTypes.url) shouldBe emptyList()
                        readZaaktypeInformatieobjecttypen(zaakTypeWithoutRelatedTypes.url) shouldBe emptyList()
                        readInformatieobjecttypen(zaakTypeWithoutRelatedTypes.url) shouldBe emptyList()
                        readEigenschappen(zaakTypeWithoutRelatedTypes.url) shouldBe emptyList()
                        listRoltypen(zaakTypeWithoutRelatedTypes.url) shouldBe emptyList()
                    }
                }

                and("the statustype of the zaaktype in another catalogus is not cached") {
                    refreshedZtcClientService.estimatedCacheSizes()["ZTC UUID -> StatusType"] shouldBe 1
                }
            }
        }

        given("statustypen that are spread over two pages") {
            val refreshedZtcClient = mockk<ZtcClient>()
            val refreshedZtcClientService = createZtcClientService(refreshedZtcClient)
            val catalogusUri = URI("https://example.com/catalogussen/${UUID.randomUUID()}")
            val zaakType = createZaakType()
            val firstPageStatusType = createStatusType().apply { zaaktype = zaakType.url }
            val secondPageStatusType = createStatusType().apply { zaaktype = zaakType.url }
            every { refreshedZtcClient.zaaktypeList(any()) } returns Results(listOf(zaakType), 1)
            every {
                refreshedZtcClient.statustypeList(any())
            } answers {
                if (firstArg<StatustypeListParameters>().getPage() == 1) {
                    Results(
                        countValue = 2,
                        resultsValue = listOf(firstPageStatusType),
                        nextValue = URI("https://example.com/statustypen?page=2"),
                        previousValue = null
                    )
                } else {
                    Results(listOf(secondPageStatusType), 2)
                }
            }
            refreshedZtcClient.stubEmptyTypeListsExceptZaaktypenAndStatustypen()

            `when`("the caches are refreshed") {
                refreshedZtcClientService.refreshCaches(catalogusUri)

                then("the statustypen of both pages are cached for the zaaktype") {
                    refreshedZtcClientService.readStatustypen(zaakType.url) shouldBe
                        listOf(firstPageStatusType, secondPageStatusType)
                    verify(exactly = 2) { refreshedZtcClient.statustypeList(any()) }
                }
            }
        }

        given("statustypen that fail to load") {
            val refreshedZtcClient = mockk<ZtcClient>()
            val refreshedZtcClientService = createZtcClientService(refreshedZtcClient)
            val catalogusUri = URI("https://example.com/catalogussen/${UUID.randomUUID()}")
            val zaakType = createZaakType()
            every { refreshedZtcClient.zaaktypeList(any()) } returns Results(listOf(zaakType), 1)
            every { refreshedZtcClient.statustypeList(any()) } throws RuntimeException("fakeExceptionMessage")
            refreshedZtcClient.stubEmptyTypeListsExceptZaaktypenAndStatustypen()

            `when`("the caches are refreshed") {
                refreshedZtcClientService.refreshCaches(catalogusUri)

                then("no statustypen are cached, but the other types are") {
                    with(refreshedZtcClientService.estimatedCacheSizes()) {
                        this["ZTC URI -> List<StatusType>"] shouldBe 0
                        this["ZTC URI -> List<ResultaatType>"] shouldBe 1
                        this["ZTC URI -> List<RolType>"] shouldBe 1
                    }
                }
            }
        }

        given("zaaktypen that fail to load") {
            val refreshedZtcClient = mockk<ZtcClient>()
            val refreshedZtcClientService = createZtcClientService(refreshedZtcClient)
            val catalogusUri = URI("https://example.com/catalogussen/${UUID.randomUUID()}")
            every { refreshedZtcClient.zaaktypeList(any()) } throws RuntimeException("fakeExceptionMessage")

            `when`("the caches are refreshed") {
                refreshedZtcClientService.refreshCaches(catalogusUri)

                then("the related types are not refreshed, because they are cached per zaaktype") {
                    verify(exactly = 0) {
                        refreshedZtcClient.statustypeList(any())
                        refreshedZtcClient.roltypeList(any())
                    }
                }
            }
        }
    }

    given("a zaaktype in the cache and no statustypen in the cache") {
        val measuredZtcClient = mockk<ZtcClient>()
        val measuredZtcClientService = createZtcClientService(measuredZtcClient)
        val zaakType = createZaakType()
        every { measuredZtcClient.zaaktypeRead(zaakType.url.extractUuid()) } returns zaakType
        measuredZtcClientService.readZaaktype(zaakType.url)

        `when`("the memory usage of the caches is measured") {
            val memoryUsages = measuredZtcClientService.estimatedMemoryUsage()

            then("the zaaktype cache uses memory and the statustype cache uses none") {
                memoryUsages["ZTC UUID -> ZaakType"].shouldNotBeNull() shouldBeGreaterThan 0
                memoryUsages["ZTC UUID -> StatusType"] shouldBe 0
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
