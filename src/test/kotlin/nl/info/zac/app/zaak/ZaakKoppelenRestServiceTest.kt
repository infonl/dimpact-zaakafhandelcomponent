/*
 * SPDX-FileCopyrightText: 2025 Lifely, 2025 Dimpact
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.zaak

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import net.atos.client.zgw.shared.model.Archiefnominatie
import net.atos.client.zgw.zrc.ZrcClientService
import net.atos.zac.policy.PolicyService
import nl.info.client.zgw.model.createZaak
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.zac.app.zaak.model.RelatieType
import nl.info.zac.search.SearchService
import nl.info.zac.search.model.ZoekResultaat
import nl.info.zac.search.model.createZaakZoekObject
import nl.info.zac.search.model.zoekobject.ZoekObjectType
import java.net.URI
import java.util.UUID

private const val OMSCHRIJVING = "fakeOmschrijving"

private const val ZAAK_TYPE_OMSCHRIJVING = "Melding evenement organiseren behandelen"

private const val STATUS_TYPE_OMSCHRIJVING = "Afgerond"

@Suppress("LargeClass")
class ZaakKoppelenRestServiceTest : BehaviorSpec({
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

    beforeEach {
        checkUnnecessaryStub()
    }

    Given("A valid zaak UUID and relationType is provided") {
        val zaakUuid = UUID.randomUUID()
        val zaakZoekUuid = UUID.randomUUID().toString()
        val relationType = RelatieType.HOOFDZAAK
        val zoekZaakIdentifier = "ZAAK-2000-00002"
        val zaakTypeURI = URI(UUID.randomUUID().toString())
        val zaakZoekObjectTypeUuid1 = UUID.randomUUID().toString()
        val page = 0
        val rows = 10
        val sourceZaak = createZaak(
            uuid = zaakUuid,
            identificatie = "ZAAK-2000-00001",
            archiefnominatie = Archiefnominatie.BLIJVEND_BEWAREN,
            zaakTypeURI = zaakTypeURI
        )

        val zaakZoekObject1 = createZaakZoekObject(
            uuidAsString = zaakZoekUuid,
            type = ZoekObjectType.ZAAK,
            zaaktypeOmschrijving = ZAAK_TYPE_OMSCHRIJVING,
            identificatie = zoekZaakIdentifier,
            omschrijving = OMSCHRIJVING,
            statustypeOmschrijving = STATUS_TYPE_OMSCHRIJVING,
            zaaktypeUuid = zaakZoekObjectTypeUuid1,
            archiefNominatie = Archiefnominatie.BLIJVEND_BEWAREN.toString()
        )

        val zoekResultaat = ZoekResultaat(listOf(zaakZoekObject1), 1)

        When("findLinkableZaken is called") {
            every { zrcClientService.readZaak(zaakUuid) } returns sourceZaak
            every { searchService.zoek(any()) } returns zoekResultaat
            every { policyService.readZaakRechten(sourceZaak).koppelen } returns true
            every { policyService.readZaakRechten(zaakZoekObject1).koppelen } returns true
            every {
                ztcClientService.readZaaktype(sourceZaak.zaaktype).deelzaaktypen
            } returns listOf(URI(zaakZoekObjectTypeUuid1))

            val result = zaakKoppelenRestService.findLinkableZaken(
                zaakUuid = zaakUuid,
                zoekZaakIdentifier = zoekZaakIdentifier,
                relationType = relationType,
                page = page,
                rows = rows
            )

            Then("a single linkable zaak should be returned") {
                result.resultCount shouldBe 1
                with(result.results.first()) {
                    id shouldBe zaakZoekUuid
                    type shouldBe zaakZoekObject1.getType()
                    identificatie shouldBe zoekZaakIdentifier
                    omschrijving shouldBe OMSCHRIJVING
                    zaaktypeOmschrijving shouldBe ZAAK_TYPE_OMSCHRIJVING
                    statustypeOmschrijving shouldBe STATUS_TYPE_OMSCHRIJVING
                    isKoppelbaar shouldBe true
                }
            }

            Then("required services should be invoked") {
                verify { zrcClientService.readZaak(zaakUuid) }
                verify { searchService.zoek(any()) }
                verify { policyService.readZaakRechten(sourceZaak) }
                verify { policyService.readZaakRechten(zaakZoekObject1) }
                verify { ztcClientService.readZaaktype(sourceZaak.zaaktype) }
            }
        }
    }
})
