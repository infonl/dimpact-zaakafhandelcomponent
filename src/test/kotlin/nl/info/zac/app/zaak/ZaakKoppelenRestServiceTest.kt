/*
 * SPDX-FileCopyrightText: 2025 INFO.nl, 2025 Dimpact
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.zaak

import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import net.atos.client.zgw.shared.model.Archiefnominatie
import net.atos.client.zgw.zrc.ZrcClientService
import net.atos.client.zgw.zrc.model.Zaak
import net.atos.zac.policy.PolicyService
import nl.info.client.zgw.model.createZaak
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.zac.app.zaak.model.RelatieType
import nl.info.zac.search.SearchService
import nl.info.zac.search.model.ZaakIndicatie.DEELZAAK
import nl.info.zac.search.model.ZaakIndicatie.HOOFDZAAK
import nl.info.zac.search.model.ZoekResultaat
import nl.info.zac.search.model.createZaakZoekObject
import nl.info.zac.search.model.zoekobject.ZaakZoekObject
import nl.info.zac.search.model.zoekobject.ZoekObjectType.ZAAK
import java.net.URI
import java.util.UUID

private const val OMSCHRIJVING = "fakeOmschrijving"

private const val ZAAK_TYPE_OMSCHRIJVING = "Melding evenement organiseren behandelen"

private const val STATUS_TYPE_OMSCHRIJVING = "Afgerond"

@Suppress("LargeClass")
class ZaakKoppelenRestServiceTest : BehaviorSpec() {
    val zoekZaakIdentifier = "ZAAK-2000-00002"
    val zaakTypeURI = URI(UUID.randomUUID().toString())
    val zaakZoekObjectTypeUuid = UUID.randomUUID().toString()
    val page = 0
    val rows = 10

    val policyService = mockk<PolicyService>()
    val searchService = mockk<SearchService>()
    val zrcClientService = mockk<ZrcClientService>()
    val ztcClientService = mockk<ZtcClientService>()
    val zaakKoppelenRestService = ZaakKoppelenRestService(
        policyService = policyService,
        searchService = searchService,
        zrcClientService = zrcClientService,
        ztcClientService = ztcClientService
    )

    override fun isolationMode() = IsolationMode.InstancePerTest

    fun checkIfRequiredServicesAreInvoked(sourceZaak: Zaak, targetZaak: ZaakZoekObject) {
        verify(exactly = 1) {
            zrcClientService.readZaak(sourceZaak.uuid)
            searchService.zoek(any())
            policyService.readZaakRechten(sourceZaak)
            policyService.readZaakRechten(targetZaak)
        }
    }

    init {

        beforeEach {
            checkUnnecessaryStub()
        }

        Given("A source zaak which is not linked and a target not linked zaak") {
            val sourceZaak = createZaak(
                identificatie = "ZAAK-2000-00001",
                archiefnominatie = Archiefnominatie.BLIJVEND_BEWAREN,
                zaakTypeURI = zaakTypeURI
            )

            val zaakZoekObject = createZaakZoekObject(
                type = ZAAK,
                zaaktypeOmschrijving = ZAAK_TYPE_OMSCHRIJVING,
                identificatie = zoekZaakIdentifier,
                omschrijving = OMSCHRIJVING,
                statustypeOmschrijving = STATUS_TYPE_OMSCHRIJVING,
                zaaktypeUuid = zaakZoekObjectTypeUuid,
                archiefNominatie = Archiefnominatie.BLIJVEND_BEWAREN.toString()
            )

            val zoekResultaat = ZoekResultaat(listOf(zaakZoekObject), 1)

            every { zrcClientService.readZaak(sourceZaak.uuid) } returns sourceZaak
            every { searchService.zoek(any()) } returns zoekResultaat
            every { policyService.readZaakRechten(sourceZaak).koppelen } returns true
            every { policyService.readZaakRechten(zaakZoekObject).koppelen } returns true

            When("findLinkableZaken with HOOFDZAAK is called") {
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
                    checkIfRequiredServicesAreInvoked(sourceZaak = sourceZaak, targetZaak = zaakZoekObject)
                    verify(exactly = 1) {
                        ztcClientService.readZaaktype(UUID.fromString(zaakZoekObjectTypeUuid))
                    }
                }
            }

            When("findLinkableZaken with DEELZAAK is called") {
                every {
                    ztcClientService.readZaaktype(sourceZaak.zaaktype).deelzaaktypen
                } returns listOf(URI(zaakZoekObjectTypeUuid))

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
                    checkIfRequiredServicesAreInvoked(sourceZaak = sourceZaak, targetZaak = zaakZoekObject)
                    verify(exactly = 1) {
                        ztcClientService.readZaaktype(sourceZaak.zaaktype)
                    }
                }
            }
        }

        Given("A source zaak which is not linked and a target hoofdzaak") {
            val sourceZaak = createZaak(
                identificatie = "ZAAK-2000-00001",
                archiefnominatie = Archiefnominatie.BLIJVEND_BEWAREN,
                zaakTypeURI = zaakTypeURI,
            )

            val zaakZoekObject = createZaakZoekObject(
                type = ZAAK,
                zaaktypeOmschrijving = ZAAK_TYPE_OMSCHRIJVING,
                identificatie = zoekZaakIdentifier,
                omschrijving = OMSCHRIJVING,
                statustypeOmschrijving = STATUS_TYPE_OMSCHRIJVING,
                zaaktypeUuid = zaakZoekObjectTypeUuid,
                archiefNominatie = Archiefnominatie.BLIJVEND_BEWAREN.toString(),
                indicatie = HOOFDZAAK
            )

            val zoekResultaat = ZoekResultaat(listOf(zaakZoekObject), 1)

            every { zrcClientService.readZaak(sourceZaak.uuid) } returns sourceZaak
            every { searchService.zoek(any()) } returns zoekResultaat
            every { policyService.readZaakRechten(sourceZaak).koppelen } returns true
            every { policyService.readZaakRechten(zaakZoekObject).koppelen } returns true

            When("findLinkableZaken with HOOFDZAAK link is called") {
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
                    checkIfRequiredServicesAreInvoked(sourceZaak = sourceZaak, targetZaak = zaakZoekObject)
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
                    checkIfRequiredServicesAreInvoked(sourceZaak = sourceZaak, targetZaak = zaakZoekObject)
                }
            }
        }

        Given("A source zaak which is not linked and a target deelzaak") {
            val sourceZaak = createZaak(
                identificatie = "ZAAK-2000-00001",
                archiefnominatie = Archiefnominatie.BLIJVEND_BEWAREN,
                zaakTypeURI = zaakTypeURI,
            )

            val zaakZoekObject = createZaakZoekObject(
                type = ZAAK,
                zaaktypeOmschrijving = ZAAK_TYPE_OMSCHRIJVING,
                identificatie = zoekZaakIdentifier,
                omschrijving = OMSCHRIJVING,
                statustypeOmschrijving = STATUS_TYPE_OMSCHRIJVING,
                zaaktypeUuid = zaakZoekObjectTypeUuid,
                archiefNominatie = Archiefnominatie.BLIJVEND_BEWAREN.toString(),
                indicatie = DEELZAAK
            )

            val zoekResultaat = ZoekResultaat(listOf(zaakZoekObject), 1)

            every { zrcClientService.readZaak(sourceZaak.uuid) } returns sourceZaak
            every { searchService.zoek(any()) } returns zoekResultaat
            every { policyService.readZaakRechten(sourceZaak).koppelen } returns true
            every { policyService.readZaakRechten(zaakZoekObject).koppelen } returns true

            When("findLinkableZaken with HOOFDZAAK link is called") {
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
                    checkIfRequiredServicesAreInvoked(sourceZaak = sourceZaak, targetZaak = zaakZoekObject,)
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
                    checkIfRequiredServicesAreInvoked(sourceZaak = sourceZaak, targetZaak = zaakZoekObject)
                }
            }
        }

        Given("A source hoofdzaak and target hoofdzaak") {
            val deelzakenTypeUuid = UUID.randomUUID()

            val hoofdzaak = createZaak(
                identificatie = "ZAAK-2000-00001",
                archiefnominatie = Archiefnominatie.BLIJVEND_BEWAREN,
                zaakTypeURI = zaakTypeURI,
                deelzaken = setOf(URI("https://example.com/deelzaak/$deelzakenTypeUuid"))
            )

            val zaakZoekObject = createZaakZoekObject(
                type = ZAAK,
                zaaktypeOmschrijving = ZAAK_TYPE_OMSCHRIJVING,
                identificatie = zoekZaakIdentifier,
                omschrijving = OMSCHRIJVING,
                statustypeOmschrijving = STATUS_TYPE_OMSCHRIJVING,
                zaaktypeUuid = zaakZoekObjectTypeUuid,
                archiefNominatie = Archiefnominatie.BLIJVEND_BEWAREN.toString(),
                indicatie = HOOFDZAAK
            )

            val zoekResultaat = ZoekResultaat(listOf(zaakZoekObject), 1)

            every { zrcClientService.readZaak(hoofdzaak.uuid) } returns hoofdzaak
            every { searchService.zoek(any()) } returns zoekResultaat
            every { policyService.readZaakRechten(hoofdzaak).koppelen } returns true
            every { policyService.readZaakRechten(zaakZoekObject).koppelen } returns true

            When("findLinkableZaken with HOOFDZAAK link is called") {
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
                    checkIfRequiredServicesAreInvoked(sourceZaak = hoofdzaak, targetZaak = zaakZoekObject)
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
                    checkIfRequiredServicesAreInvoked(sourceZaak = hoofdzaak, targetZaak = zaakZoekObject)
                }
            }
        }

        Given("A source hoofdzaak and target deelzaak") {
            val deelzakenTypeUuid = UUID.randomUUID()

            val hoofdzaak = createZaak(
                identificatie = "ZAAK-2000-00001",
                archiefnominatie = Archiefnominatie.BLIJVEND_BEWAREN,
                zaakTypeURI = zaakTypeURI,
                deelzaken = setOf(URI("https://example.com/deelzaak/$deelzakenTypeUuid"))
            )

            val zaakZoekObject = createZaakZoekObject(
                type = ZAAK,
                zaaktypeOmschrijving = ZAAK_TYPE_OMSCHRIJVING,
                identificatie = zoekZaakIdentifier,
                omschrijving = OMSCHRIJVING,
                statustypeOmschrijving = STATUS_TYPE_OMSCHRIJVING,
                zaaktypeUuid = zaakZoekObjectTypeUuid,
                archiefNominatie = Archiefnominatie.BLIJVEND_BEWAREN.toString(),
                indicatie = DEELZAAK
            )

            val zoekResultaat = ZoekResultaat(listOf(zaakZoekObject), 1)

            every { zrcClientService.readZaak(hoofdzaak.uuid) } returns hoofdzaak
            every { searchService.zoek(any()) } returns zoekResultaat
            every { policyService.readZaakRechten(hoofdzaak).koppelen } returns true
            every { policyService.readZaakRechten(zaakZoekObject).koppelen } returns true

            When("findLinkableZaken with HOOFDZAAK link is called") {
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
                    checkIfRequiredServicesAreInvoked(sourceZaak = hoofdzaak, targetZaak = zaakZoekObject)
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
                    checkIfRequiredServicesAreInvoked(sourceZaak = hoofdzaak, targetZaak = zaakZoekObject)
                }
            }
        }

        Given("A source hoofdzaak and target not linked zaak") {
            val deelzakenTypeUuid = UUID.randomUUID()

            val hoofdzaak = createZaak(
                identificatie = "ZAAK-2000-00001",
                archiefnominatie = Archiefnominatie.BLIJVEND_BEWAREN,
                zaakTypeURI = zaakTypeURI,
                deelzaken = setOf(URI("https://example.com/deelzaak/$deelzakenTypeUuid"))
            )

            val zaakZoekObject = createZaakZoekObject(
                type = ZAAK,
                zaaktypeOmschrijving = ZAAK_TYPE_OMSCHRIJVING,
                identificatie = zoekZaakIdentifier,
                omschrijving = OMSCHRIJVING,
                statustypeOmschrijving = STATUS_TYPE_OMSCHRIJVING,
                zaaktypeUuid = zaakZoekObjectTypeUuid,
                archiefNominatie = Archiefnominatie.BLIJVEND_BEWAREN.toString()
            )

            val zoekResultaat = ZoekResultaat(listOf(zaakZoekObject), 1)

            every { zrcClientService.readZaak(hoofdzaak.uuid) } returns hoofdzaak
            every { searchService.zoek(any()) } returns zoekResultaat
            every { policyService.readZaakRechten(hoofdzaak).koppelen } returns true
            every { policyService.readZaakRechten(zaakZoekObject).koppelen } returns true

            When("findLinkableZaken with HOOFDZAAK link is called") {
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
                    checkIfRequiredServicesAreInvoked(sourceZaak = hoofdzaak, targetZaak = zaakZoekObject)
                }
            }

            When("findLinkableZaken with DEELZAAK link is called") {
                every {
                    ztcClientService.readZaaktype(hoofdzaak.zaaktype).deelzaaktypen
                } returns listOf(URI(zaakZoekObjectTypeUuid))

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
                    checkIfRequiredServicesAreInvoked(sourceZaak = hoofdzaak, targetZaak = zaakZoekObject)
                    verify(exactly = 1) {
                        ztcClientService.readZaaktype(hoofdzaak.zaaktype)
                    }
                }
            }
        }

        Given("A source deelzaak and target hoofdzaak") {
            val deelzaakUuid = UUID.randomUUID()

            val deelzaak = createZaak(
                identificatie = "ZAAK-2000-00001",
                archiefnominatie = Archiefnominatie.BLIJVEND_BEWAREN,
                zaakTypeURI = zaakTypeURI,
                hoofdzaakUri = URI("https://example.com/deelzaak/$deelzaakUuid")
            )

            val zaakZoekObject = createZaakZoekObject(
                type = ZAAK,
                zaaktypeOmschrijving = ZAAK_TYPE_OMSCHRIJVING,
                identificatie = zoekZaakIdentifier,
                omschrijving = OMSCHRIJVING,
                statustypeOmschrijving = STATUS_TYPE_OMSCHRIJVING,
                zaaktypeUuid = zaakZoekObjectTypeUuid,
                archiefNominatie = Archiefnominatie.BLIJVEND_BEWAREN.toString(),
                indicatie = HOOFDZAAK
            )

            val zoekResultaat = ZoekResultaat(listOf(zaakZoekObject), 1)

            every { zrcClientService.readZaak(deelzaak.uuid) } returns deelzaak
            every { searchService.zoek(any()) } returns zoekResultaat
            every { policyService.readZaakRechten(deelzaak).koppelen } returns true
            every { policyService.readZaakRechten(zaakZoekObject).koppelen } returns true

            When("findLinkableZaken with HOOFDZAAK link is called") {
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
                    checkIfRequiredServicesAreInvoked(sourceZaak = deelzaak, targetZaak = zaakZoekObject)
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
                    checkIfRequiredServicesAreInvoked(sourceZaak = deelzaak, targetZaak = zaakZoekObject)
                }
            }
        }

        Given("A source deelzaak and target deelzaak") {
            val deelzaakUuid = UUID.randomUUID()

            val deelzaak = createZaak(
                identificatie = "ZAAK-2000-00001",
                archiefnominatie = Archiefnominatie.BLIJVEND_BEWAREN,
                zaakTypeURI = zaakTypeURI,
                hoofdzaakUri = URI("https://example.com/deelzaak/$deelzaakUuid")
            )

            val zaakZoekObject = createZaakZoekObject(
                type = ZAAK,
                zaaktypeOmschrijving = ZAAK_TYPE_OMSCHRIJVING,
                identificatie = zoekZaakIdentifier,
                omschrijving = OMSCHRIJVING,
                statustypeOmschrijving = STATUS_TYPE_OMSCHRIJVING,
                zaaktypeUuid = zaakZoekObjectTypeUuid,
                archiefNominatie = Archiefnominatie.BLIJVEND_BEWAREN.toString(),
                indicatie = DEELZAAK
            )

            val zoekResultaat = ZoekResultaat(listOf(zaakZoekObject), 1)

            every { zrcClientService.readZaak(deelzaak.uuid) } returns deelzaak
            every { searchService.zoek(any()) } returns zoekResultaat
            every { policyService.readZaakRechten(deelzaak).koppelen } returns true
            every { policyService.readZaakRechten(zaakZoekObject).koppelen } returns true

            When("findLinkableZaken with HOOFDZAAK link is called") {
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
                    checkIfRequiredServicesAreInvoked(sourceZaak = deelzaak, targetZaak = zaakZoekObject)
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
                    checkIfRequiredServicesAreInvoked(sourceZaak = deelzaak, targetZaak = zaakZoekObject)
                }
            }
        }

        Given("A source deelzaak and target not linked zaak") {
            val deelzaakUuid = UUID.randomUUID()

            val deelzaak = createZaak(
                identificatie = "ZAAK-2000-00001",
                archiefnominatie = Archiefnominatie.BLIJVEND_BEWAREN,
                zaakTypeURI = zaakTypeURI,
                hoofdzaakUri = URI("https://example.com/deelzaak/$deelzaakUuid")
            )

            val zaakZoekObject = createZaakZoekObject(
                type = ZAAK,
                zaaktypeOmschrijving = ZAAK_TYPE_OMSCHRIJVING,
                identificatie = zoekZaakIdentifier,
                omschrijving = OMSCHRIJVING,
                statustypeOmschrijving = STATUS_TYPE_OMSCHRIJVING,
                zaaktypeUuid = zaakZoekObjectTypeUuid,
                archiefNominatie = Archiefnominatie.BLIJVEND_BEWAREN.toString()
            )

            val zoekResultaat = ZoekResultaat(listOf(zaakZoekObject), 1)

            every { zrcClientService.readZaak(deelzaak.uuid) } returns deelzaak
            every { searchService.zoek(any()) } returns zoekResultaat
            every { policyService.readZaakRechten(deelzaak).koppelen } returns true
            every { policyService.readZaakRechten(zaakZoekObject).koppelen } returns true

            When("findLinkableZaken with HOOFDZAAK link is called") {
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
                    checkIfRequiredServicesAreInvoked(sourceZaak = deelzaak, targetZaak = zaakZoekObject)
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
                    checkIfRequiredServicesAreInvoked(sourceZaak = deelzaak, targetZaak = zaakZoekObject)
                }
            }
        }
    }
}
