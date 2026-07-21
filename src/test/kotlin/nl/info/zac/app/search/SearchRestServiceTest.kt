/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.search

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import nl.info.client.zgw.model.createZaak
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.model.createZaakType
import nl.info.zac.app.search.converter.RestZoekParametersConverter
import nl.info.zac.app.search.converter.RestZoekResultaatConverter
import nl.info.zac.app.search.model.RestZaakKoppelenZoekObject
import nl.info.zac.app.search.model.RestZoekKoppelenParameters
import nl.info.zac.app.search.model.RestZoekResultaat
import nl.info.zac.app.search.model.createRestZoekKoppelenParameters
import nl.info.zac.app.search.model.createRestZoekParameters
import nl.info.zac.app.search.model.createRestZoekResultaatForDocumentZoekObjects
import nl.info.zac.app.search.model.createRestZoekResultaatForTaakZoekObjects
import nl.info.zac.app.search.model.createRestZoekResultaatForZaakKoppelenZoekObjects
import nl.info.zac.app.search.model.createRestZoekResultaatForZaakZoekObjects
import nl.info.zac.app.search.model.createZoekParameters
import nl.info.zac.app.search.model.createZoekResultaatForZaakZoekObjecten
import nl.info.zac.policy.PolicyService
import nl.info.zac.policy.exception.PolicyException
import nl.info.zac.policy.output.createOverigeRechten
import nl.info.zac.policy.output.createWerklijstRechten
import nl.info.zac.search.SearchService
import nl.info.zac.search.model.ZoekParameters
import nl.info.zac.search.model.ZoekResultaat
import nl.info.zac.search.model.createZaakZoekObject
import nl.info.zac.search.model.zoekobject.ZoekObjectType
import java.net.URI
import java.util.UUID

