/*
 * SPDX-FileCopyrightText: 2025 INFO.nl, 2025 Dimpact
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.zaak

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import jakarta.enterprise.inject.Instance
import net.atos.zac.event.EventingService
import net.atos.zac.websocket.event.ScreenEvent
import nl.info.client.zgw.model.createZaak
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.client.zgw.zrc.model.generated.ArchiefnominatieEnum
import nl.info.client.zgw.zrc.model.generated.GerelateerdeZaak
import nl.info.client.zgw.zrc.model.generated.Zaak
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.model.createZaakType
import nl.info.zac.app.zaak.model.RelatieType
import nl.info.zac.app.zaak.model.createRestZaakLinkData
import nl.info.zac.app.zaak.model.createRestZaakUnlinkData
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.authentication.createLoggedInUser
import nl.info.zac.policy.PolicyService
import nl.info.zac.policy.exception.PolicyException
import nl.info.zac.policy.output.createZaakRechten
import nl.info.zac.search.IndexingService
import nl.info.zac.search.SearchService
import nl.info.zac.search.model.ZaakIndicatie.DEELZAAK
import nl.info.zac.search.model.ZaakIndicatie.HOOFDZAAK
import nl.info.zac.search.model.ZoekParameters
import nl.info.zac.search.model.ZoekResultaat
import nl.info.zac.search.model.ZoekVeld
import nl.info.zac.search.model.createZaakZoekObject
import nl.info.zac.search.model.zoekobject.ZoekObjectType.ZAAK
import nl.info.zac.zaak.ZaakService
import java.net.URI
import java.util.UUID

private const val OMSCHRIJVING = "fakeOmschrijving"
private const val ZAAK_TYPE_OMSCHRIJVING = "fakeZaakTypeOmschrijving"
private const val STATUS_TYPE_OMSCHRIJVING = "Afgerond"

@Suppress("LargeClass")
class ZaakKoppelenRestServiceTest : BehaviorSpec({
    isolationMode = IsolationMode.InstancePerTest
    val zoekZaakIdentifier = "ZAAK-2000-00002"
    val zaakTypeUuid = UUID.randomUUID()
    val zaakTypeURI = URI(zaakTypeUuid.toString())
    val zaakZoekObjectTypeUuid = UUID.randomUUID().toString()
    val page = 0
    val rows = 10

    val eventingService = mockk<EventingService>()
    val indexingService = mockk<IndexingService>()
    val policyService = mockk<PolicyService>()
    val searchService = mockk<SearchService>()
    val zaakService = mockk<ZaakService>()
    val zrcClientService = mockk<ZrcClientService>()
    val ztcClientService = mockk<ZtcClientService>()
    val loggedInUserInstance = mockk<Instance<LoggedInUser>>()
    val zaakKoppelenRestService = ZaakKoppelenRestService(
        eventingService = eventingService,
        indexingService = indexingService,
        policyService = policyService,
        searchService = searchService,
        zaakService = zaakService,
        zrcClientService = zrcClientService,
        ztcClientService = ztcClientService,
        loggedInUserInstance = loggedInUserInstance
    )

    afterEach {
        checkUnnecessaryStub()
    }

    Given("A source zaak which is not linked and a target not linked zaak") {
        val sourceZaak = createZaak(
            identificatie = "ZAAK-2000-00001",
            archiefnominatie = ArchiefnominatieEnum.BLIJVEND_BEWAREN,
            zaaktypeUri = zaakTypeURI
        )
        val zaakZoekObject = createZaakZoekObject(
            type = ZAAK,
            zaaktypeOmschrijving = ZAAK_TYPE_OMSCHRIJVING,
            identificatie = zoekZaakIdentifier,
            omschrijving = OMSCHRIJVING,
            statustypeOmschrijving = STATUS_TYPE_OMSCHRIJVING,
            zaaktypeUuid = zaakZoekObjectTypeUuid,
            archiefNominatie = ArchiefnominatieEnum.BLIJVEND_BEWAREN.toString()
        )
        val zoekResultaat = ZoekResultaat(listOf(zaakZoekObject), 1)
        val loggedInUser = createLoggedInUser()
        val zaakType = createZaakType().apply {
            deelzaaktypen = listOf(URI(zaakZoekObjectTypeUuid))
        }

        every { zrcClientService.readZaak(sourceZaak.uuid) } returns sourceZaak
        every { searchService.zoek(any()) } returns zoekResultaat
        every { zaakService.readZaakTypeByZaak(sourceZaak) } returns zaakType
        every { policyService.readZaakRechten(sourceZaak, zaakType, loggedInUser) } returns createZaakRechten()
        every { loggedInUserInstance.get() } returns loggedInUser

        When("findLinkableZaken with GERELATEERD is called") {
            every { policyService.readZaakRechtenForZaakZoekObject(zaakZoekObject) } returns createZaakRechten()

            val result = zaakKoppelenRestService.findLinkableZaken(
                zaakUuid = sourceZaak.uuid,
                zoekZaakIdentifier = zoekZaakIdentifier,
                relationType = RelatieType.GERELATEERD,
                page = page,
                rows = rows
            )

            Then("a single linkable zaak should be returned") {
                result.resultCount shouldBe 1
            }

            And("link is allowed") {
                with(result.results.first()) {
                    id shouldBe zaakZoekObject.getObjectId()
                    type shouldBe zaakZoekObject.getType()
                    identificatie shouldBe zoekZaakIdentifier
                    omschrijving shouldBe OMSCHRIJVING
                    zaaktypeOmschrijving shouldBe ZAAK_TYPE_OMSCHRIJVING
                    statustypeOmschrijving shouldBe STATUS_TYPE_OMSCHRIJVING
                    isKoppelbaar shouldBe true
                }
            }

            And("required services should've been invoked") {
                verify(exactly = 1) {
                    zrcClientService.readZaak(sourceZaak.uuid)
                    searchService.zoek(any())
                    zaakService.readZaakTypeByZaak(sourceZaak)
                    policyService.readZaakRechten(sourceZaak, zaakType, loggedInUser)
                    policyService.readZaakRechtenForZaakZoekObject(zaakZoekObject)
                }
            }
        }

        When("findLinkableZaken with HOOFDZAAK is called") {
            every { policyService.readZaakRechtenForZaakZoekObject(zaakZoekObject) } returns createZaakRechten()
            every {
                ztcClientService.readZaaktype(UUID.fromString(zaakZoekObjectTypeUuid)).deelzaaktypen
            } returns listOf(zaakTypeURI)

            val result = zaakKoppelenRestService.findLinkableZaken(
                zaakUuid = sourceZaak.uuid,
                zoekZaakIdentifier = zoekZaakIdentifier,
                relationType = RelatieType.HOOFDZAAK,
                page = page,
                rows = rows
            )

            Then("a single linkable zaak should be returned") {
                result.resultCount shouldBe 1
            }

            And("link is allowed") {
                with(result.results.first()) {
                    id shouldBe zaakZoekObject.getObjectId()
                    type shouldBe zaakZoekObject.getType()
                    identificatie shouldBe zoekZaakIdentifier
                    omschrijving shouldBe OMSCHRIJVING
                    zaaktypeOmschrijving shouldBe ZAAK_TYPE_OMSCHRIJVING
                    statustypeOmschrijving shouldBe STATUS_TYPE_OMSCHRIJVING
                    isKoppelbaar shouldBe true
                }
            }

            And("required services should've be invoked") {
                verify(exactly = 1) {
                    zrcClientService.readZaak(sourceZaak.uuid)
                    searchService.zoek(any())
                    zaakService.readZaakTypeByZaak(sourceZaak)
                    policyService.readZaakRechten(sourceZaak, zaakType, loggedInUser)
                    policyService.readZaakRechtenForZaakZoekObject(zaakZoekObject)
                }
                verify(exactly = 1) {
                    ztcClientService.readZaaktype(UUID.fromString(zaakZoekObjectTypeUuid))
                }
            }
        }

        When("findLinkableZaken with DEELZAAK is called") {
            every { policyService.readZaakRechtenForZaakZoekObject(zaakZoekObject) } returns createZaakRechten()

            val result = zaakKoppelenRestService.findLinkableZaken(
                zaakUuid = sourceZaak.uuid,
                zoekZaakIdentifier = zoekZaakIdentifier,
                relationType = RelatieType.DEELZAAK,
                page = page,
                rows = rows
            )

            Then("a single linkable zaak should be returned") {
                result.resultCount shouldBe 1
            }

            And("link is allowed") {
                with(result.results.first()) {
                    id shouldBe zaakZoekObject.getObjectId()
                    type shouldBe zaakZoekObject.getType()
                    identificatie shouldBe zoekZaakIdentifier
                    omschrijving shouldBe OMSCHRIJVING
                    zaaktypeOmschrijving shouldBe ZAAK_TYPE_OMSCHRIJVING
                    statustypeOmschrijving shouldBe STATUS_TYPE_OMSCHRIJVING
                    isKoppelbaar shouldBe true
                }
            }

            And("required services should've be invoked") {
                verify(exactly = 1) {
                    zrcClientService.readZaak(sourceZaak.uuid)
                    searchService.zoek(any())
                    zaakService.readZaakTypeByZaak(sourceZaak)
                    policyService.readZaakRechten(sourceZaak, zaakType, loggedInUser)
                    policyService.readZaakRechtenForZaakZoekObject(zaakZoekObject)
                }
            }
        }
    }

    Given("A source zaak which is not linked and a target hoofdzaak") {
        val sourceZaak = createZaak(
            identificatie = "ZAAK-2000-00001",
            archiefnominatie = ArchiefnominatieEnum.BLIJVEND_BEWAREN,
            zaaktypeUri = zaakTypeURI,
        )
        val zaakZoekObject = createZaakZoekObject(
            type = ZAAK,
            zaaktypeOmschrijving = ZAAK_TYPE_OMSCHRIJVING,
            identificatie = zoekZaakIdentifier,
            omschrijving = OMSCHRIJVING,
            statustypeOmschrijving = STATUS_TYPE_OMSCHRIJVING,
            zaaktypeUuid = zaakZoekObjectTypeUuid,
            archiefNominatie = ArchiefnominatieEnum.BLIJVEND_BEWAREN.toString(),
            indicatie = HOOFDZAAK
        )
        val zoekResultaat = ZoekResultaat(listOf(zaakZoekObject), 1)
        val loggedInUser = createLoggedInUser()
        val zaakType = createZaakType().apply { deelzaaktypen = emptyList() }

        every { zrcClientService.readZaak(sourceZaak.uuid) } returns sourceZaak
        every { searchService.zoek(any()) } returns zoekResultaat
        every { zaakService.readZaakTypeByZaak(sourceZaak) } returns zaakType
        every { policyService.readZaakRechten(sourceZaak, zaakType, loggedInUser) } returns createZaakRechten()
        every { policyService.readZaakRechtenForZaakZoekObject(zaakZoekObject) } returns createZaakRechten()
        every { loggedInUserInstance.get() } returns loggedInUser

        When("findLinkableZaken with HOOFDZAAK link is called") {
            every {
                ztcClientService.readZaaktype(UUID.fromString(zaakZoekObjectTypeUuid)).deelzaaktypen
            } returns emptyList()

            val result = zaakKoppelenRestService.findLinkableZaken(
                zaakUuid = sourceZaak.uuid,
                zoekZaakIdentifier = zoekZaakIdentifier,
                relationType = RelatieType.HOOFDZAAK,
                page = page,
                rows = rows
            )

            Then("a single linkable zaak should be returned") {
                result.resultCount shouldBe 1
            }

            And("link is not possible") {
                with(result.results.first()) {
                    id shouldBe zaakZoekObject.getObjectId()
                    type shouldBe zaakZoekObject.getType()
                    identificatie shouldBe zoekZaakIdentifier
                    omschrijving shouldBe OMSCHRIJVING
                    zaaktypeOmschrijving shouldBe ZAAK_TYPE_OMSCHRIJVING
                    statustypeOmschrijving shouldBe STATUS_TYPE_OMSCHRIJVING
                    isKoppelbaar shouldBe false
                }
            }

            And("required services should've be invoked") {
                verify(exactly = 1) {
                    zrcClientService.readZaak(sourceZaak.uuid)
                    searchService.zoek(any())
                    zaakService.readZaakTypeByZaak(sourceZaak)
                    policyService.readZaakRechten(sourceZaak, zaakType, loggedInUser)
                    policyService.readZaakRechtenForZaakZoekObject(zaakZoekObject)
                }
            }
        }

        When("findLinkableZaken with DEELZAAK link is called") {
            val result = zaakKoppelenRestService.findLinkableZaken(
                zaakUuid = sourceZaak.uuid,
                zoekZaakIdentifier = zoekZaakIdentifier,
                relationType = RelatieType.DEELZAAK,
                page = page,
                rows = rows
            )

            Then("a single linkable zaak should be returned") {
                result.resultCount shouldBe 1
            }

            And("link is not possible") {
                with(result.results.first()) {
                    id shouldBe zaakZoekObject.getObjectId()
                    type shouldBe zaakZoekObject.getType()
                    identificatie shouldBe zoekZaakIdentifier
                    omschrijving shouldBe OMSCHRIJVING
                    zaaktypeOmschrijving shouldBe ZAAK_TYPE_OMSCHRIJVING
                    statustypeOmschrijving shouldBe STATUS_TYPE_OMSCHRIJVING
                    isKoppelbaar shouldBe false
                }
            }

            And("required services should've be invoked") {
                verify(exactly = 1) {
                    zrcClientService.readZaak(sourceZaak.uuid)
                    searchService.zoek(any())
                    zaakService.readZaakTypeByZaak(sourceZaak)
                    policyService.readZaakRechten(sourceZaak, zaakType, loggedInUser)
                    policyService.readZaakRechtenForZaakZoekObject(zaakZoekObject)
                }
            }
        }
    }

    Given("A source zaak which is not linked and a target deelzaak") {
        val sourceZaak = createZaak(
            identificatie = "ZAAK-2000-00001",
            archiefnominatie = ArchiefnominatieEnum.BLIJVEND_BEWAREN,
            zaaktypeUri = zaakTypeURI,
        )
        val zaakZoekObject = createZaakZoekObject(
            type = ZAAK,
            zaaktypeOmschrijving = ZAAK_TYPE_OMSCHRIJVING,
            identificatie = zoekZaakIdentifier,
            omschrijving = OMSCHRIJVING,
            statustypeOmschrijving = STATUS_TYPE_OMSCHRIJVING,
            zaaktypeUuid = zaakZoekObjectTypeUuid,
            archiefNominatie = ArchiefnominatieEnum.BLIJVEND_BEWAREN.toString(),
            indicatie = DEELZAAK
        )
        val zoekResultaat = ZoekResultaat(listOf(zaakZoekObject), 1)
        val loggedInUser = createLoggedInUser()
        val zaakType = createZaakType().apply { deelzaaktypen = emptyList() }

        every { zrcClientService.readZaak(sourceZaak.uuid) } returns sourceZaak
        every { searchService.zoek(any()) } returns zoekResultaat
        every { zaakService.readZaakTypeByZaak(sourceZaak) } returns zaakType
        every { policyService.readZaakRechten(sourceZaak, zaakType, loggedInUser) } returns createZaakRechten()
        every { policyService.readZaakRechtenForZaakZoekObject(zaakZoekObject) } returns createZaakRechten()
        every { loggedInUserInstance.get() } returns loggedInUser

        When("findLinkableZaken with HOOFDZAAK link is called") {
            every {
                ztcClientService.readZaaktype(UUID.fromString(zaakZoekObjectTypeUuid)).deelzaaktypen
            } returns emptyList()

            val result = zaakKoppelenRestService.findLinkableZaken(
                zaakUuid = sourceZaak.uuid,
                zoekZaakIdentifier = zoekZaakIdentifier,
                relationType = RelatieType.HOOFDZAAK,
                page = page,
                rows = rows
            )

            Then("a single linkable zaak should be returned") {
                result.resultCount shouldBe 1
            }

            And("link is not possible") {
                with(result.results.first()) {
                    id shouldBe zaakZoekObject.getObjectId()
                    type shouldBe zaakZoekObject.getType()
                    identificatie shouldBe zoekZaakIdentifier
                    omschrijving shouldBe OMSCHRIJVING
                    zaaktypeOmschrijving shouldBe ZAAK_TYPE_OMSCHRIJVING
                    statustypeOmschrijving shouldBe STATUS_TYPE_OMSCHRIJVING
                    isKoppelbaar shouldBe false
                }
            }

            And("required services should've be invoked") {
                verify(exactly = 1) {
                    zrcClientService.readZaak(sourceZaak.uuid)
                    searchService.zoek(any())
                    zaakService.readZaakTypeByZaak(sourceZaak)
                    policyService.readZaakRechten(sourceZaak, zaakType, loggedInUser)
                    policyService.readZaakRechtenForZaakZoekObject(zaakZoekObject)
                }
            }
        }

        When("findLinkableZaken with DEELZAAK link is called") {
            val result = zaakKoppelenRestService.findLinkableZaken(
                zaakUuid = sourceZaak.uuid,
                zoekZaakIdentifier = zoekZaakIdentifier,
                relationType = RelatieType.DEELZAAK,
                page = page,
                rows = rows
            )

            Then("a single linkable zaak should be returned") {
                result.resultCount shouldBe 1
            }

            And("link is not possible") {
                with(result.results.first()) {
                    id shouldBe zaakZoekObject.getObjectId()
                    type shouldBe zaakZoekObject.getType()
                    identificatie shouldBe zoekZaakIdentifier
                    omschrijving shouldBe OMSCHRIJVING
                    zaaktypeOmschrijving shouldBe ZAAK_TYPE_OMSCHRIJVING
                    statustypeOmschrijving shouldBe STATUS_TYPE_OMSCHRIJVING
                    isKoppelbaar shouldBe false
                }
            }

            And("required services should've be invoked") {
                verify(exactly = 1) {
                    zrcClientService.readZaak(sourceZaak.uuid)
                    searchService.zoek(any())
                    zaakService.readZaakTypeByZaak(sourceZaak)
                    policyService.readZaakRechten(sourceZaak, zaakType, loggedInUser)
                    policyService.readZaakRechtenForZaakZoekObject(zaakZoekObject)
                }
            }
        }
    }

    Given("A source hoofdzaak and target hoofdzaak") {
        val deelzakenTypeUuid = UUID.randomUUID()
        val hoofdzaak = createZaak(
            identificatie = "ZAAK-2000-00001",
            archiefnominatie = ArchiefnominatieEnum.BLIJVEND_BEWAREN,
            zaaktypeUri = zaakTypeURI,
            deelzaken = listOf(URI("https://example.com/deelzaak/$deelzakenTypeUuid"))
        )
        val zaakZoekObject = createZaakZoekObject(
            type = ZAAK,
            zaaktypeOmschrijving = ZAAK_TYPE_OMSCHRIJVING,
            identificatie = zoekZaakIdentifier,
            omschrijving = OMSCHRIJVING,
            statustypeOmschrijving = STATUS_TYPE_OMSCHRIJVING,
            zaaktypeUuid = zaakZoekObjectTypeUuid,
            archiefNominatie = ArchiefnominatieEnum.BLIJVEND_BEWAREN.toString(),
            indicatie = HOOFDZAAK
        )
        val zoekResultaat = ZoekResultaat(listOf(zaakZoekObject), 1)
        val loggedInUser = createLoggedInUser()
        val zaakType = createZaakType().apply { deelzaaktypen = emptyList() }

        every { zrcClientService.readZaak(hoofdzaak.uuid) } returns hoofdzaak
        every { searchService.zoek(any()) } returns zoekResultaat
        every { zaakService.readZaakTypeByZaak(hoofdzaak) } returns zaakType
        every { policyService.readZaakRechten(hoofdzaak, zaakType, loggedInUser) } returns createZaakRechten()
        every { policyService.readZaakRechtenForZaakZoekObject(zaakZoekObject) } returns createZaakRechten()
        every { loggedInUserInstance.get() } returns loggedInUser

        When("findLinkableZaken with HOOFDZAAK link is called") {
            every {
                ztcClientService.readZaaktype(UUID.fromString(zaakZoekObjectTypeUuid)).deelzaaktypen
            } returns emptyList()

            val result = zaakKoppelenRestService.findLinkableZaken(
                zaakUuid = hoofdzaak.uuid,
                zoekZaakIdentifier = zoekZaakIdentifier,
                relationType = RelatieType.HOOFDZAAK,
                page = page,
                rows = rows
            )

            Then("a single linkable zaak should be returned") {
                result.resultCount shouldBe 1
            }

            And("link is not allowed") {
                with(result.results.first()) {
                    id shouldBe zaakZoekObject.getObjectId()
                    type shouldBe zaakZoekObject.getType()
                    identificatie shouldBe zoekZaakIdentifier
                    omschrijving shouldBe OMSCHRIJVING
                    zaaktypeOmschrijving shouldBe ZAAK_TYPE_OMSCHRIJVING
                    statustypeOmschrijving shouldBe STATUS_TYPE_OMSCHRIJVING
                    isKoppelbaar shouldBe false
                }
            }

            And("required services should've be invoked") {
                verify(exactly = 1) {
                    zrcClientService.readZaak(hoofdzaak.uuid)
                    searchService.zoek(any())
                    zaakService.readZaakTypeByZaak(hoofdzaak)
                    policyService.readZaakRechten(hoofdzaak, zaakType, loggedInUser)
                    policyService.readZaakRechtenForZaakZoekObject(zaakZoekObject)
                }
            }
        }

        When("findLinkableZaken with DEELZAAK link is called") {
            val result = zaakKoppelenRestService.findLinkableZaken(
                zaakUuid = hoofdzaak.uuid,
                zoekZaakIdentifier = zoekZaakIdentifier,
                relationType = RelatieType.DEELZAAK,
                page = page,
                rows = rows
            )

            Then("a single linkable zaak should be returned") {
                result.resultCount shouldBe 1
            }

            And("link is not allowed") {
                with(result.results.first()) {
                    id shouldBe zaakZoekObject.getObjectId()
                    type shouldBe zaakZoekObject.getType()
                    identificatie shouldBe zoekZaakIdentifier
                    omschrijving shouldBe OMSCHRIJVING
                    zaaktypeOmschrijving shouldBe ZAAK_TYPE_OMSCHRIJVING
                    statustypeOmschrijving shouldBe STATUS_TYPE_OMSCHRIJVING
                    isKoppelbaar shouldBe false
                }
            }

            And("required services should've be invoked") {
                verify(exactly = 1) {
                    zrcClientService.readZaak(hoofdzaak.uuid)
                    searchService.zoek(any())
                    zaakService.readZaakTypeByZaak(hoofdzaak)
                    policyService.readZaakRechten(hoofdzaak, zaakType, loggedInUser)
                    policyService.readZaakRechtenForZaakZoekObject(zaakZoekObject)
                }
            }
        }
    }

    Given("A source hoofdzaak and target deelzaak") {
        val deelzakenTypeUuid = UUID.randomUUID()
        val hoofdzaak = createZaak(
            identificatie = "ZAAK-2000-00001",
            archiefnominatie = ArchiefnominatieEnum.BLIJVEND_BEWAREN,
            zaaktypeUri = zaakTypeURI,
            deelzaken = listOf(URI("https://example.com/deelzaak/$deelzakenTypeUuid"))
        )
        val zaakZoekObject = createZaakZoekObject(
            type = ZAAK,
            zaaktypeOmschrijving = ZAAK_TYPE_OMSCHRIJVING,
            identificatie = zoekZaakIdentifier,
            omschrijving = OMSCHRIJVING,
            statustypeOmschrijving = STATUS_TYPE_OMSCHRIJVING,
            zaaktypeUuid = zaakZoekObjectTypeUuid,
            archiefNominatie = ArchiefnominatieEnum.BLIJVEND_BEWAREN.toString(),
            indicatie = DEELZAAK
        )
        val zoekResultaat = ZoekResultaat(listOf(zaakZoekObject), 1)
        val loggedInUser = createLoggedInUser()
        val zaakType = createZaakType().apply { deelzaaktypen = emptyList() }

        every { zrcClientService.readZaak(hoofdzaak.uuid) } returns hoofdzaak
        every { searchService.zoek(any()) } returns zoekResultaat
        every { zaakService.readZaakTypeByZaak(hoofdzaak) } returns zaakType
        every { policyService.readZaakRechten(hoofdzaak, zaakType, loggedInUser) } returns createZaakRechten()
        every { policyService.readZaakRechtenForZaakZoekObject(zaakZoekObject) } returns createZaakRechten()
        every { loggedInUserInstance.get() } returns loggedInUser

        When("findLinkableZaken with HOOFDZAAK link is called") {
            every {
                ztcClientService.readZaaktype(UUID.fromString(zaakZoekObjectTypeUuid)).deelzaaktypen
            } returns emptyList()

            val result = zaakKoppelenRestService.findLinkableZaken(
                zaakUuid = hoofdzaak.uuid,
                zoekZaakIdentifier = zoekZaakIdentifier,
                relationType = RelatieType.HOOFDZAAK,
                page = page,
                rows = rows
            )

            Then("a single linkable zaak should be returned") {
                result.resultCount shouldBe 1
            }

            And("link is not possible") {
                with(result.results.first()) {
                    id shouldBe zaakZoekObject.getObjectId()
                    type shouldBe zaakZoekObject.getType()
                    identificatie shouldBe zoekZaakIdentifier
                    omschrijving shouldBe OMSCHRIJVING
                    zaaktypeOmschrijving shouldBe ZAAK_TYPE_OMSCHRIJVING
                    statustypeOmschrijving shouldBe STATUS_TYPE_OMSCHRIJVING
                    isKoppelbaar shouldBe false
                }
            }

            And("required services should've be invoked") {
                verify(exactly = 1) {
                    zrcClientService.readZaak(hoofdzaak.uuid)
                    searchService.zoek(any())
                    zaakService.readZaakTypeByZaak(hoofdzaak)
                    policyService.readZaakRechten(hoofdzaak, zaakType, loggedInUser)
                    policyService.readZaakRechtenForZaakZoekObject(zaakZoekObject)
                }
            }
        }

        When("findLinkableZaken with DEELZAAK link is called") {
            val result = zaakKoppelenRestService.findLinkableZaken(
                zaakUuid = hoofdzaak.uuid,
                zoekZaakIdentifier = zoekZaakIdentifier,
                relationType = RelatieType.DEELZAAK,
                page = page,
                rows = rows
            )

            Then("a single linkable zaak should be returned") {
                result.resultCount shouldBe 1
            }

            And("link is not possible") {
                with(result.results.first()) {
                    id shouldBe zaakZoekObject.getObjectId()
                    type shouldBe zaakZoekObject.getType()
                    identificatie shouldBe zoekZaakIdentifier
                    omschrijving shouldBe OMSCHRIJVING
                    zaaktypeOmschrijving shouldBe ZAAK_TYPE_OMSCHRIJVING
                    statustypeOmschrijving shouldBe STATUS_TYPE_OMSCHRIJVING
                    isKoppelbaar shouldBe false
                }
            }

            And("required services should've be invoked") {
                verify(exactly = 1) {
                    zrcClientService.readZaak(hoofdzaak.uuid)
                    searchService.zoek(any())
                    zaakService.readZaakTypeByZaak(hoofdzaak)
                    policyService.readZaakRechten(hoofdzaak, zaakType, loggedInUser)
                    policyService.readZaakRechtenForZaakZoekObject(zaakZoekObject)
                }
            }
        }
    }

    Given("A source hoofdzaak and target not linked zaak") {
        val deelzakenTypeUuid = UUID.randomUUID()
        val hoofdzaak = createZaak(
            identificatie = "ZAAK-2000-00001",
            archiefnominatie = ArchiefnominatieEnum.BLIJVEND_BEWAREN,
            zaaktypeUri = zaakTypeURI,
            deelzaken = listOf(URI("https://example.com/deelzaak/$deelzakenTypeUuid"))
        )
        val zaakZoekObject = createZaakZoekObject(
            type = ZAAK,
            zaaktypeOmschrijving = ZAAK_TYPE_OMSCHRIJVING,
            identificatie = zoekZaakIdentifier,
            omschrijving = OMSCHRIJVING,
            statustypeOmschrijving = STATUS_TYPE_OMSCHRIJVING,
            zaaktypeUuid = zaakZoekObjectTypeUuid,
            archiefNominatie = ArchiefnominatieEnum.BLIJVEND_BEWAREN.toString()
        )
        val zoekResultaat = ZoekResultaat(listOf(zaakZoekObject), 1)
        val loggedInUser = createLoggedInUser()
        val zaakType = createZaakType().apply {
            deelzaaktypen = listOf(URI(zaakZoekObjectTypeUuid))
        }

        every { zrcClientService.readZaak(hoofdzaak.uuid) } returns hoofdzaak
        every { searchService.zoek(any()) } returns zoekResultaat
        every { zaakService.readZaakTypeByZaak(hoofdzaak) } returns zaakType
        every { policyService.readZaakRechten(hoofdzaak, zaakType, loggedInUser) } returns createZaakRechten()
        every { policyService.readZaakRechtenForZaakZoekObject(zaakZoekObject) } returns createZaakRechten()
        every { loggedInUserInstance.get() } returns loggedInUser

        When("findLinkableZaken with HOOFDZAAK link is called") {
            every {
                ztcClientService.readZaaktype(UUID.fromString(zaakZoekObjectTypeUuid)).deelzaaktypen
            } returns emptyList()

            val result = zaakKoppelenRestService.findLinkableZaken(
                zaakUuid = hoofdzaak.uuid,
                zoekZaakIdentifier = zoekZaakIdentifier,
                relationType = RelatieType.HOOFDZAAK,
                page = page,
                rows = rows
            )

            Then("a single linkable zaak should be returned") {
                result.resultCount shouldBe 1
            }

            And("link is not possible") {
                with(result.results.first()) {
                    id shouldBe zaakZoekObject.getObjectId()
                    type shouldBe zaakZoekObject.getType()
                    identificatie shouldBe zoekZaakIdentifier
                    omschrijving shouldBe OMSCHRIJVING
                    zaaktypeOmschrijving shouldBe ZAAK_TYPE_OMSCHRIJVING
                    statustypeOmschrijving shouldBe STATUS_TYPE_OMSCHRIJVING
                    isKoppelbaar shouldBe false
                }
            }

            And("required services should've be invoked") {
                verify(exactly = 1) {
                    zrcClientService.readZaak(hoofdzaak.uuid)
                    searchService.zoek(any())
                    zaakService.readZaakTypeByZaak(hoofdzaak)
                    policyService.readZaakRechten(hoofdzaak, zaakType, loggedInUser)
                    policyService.readZaakRechtenForZaakZoekObject(zaakZoekObject)
                }
            }
        }

        When("findLinkableZaken with DEELZAAK link is called") {
            val result = zaakKoppelenRestService.findLinkableZaken(
                zaakUuid = hoofdzaak.uuid,
                zoekZaakIdentifier = zoekZaakIdentifier,
                relationType = RelatieType.DEELZAAK,
                page = page,
                rows = rows
            )

            Then("a single linkable zaak should be returned") {
                result.resultCount shouldBe 1
            }

            And("link is possible") {
                with(result.results.first()) {
                    id shouldBe zaakZoekObject.getObjectId()
                    type shouldBe zaakZoekObject.getType()
                    identificatie shouldBe zoekZaakIdentifier
                    omschrijving shouldBe OMSCHRIJVING
                    zaaktypeOmschrijving shouldBe ZAAK_TYPE_OMSCHRIJVING
                    statustypeOmschrijving shouldBe STATUS_TYPE_OMSCHRIJVING
                    isKoppelbaar shouldBe true
                }
            }

            And("required services should've be invoked") {
                verify(exactly = 1) {
                    zrcClientService.readZaak(hoofdzaak.uuid)
                    searchService.zoek(any())
                    zaakService.readZaakTypeByZaak(hoofdzaak)
                    policyService.readZaakRechten(hoofdzaak, zaakType, loggedInUser)
                    policyService.readZaakRechtenForZaakZoekObject(zaakZoekObject)
                }
            }
        }
    }

    Given("A source deelzaak and target hoofdzaak") {
        val deelzaakUuid = UUID.randomUUID()
        val deelzaak = createZaak(
            identificatie = "ZAAK-2000-00001",
            archiefnominatie = ArchiefnominatieEnum.BLIJVEND_BEWAREN,
            zaaktypeUri = zaakTypeURI,
            hoofdzaakUri = URI("https://example.com/deelzaak/$deelzaakUuid")
        )
        val zaakZoekObject = createZaakZoekObject(
            type = ZAAK,
            zaaktypeOmschrijving = ZAAK_TYPE_OMSCHRIJVING,
            identificatie = zoekZaakIdentifier,
            omschrijving = OMSCHRIJVING,
            statustypeOmschrijving = STATUS_TYPE_OMSCHRIJVING,
            zaaktypeUuid = zaakZoekObjectTypeUuid,
            archiefNominatie = ArchiefnominatieEnum.BLIJVEND_BEWAREN.toString(),
            indicatie = HOOFDZAAK
        )
        val zoekResultaat = ZoekResultaat(listOf(zaakZoekObject), 1)
        val loggedInUser = createLoggedInUser()
        val zaakType = createZaakType().apply { deelzaaktypen = emptyList() }

        every { zrcClientService.readZaak(deelzaak.uuid) } returns deelzaak
        every { searchService.zoek(any()) } returns zoekResultaat
        every { zaakService.readZaakTypeByZaak(deelzaak) } returns zaakType
        every { policyService.readZaakRechten(deelzaak, zaakType, loggedInUser) } returns createZaakRechten()
        every { policyService.readZaakRechtenForZaakZoekObject(zaakZoekObject) } returns createZaakRechten()
        every { loggedInUserInstance.get() } returns loggedInUser

        When("findLinkableZaken with HOOFDZAAK link is called") {
            every {
                ztcClientService.readZaaktype(UUID.fromString(zaakZoekObjectTypeUuid)).deelzaaktypen
            } returns emptyList()

            val result = zaakKoppelenRestService.findLinkableZaken(
                zaakUuid = deelzaak.uuid,
                zoekZaakIdentifier = zoekZaakIdentifier,
                relationType = RelatieType.HOOFDZAAK,
                page = page,
                rows = rows
            )

            Then("a single linkable zaak should be returned") {
                result.resultCount shouldBe 1
            }

            And("link is not possible") {
                with(result.results.first()) {
                    id shouldBe zaakZoekObject.getObjectId()
                    type shouldBe zaakZoekObject.getType()
                    identificatie shouldBe zoekZaakIdentifier
                    omschrijving shouldBe OMSCHRIJVING
                    zaaktypeOmschrijving shouldBe ZAAK_TYPE_OMSCHRIJVING
                    statustypeOmschrijving shouldBe STATUS_TYPE_OMSCHRIJVING
                    isKoppelbaar shouldBe false
                }
            }

            And("required services should've be invoked") {
                verify(exactly = 1) {
                    zrcClientService.readZaak(deelzaak.uuid)
                    searchService.zoek(any())
                    zaakService.readZaakTypeByZaak(deelzaak)
                    policyService.readZaakRechten(deelzaak, zaakType, loggedInUser)
                    policyService.readZaakRechtenForZaakZoekObject(zaakZoekObject)
                }
            }
        }

        When("findLinkableZaken with DEELZAAK link is called") {
            val result = zaakKoppelenRestService.findLinkableZaken(
                zaakUuid = deelzaak.uuid,
                zoekZaakIdentifier = zoekZaakIdentifier,
                relationType = RelatieType.DEELZAAK,
                page = page,
                rows = rows
            )

            Then("a single linkable zaak should be returned") {
                result.resultCount shouldBe 1
            }

            And("link is not possible") {
                with(result.results.first()) {
                    id shouldBe zaakZoekObject.getObjectId()
                    type shouldBe zaakZoekObject.getType()
                    identificatie shouldBe zoekZaakIdentifier
                    omschrijving shouldBe OMSCHRIJVING
                    zaaktypeOmschrijving shouldBe ZAAK_TYPE_OMSCHRIJVING
                    statustypeOmschrijving shouldBe STATUS_TYPE_OMSCHRIJVING
                    isKoppelbaar shouldBe false
                }
            }

            And("required services should've be invoked") {
                verify(exactly = 1) {
                    zrcClientService.readZaak(deelzaak.uuid)
                    searchService.zoek(any())
                    zaakService.readZaakTypeByZaak(deelzaak)
                    policyService.readZaakRechten(deelzaak, zaakType, loggedInUser)
                    policyService.readZaakRechtenForZaakZoekObject(zaakZoekObject)
                }
            }
        }
    }

    Given("A source deelzaak and target deelzaak") {
        val deelzaakUuid = UUID.randomUUID()
        val deelzaak = createZaak(
            identificatie = "ZAAK-2000-00001",
            archiefnominatie = ArchiefnominatieEnum.BLIJVEND_BEWAREN,
            zaaktypeUri = zaakTypeURI,
            hoofdzaakUri = URI("https://example.com/deelzaak/$deelzaakUuid")
        )
        val zaakZoekObject = createZaakZoekObject(
            type = ZAAK,
            zaaktypeOmschrijving = ZAAK_TYPE_OMSCHRIJVING,
            identificatie = zoekZaakIdentifier,
            omschrijving = OMSCHRIJVING,
            statustypeOmschrijving = STATUS_TYPE_OMSCHRIJVING,
            zaaktypeUuid = zaakZoekObjectTypeUuid,
            archiefNominatie = ArchiefnominatieEnum.BLIJVEND_BEWAREN.toString(),
            indicatie = DEELZAAK
        )
        val zoekResultaat = ZoekResultaat(listOf(zaakZoekObject), 1)
        val loggedInUser = createLoggedInUser()
        val zaakType = createZaakType().apply { deelzaaktypen = emptyList() }

        every { zrcClientService.readZaak(deelzaak.uuid) } returns deelzaak
        every { searchService.zoek(any()) } returns zoekResultaat
        every { zaakService.readZaakTypeByZaak(deelzaak) } returns zaakType
        every { policyService.readZaakRechten(deelzaak, zaakType, loggedInUser) } returns createZaakRechten()
        every { policyService.readZaakRechtenForZaakZoekObject(zaakZoekObject) } returns createZaakRechten()
        every { loggedInUserInstance.get() } returns loggedInUser

        When("findLinkableZaken with HOOFDZAAK link is called") {
            every {
                ztcClientService.readZaaktype(UUID.fromString(zaakZoekObjectTypeUuid)).deelzaaktypen
            } returns emptyList()

            val result = zaakKoppelenRestService.findLinkableZaken(
                zaakUuid = deelzaak.uuid,
                zoekZaakIdentifier = zoekZaakIdentifier,
                relationType = RelatieType.HOOFDZAAK,
                page = page,
                rows = rows
            )

            Then("a single linkable zaak should be returned") {
                result.resultCount shouldBe 1
            }

            And("link is not possible") {
                with(result.results.first()) {
                    id shouldBe zaakZoekObject.getObjectId()
                    type shouldBe zaakZoekObject.getType()
                    identificatie shouldBe zoekZaakIdentifier
                    omschrijving shouldBe OMSCHRIJVING
                    zaaktypeOmschrijving shouldBe ZAAK_TYPE_OMSCHRIJVING
                    statustypeOmschrijving shouldBe STATUS_TYPE_OMSCHRIJVING
                    isKoppelbaar shouldBe false
                }
            }

            And("required services should've be invoked") {
                verify(exactly = 1) {
                    zrcClientService.readZaak(deelzaak.uuid)
                    searchService.zoek(any())
                    zaakService.readZaakTypeByZaak(deelzaak)
                    policyService.readZaakRechten(deelzaak, zaakType, loggedInUser)
                    policyService.readZaakRechtenForZaakZoekObject(zaakZoekObject)
                }
            }
        }

        When("findLinkableZaken with DEELZAAK link is called") {
            val result = zaakKoppelenRestService.findLinkableZaken(
                zaakUuid = deelzaak.uuid,
                zoekZaakIdentifier = zoekZaakIdentifier,
                relationType = RelatieType.DEELZAAK,
                page = page,
                rows = rows
            )

            Then("a single linkable zaak should be returned") {
                result.resultCount shouldBe 1
            }

            And("link is not possible") {
                with(result.results.first()) {
                    id shouldBe zaakZoekObject.getObjectId()
                    type shouldBe zaakZoekObject.getType()
                    identificatie shouldBe zoekZaakIdentifier
                    omschrijving shouldBe OMSCHRIJVING
                    zaaktypeOmschrijving shouldBe ZAAK_TYPE_OMSCHRIJVING
                    statustypeOmschrijving shouldBe STATUS_TYPE_OMSCHRIJVING
                    isKoppelbaar shouldBe false
                }
            }

            And("required services should've be invoked") {
                verify(exactly = 1) {
                    zrcClientService.readZaak(deelzaak.uuid)
                    searchService.zoek(any())
                    zaakService.readZaakTypeByZaak(deelzaak)
                    policyService.readZaakRechten(deelzaak, zaakType, loggedInUser)
                    policyService.readZaakRechtenForZaakZoekObject(zaakZoekObject)
                }
            }
        }
    }

    Given("A source deelzaak and target not linked zaak") {
        val zaakSearchTextTrimmed = "ZAAK-2000-00002"
        val zaakSearchText = " ZAAK-2000-00002  "
        val deelzaakUuid = UUID.randomUUID()
        val deelzaak = createZaak(
            identificatie = "ZAAK-2000-00001",
            archiefnominatie = ArchiefnominatieEnum.BLIJVEND_BEWAREN,
            zaaktypeUri = zaakTypeURI,
            hoofdzaakUri = URI("https://example.com/deelzaak/$deelzaakUuid")
        )
        val zaakZoekObject = createZaakZoekObject(
            type = ZAAK,
            zaaktypeOmschrijving = ZAAK_TYPE_OMSCHRIJVING,
            identificatie = zaakSearchTextTrimmed,
            omschrijving = OMSCHRIJVING,
            statustypeOmschrijving = STATUS_TYPE_OMSCHRIJVING,
            zaaktypeUuid = zaakZoekObjectTypeUuid,
            archiefNominatie = ArchiefnominatieEnum.BLIJVEND_BEWAREN.toString()
        )
        val zoekResultaat = ZoekResultaat(listOf(zaakZoekObject), 1)
        val zoekParametersSlot = slot<ZoekParameters>()
        val loggedInUser = createLoggedInUser()
        val zaakType = createZaakType().apply { deelzaaktypen = emptyList() }

        every { zrcClientService.readZaak(deelzaak.uuid) } returns deelzaak
        every { searchService.zoek(capture(zoekParametersSlot)) } returns zoekResultaat
        every { zaakService.readZaakTypeByZaak(deelzaak) } returns zaakType
        every { policyService.readZaakRechten(deelzaak, zaakType, loggedInUser) } returns createZaakRechten()
        every { policyService.readZaakRechtenForZaakZoekObject(zaakZoekObject) } returns createZaakRechten()
        every { loggedInUserInstance.get() } returns loggedInUser

        When("findLinkableZaken with HOOFDZAAK link is called") {
            every {
                ztcClientService.readZaaktype(UUID.fromString(zaakZoekObjectTypeUuid)).deelzaaktypen
            } returns emptyList()

            val result = zaakKoppelenRestService.findLinkableZaken(
                zaakUuid = deelzaak.uuid,
                zoekZaakIdentifier = zaakSearchText,
                relationType = RelatieType.HOOFDZAAK,
                page = page,
                rows = rows
            )

            Then("the used search parameters should be as expected") {
                with(zoekParametersSlot.captured) {
                    getOrZoeken() shouldBe listOf(
                        ZoekVeld.ZAAK_IDENTIFICATIE to zaakSearchTextTrimmed,
                        ZoekVeld.ZAAK_OMSCHRIJVING to zaakSearchTextTrimmed
                    )
                }
            }

            And("a single linkable zaak should be returned") {
                result.resultCount shouldBe 1
            }

            And("link is not possible") {
                with(result.results.first()) {
                    id shouldBe zaakZoekObject.getObjectId()
                    type shouldBe zaakZoekObject.getType()
                    identificatie shouldBe zaakSearchTextTrimmed
                    omschrijving shouldBe OMSCHRIJVING
                    zaaktypeOmschrijving shouldBe ZAAK_TYPE_OMSCHRIJVING
                    statustypeOmschrijving shouldBe STATUS_TYPE_OMSCHRIJVING
                    isKoppelbaar shouldBe false
                }
            }

            And("required services should've be invoked") {
                verify(exactly = 1) {
                    zrcClientService.readZaak(deelzaak.uuid)
                    searchService.zoek(any())
                    zaakService.readZaakTypeByZaak(deelzaak)
                    policyService.readZaakRechten(deelzaak, zaakType, loggedInUser)
                    policyService.readZaakRechtenForZaakZoekObject(zaakZoekObject)
                }
            }
        }

        When("findLinkableZaken with DEELZAAK link is called") {
            val result = zaakKoppelenRestService.findLinkableZaken(
                zaakUuid = deelzaak.uuid,
                zoekZaakIdentifier = zoekZaakIdentifier,
                relationType = RelatieType.DEELZAAK,
                page = page,
                rows = rows
            )

            Then("a single linkable zaak should be returned") {
                result.resultCount shouldBe 1
            }

            And("link is not possible") {
                with(result.results.first()) {
                    id shouldBe zaakZoekObject.getObjectId()
                    type shouldBe zaakZoekObject.getType()
                    identificatie shouldBe zoekZaakIdentifier
                    omschrijving shouldBe OMSCHRIJVING
                    zaaktypeOmschrijving shouldBe ZAAK_TYPE_OMSCHRIJVING
                    statustypeOmschrijving shouldBe STATUS_TYPE_OMSCHRIJVING
                    isKoppelbaar shouldBe false
                }
            }

            And("required services should've be invoked") {
                verify(exactly = 1) {
                    zrcClientService.readZaak(deelzaak.uuid)
                    searchService.zoek(any())
                    zaakService.readZaakTypeByZaak(deelzaak)
                    policyService.readZaakRechten(deelzaak, zaakType, loggedInUser)
                    policyService.readZaakRechtenForZaakZoekObject(zaakZoekObject)
                }
            }
        }
    }

    Given("A source zaak without koppelen rights") {
        val sourceZaak = createZaak(
            identificatie = "ZAAK-2000-00001",
            archiefnominatie = ArchiefnominatieEnum.BLIJVEND_BEWAREN,
            zaaktypeUri = zaakTypeURI
        )
        val zaakZoekObject = createZaakZoekObject(
            type = ZAAK,
            zaaktypeOmschrijving = ZAAK_TYPE_OMSCHRIJVING,
            identificatie = zoekZaakIdentifier,
            omschrijving = OMSCHRIJVING,
            statustypeOmschrijving = STATUS_TYPE_OMSCHRIJVING,
            zaaktypeUuid = zaakZoekObjectTypeUuid,
            archiefNominatie = ArchiefnominatieEnum.BLIJVEND_BEWAREN.toString()
        )
        val zoekResultaat = ZoekResultaat(listOf(zaakZoekObject), 1)
        val loggedInUser = createLoggedInUser()
        val zaakType = createZaakType()

        every { zrcClientService.readZaak(sourceZaak.uuid) } returns sourceZaak
        every { searchService.zoek(any()) } returns zoekResultaat
        every { zaakService.readZaakTypeByZaak(sourceZaak) } returns zaakType
        every { policyService.readZaakRechten(sourceZaak, zaakType, loggedInUser) } returns createZaakRechten(koppelen = false)
        every { policyService.readZaakRechtenForZaakZoekObject(zaakZoekObject) } returns createZaakRechten()
        every { loggedInUserInstance.get() } returns loggedInUser

        When("findLinkableZaken with GERELATEERD is called") {
            val result = zaakKoppelenRestService.findLinkableZaken(
                zaakUuid = sourceZaak.uuid,
                zoekZaakIdentifier = zoekZaakIdentifier,
                relationType = RelatieType.GERELATEERD,
                page = page,
                rows = rows
            )

            Then("the target zaak should not be linkable") {
                result.resultCount shouldBe 1
                result.results.first().isKoppelbaar shouldBe false
            }
        }
    }

    Given("A source zaak with koppelen rights and a target zaak without koppelen rights") {
        val sourceZaak = createZaak(
            identificatie = "ZAAK-2000-00001",
            archiefnominatie = ArchiefnominatieEnum.BLIJVEND_BEWAREN,
            zaaktypeUri = zaakTypeURI
        )
        val zaakZoekObject = createZaakZoekObject(
            type = ZAAK,
            zaaktypeOmschrijving = ZAAK_TYPE_OMSCHRIJVING,
            identificatie = zoekZaakIdentifier,
            omschrijving = OMSCHRIJVING,
            statustypeOmschrijving = STATUS_TYPE_OMSCHRIJVING,
            zaaktypeUuid = zaakZoekObjectTypeUuid,
            archiefNominatie = ArchiefnominatieEnum.BLIJVEND_BEWAREN.toString()
        )
        val zoekResultaat = ZoekResultaat(listOf(zaakZoekObject), 1)
        val loggedInUser = createLoggedInUser()
        val zaakType = createZaakType()

        every { zrcClientService.readZaak(sourceZaak.uuid) } returns sourceZaak
        every { searchService.zoek(any()) } returns zoekResultaat
        every { zaakService.readZaakTypeByZaak(sourceZaak) } returns zaakType
        every { policyService.readZaakRechten(sourceZaak, zaakType, loggedInUser) } returns createZaakRechten()
        every { policyService.readZaakRechtenForZaakZoekObject(zaakZoekObject) } returns createZaakRechten(lezen = false)
        every { loggedInUserInstance.get() } returns loggedInUser

        When("findLinkableZaken with GERELATEERD is called") {
            val result = zaakKoppelenRestService.findLinkableZaken(
                zaakUuid = sourceZaak.uuid,
                zoekZaakIdentifier = zoekZaakIdentifier,
                relationType = RelatieType.GERELATEERD,
                page = page,
                rows = rows
            )

            Then("the target zaak should not be linkable") {
                result.resultCount shouldBe 1
                result.results.first().isKoppelbaar shouldBe false
            }
        }
    }

    Given("Two open not linked zaken") {
        val sourceZaak = createZaak(
            identificatie = "ZAAK-2000-00001",
            zaaktypeUri = zaakTypeURI
        )
        val zaakZoekObject = createZaakZoekObject(
            type = ZAAK,
            zaaktypeOmschrijving = ZAAK_TYPE_OMSCHRIJVING,
            identificatie = zoekZaakIdentifier,
            omschrijving = OMSCHRIJVING,
            statustypeOmschrijving = STATUS_TYPE_OMSCHRIJVING,
            zaaktypeUuid = zaakZoekObjectTypeUuid,
            archiefNominatie = null
        )
        val zoekResultaat = ZoekResultaat(listOf(zaakZoekObject), 1)
        val loggedInUser = createLoggedInUser()
        val zaakType = createZaakType().apply {
            deelzaaktypen = listOf(URI(zaakZoekObjectTypeUuid))
        }

        every { zrcClientService.readZaak(sourceZaak.uuid) } returns sourceZaak
        every { searchService.zoek(any()) } returns zoekResultaat
        every { zaakService.readZaakTypeByZaak(sourceZaak) } returns zaakType
        every { policyService.readZaakRechten(sourceZaak, zaakType, loggedInUser) } returns createZaakRechten()
        every { loggedInUserInstance.get() } returns loggedInUser

        When("findLinkableZaken with HOOFDZAAK is called") {
            every { policyService.readZaakRechtenForZaakZoekObject(zaakZoekObject) } returns createZaakRechten()
            every {
                ztcClientService.readZaaktype(UUID.fromString(zaakZoekObjectTypeUuid)).deelzaaktypen
            } returns listOf(zaakTypeURI)

            val result = zaakKoppelenRestService.findLinkableZaken(
                zaakUuid = sourceZaak.uuid,
                zoekZaakIdentifier = zoekZaakIdentifier,
                relationType = RelatieType.HOOFDZAAK,
                page = page,
                rows = rows
            )

            Then("the target zaak should be linkable") {
                result.resultCount shouldBe 1
                result.results.first().isKoppelbaar shouldBe true
            }
        }

        When("findLinkableZaken with DEELZAAK is called") {
            every { policyService.readZaakRechtenForZaakZoekObject(zaakZoekObject) } returns createZaakRechten()
            val result = zaakKoppelenRestService.findLinkableZaken(
                zaakUuid = sourceZaak.uuid,
                zoekZaakIdentifier = zoekZaakIdentifier,
                relationType = RelatieType.DEELZAAK,
                page = page,
                rows = rows
            )

            Then("the target zaak should be linkable") {
                result.resultCount shouldBe 1
                result.results.first().isKoppelbaar shouldBe true
            }
        }

        When("findLinkableZaken with GERELATEERD is called") {
            every { policyService.readZaakRechtenForZaakZoekObject(zaakZoekObject) } returns createZaakRechten()
            val result = zaakKoppelenRestService.findLinkableZaken(
                zaakUuid = sourceZaak.uuid,
                zoekZaakIdentifier = zoekZaakIdentifier,
                relationType = RelatieType.GERELATEERD,
                page = page,
                rows = rows
            )

            Then("the target zaak should be linkable") {
                result.resultCount shouldBe 1
                result.results.first().isKoppelbaar shouldBe true
            }
        }
    }

    Given("An open not linked source zaak and a closed not linked target zaak") {
        val sourceZaak = createZaak(
            identificatie = "ZAAK-2000-00001",
            zaaktypeUri = zaakTypeURI
        )
        val zaakZoekObject = createZaakZoekObject(
            type = ZAAK,
            zaaktypeOmschrijving = ZAAK_TYPE_OMSCHRIJVING,
            identificatie = zoekZaakIdentifier,
            omschrijving = OMSCHRIJVING,
            statustypeOmschrijving = STATUS_TYPE_OMSCHRIJVING,
            zaaktypeUuid = zaakZoekObjectTypeUuid,
            archiefNominatie = ArchiefnominatieEnum.BLIJVEND_BEWAREN.toString(),
        )
        val zoekResultaat = ZoekResultaat(listOf(zaakZoekObject), 1)
        val loggedInUser = createLoggedInUser()
        val zaakType = createZaakType().apply { deelzaaktypen = emptyList() }

        every { zrcClientService.readZaak(sourceZaak.uuid) } returns sourceZaak
        every { searchService.zoek(any()) } returns zoekResultaat
        every { zaakService.readZaakTypeByZaak(sourceZaak) } returns zaakType
        every { loggedInUserInstance.get() } returns loggedInUser
        every { policyService.readZaakRechten(sourceZaak, zaakType, loggedInUser) } returns createZaakRechten()

        When("findLinkableZaken with HOOFDZAAK is called") {
            every { policyService.readZaakRechtenForZaakZoekObject(zaakZoekObject) } returns createZaakRechten()
            every {
                ztcClientService.readZaaktype(UUID.fromString(zaakZoekObjectTypeUuid)).deelzaaktypen
            } returns emptyList()

            val result = zaakKoppelenRestService.findLinkableZaken(
                zaakUuid = sourceZaak.uuid,
                zoekZaakIdentifier = zoekZaakIdentifier,
                relationType = RelatieType.HOOFDZAAK,
                page = page,
                rows = rows
            )

            Then("the target zaak should not be linkable") {
                result.resultCount shouldBe 1
                result.results.first().isKoppelbaar shouldBe false
            }
        }

        When("findLinkableZaken with DEELZAAK is called") {
            every { policyService.readZaakRechtenForZaakZoekObject(zaakZoekObject) } returns createZaakRechten()
            val result = zaakKoppelenRestService.findLinkableZaken(
                zaakUuid = sourceZaak.uuid,
                zoekZaakIdentifier = zoekZaakIdentifier,
                relationType = RelatieType.DEELZAAK,
                page = page,
                rows = rows
            )

            Then("the target zaak should not be linkable") {
                result.resultCount shouldBe 1
                result.results.first().isKoppelbaar shouldBe false
            }
        }

        When("findLinkableZaken with GERELATEERD is called") {
            every { policyService.readZaakRechtenForZaakZoekObject(zaakZoekObject) } returns createZaakRechten()
            val result = zaakKoppelenRestService.findLinkableZaken(
                zaakUuid = sourceZaak.uuid,
                zoekZaakIdentifier = zoekZaakIdentifier,
                relationType = RelatieType.GERELATEERD,
                page = page,
                rows = rows
            )

            Then("the target zaak should be linkable") {
                result.resultCount shouldBe 1
                result.results.first().isKoppelbaar shouldBe true
            }
        }
    }

    Given("A closed not linked source zaak and an open not linked target zaak") {
        val sourceZaak = createZaak(
            identificatie = "ZAAK-2000-00001",
            archiefnominatie = ArchiefnominatieEnum.BLIJVEND_BEWAREN,
            zaaktypeUri = zaakTypeURI
        )
        val zaakZoekObject = createZaakZoekObject(
            type = ZAAK,
            zaaktypeOmschrijving = ZAAK_TYPE_OMSCHRIJVING,
            identificatie = zoekZaakIdentifier,
            omschrijving = OMSCHRIJVING,
            statustypeOmschrijving = STATUS_TYPE_OMSCHRIJVING,
            zaaktypeUuid = zaakZoekObjectTypeUuid,
            archiefNominatie = null
        )
        val zoekResultaat = ZoekResultaat(listOf(zaakZoekObject), 1)
        val loggedInUser = createLoggedInUser()
        val zaakType = createZaakType().apply { deelzaaktypen = emptyList() }

        every { zrcClientService.readZaak(sourceZaak.uuid) } returns sourceZaak
        every { searchService.zoek(any()) } returns zoekResultaat
        every { zaakService.readZaakTypeByZaak(sourceZaak) } returns zaakType
        every { loggedInUserInstance.get() } returns loggedInUser
        every { policyService.readZaakRechten(sourceZaak, zaakType, loggedInUser) } returns createZaakRechten()

        When("findLinkableZaken with HOOFDZAAK is called") {
            every { policyService.readZaakRechtenForZaakZoekObject(zaakZoekObject) } returns createZaakRechten()
            every {
                ztcClientService.readZaaktype(UUID.fromString(zaakZoekObjectTypeUuid)).deelzaaktypen
            } returns emptyList()

            val result = zaakKoppelenRestService.findLinkableZaken(
                zaakUuid = sourceZaak.uuid,
                zoekZaakIdentifier = zoekZaakIdentifier,
                relationType = RelatieType.HOOFDZAAK,
                page = page,
                rows = rows
            )

            Then("the target zaak should not be linkable") {
                result.resultCount shouldBe 1
                result.results.first().isKoppelbaar shouldBe false
            }
        }

        When("findLinkableZaken with DEELZAAK is called") {
            every { policyService.readZaakRechtenForZaakZoekObject(zaakZoekObject) } returns createZaakRechten()
            val result = zaakKoppelenRestService.findLinkableZaken(
                zaakUuid = sourceZaak.uuid,
                zoekZaakIdentifier = zoekZaakIdentifier,
                relationType = RelatieType.DEELZAAK,
                page = page,
                rows = rows
            )

            Then("the target zaak should not be linkable") {
                result.resultCount shouldBe 1
                result.results.first().isKoppelbaar shouldBe false
            }
        }

        When("findLinkableZaken with GERELATEERD is called") {
            every { policyService.readZaakRechtenForZaakZoekObject(zaakZoekObject) } returns createZaakRechten()
            val result = zaakKoppelenRestService.findLinkableZaken(
                zaakUuid = sourceZaak.uuid,
                zoekZaakIdentifier = zoekZaakIdentifier,
                relationType = RelatieType.GERELATEERD,
                page = page,
                rows = rows
            )

            Then("the target zaak should be linkable") {
                result.resultCount shouldBe 1
                result.results.first().isKoppelbaar shouldBe true
            }
        }
    }

    Context("Linking a zaak") {
        Given("Two open zaken with zaak link data using a 'hoofdzaak' relatie and no reverse relation") {
            val zaak = createZaak()
            val zaakType = createZaakType()
            val teKoppelenZaak = createZaak()
            val teKoppelenZaakType = createZaakType().apply { deelzaaktypen = listOf(zaak.zaaktype) }
            val restZaakLinkData = createRestZaakLinkData(
                zaakUuid = zaak.uuid,
                teKoppelenZaakUuid = teKoppelenZaak.uuid,
                relatieType = RelatieType.HOOFDZAAK
            )
            val patchZaakUUIDSlot = slot<UUID>()
            val patchZaakSlot = slot<Zaak>()
            val loggedInUser = createLoggedInUser()
            every { zaakService.readZaakAndZaakTypeByZaakUUID(restZaakLinkData.zaakUuid) } returns Pair(zaak, zaakType)
            every {
                zaakService.readZaakAndZaakTypeByZaakUUID(restZaakLinkData.teKoppelenZaakUuid)
            } returns Pair(teKoppelenZaak, teKoppelenZaakType)
            every { policyService.readZaakRechten(zaak, zaakType, loggedInUser) } returns createZaakRechten()
            every { policyService.readZaakRechten(teKoppelenZaak, teKoppelenZaakType, loggedInUser) } returns createZaakRechten()
            every { zrcClientService.patchZaak(capture(patchZaakUUIDSlot), capture(patchZaakSlot)) } returns zaak
            every { indexingService.addOrUpdateZaak(teKoppelenZaak.uuid, false) } just runs
            every { eventingService.send(any<ScreenEvent>()) } just runs
            every { loggedInUserInstance.get() } returns loggedInUser

            When("the zaken are linked") {
                zaakKoppelenRestService.linkZaak(restZaakLinkData)

                Then("the two zaken are successfully linked, the index is updated and a screen event is sent") {
                    verify(exactly = 1) {
                        zrcClientService.patchZaak(any(), any())
                    }
                    patchZaakUUIDSlot.captured shouldBe zaak.uuid
                    with(patchZaakSlot.captured) {
                        hoofdzaak shouldBe teKoppelenZaak.url
                    }
                }
            }
        }

        Given("An open zaak and a closed zaak") {
            val zaak = createZaak()
            val zaakType = createZaakType()
            val teKoppelenZaak = createZaak()
            val teKoppelenZaakType = createZaakType().apply { deelzaaktypen = listOf(zaak.zaaktype) }
            val restZaakLinkData = createRestZaakLinkData(
                zaakUuid = zaak.uuid,
                teKoppelenZaakUuid = teKoppelenZaak.uuid,
                relatieType = RelatieType.HOOFDZAAK
            )
            val loggedInUser = createLoggedInUser()

            every { zaakService.readZaakAndZaakTypeByZaakUUID(restZaakLinkData.zaakUuid) } returns Pair(zaak, zaakType)
            every {
                zaakService.readZaakAndZaakTypeByZaakUUID(restZaakLinkData.teKoppelenZaakUuid)
            } returns Pair(teKoppelenZaak, teKoppelenZaakType)
            every { policyService.readZaakRechten(zaak, zaakType, loggedInUser) } returns createZaakRechten()
            every { policyService.readZaakRechten(teKoppelenZaak, teKoppelenZaakType, loggedInUser) } returns createZaakRechten()
            every { loggedInUserInstance.get() } returns loggedInUser

            val patchZaakUUIDSlot = slot<UUID>()
            val patchZaakSlot = slot<Zaak>()
            every {
                zrcClientService.patchZaak(capture(patchZaakUUIDSlot), capture(patchZaakSlot))
            } returns zaak
            every { indexingService.addOrUpdateZaak(teKoppelenZaak.uuid, false) } just runs
            every { eventingService.send(any<ScreenEvent>()) } just runs

            When("the zaken are linked") {
                zaakKoppelenRestService.linkZaak(restZaakLinkData)

                Then("the two zaken are successfully linked, the index is updated and a screen event is sent") {
                    verify(exactly = 1) {
                        zrcClientService.patchZaak(any(), any())
                    }
                    patchZaakUUIDSlot.captured shouldBe zaak.uuid
                    with(patchZaakSlot.captured) {
                        hoofdzaak shouldBe teKoppelenZaak.url
                    }
                }
            }
        }

        Given("Two open zaken with zaak link data using a 'deelzaak' relatie") {
            val zaak = createZaak()
            val teKoppelenZaak = createZaak()
            val zaakType = createZaakType().apply { deelzaaktypen = listOf(teKoppelenZaak.zaaktype) }
            val teKoppelenZaakType = createZaakType()
            val restZaakLinkData = createRestZaakLinkData(
                zaakUuid = zaak.uuid,
                teKoppelenZaakUuid = teKoppelenZaak.uuid,
                relatieType = RelatieType.DEELZAAK
            )
            val patchZaakUUIDSlot = slot<UUID>()
            val patchZaakSlot = slot<Zaak>()
            val loggedInUser = createLoggedInUser()
            every { zaakService.readZaakAndZaakTypeByZaakUUID(restZaakLinkData.zaakUuid) } returns Pair(zaak, zaakType)
            every {
                zaakService.readZaakAndZaakTypeByZaakUUID(restZaakLinkData.teKoppelenZaakUuid)
            } returns Pair(teKoppelenZaak, teKoppelenZaakType)
            every { policyService.readZaakRechten(zaak, zaakType, loggedInUser) } returns createZaakRechten()
            every {
                policyService.readZaakRechten(teKoppelenZaak, teKoppelenZaakType, loggedInUser)
            } returns createZaakRechten()
            every { zrcClientService.patchZaak(capture(patchZaakUUIDSlot), capture(patchZaakSlot)) } returns zaak
            every { indexingService.addOrUpdateZaak(zaak.uuid, false) } just runs
            every { eventingService.send(any<ScreenEvent>()) } just runs
            every { loggedInUserInstance.get() } returns loggedInUser

            When("the zaken are linked") {
                zaakKoppelenRestService.linkZaak(restZaakLinkData)

                Then("the two zaken are successfully linked as hoofd- and deelzaak") {
                    verify(exactly = 1) {
                        zrcClientService.patchZaak(any(), any())
                    }
                    patchZaakUUIDSlot.captured shouldBe teKoppelenZaak.uuid
                    with(patchZaakSlot.captured) {
                        hoofdzaak shouldBe zaak.url
                    }
                }
            }
        }

        Given("Zaak link data using an unsupported relatie type") {
            val zaak = createZaak()
            val zaakType = createZaakType()
            val teKoppelenZaak = createZaak()
            val teKoppelenZaakType = createZaakType()
            val restZaakLinkData = createRestZaakLinkData(
                zaakUuid = zaak.uuid,
                teKoppelenZaakUuid = teKoppelenZaak.uuid,
                relatieType = RelatieType.VERVOLG
            )
            val loggedInUser = createLoggedInUser()
            every { zaakService.readZaakAndZaakTypeByZaakUUID(restZaakLinkData.zaakUuid) } returns Pair(zaak, zaakType)
            every {
                zaakService.readZaakAndZaakTypeByZaakUUID(restZaakLinkData.teKoppelenZaakUuid)
            } returns Pair(teKoppelenZaak, teKoppelenZaakType)
            every { loggedInUserInstance.get() } returns loggedInUser

            When("the zaken are linked") {
                val illegalArgumentException = shouldThrow<IllegalArgumentException> {
                    zaakKoppelenRestService.linkZaak(restZaakLinkData)
                }

                Then("an IllegalArgumentException is thrown") {
                    illegalArgumentException.message shouldBe
                        "RelatieType VERVOLG cannot be used for linking zaken"
                }
            }
        }

        Given("Two open zaken with zaak link data using a 'gerelateerd' relatie and a reason") {
            val zaak = createZaak()
            val zaakType = createZaakType()
            val teKoppelenZaak = createZaak()
            val teKoppelenZaakType = createZaakType()
            val restZaakLinkData = createRestZaakLinkData(
                zaakUuid = zaak.uuid,
                teKoppelenZaakUuid = teKoppelenZaak.uuid,
                relatieType = RelatieType.GERELATEERD,
                reden = "fakeReden"
            )
            val patchZaakUUIDSlot = slot<UUID>()
            val patchZaakSlot = slot<Zaak>()
            val loggedInUser = createLoggedInUser()
            every { zaakService.readZaakAndZaakTypeByZaakUUID(restZaakLinkData.zaakUuid) } returns Pair(zaak, zaakType)
            every {
                zaakService.readZaakAndZaakTypeByZaakUUID(restZaakLinkData.teKoppelenZaakUuid)
            } returns Pair(teKoppelenZaak, teKoppelenZaakType)
            every { policyService.readZaakRechten(zaak, zaakType, loggedInUser) } returns createZaakRechten()
            every {
                policyService.readZaakRechten(teKoppelenZaak, teKoppelenZaakType, loggedInUser)
            } returns createZaakRechten()
            every {
                zrcClientService.patchZaak(capture(patchZaakUUIDSlot), capture(patchZaakSlot), "fakeReden")
            } returns zaak
            every { loggedInUserInstance.get() } returns loggedInUser

            When("the zaken are linked with relatie type GERELATEERD") {
                zaakKoppelenRestService.linkZaak(restZaakLinkData)

                Then("patchZaak is called once with the source zaak UUID") {
                    verify(exactly = 1) {
                        zrcClientService.patchZaak(any(), any(), any())
                    }
                    patchZaakUUIDSlot.captured shouldBe zaak.uuid
                }

                Then("the patched zaak has one gerelateerdeZaken item pointing to the target zaak") {
                    patchZaakSlot.captured.gerelateerdeZaken shouldHaveSize 1
                    patchZaakSlot.captured.gerelateerdeZaken[0].url shouldBe teKoppelenZaak.url
                }
            }
        }
    }

    Context("Unlinking a zaak") {
        Given("A zaak with a gerelateerde zaak linked to it") {
            val gekoppeldeZaak = createZaak()
            val gekoppeldeZaakType = createZaakType()
            val zaak = createZaak().apply {
                addGerelateerdeZakenItem(GerelateerdeZaak().apply { url = gekoppeldeZaak.url })
            }
            val zaakType = createZaakType()
            val restZaakUnlinkData = createRestZaakUnlinkData(
                zaakUuid = zaak.uuid,
                gekoppeldeZaakIdentificatie = gekoppeldeZaak.identificatie,
                relationType = RelatieType.GERELATEERD,
                reason = "fakeReden"
            )
            val patchZaakUUIDSlot = slot<UUID>()
            val patchZaakSlot = slot<Zaak>()
            val loggedInUser = createLoggedInUser()

            every { zaakService.readZaakAndZaakTypeByZaakUUID(zaak.uuid) } returns Pair(zaak, zaakType)
            every {
                zaakService.readZaakAndZaakTypeByZaakID(restZaakUnlinkData.gekoppeldeZaakIdentificatie)
            } returns Pair(gekoppeldeZaak, gekoppeldeZaakType)
            every { policyService.readZaakRechten(zaak, zaakType, loggedInUser) } returns createZaakRechten()
            every {
                policyService.readZaakRechten(gekoppeldeZaak, gekoppeldeZaakType, loggedInUser)
            } returns createZaakRechten()
            every {
                zrcClientService.patchZaak(capture(patchZaakUUIDSlot), capture(patchZaakSlot), "fakeReden")
            } returns zaak
            every { loggedInUserInstance.get() } returns loggedInUser

            When("the gerelateerde zaak is unlinked") {
                zaakKoppelenRestService.unlinkZaak(restZaakUnlinkData)

                Then("patchZaak is called once with the source zaak UUID") {
                    verify(exactly = 1) {
                        zrcClientService.patchZaak(any(), any(), any())
                    }
                    patchZaakUUIDSlot.captured shouldBe zaak.uuid
                }

                Then(
                    "the patched zaak is a GerelateerdeZakenZaakPatch with gerelateerdeZaken set to an empty list"
                ) {
                    patchZaakSlot.captured.gerelateerdeZaken shouldBe emptyList()
                }
            }
        }

        Given("A zaak without koppelen right on the source zaak") {
            val gekoppeldeZaak = createZaak()
            val gekoppeldeZaakType = createZaakType()
            val zaak = createZaak()
            val zaakType = createZaakType()
            val restZaakUnlinkData = createRestZaakUnlinkData(
                zaakUuid = zaak.uuid,
                gekoppeldeZaakIdentificatie = gekoppeldeZaak.identificatie,
                relationType = RelatieType.GERELATEERD,
                reason = "fakeReden"
            )
            val loggedInUser = createLoggedInUser()

            every { zaakService.readZaakAndZaakTypeByZaakUUID(zaak.uuid) } returns Pair(zaak, zaakType)
            every {
                zaakService.readZaakAndZaakTypeByZaakID(restZaakUnlinkData.gekoppeldeZaakIdentificatie)
            } returns Pair(gekoppeldeZaak, gekoppeldeZaakType)
            every {
                policyService.readZaakRechten(zaak, zaakType, loggedInUser)
            } returns createZaakRechten(koppelen = false)
            every {
                policyService.readZaakRechten(gekoppeldeZaak, gekoppeldeZaakType, loggedInUser)
            } returns createZaakRechten()
            every { loggedInUserInstance.get() } returns loggedInUser

            When("unlinkZaak is called") {
                val policyException = shouldThrow<PolicyException> {
                    zaakKoppelenRestService.unlinkZaak(restZaakUnlinkData)
                }

                Then("a PolicyException is thrown") {
                    policyException shouldNotBe null
                }
            }
        }

        Given("A gerelateerde zaak without lezen right on the linked zaak") {
            val gekoppeldeZaak = createZaak()
            val gekoppeldeZaakType = createZaakType()
            val zaak = createZaak()
            val zaakType = createZaakType()
            val restZaakUnlinkData = createRestZaakUnlinkData(
                zaakUuid = zaak.uuid,
                gekoppeldeZaakIdentificatie = gekoppeldeZaak.identificatie,
                relationType = RelatieType.GERELATEERD,
                reason = "fakeReden"
            )
            val loggedInUser = createLoggedInUser()

            every { zaakService.readZaakAndZaakTypeByZaakUUID(zaak.uuid) } returns Pair(zaak, zaakType)
            every {
                zaakService.readZaakAndZaakTypeByZaakID(restZaakUnlinkData.gekoppeldeZaakIdentificatie)
            } returns Pair(gekoppeldeZaak, gekoppeldeZaakType)
            every {
                policyService.readZaakRechten(zaak, zaakType, loggedInUser)
            } returns createZaakRechten(koppelen = true)
            every {
                policyService.readZaakRechten(gekoppeldeZaak, gekoppeldeZaakType, loggedInUser)
            } returns createZaakRechten(lezen = false)
            every { loggedInUserInstance.get() } returns loggedInUser

            When("unlinkZaak is called") {
                val policyException = shouldThrow<PolicyException> {
                    zaakKoppelenRestService.unlinkZaak(restZaakUnlinkData)
                }

                Then("a PolicyException is thrown") {
                    policyException shouldNotBe null
                }
            }
        }

        Given("A hoofdzaak without koppelen right on the linked zaak") {
            val gekoppeldeZaak = createZaak()
            val gekoppeldeZaakType = createZaakType()
            val zaak = createZaak()
            val zaakType = createZaakType()
            val restZaakUnlinkData = createRestZaakUnlinkData(
                zaakUuid = zaak.uuid,
                gekoppeldeZaakIdentificatie = gekoppeldeZaak.identificatie,
                relationType = RelatieType.HOOFDZAAK,
                reason = "fakeReden"
            )
            val loggedInUser = createLoggedInUser()

            every { zaakService.readZaakAndZaakTypeByZaakUUID(zaak.uuid) } returns Pair(zaak, zaakType)
            every {
                zaakService.readZaakAndZaakTypeByZaakID(restZaakUnlinkData.gekoppeldeZaakIdentificatie)
            } returns Pair(gekoppeldeZaak, gekoppeldeZaakType)
            every {
                policyService.readZaakRechten(zaak, zaakType, loggedInUser)
            } returns createZaakRechten(koppelen = true)
            every {
                policyService.readZaakRechten(gekoppeldeZaak, gekoppeldeZaakType, loggedInUser)
            } returns createZaakRechten(koppelen = false)
            every { loggedInUserInstance.get() } returns loggedInUser

            When("unlinkZaak is called") {
                val policyException = shouldThrow<PolicyException> {
                    zaakKoppelenRestService.unlinkZaak(restZaakUnlinkData)
                }

                Then("a PolicyException is thrown") {
                    policyException shouldNotBe null
                }
            }
        }

        Given("A deelzaak linked to a hoofdzaak") {
            val gekoppeldeZaak = createZaak()
            val gekoppeldeZaakType = createZaakType()
            val zaak = createZaak()
            val zaakType = createZaakType()
            val restZaakUnlinkData = createRestZaakUnlinkData(
                zaakUuid = zaak.uuid,
                gekoppeldeZaakIdentificatie = gekoppeldeZaak.identificatie,
                relationType = RelatieType.DEELZAAK,
                reason = "fakeReden"
            )
            val patchZaakUUIDSlot = slot<UUID>()
            val patchZaakSlot = slot<Zaak>()
            val loggedInUser = createLoggedInUser()

            every { zaakService.readZaakAndZaakTypeByZaakUUID(zaak.uuid) } returns Pair(zaak, zaakType)
            every {
                zaakService.readZaakAndZaakTypeByZaakID(restZaakUnlinkData.gekoppeldeZaakIdentificatie)
            } returns Pair(gekoppeldeZaak, gekoppeldeZaakType)
            every {
                policyService.readZaakRechten(zaak, zaakType, loggedInUser)
            } returns createZaakRechten(koppelen = true)
            every {
                policyService.readZaakRechten(gekoppeldeZaak, gekoppeldeZaakType, loggedInUser)
            } returns createZaakRechten(koppelen = true)
            every {
                zrcClientService.patchZaak(capture(patchZaakUUIDSlot), capture(patchZaakSlot), "fakeReden")
            } returns zaak
            every { indexingService.addOrUpdateZaak(zaak.uuid, false) } just runs
            every { eventingService.send(any<ScreenEvent>()) } just runs
            every { loggedInUserInstance.get() } returns loggedInUser

            When("unlinkZaak is called with DEELZAAK relatie type") {
                zaakKoppelenRestService.unlinkZaak(restZaakUnlinkData)

                Then("the deelzaak is successfully unlinked from the hoofdzaak") {
                    verify(exactly = 1) {
                        zrcClientService.patchZaak(any(), any(), any())
                    }
                    patchZaakUUIDSlot.captured shouldBe gekoppeldeZaak.uuid
                }
            }
        }

        Given("Zaak unlink data using an unsupported relatie type") {
            val gekoppeldeZaak = createZaak()
            val gekoppeldeZaakType = createZaakType()
            val zaak = createZaak()
            val zaakType = createZaakType()
            val restZaakUnlinkData = createRestZaakUnlinkData(
                zaakUuid = zaak.uuid,
                gekoppeldeZaakIdentificatie = gekoppeldeZaak.identificatie,
                relationType = RelatieType.VERVOLG,
                reason = "fakeReden"
            )

            every { zaakService.readZaakAndZaakTypeByZaakUUID(zaak.uuid) } returns Pair(zaak, zaakType)
            every {
                zaakService.readZaakAndZaakTypeByZaakID(restZaakUnlinkData.gekoppeldeZaakIdentificatie)
            } returns Pair(gekoppeldeZaak, gekoppeldeZaakType)

            When("unlinkZaak is called") {
                val illegalArgumentException = shouldThrow<IllegalArgumentException> {
                    zaakKoppelenRestService.unlinkZaak(restZaakUnlinkData)
                }

                Then("an IllegalArgumentException is thrown") {
                    illegalArgumentException.message shouldBe
                        "RelatieType VERVOLG cannot be used for unlinking zaken"
                }
            }
        }

        Given("A gerelateerde zaak with lezen but without koppelen on the linked zaak") {
            val gekoppeldeZaak = createZaak()
            val gekoppeldeZaakType = createZaakType()
            val zaak = createZaak().apply {
                addGerelateerdeZakenItem(GerelateerdeZaak().apply { url = gekoppeldeZaak.url })
            }
            val zaakType = createZaakType()
            val restZaakUnlinkData = createRestZaakUnlinkData(
                zaakUuid = zaak.uuid,
                gekoppeldeZaakIdentificatie = gekoppeldeZaak.identificatie,
                relationType = RelatieType.GERELATEERD,
                reason = "fakeReden"
            )
            val loggedInUser = createLoggedInUser()

            every { zaakService.readZaakAndZaakTypeByZaakUUID(zaak.uuid) } returns Pair(zaak, zaakType)
            every {
                zaakService.readZaakAndZaakTypeByZaakID(restZaakUnlinkData.gekoppeldeZaakIdentificatie)
            } returns Pair(gekoppeldeZaak, gekoppeldeZaakType)
            every {
                policyService.readZaakRechten(zaak, zaakType, loggedInUser)
            } returns createZaakRechten(koppelen = true)
            every {
                policyService.readZaakRechten(gekoppeldeZaak, gekoppeldeZaakType, loggedInUser)
            } returns createZaakRechten(lezen = true, koppelen = false)
            every { zrcClientService.patchZaak(any(), any(), "fakeReden") } returns zaak
            every { loggedInUserInstance.get() } returns loggedInUser

            When("unlinkZaak is called") {
                zaakKoppelenRestService.unlinkZaak(restZaakUnlinkData)

                Then("unlinking succeeds without PolicyException") {
                    verify(exactly = 1) {
                        zrcClientService.patchZaak(any(), any(), any())
                    }
                }
            }
        }
    }
})
