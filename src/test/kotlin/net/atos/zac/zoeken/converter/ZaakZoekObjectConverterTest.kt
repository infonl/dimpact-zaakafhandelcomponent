package net.atos.zac.zoeken.converter

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.maps.shouldContain
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import net.atos.client.vrl.VrlClientService
import net.atos.client.zgw.shared.ZGWApiService
import net.atos.client.zgw.shared.model.createResultsOfZaakObjecten
import net.atos.client.zgw.zrc.ZRCClientService
import net.atos.client.zgw.zrc.model.createNatuurlijkPersoon
import net.atos.client.zgw.zrc.model.createRolMedewerker
import net.atos.client.zgw.zrc.model.createRolNatuurlijkPersoon
import net.atos.client.zgw.zrc.model.createZaak
import net.atos.client.zgw.zrc.model.createZaakStatus
import net.atos.client.zgw.zrc.model.zaakobjecten.Zaakobject
import net.atos.client.zgw.ztc.ZTCClientService
import net.atos.client.zgw.ztc.model.createRolType
import net.atos.client.zgw.ztc.model.createStatusType
import net.atos.client.zgw.ztc.model.createZaakType
import net.atos.zac.configuratie.ConfiguratieService
import net.atos.zac.flowable.FlowableTaskService
import net.atos.zac.identity.IdentityService
import net.atos.zac.identity.model.createUser
import net.atos.zac.zoeken.model.ZaakIndicatie
import net.atos.zac.zoeken.model.index.ZoekObjectType
import java.net.URI
import java.time.ZoneId
import java.util.Date
import java.util.Optional