class SearchRestServiceTest : BehaviorSpec({
    val searchService = mockk<SearchService>()
    val restZoekParametersConverter = mockk<RestZoekParametersConverter>()
    val restZoekResultaatConverter = mockk<RestZoekResultaatConverter>()
    val policyService = mockk<PolicyService>()
    val zrcClientService = mockk<ZrcClientService>()
    val ztcClientService = mockk<ZtcClientService>()

    val searchRestService = SearchRestService(
        searchService = searchService,
        restZoekZaakParametersConverter = restZoekParametersConverter,
        restZoekResultaatConverter = restZoekResultaatConverter,
        policyService = policyService,
        zrcClientService = zrcClientService,
        ztcClientService = ztcClientService
    )

    afterEach {
        checkUnnecessaryStub()
    }

    given("A search request for ZAAK type") {
        val restZoekParameters = createRestZoekParameters(type = ZoekObjectType.ZAAK)
        val zoekParameters = createZoekParameters()
        val zoekResultaat = createZoekResultaatForZaakZoekObjecten()
        val restZoekResultaat = createRestZoekResultaatForZaakZoekObjects()

        every { policyService.readWerklijstRechten() } returns createWerklijstRechten(zakenTaken = true)
        every { restZoekParametersConverter.convert(restZoekParameters) } returns zoekParameters
        every { searchService.search(zoekParameters) } returns zoekResultaat
        every { restZoekResultaatConverter.convert(zoekResultaat, restZoekParameters) } returns restZoekResultaat

        `when`("listSearchResults is called") {
            val result = searchRestService.listSearchResults(restZoekParameters)

            then("the search result should be returned") {
                result shouldBe restZoekResultaat
            }

            And("the correct services should be invoked") {
                verify(exactly = 1) {
                    policyService.readWerklijstRechten()
                    restZoekParametersConverter.convert(restZoekParameters)
                    searchService.search(zoekParameters)
                    restZoekResultaatConverter.convert(zoekResultaat, restZoekParameters)
                }
            }
        }
    }

    given("A search request for TAAK type") {
        val restZoekParameters = createRestZoekParameters(type = ZoekObjectType.TAAK)
        val zoekParameters = createZoekParameters()
        val zoekResultaat = createZoekResultaatForZaakZoekObjecten()
        val restZoekResultaat = createRestZoekResultaatForTaakZoekObjects()

        every { policyService.readWerklijstRechten() } returns createWerklijstRechten(zakenTaken = true)
        every { restZoekParametersConverter.convert(restZoekParameters) } returns zoekParameters
        every { searchService.search(zoekParameters) } returns zoekResultaat
        every { restZoekResultaatConverter.convert(zoekResultaat, restZoekParameters) } returns restZoekResultaat

        `when`("listSearchResults is called") {
            val result = searchRestService.listSearchResults(restZoekParameters)

            then("the search result should be returned") {
                result shouldBe restZoekResultaat
            }
        }
    }

    given("A search request for DOCUMENT type") {
        val restZoekParameters = createRestZoekParameters(type = ZoekObjectType.DOCUMENT)
        val zoekParameters = createZoekParameters()
        val zoekResultaat = createZoekResultaatForZaakZoekObjecten()
        val restZoekResultaat = createRestZoekResultaatForDocumentZoekObjects()

        every { policyService.readOverigeRechten() } returns createOverigeRechten(zoeken = true)
        every { restZoekParametersConverter.convert(restZoekParameters) } returns zoekParameters
        every { searchService.search(zoekParameters) } returns zoekResultaat
        every { restZoekResultaatConverter.convert(zoekResultaat, restZoekParameters) } returns restZoekResultaat

        `when`("listSearchResults is called") {
            val result = searchRestService.listSearchResults(restZoekParameters)

            then("the search result should be returned") {
                result shouldBe restZoekResultaat
            }

            And("the overigeRechten should be checked") {
                verify(exactly = 1) {
                    policyService.readOverigeRechten()
                }
            }
        }
    }

    given("A search request for ZAAK type without permission") {
        val restZoekParameters = createRestZoekParameters(type = ZoekObjectType.ZAAK)

        every { policyService.readWerklijstRechten() } returns createWerklijstRechten(zakenTaken = false)

        `when`("listSearchResults is called") {
            val policyException = shouldThrow<PolicyException> {
                searchRestService.listSearchResults(restZoekParameters)
            }

            then("a PolicyException should be thrown") {
                policyException shouldNotBe null
            }
        }
    }

    given("A search request for DOCUMENT type without permission") {
        val restZoekParameters = createRestZoekParameters(type = ZoekObjectType.DOCUMENT)

        every { policyService.readOverigeRechten() } returns createOverigeRechten(zoeken = false)

        `when`("listSearchResults is called") {
            val policyException = shouldThrow<PolicyException> {
                searchRestService.listSearchResults(restZoekParameters)
            }
            then("a PolicyException should be thrown") {
                policyException shouldNotBe null
            }
        }
    }

    given("A request to list zaken for information object type") {
        val zaakIdentification = "fakeZaakIdentification"
        val informationObjectTypeUuid = UUID.randomUUID()
        val zaaktypeUuid = UUID.randomUUID()
        val restZoekKoppelenParameters = RestZoekKoppelenParameters(
            page = 2,
            rows = 10,
            zaakIdentificator = zaakIdentification,
            informationObjectTypeUuid = informationObjectTypeUuid
        )
        val zaakZoekObject = createZaakZoekObject(identificatie = zaakIdentification)
        val zoekResultaat = ZoekResultaat(listOf(zaakZoekObject), 1)
        val zaak = createZaak(
            identificatie = zaakIdentification,
            zaaktypeUri = URI("https://example.com/zaaktype/$zaaktypeUuid")
        )
        val zaakType = createZaakType(
            informatieObjectTypen = listOf(URI("https://example.com/informatieobjecttype/$informationObjectTypeUuid"))
        )
        val restZoekResultaat = mockk<RestZoekResultaat<RestZaakKoppelenZoekObject>>()
        val zoekParametersSlot = slot<ZoekParameters>()

        every { policyService.readWerklijstRechten() } returns createWerklijstRechten(zakenTaken = true)
        every { searchService.search(capture(zoekParametersSlot)) } returns zoekResultaat
        every { zrcClientService.readZaakByID(zaakIdentification) } returns zaak
        every { ztcClientService.readZaaktype(zaaktypeUuid) } returns zaakType
        every { restZoekResultaatConverter.convert(zoekResultaat, listOf(true)) } returns restZoekResultaat

        `when`("listZakenForInformationObjectType is called") {
            val result = searchRestService.listZakenForInformationObjectType(restZoekKoppelenParameters)

            then("the search result should be returned") {
                result shouldBe restZoekResultaat
            }

            And("the correct services should be invoked") {
                verify(exactly = 1) {
                    policyService.readWerklijstRechten()
                    searchService.search(any<ZoekParameters>())
                    zrcClientService.readZaakByID(zaakIdentification)
                    ztcClientService.readZaaktype(zaaktypeUuid)
                    restZoekResultaatConverter.convert(zoekResultaat, listOf(true))
                }
                with(zoekParametersSlot.captured) {
                    this.rows shouldBe 10
                    this.start shouldBe 2 * 10
                }
            }
        }
    }

    given("A request to list zaken for information object type where document is not linkable") {
        val zaakIdentification = "ZAAK-2024-00002"
        val informationObjectTypeUuid = UUID.randomUUID()
        val differentInformationObjectTypeUuid = UUID.randomUUID()
        val zaaktypeUuid = UUID.randomUUID()
        val restZoekKoppelenParameters = createRestZoekKoppelenParameters(
            page = 0,
            rows = 10,
            zaakIdentificator = zaakIdentification,
            informationObjectTypeUuid = informationObjectTypeUuid
        )
        val zaakZoekObject = createZaakZoekObject(identificatie = zaakIdentification)
        val zoekResultaat = createZoekResultaatForZaakZoekObjecten(listOf(zaakZoekObject), 1)
        val zaak = createZaak(
            identificatie = zaakIdentification,
            zaaktypeUri = URI("https://example.com/zaaktype/$zaaktypeUuid")
        )
        val zaakType = createZaakType(
            informatieObjectTypen = listOf(
                URI("https://example.com/informatieobjecttype/$differentInformationObjectTypeUuid")
            )
        )
        val restZoekResultaat = createRestZoekResultaatForZaakKoppelenZoekObjects()

        every { policyService.readWerklijstRechten() } returns createWerklijstRechten(zakenTaken = true)
        every { searchService.search(any<ZoekParameters>()) } returns zoekResultaat
        every { zrcClientService.readZaakByID(zaakIdentification) } returns zaak
        every { ztcClientService.readZaaktype(zaaktypeUuid) } returns zaakType
        every { restZoekResultaatConverter.convert(zoekResultaat, listOf(false)) } returns restZoekResultaat

        `when`("listZakenForInformationObjectType is called") {
            val result = searchRestService.listZakenForInformationObjectType(restZoekKoppelenParameters)

            then("the search result should be returned with document not linkable") {
                result shouldBe restZoekResultaat
            }

            And("the converter should be called with false for linkability") {
                verify(exactly = 1) {
                    restZoekResultaatConverter.convert(zoekResultaat, listOf(false))
                }
            }
        }
    }

    given("A request to list zaken for information object type without permission") {
        val restZoekKoppelenParameters = createRestZoekKoppelenParameters()

        every { policyService.readWerklijstRechten() } returns createWerklijstRechten(zakenTaken = false)

        `when`("listZakenForInformationObjectType is called") {
            val policyException = shouldThrow<PolicyException> {
                searchRestService.listZakenForInformationObjectType(restZoekKoppelenParameters)
            }
            then("a PolicyException should be thrown") {
                policyException shouldNotBe null
            }
        }
    }

    given("A request to list zaken with empty search results") {
        val zaakIdentification = "fakeZaakIdentification"
        val informationObjectTypeUuid = UUID.randomUUID()
        val restZoekKoppelenParameters = createRestZoekKoppelenParameters(
            page = 0,
            rows = 10,
            zaakIdentificator = zaakIdentification,
            informationObjectTypeUuid = informationObjectTypeUuid
        )
        val zoekResultaat = createZoekResultaatForZaakZoekObjecten(items = emptyList(), count = 0)
        val restZoekResultaat = createRestZoekResultaatForZaakKoppelenZoekObjects()

        every { policyService.readWerklijstRechten() } returns createWerklijstRechten(zakenTaken = true)
        every { searchService.search(any<ZoekParameters>()) } returns zoekResultaat
        every { restZoekResultaatConverter.convert(zoekResultaat, emptyList()) } returns restZoekResultaat

        `when`("listZakenForInformationObjectType is called") {
            val result = searchRestService.listZakenForInformationObjectType(restZoekKoppelenParameters)

            then("the search result should be returned with empty list") {
                result shouldBe restZoekResultaat
            }

            And("no zaak or zaaktype services should be invoked") {
                verify(exactly = 0) {
                    zrcClientService.readZaakByID(any())
                    ztcClientService.readZaaktype(any<UUID>())
                }
            }
        }
    }
})
