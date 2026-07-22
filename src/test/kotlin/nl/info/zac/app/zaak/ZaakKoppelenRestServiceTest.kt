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
import nl.info.zac.app.search.model.RestDatumRange
import nl.info.zac.app.zaak.converter.RestZaaktypeConverter
import nl.info.zac.app.zaak.model.RelatieType
import nl.info.zac.app.zaak.model.createRestFindLinkableZakenRequest
import nl.info.zac.app.zaak.model.createRestZaakLinkData
import nl.info.zac.app.zaak.model.createRestZaakUnlinkData
import nl.info.zac.app.zaak.model.createRestZaaktype
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.authentication.createLoggedInUser
import nl.info.zac.configuration.ConfigurationService
import nl.info.zac.policy.PolicyService
import nl.info.zac.policy.exception.PolicyException
import nl.info.zac.policy.output.createOverigeRechten
import nl.info.zac.policy.output.createZaakRechten
import nl.info.zac.search.IndexingService
import nl.info.zac.search.SearchService
import nl.info.zac.search.model.DatumVeld
import nl.info.zac.search.model.FilterVeld
import nl.info.zac.search.model.ZoekParameters
import nl.info.zac.search.model.ZoekResultaat
import nl.info.zac.search.model.ZoekVeld
import nl.info.zac.search.model.createZaakZoekObject
import nl.info.zac.search.model.zoekobject.ZoekObjectType.ZAAK
import nl.info.zac.zaak.ZaakService
import java.net.URI
import java.time.LocalDate
import java.util.UUID