@MockKExtension.CheckUnnecessaryStub
class ZaakZoekObjectConverterTest : BehaviorSpec({
    val zrcClientService = mockk<ZRCClientService>()
    val ztcClientService = mockk<ZTCClientService>()
    val vrlClientService = mockk<VrlClientService>()
    val zgwApiService = mockk<ZGWApiService>()
    val identityService = mockk<IdentityService>()
    val flowableTaskService = mockk<FlowableTaskService>()

    val zaakZoekenObjectConverter = ZaakZoekObjectConverter(
        zrcClientService,
        ztcClientService,
        vrlClientService,
        zgwApiService,
        identityService,
        flowableTaskService
    )

    Given("a zaak with betrokkenen, without open tasks, zaak objecten and communication channels") {
        val zaakType = createZaakType()
        val zaak = createZaak(
            zaakTypeURI = zaakType.url
        )
        val rolInitiator = createRolNatuurlijkPersoon(
            rolType = createRolType(omschrijving = "dummy_role_initiator")
        )
        val rolAdviseur = createRolNatuurlijkPersoon(
            rolType = createRolType(omschrijving = "dummy_role_adviseur"),
            natuurlijkPersoon = createNatuurlijkPersoon(bsn = "dummyBsnAdviseur")
        )
        val rolBelanghebbende = createRolNatuurlijkPersoon(
            rolType = createRolType(omschrijving = "dummy_role_belanghebbende"),
            natuurlijkPersoon = createNatuurlijkPersoon(bsn = "dummyBsnBelanghebbende")

        )
        val rollenZaak = listOf(rolAdviseur, rolBelanghebbende)
        val rolMedewerkerBehandelaar = createRolMedewerker()
        val userBehandelaar = createUser()
        val zaakObjectenList = emptyList<Zaakobject>()
        val zaakStatus = createZaakStatus()
        val zaakStatusType = createStatusType()

        every { zrcClientService.readZaak(zaak.uuid) } returns zaak
        every { zgwApiService.findInitiatorForZaak(zaak) } returns Optional.of(rolInitiator)
        every { zrcClientService.listRollen(zaak) } returns rollenZaak
        every { zgwApiService.findGroepForZaak(zaak) } returns Optional.empty()
        every { zgwApiService.findBehandelaarForZaak(zaak) } returns Optional.of(rolMedewerkerBehandelaar)
        every {
            identityService.readUser(rolMedewerkerBehandelaar.betrokkeneIdentificatie.identificatie)
        } returns userBehandelaar
        every { ztcClientService.readZaaktype(zaak.zaaktype) } returns zaakType
        every { zrcClientService.readStatus(zaak.status) } returns zaakStatus
        every { ztcClientService.readStatustype(zaakStatus.statustype) } returns zaakStatusType
        every { flowableTaskService.countOpenTasksForZaak(zaak.uuid) } returns 0
        every { zrcClientService.listZaakobjecten(any()) } returns createResultsOfZaakObjecten(
            list = zaakObjectenList,
            count = zaakObjectenList.size
        )

        When("the zaak is converted to a zaak zoek object") {
            val zaakZoekObject = zaakZoekenObjectConverter.convert(zaak.uuid.toString())

            Then("the zaak zoek object should contain expected data that is converted from the zaak") {
                with(zaakZoekObject) {
                    uuid shouldBe zaak.uuid.toString()
                    type shouldBe ZoekObjectType.ZAAK
                    identificatie shouldBe zaak.identificatie
                    omschrijving shouldBe zaak.omschrijving
                    toelichting shouldBe zaak.toelichting
                    registratiedatum shouldBe Date.from(zaak.registratiedatum.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant())
                    vertrouwelijkheidaanduiding shouldBe zaak.vertrouwelijkheidaanduiding.toString()
                    isAfgehandeld shouldBe !zaak.isOpen
                    initiatorIdentificatie shouldBe rolInitiator.identificatienummer
                    // locatie conversion is not implemented (yet?)
                    locatie shouldBe null

                    betrokkenen.size shouldBe rollenZaak.size
                    betrokkenen shouldContain Pair(
                        "zaak_betrokkene_${rolAdviseur.omschrijving}",
                        listOf(rolAdviseur.identificatienummer)
                    )
                    betrokkenen shouldContain Pair(
                        "zaak_betrokkene_${rolBelanghebbende.omschrijving}",
                        listOf(rolBelanghebbende.identificatienummer)
                    )
                    zaakIndicaties shouldNotContain ZaakIndicatie.BESLOTEN
                    zaakIndicaties shouldNotContain ZaakIndicatie.HEROPEND
                }
            }
        }
    }

    Given("a reopened zaak with status and decisions") {
        val zaakType = createZaakType(
            besluittypen = setOf(URI("decision1"), URI("decision2"))
        )
        val zaak = createZaak(
            zaakTypeURI = zaakType.url,
            status = URI("status")
        )
        val rolInitiator = createRolNatuurlijkPersoon(
            rolType = createRolType(omschrijving = "dummy_role_initiator")
        )
        val rolAdviseur = createRolNatuurlijkPersoon(
            rolType = createRolType(omschrijving = "dummy_role_adviseur"),
            natuurlijkPersoon = createNatuurlijkPersoon(bsn = "dummyBsnAdviseur")
        )
        val rolBelanghebbende = createRolNatuurlijkPersoon(
            rolType = createRolType(omschrijving = "dummy_role_belanghebbende"),
            natuurlijkPersoon = createNatuurlijkPersoon(bsn = "dummyBsnBelanghebbende")

        )
        val rollenZaak = listOf(rolAdviseur, rolBelanghebbende)
        val rolMedewerkerBehandelaar = createRolMedewerker()
        val userBehandelaar = createUser()
        val zaakObjectenList = emptyList<Zaakobject>()
        val zaakStatus = createZaakStatus()
        val zaakStatusType = createStatusType().apply {
            omschrijving = ConfiguratieService.STATUSTYPE_OMSCHRIJVING_HEROPEND
        }

        every { zrcClientService.readZaak(zaak.uuid) } returns zaak
        every { zgwApiService.findInitiatorForZaak(zaak) } returns Optional.of(rolInitiator)
        every { zrcClientService.listRollen(zaak) } returns rollenZaak
        every { zgwApiService.findGroepForZaak(zaak) } returns Optional.empty()
        every { zgwApiService.findBehandelaarForZaak(zaak) } returns Optional.of(rolMedewerkerBehandelaar)
        every {
            identityService.readUser(rolMedewerkerBehandelaar.betrokkeneIdentificatie.identificatie)
        } returns userBehandelaar
        every { ztcClientService.readZaaktype(zaak.zaaktype) } returns zaakType
        every { zrcClientService.readStatus(zaak.status) } returns zaakStatus
        every { ztcClientService.readStatustype(zaakStatus.statustype) } returns zaakStatusType
        every { flowableTaskService.countOpenTasksForZaak(zaak.uuid) } returns 0
        every { zrcClientService.listZaakobjecten(any()) } returns createResultsOfZaakObjecten(
            list = zaakObjectenList,
            count = zaakObjectenList.size
        )

        When("the zaak is converted to a zaak zoek object") {
            val zaakZoekObject = zaakZoekenObjectConverter.convert(zaak.uuid.toString())

            Then("the zaak zoek object should contain expected data that is converted from the zaak") {
                with(zaakZoekObject) {
                    uuid shouldBe zaak.uuid.toString()
                    type shouldBe ZoekObjectType.ZAAK
                    identificatie shouldBe zaak.identificatie
                    omschrijving shouldBe zaak.omschrijving
                    toelichting shouldBe zaak.toelichting
                    registratiedatum shouldBe Date.from(zaak.registratiedatum.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant())
                    vertrouwelijkheidaanduiding shouldBe zaak.vertrouwelijkheidaanduiding.toString()
                    isAfgehandeld shouldBe !zaak.isOpen
                    initiatorIdentificatie shouldBe rolInitiator.identificatienummer
                    // locatie conversion is not implemented (yet?)
                    locatie shouldBe null

                    betrokkenen.size shouldBe rollenZaak.size
                    betrokkenen shouldContain Pair(
                        "zaak_betrokkene_${rolAdviseur.omschrijving}",
                        listOf(rolAdviseur.identificatienummer)
                    )
                    betrokkenen shouldContain Pair(
                        "zaak_betrokkene_${rolBelanghebbende.omschrijving}",
                        listOf(rolBelanghebbende.identificatienummer)
                    )
                    zaakIndicaties shouldContain ZaakIndicatie.BESLOTEN
                    zaakIndicaties shouldContain ZaakIndicatie.HEROPEND
                }
            }
        }
    }
})
