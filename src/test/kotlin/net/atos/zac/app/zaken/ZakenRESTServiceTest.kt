/*
 * SPDX-FileCopyrightText: 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.zaken

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.core.test.TestCase
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import net.atos.client.or.`object`.ObjectsClientService
import net.atos.client.or.`object`.model.createObjectRegistratieObject
import net.atos.client.zgw.shared.ZGWApiService
import net.atos.client.zgw.zrc.ZRCClientService
import net.atos.client.zgw.zrc.model.BetrokkeneType
import net.atos.client.zgw.zrc.model.RolNatuurlijkPersoon
import net.atos.client.zgw.zrc.model.RolOrganisatorischeEenheid
import net.atos.client.zgw.zrc.model.Zaak
import net.atos.client.zgw.zrc.model.createNatuurlijkPersoon
import net.atos.client.zgw.zrc.model.createRolNatuurlijkPersoon
import net.atos.client.zgw.zrc.model.createZaak
import net.atos.client.zgw.zrc.model.createZaakobjectOpenbareRuimte
import net.atos.client.zgw.zrc.model.createZaakobjectPand
import net.atos.client.zgw.ztc.ZTCClientService
import net.atos.client.zgw.ztc.model.AardVanRol
import net.atos.client.zgw.ztc.model.createRolType
import net.atos.client.zgw.ztc.model.createZaakType
import net.atos.zac.aanvraag.InboxProductaanvraagService
import net.atos.zac.aanvraag.ProductaanvraagService
import net.atos.zac.aanvraag.createProductaanvraagDenhaag
import net.atos.zac.app.bag.converter.RESTBAGConverter
import net.atos.zac.app.zaken.converter.RESTZaakConverter
import net.atos.zac.app.zaken.model.createRESTZaak
import net.atos.zac.app.zaken.model.createRESTZaakAanmaakGegevens
import net.atos.zac.authentication.LoggedInUser
import net.atos.zac.authentication.createLoggedInUser
import net.atos.zac.flowable.CMMNService
import net.atos.zac.flowable.ZaakVariabelenService
import net.atos.zac.identity.IdentityService
import net.atos.zac.identity.model.createGroup
import net.atos.zac.policy.PolicyService
import net.atos.zac.policy.output.OverigeRechten
import net.atos.zac.policy.output.createZaakRechten
import net.atos.zac.zaaksturing.ZaakafhandelParameterService
import net.atos.zac.zaaksturing.model.createZaakafhandelParameters
import org.junit.jupiter.api.Assertions.assertEquals
import jakarta.enterprise.inject.Instance

class ZakenRESTServiceTest : BehaviorSpec() {
    val cmmnService = mockk<CMMNService>()
    val identityService = mockk<IdentityService>()
    val inboxProductaanvraagService = mockk<InboxProductaanvraagService>()
    val loggedInUserInstance = mockk<Instance<LoggedInUser>>()
    val objectsClientService = mockk<ObjectsClientService>()
    val policyService = mockk<PolicyService>()
    val productaanvraagService = mockk<ProductaanvraagService>()
    val restBagConverter = mockk<RESTBAGConverter>()
    val restZaakConverter = mockk<RESTZaakConverter>()
    val zaakafhandelParameterService = mockk<ZaakafhandelParameterService>()
    val zaakVariabelenService = mockk<ZaakVariabelenService>()
    val zgwApiService = mockk<ZGWApiService>()
    val zrcClientService = mockk<ZRCClientService>()
    val ztcClientService = mockk<ZTCClientService>()

    // We have to use @InjectMockKs since the class under test uses field injection instead of constructor injection.
    // This is because WildFly does not support constructor injection for JAX-RS REST services completely.
    @InjectMockKs
    lateinit var zakenRESTService: ZakenRESTService

    override suspend fun beforeTest(testCase: TestCase) {
        MockKAnnotations.init(this)
    }

    init {
        given("zaak input data is provided") {
            When("createZaak is called") {
                then("a zaak is created using the ZGW API and a zaak is started in the ZAC CMMN service") {
                    val group = createGroup()
                    val formulierData = mapOf(Pair("dummyKey", "dummyValue"))
                    val natuurlijkPersoon = createNatuurlijkPersoon()
                    val objectRegistratieObject = createObjectRegistratieObject()
                    val productaanvraagDenhaag = createProductaanvraagDenhaag()
                    val restZaak = createRESTZaak()
                    val restZaakAanmaakGegevens = createRESTZaakAanmaakGegevens()
                    val restZaakType = restZaakAanmaakGegevens.zaak.zaaktype
                    val rolNatuurlijkPersoon = createRolNatuurlijkPersoon(natuurlijkPersoon = natuurlijkPersoon)
                    val user = createLoggedInUser()
                    val zaakAfhandelParameters = createZaakafhandelParameters()
                    val zaakObjectPand = createZaakobjectPand()
                    val zaakObjectOpenbareRuimte = createZaakobjectOpenbareRuimte()
                    val zaakType = createZaakType()
                    val zaak = createZaak(zaakType.url)

                    every { cmmnService.startCase(zaak, zaakType, zaakAfhandelParameters, null) } just runs
                    every { identityService.readGroup(restZaakAanmaakGegevens.zaak.groep.id) } returns group
                    every { identityService.readUser(restZaakAanmaakGegevens.zaak.behandelaar.id) } returns user
                    every {
                        inboxProductaanvraagService.delete(restZaakAanmaakGegevens.inboxProductaanvraag.id)
                    } just runs
                    every { loggedInUserInstance.get() } returns createLoggedInUser()
                    every {
                        objectsClientService
                            .readObject(restZaakAanmaakGegevens.inboxProductaanvraag.productaanvraagObjectUUID)
                    } returns objectRegistratieObject
                    every { policyService.readOverigeRechten() } returns OverigeRechten(true, false, false)
                    every { policyService.readZaakRechten(zaak) } returns createZaakRechten()
                    every { productaanvraagService.getFormulierData(objectRegistratieObject) } returns formulierData
                    every {
                        productaanvraagService.getProductaanvraag(objectRegistratieObject)
                    } returns productaanvraagDenhaag
                    every { productaanvraagService.pairAanvraagPDFWithZaak(productaanvraagDenhaag, zaak.url) } just runs
                    every {
                        productaanvraagService.pairBijlagenWithZaak(productaanvraagDenhaag.attachments, zaak.url)
                    } just runs
                    every {
                        productaanvraagService.pairProductaanvraagWithZaak(
                            objectRegistratieObject,
                            zaak.url
                        )
                    } just runs
                    every {
                        restBagConverter.convertToZaakobject(restZaakAanmaakGegevens.bagObjecten[0], zaak)
                    } returns zaakObjectPand
                    every {
                        restBagConverter.convertToZaakobject(
                            restZaakAanmaakGegevens.bagObjecten[1],
                            zaak
                        )
                    } returns zaakObjectOpenbareRuimte
                    every { restZaakConverter.convert(zaak) } returns restZaak
                    every { restZaakConverter.convert(restZaakAanmaakGegevens.zaak, zaakType) } returns zaak
                    every {
                        zaakafhandelParameterService.readZaakafhandelParameters(zaakType.uuid)
                    } returns zaakAfhandelParameters
                    every { zaakVariabelenService.setZaakdata(zaak.uuid, formulierData) } just runs
                    every { zgwApiService.createZaak(zaak) } returns zaak
                    every { zrcClientService.createRol(any(), any()) } returns rolNatuurlijkPersoon
                    every { zrcClientService.updateRol(zaak, any(), any()) } just runs
                    every { zrcClientService.createZaak(zaak) } returns zaak
                    every { zrcClientService.createZaakobject(zaakObjectPand) } returns zaakObjectPand
                    every { zrcClientService.createZaakobject(zaakObjectOpenbareRuimte) } returns zaakObjectOpenbareRuimte
                    every { ztcClientService.readZaaktype(restZaakType.uuid) } returns zaakType
                    every {
                        ztcClientService.readRoltype(AardVanRol.INITIATOR, zaak.zaaktype)
                    } returns createRolType(rol = AardVanRol.INITIATOR)
                    every {
                        ztcClientService.readRoltype(AardVanRol.BEHANDELAAR, zaak.zaaktype)
                    } returns createRolType(rol = AardVanRol.BEHANDELAAR)

                    val restZaakReturned = zakenRESTService.createZaak(restZaakAanmaakGegevens)

                    val zaakCreatedSlot = slot<Zaak>()
                    val rolNatuurlijkPersoonSlot = slot<RolNatuurlijkPersoon>()
                    val rolGroupSlotOrganisatorischeEenheidSlot = slot<RolOrganisatorischeEenheid>()
                    verify(exactly = 1) {
                        ztcClientService.readZaaktype(restZaakAanmaakGegevens.zaak.zaaktype.uuid)
                        zgwApiService.createZaak(capture(zaakCreatedSlot))
                        zrcClientService.createRol(
                            capture(rolNatuurlijkPersoonSlot),
                            "Toegekend door de medewerker tijdens het behandelen van de zaak"
                        )
                        zrcClientService.updateRol(
                            zaak,
                            capture(rolGroupSlotOrganisatorischeEenheidSlot),
                            "Aanmaken zaak"
                        )
                        cmmnService.startCase(zaak, zaakType, zaakAfhandelParameters, null)
                        zrcClientService.createZaakobject(zaakObjectPand)
                        zrcClientService.createZaakobject(zaakObjectOpenbareRuimte)
                    }
                    with(restZaakReturned) {
                        assert(uuid != null)
                    }
                    with(zaakCreatedSlot.captured) {
                        assertEquals(this, zaak)
                    }
                    with(rolNatuurlijkPersoonSlot.captured) {
                        assertEquals(this.zaak, zaak.url)
                        assertEquals(this.betrokkeneType, BetrokkeneType.NATUURLIJK_PERSOON)
                    }
                    with(rolGroupSlotOrganisatorischeEenheidSlot.captured) {
                        assertEquals(this.zaak, zaak.url)
                        assertEquals(this.betrokkeneType, BetrokkeneType.ORGANISATORISCHE_EENHEID)
                    }
                }
            }
        }
    }
}