@Suppress("LargeClass")
class ZaakKoppelenRestServiceTest : BehaviorSpec({
    isolationMode = IsolationMode.InstancePerTest
    val eventingService = mockk<EventingService>()
    val indexingService = mockk<IndexingService>()
    val policyService = mockk<PolicyService>()
    val searchService = mockk<SearchService>()
    val zaakService = mockk<ZaakService>()
    val zrcClientService = mockk<ZrcClientService>()
    val ztcClientService = mockk<ZtcClientService>()
    val loggedInUserInstance = mockk<Instance<LoggedInUser>>()
    val restZaaktypeConverter = mockk<RestZaaktypeConverter>()
    val configurationService = mockk<ConfigurationService>()
    val zaakKoppelenRestService = ZaakKoppelenRestService(
        eventingService = eventingService,
        indexingService = indexingService,
        policyService = policyService,
        searchService = searchService,
        zaakService = zaakService,
        zrcClientService = zrcClientService,
        ztcClientService = ztcClientService,
        loggedInUserInstance = loggedInUserInstance,
        restZaaktypeConverter = restZaaktypeConverter,
        configurationService = configurationService
    )

    afterEach {
        checkUnnecessaryStub()
    }

    given("A source zaak which is not linked and a target not linked zaak") {
        val sourceZaak = createZaak(
            archiefnominatie = ArchiefnominatieEnum.BLIJVEND_BEWAREN,
        )
        val zaakZoekObject = createZaakZoekObject(
            type = ZAAK,
            archiefNominatie = ArchiefnominatieEnum.BLIJVEND_BEWAREN.toString()
        )
        val zoekResultaat = ZoekResultaat(listOf(zaakZoekObject), 1)
        val loggedInUser = createLoggedInUser()
        val zaakType = createZaakType().apply {
            deelzaaktypen = listOf(URI(zaakZoekObject.zaaktypeUuid))
        }

        every { zrcClientService.readZaak(sourceZaak.uuid) } returns sourceZaak
        every { searchService.search(any()) } returns zoekResultaat
        every { zaakService.readZaakTypeByZaak(sourceZaak) } returns zaakType
        every { policyService.readZaakRechten(sourceZaak, zaakType, loggedInUser) } returns createZaakRechten()
        every { loggedInUserInstance.get() } returns loggedInUser

        `when`("findLinkableZaken with GERELATEERD is called") {
            every { policyService.readZaakRechtenForZaakZoekObject(zaakZoekObject) } returns createZaakRechten()

            val result = zaakKoppelenRestService.findLinkableZaken(
                sourceZaak.uuid,
                createRestFindLinkableZakenRequest(
                    zoekZaakIdentifier = zaakZoekObject.identificatie,
                    relationType = RelatieType.GERELATEERD
                )
            )

            then("a single linkable zaak should be returned") {
                result.resultCount shouldBe 1
            }

            And("link is allowed") {
                with(result.results.first()) {
                    id shouldBe zaakZoekObject.getObjectId()
                    type shouldBe zaakZoekObject.getType()
                    identificatie shouldBe zaakZoekObject.identificatie
                    omschrijving shouldBe zaakZoekObject.omschrijving
                    zaaktypeOmschrijving shouldBe zaakZoekObject.zaaktypeOmschrijving
                    statustypeOmschrijving shouldBe zaakZoekObject.statustypeOmschrijving
                    isKoppelbaar shouldBe true
                }
            }

            And("required services should've been invoked") {
                verify(exactly = 1) {
                    zrcClientService.readZaak(sourceZaak.uuid)
                    searchService.search(any())
                    zaakService.readZaakTypeByZaak(sourceZaak)
                    policyService.readZaakRechten(sourceZaak, zaakType, loggedInUser)
                    policyService.readZaakRechtenForZaakZoekObject(zaakZoekObject)
                }
            }
        }

        `when`("findLinkableZaken with HOOFDZAAK is called") {
            every { policyService.readZaakRechtenForZaakZoekObject(zaakZoekObject) } returns createZaakRechten()
            every {
                ztcClientService.readZaaktype(UUID.fromString(zaakZoekObject.zaaktypeUuid)).deelzaaktypen
            } returns listOf(sourceZaak.zaaktype)

            val result = zaakKoppelenRestService.findLinkableZaken(
                sourceZaak.uuid,
                createRestFindLinkableZakenRequest(
                    zoekZaakIdentifier = zaakZoekObject.identificatie,
                    relationType = RelatieType.HOOFDZAAK
                )
            )

            then("a single linkable zaak should be returned") {
                result.resultCount shouldBe 1
            }

            And("link is allowed") {
                with(result.results.first()) {
                    id shouldBe zaakZoekObject.getObjectId()
                    type shouldBe zaakZoekObject.getType()
                    identificatie shouldBe zaakZoekObject.identificatie
                    omschrijving shouldBe zaakZoekObject.omschrijving
                    zaaktypeOmschrijving shouldBe zaakZoekObject.zaaktypeOmschrijving
                    statustypeOmschrijving shouldBe zaakZoekObject.statustypeOmschrijving
                    isKoppelbaar shouldBe true
                }
            }

            And("required services should've be invoked") {
                verify(exactly = 1) {
                    zrcClientService.readZaak(sourceZaak.uuid)
                    searchService.search(any())
                    zaakService.readZaakTypeByZaak(sourceZaak)
                    policyService.readZaakRechten(sourceZaak, zaakType, loggedInUser)
                    policyService.readZaakRechtenForZaakZoekObject(zaakZoekObject)
                }
                verify(exactly = 1) {
                    ztcClientService.readZaaktype(UUID.fromString(zaakZoekObject.zaaktypeUuid)).deelzaaktypen
                }
            }
        }

        `when`("findLinkableZaken with DEELZAAK is called") {
            every { policyService.readZaakRechtenForZaakZoekObject(zaakZoekObject) } returns createZaakRechten()

            val result = zaakKoppelenRestService.findLinkableZaken(
                sourceZaak.uuid,
                createRestFindLinkableZakenRequest(
                    zoekZaakIdentifier = zaakZoekObject.identificatie,
                    relationType = RelatieType.DEELZAAK
                )
            )

            then("a single linkable zaak should be returned") {
                result.resultCount shouldBe 1
            }

            And("link is allowed") {
                with(result.results.first()) {
                    id shouldBe zaakZoekObject.getObjectId()
                    type shouldBe zaakZoekObject.getType()
                    identificatie shouldBe zaakZoekObject.identificatie
                    omschrijving shouldBe zaakZoekObject.omschrijving
                    zaaktypeOmschrijving shouldBe zaakZoekObject.zaaktypeOmschrijving
                    statustypeOmschrijving shouldBe zaakZoekObject.statustypeOmschrijving
                    isKoppelbaar shouldBe true
                }
            }

            And("required services should've be invoked") {
                verify(exactly = 1) {
                    zrcClientService.readZaak(sourceZaak.uuid)
                    searchService.search(any())
                    zaakService.readZaakTypeByZaak(sourceZaak)
                    policyService.readZaakRechten(sourceZaak, zaakType, loggedInUser)
                    policyService.readZaakRechtenForZaakZoekObject(zaakZoekObject)
                }
            }
        }
    }

    context("Linking a zaak") {
        given("Two open zaken with zaak link data using a 'hoofdzaak' relatie and no reverse relation") {
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

            `when`("the zaken are linked") {
                zaakKoppelenRestService.linkZaak(restZaakLinkData)

                then("the two zaken are successfully linked, the index is updated and a screen event is sent") {
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

        given("Two open zaken with zaak link data using a 'deelzaak' relatie") {
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

            `when`("the zaken are linked") {
                zaakKoppelenRestService.linkZaak(restZaakLinkData)

                then("the two zaken are successfully linked as hoofd- and deelzaak") {
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

        given("Zaak link data using an unsupported relatie type") {
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

            `when`("the zaken are linked") {
                val illegalArgumentException = shouldThrow<IllegalArgumentException> {
                    zaakKoppelenRestService.linkZaak(restZaakLinkData)
                }

                then("an IllegalArgumentException is thrown") {
                    illegalArgumentException.message shouldBe
                        "RelatieType VERVOLG cannot be used for linking zaken"
                }
            }
        }

        given("Two open zaken with zaak link data using a 'gerelateerd' relatie and a reason") {
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

            `when`("the zaken are linked with relatie type GERELATEERD") {
                zaakKoppelenRestService.linkZaak(restZaakLinkData)

                then("patchZaak is called once with the source zaak UUID") {
                    verify(exactly = 1) {
                        zrcClientService.patchZaak(any(), any(), any())
                    }
                    patchZaakUUIDSlot.captured shouldBe zaak.uuid
                }

                then("the patched zaak has one gerelateerdeZaken item pointing to the target zaak") {
                    patchZaakSlot.captured.gerelateerdeZaken shouldHaveSize 1
                    patchZaakSlot.captured.gerelateerdeZaken[0].url shouldBe teKoppelenZaak.url
                }
            }
        }
    }

    context("Unlinking a zaak") {
        given("A zaak with a gerelateerde zaak linked to it") {
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

            `when`("the gerelateerde zaak is unlinked") {
                zaakKoppelenRestService.unlinkZaak(restZaakUnlinkData)

                then("patchZaak is called once with the source zaak UUID") {
                    verify(exactly = 1) {
                        zrcClientService.patchZaak(any(), any(), any())
                    }
                    patchZaakUUIDSlot.captured shouldBe zaak.uuid
                }

                then(
                    "the patched zaak is a GerelateerdeZakenZaakPatch with gerelateerdeZaken set to an empty list"
                ) {
                    patchZaakSlot.captured.gerelateerdeZaken shouldBe emptyList()
                }
            }
        }

        given("A zaak without koppelen right on the source zaak") {
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

            `when`("unlinkZaak is called") {
                val policyException = shouldThrow<PolicyException> {
                    zaakKoppelenRestService.unlinkZaak(restZaakUnlinkData)
                }

                then("a PolicyException is thrown") {
                    policyException shouldNotBe null
                }
            }
        }

        given("A gerelateerde zaak without lezen right on the linked zaak") {
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

            `when`("unlinkZaak is called") {
                val policyException = shouldThrow<PolicyException> {
                    zaakKoppelenRestService.unlinkZaak(restZaakUnlinkData)
                }

                then("a PolicyException is thrown") {
                    policyException shouldNotBe null
                }
            }
        }

        given("A hoofdzaak without koppelen right on the linked zaak") {
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

            `when`("unlinkZaak is called") {
                val policyException = shouldThrow<PolicyException> {
                    zaakKoppelenRestService.unlinkZaak(restZaakUnlinkData)
                }

                then("a PolicyException is thrown") {
                    policyException shouldNotBe null
                }
            }
        }

        given("A deelzaak linked to a hoofdzaak") {
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

            `when`("unlinkZaak is called with DEELZAAK relatie type") {
                zaakKoppelenRestService.unlinkZaak(restZaakUnlinkData)

                then("the deelzaak is successfully unlinked from the hoofdzaak") {
                    verify(exactly = 1) {
                        zrcClientService.patchZaak(any(), any(), any())
                    }
                    patchZaakUUIDSlot.captured shouldBe gekoppeldeZaak.uuid
                }
            }
        }

        given("Zaak unlink data using an unsupported relatie type") {
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

            `when`("unlinkZaak is called") {
                val illegalArgumentException = shouldThrow<IllegalArgumentException> {
                    zaakKoppelenRestService.unlinkZaak(restZaakUnlinkData)
                }

                then("an IllegalArgumentException is thrown") {
                    illegalArgumentException.message shouldBe
                        "RelatieType VERVOLG cannot be used for unlinking zaken"
                }
            }
        }

        given("A gerelateerde zaak with lezen but without koppelen on the linked zaak") {
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

            `when`("unlinkZaak is called") {
                zaakKoppelenRestService.unlinkZaak(restZaakUnlinkData)

                then("unlinking succeeds without PolicyException") {
                    verify(exactly = 1) {
                        zrcClientService.patchZaak(any(), any(), any())
                    }
                }
            }
        }
    }

    context("Building search parameters for findLinkableZaken") {
        given("a linkable zaken search setup") {
            val fakeZaakTypeUuid = UUID.randomUUID().toString()
            val sourceZaak = createZaak(
                archiefnominatie = ArchiefnominatieEnum.BLIJVEND_BEWAREN,
            )
            val zaakZoekObject = createZaakZoekObject(
                type = ZAAK,
                archiefNominatie = ArchiefnominatieEnum.BLIJVEND_BEWAREN.toString()
            )
            val zoekResultaat = ZoekResultaat(listOf(zaakZoekObject), 1)
            val loggedInUser = createLoggedInUser()
            val zaakType = createZaakType().apply {
                deelzaaktypen = listOf(URI(zaakZoekObject.zaaktypeUuid))
            }
            val zoekParametersSlot = slot<ZoekParameters>()

            every { zrcClientService.readZaak(sourceZaak.uuid) } returns sourceZaak
            every { searchService.search(capture(zoekParametersSlot)) } returns zoekResultaat
            every { zaakService.readZaakTypeByZaak(sourceZaak) } returns zaakType
            every { policyService.readZaakRechten(sourceZaak, zaakType, loggedInUser) } returns createZaakRechten()
            every { policyService.readZaakRechtenForZaakZoekObject(zaakZoekObject) } returns createZaakRechten()
            every { loggedInUserInstance.get() } returns loggedInUser

            `when`("all search fields are set") {
                zaakKoppelenRestService.findLinkableZaken(
                    zaakUuid = sourceZaak.uuid,
                    createRestFindLinkableZakenRequest(
                        zoekZaakIdentifier = "  fakeZaakIdentifier  ",
                        zoekZaakOmschrijving = "  fakeOmschrijving  ",
                        zoekZaakTypeOmschrijving = "fakeZaakTypeOmschrijving",
                        relationType = RelatieType.GERELATEERD
                    )
                )

                then("the search parameters should contain trimmed values and zaaktype filter") {
                    zoekParametersSlot.captured.getZoeken()[ZoekVeld.ZAAK_IDENTIFICATIE] shouldBe "fakeZaakIdentifier"
                    zoekParametersSlot.captured.getZoeken()[ZoekVeld.ZAAK_OMSCHRIJVING] shouldBe "fakeOmschrijving"
                    zoekParametersSlot.captured.getFilters()[FilterVeld.ZAAK_ZAAKTYPE]!!
                        .values.first() shouldBe "fakeZaakTypeOmschrijving"
                }
            }

            `when`("all search fields are null") {
                zaakKoppelenRestService.findLinkableZaken(
                    zaakUuid = sourceZaak.uuid,
                    createRestFindLinkableZakenRequest(
                        zoekZaakIdentifier = null,
                        zoekZaakOmschrijving = null,
                        zoekZaakTypeOmschrijving = null,
                        relationType = RelatieType.GERELATEERD
                    )
                )

                then("the search parameters should not contain any optional search fields") {
                    zoekParametersSlot.captured.getZoeken().containsKey(ZoekVeld.ZAAK_IDENTIFICATIE) shouldBe false
                    zoekParametersSlot.captured.getZoeken().containsKey(ZoekVeld.ZAAK_OMSCHRIJVING) shouldBe false
                    zoekParametersSlot.captured.getFilters().containsKey(FilterVeld.ZAAK_ZAAKTYPE_UUID) shouldBe false
                }
            }

            `when`("string search fields are blank") {
                zaakKoppelenRestService.findLinkableZaken(
                    zaakUuid = sourceZaak.uuid,
                    createRestFindLinkableZakenRequest(
                        zoekZaakIdentifier = "   ",
                        zoekZaakOmschrijving = "   ",
                        zoekZaakTypeOmschrijving = null,
                        relationType = RelatieType.GERELATEERD
                    )
                )

                then("the search parameters should not contain any optional search fields") {
                    zoekParametersSlot.captured.getZoeken().containsKey(ZoekVeld.ZAAK_IDENTIFICATIE) shouldBe false
                    zoekParametersSlot.captured.getZoeken().containsKey(ZoekVeld.ZAAK_OMSCHRIJVING) shouldBe false
                    zoekParametersSlot.captured.getFilters().containsKey(FilterVeld.ZAAK_ZAAKTYPE_UUID) shouldBe false
                }
            }

            `when`("startdatum has both van and tot set") {
                val fakeVan = LocalDate.of(2025, 1, 1)
                val fakeTot = LocalDate.of(2025, 6, 30)
                zaakKoppelenRestService.findLinkableZaken(
                    zaakUuid = sourceZaak.uuid,
                    createRestFindLinkableZakenRequest(
                        zoekZaakIdentifier = null,
                        startdatum = RestDatumRange(van = fakeVan, tot = fakeTot),
                        relationType = RelatieType.GERELATEERD
                    )
                )

                then("the search parameters should contain the ZAAK_STARTDATUM datum range") {
                    zoekParametersSlot.captured.datums[DatumVeld.ZAAK_STARTDATUM] shouldNotBe null
                    zoekParametersSlot.captured.datums[DatumVeld.ZAAK_STARTDATUM]!!.van shouldBe fakeVan
                    zoekParametersSlot.captured.datums[DatumVeld.ZAAK_STARTDATUM]!!.tot shouldBe fakeTot
                }
            }

            `when`("startdatum has only van set") {
                val fakeVan = LocalDate.of(2025, 3, 15)
                zaakKoppelenRestService.findLinkableZaken(
                    zaakUuid = sourceZaak.uuid,
                    createRestFindLinkableZakenRequest(
                        zoekZaakIdentifier = null,
                        startdatum = RestDatumRange(van = fakeVan, tot = null),
                        relationType = RelatieType.GERELATEERD
                    )
                )

                then("the search parameters should contain the ZAAK_STARTDATUM datum range with only van") {
                    zoekParametersSlot.captured.datums[DatumVeld.ZAAK_STARTDATUM] shouldNotBe null
                    zoekParametersSlot.captured.datums[DatumVeld.ZAAK_STARTDATUM]!!.van shouldBe fakeVan
                    zoekParametersSlot.captured.datums[DatumVeld.ZAAK_STARTDATUM]!!.tot shouldBe null
                }
            }

            `when`("startdatum has only tot set") {
                val fakeTot = LocalDate.of(2025, 12, 31)
                zaakKoppelenRestService.findLinkableZaken(
                    zaakUuid = sourceZaak.uuid,
                    createRestFindLinkableZakenRequest(
                        zoekZaakIdentifier = null,
                        startdatum = RestDatumRange(van = null, tot = fakeTot),
                        relationType = RelatieType.GERELATEERD
                    )
                )

                then("the search parameters should contain the ZAAK_STARTDATUM datum range with only tot") {
                    zoekParametersSlot.captured.datums[DatumVeld.ZAAK_STARTDATUM] shouldNotBe null
                    zoekParametersSlot.captured.datums[DatumVeld.ZAAK_STARTDATUM]!!.van shouldBe null
                    zoekParametersSlot.captured.datums[DatumVeld.ZAAK_STARTDATUM]!!.tot shouldBe fakeTot
                }
            }

            `when`("startdatum is null") {
                zaakKoppelenRestService.findLinkableZaken(
                    zaakUuid = sourceZaak.uuid,
                    createRestFindLinkableZakenRequest(
                        zoekZaakIdentifier = null,
                        startdatum = null,
                        relationType = RelatieType.GERELATEERD
                    )
                )

                then("the search parameters should not contain the ZAAK_STARTDATUM datum range") {
                    zoekParametersSlot.captured.datums.containsKey(DatumVeld.ZAAK_STARTDATUM) shouldBe false
                }
            }

            `when`("startdatum has both van and tot null") {
                zaakKoppelenRestService.findLinkableZaken(
                    zaakUuid = sourceZaak.uuid,
                    createRestFindLinkableZakenRequest(
                        zoekZaakIdentifier = null,
                        startdatum = RestDatumRange(van = null, tot = null),
                        relationType = RelatieType.GERELATEERD
                    )
                )

                then("the search parameters should not contain the ZAAK_STARTDATUM datum range") {
                    zoekParametersSlot.captured.datums.containsKey(DatumVeld.ZAAK_STARTDATUM) shouldBe false
                }
            }

            `when`("einddatum has both van and tot set") {
                val fakeVan = LocalDate.of(2025, 2, 1)
                val fakeTot = LocalDate.of(2025, 8, 31)
                zaakKoppelenRestService.findLinkableZaken(
                    zaakUuid = sourceZaak.uuid,
                    createRestFindLinkableZakenRequest(
                        zoekZaakIdentifier = null,
                        einddatum = RestDatumRange(van = fakeVan, tot = fakeTot),
                        relationType = RelatieType.GERELATEERD
                    )
                )

                then("the search parameters should contain the ZAAK_EINDDATUM datum range") {
                    zoekParametersSlot.captured.datums[DatumVeld.ZAAK_EINDDATUM] shouldNotBe null
                    zoekParametersSlot.captured.datums[DatumVeld.ZAAK_EINDDATUM]!!.van shouldBe fakeVan
                    zoekParametersSlot.captured.datums[DatumVeld.ZAAK_EINDDATUM]!!.tot shouldBe fakeTot
                }
            }

            `when`("einddatum has only van set") {
                val fakeVan = LocalDate.of(2025, 4, 10)
                zaakKoppelenRestService.findLinkableZaken(
                    zaakUuid = sourceZaak.uuid,
                    createRestFindLinkableZakenRequest(
                        zoekZaakIdentifier = null,
                        einddatum = RestDatumRange(van = fakeVan, tot = null),
                        relationType = RelatieType.GERELATEERD
                    )
                )

                then("the search parameters should contain the ZAAK_EINDDATUM datum range with only van") {
                    zoekParametersSlot.captured.datums[DatumVeld.ZAAK_EINDDATUM] shouldNotBe null
                    zoekParametersSlot.captured.datums[DatumVeld.ZAAK_EINDDATUM]!!.van shouldBe fakeVan
                    zoekParametersSlot.captured.datums[DatumVeld.ZAAK_EINDDATUM]!!.tot shouldBe null
                }
            }

            `when`("einddatum has only tot set") {
                val fakeTot = LocalDate.of(2025, 11, 15)
                zaakKoppelenRestService.findLinkableZaken(
                    zaakUuid = sourceZaak.uuid,
                    createRestFindLinkableZakenRequest(
                        zoekZaakIdentifier = null,
                        einddatum = RestDatumRange(van = null, tot = fakeTot),
                        relationType = RelatieType.GERELATEERD
                    )
                )

                then("the search parameters should contain the ZAAK_EINDDATUM datum range with only tot") {
                    zoekParametersSlot.captured.datums[DatumVeld.ZAAK_EINDDATUM] shouldNotBe null
                    zoekParametersSlot.captured.datums[DatumVeld.ZAAK_EINDDATUM]!!.van shouldBe null
                    zoekParametersSlot.captured.datums[DatumVeld.ZAAK_EINDDATUM]!!.tot shouldBe fakeTot
                }
            }

            `when`("einddatum is null") {
                zaakKoppelenRestService.findLinkableZaken(
                    zaakUuid = sourceZaak.uuid,
                    createRestFindLinkableZakenRequest(
                        zoekZaakIdentifier = null,
                        einddatum = null,
                        relationType = RelatieType.GERELATEERD
                    )
                )

                then("the search parameters should not contain the ZAAK_EINDDATUM datum range") {
                    zoekParametersSlot.captured.datums.containsKey(DatumVeld.ZAAK_EINDDATUM) shouldBe false
                }
            }

            `when`("einddatum has both van and tot null") {
                zaakKoppelenRestService.findLinkableZaken(
                    zaakUuid = sourceZaak.uuid,
                    createRestFindLinkableZakenRequest(
                        zoekZaakIdentifier = null,
                        einddatum = RestDatumRange(van = null, tot = null),
                        relationType = RelatieType.GERELATEERD
                    )
                )

                then("the search parameters should not contain the ZAAK_EINDDATUM datum range") {
                    zoekParametersSlot.captured.datums.containsKey(DatumVeld.ZAAK_EINDDATUM) shouldBe false
                }
            }
        }

        context("listZaaktypesToLink") {
            val fakeCatalogusUri = URI("https://example.com/catalogus/fakeUuid")

            given("a mix of valid, concept, expired and non-permitted zaaktypen") {
                val validZaakType = createZaakType(
                    omschrijving = "fakeValidZaakType",
                    concept = false,
                    beginGeldigheid = LocalDate.now(),
                    eindeGeldigheid = null
                )
                val conceptZaakType = createZaakType(
                    omschrijving = "fakeConceptZaakType",
                    concept = true,
                    beginGeldigheid = LocalDate.now(),
                    eindeGeldigheid = null
                )
                val expiredZaakType = createZaakType(
                    omschrijving = "fakeExpiredZaakType",
                    concept = false,
                    beginGeldigheid = LocalDate.now().minusYears(2),
                    eindeGeldigheid = LocalDate.now().minusDays(1)
                )
                val noPermissionZaakType = createZaakType(
                    omschrijving = "fakeNoPermissionZaakType",
                    concept = false,
                    beginGeldigheid = LocalDate.now(),
                    eindeGeldigheid = null
                )
                val fakeRestZaaktype = createRestZaaktype()

                every { configurationService.readDefaultCatalogusURI() } returns fakeCatalogusUri
                every {
                    ztcClientService.listZaaktypen(fakeCatalogusUri)
                } returns listOf(validZaakType, conceptZaakType, expiredZaakType, noPermissionZaakType)
                every {
                    policyService.readOverigeRechten(validZaakType.omschrijving)
                } returns createOverigeRechten(zoeken = true)
                every {
                    policyService.readOverigeRechten(conceptZaakType.omschrijving)
                } returns createOverigeRechten(zoeken = true)
                every {
                    policyService.readOverigeRechten(expiredZaakType.omschrijving)
                } returns createOverigeRechten(zoeken = true)
                every {
                    policyService.readOverigeRechten(noPermissionZaakType.omschrijving)
                } returns createOverigeRechten(zoeken = false)
                every { restZaaktypeConverter.convert(validZaakType) } returns fakeRestZaaktype

                `when`("listZaaktypesToLink is called") {
                    val result = zaakKoppelenRestService.listZaaktypesToLink()

                    then("only the valid zaaktype with permission should be returned") {
                        result shouldHaveSize 1
                        result.first() shouldBe fakeRestZaaktype
                    }
                }
            }
        }
    }
})
