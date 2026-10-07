/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.bag

import jakarta.enterprise.inject.Instance
import jakarta.inject.Inject
import jakarta.inject.Singleton
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import net.atos.zac.app.shared.RESTResultaat
import nl.info.client.bag.BagClientService
import nl.info.client.bag.model.BevraagAdressenParameters
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.client.zgw.zrc.model.generated.ObjectTypeEnum
import nl.info.client.zgw.zrc.model.generated.Zaak
import nl.info.client.zgw.zrc.model.zaakobjecten.ZaakobjectListParameters
import nl.info.zac.app.bag.model.BagObjectType
import nl.info.zac.app.bag.model.RestBagAdres
import nl.info.zac.app.bag.model.RestBagObject
import nl.info.zac.app.bag.model.RestBagObjectGegevens
import nl.info.zac.app.bag.model.RestListAdressenParameters
import nl.info.zac.app.bag.model.toRestBagAdres
import nl.info.zac.app.bag.model.toRestBagObjectGegevens
import nl.info.zac.app.bag.model.toRestNummeraanduiding
import nl.info.zac.app.bag.model.toRestOpenbareRuimte
import nl.info.zac.app.bag.model.toRestPand
import nl.info.zac.app.bag.model.toRestWoonplaats
import nl.info.zac.app.bag.model.toZaakobjectRequest
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.policy.PolicyService
import nl.info.zac.policy.assertPolicy
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import java.util.UUID

@Path("bag")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Singleton
@NoArgConstructor
@AllOpen
class BagRestService @Inject constructor(
    private val bagClientService: BagClientService,
    private val zrcClientService: ZrcClientService,
    private val policyService: PolicyService,
    private val loggedInUserInstance: Instance<LoggedInUser>
) {
    @PUT
    @Path("adres")
    fun listAdressen(listAdressenParameters: RestListAdressenParameters): RESTResultaat<RestBagAdres> {
        val bevraagAdressenParameters = BevraagAdressenParameters().apply {
            q = listAdressenParameters.trefwoorden
            expand = listOf(
                BagObjectType.NUMMERAANDUIDING,
                BagObjectType.OPENBARE_RUIMTE,
                BagObjectType.PAND,
                BagObjectType.WOONPLAATS
            ).joinToString(",") { it.expand }
        }
        return RESTResultaat(bagClientService.listAdressen(bevraagAdressenParameters).map { it.toRestBagAdres() })
    }

    @GET
    @Path("/{type}/{id}")
    fun read(@PathParam("type") type: BagObjectType, @PathParam("id") id: String): RestBagObject? =
        when (type) {
            BagObjectType.ADRES -> bagClientService.readAdres(id).toRestBagAdres()
            BagObjectType.WOONPLAATS -> bagClientService.readWoonplaats(id).toRestWoonplaats()
            BagObjectType.PAND -> bagClientService.readPand(id).toRestPand()
            BagObjectType.OPENBARE_RUIMTE -> bagClientService.readOpenbareRuimte(id).toRestOpenbareRuimte()
            BagObjectType.NUMMERAANDUIDING -> bagClientService.readNummeraanduiding(id).toRestNummeraanduiding()
            // (Nog) geen zelfstandige entiteit
            BagObjectType.ADRESSEERBAAR_OBJECT -> null
        }

    @POST
    fun create(bagObjectGegevens: RestBagObjectGegevens) {
        val loggedInUser = loggedInUserInstance.get()
        val zaak = zrcClientService.readZaak(bagObjectGegevens.zaakUuid)
        assertPolicy(policyService.readZaakRechten(zaak, loggedInUser).canToevoegenBagObject)
        val bagObject = bagObjectGegevens.bagObject ?: throw IllegalArgumentException("BAG object is required")
        if (isNogNietGekoppeld(bagObject, zaak)) {
            zrcClientService.createZaakobject(bagObject.toZaakobjectRequest(zaak))
        }
    }

    @DELETE
    fun delete(bagObjectGegevens: RestBagObjectGegevens) {
        val loggedInUser = loggedInUserInstance.get()
        val zaak = zrcClientService.readZaak(bagObjectGegevens.zaakUuid)
        assertPolicy(policyService.readZaakRechten(zaak, loggedInUser).canBehandelen)
        val zaakobjectUuid = bagObjectGegevens.uuid ?: throw IllegalArgumentException("Zaakobject UUID is required")
        val zaakobject = zrcClientService.readZaakobject(zaakobjectUuid)
        zrcClientService.deleteZaakobject(zaakobject, bagObjectGegevens.redenWijzigen)
    }

    @GET
    @Path("zaak/{zaakUuid}")
    fun listBagObjectsForZaak(@PathParam("zaakUuid") zaakUuid: UUID): List<RestBagObjectGegevens> {
        val loggedInUser = loggedInUserInstance.get()
        val zaak = zrcClientService.readZaak(zaakUuid)
        assertPolicy(policyService.readZaakRechten(zaak, loggedInUser).canLezen)
        val zaakobjecten = zrcClientService.listZaakobjecten(
            ZaakobjectListParameters().apply { this.zaak = zaak.url }
        )
        return if (zaakobjecten.count() > 0) {
            zaakobjecten.results().filter { it.isBagObject }.map { it.toRestBagObjectGegevens() }
        } else {
            emptyList()
        }
    }

    private fun isNogNietGekoppeld(restBagObject: RestBagObject, zaak: Zaak): Boolean {
        val zaakobjectListParameters = ZaakobjectListParameters().apply {
            this.zaak = zaak.url
            `object` = restBagObject.url
            objectType = when (restBagObject.bagObjectType) {
                BagObjectType.ADRES -> ObjectTypeEnum.ADRES
                BagObjectType.NUMMERAANDUIDING -> ObjectTypeEnum.OVERIGE
                BagObjectType.WOONPLAATS -> ObjectTypeEnum.WOONPLAATS
                BagObjectType.PAND -> ObjectTypeEnum.PAND
                BagObjectType.OPENBARE_RUIMTE -> ObjectTypeEnum.OPENBARE_RUIMTE
                BagObjectType.ADRESSEERBAAR_OBJECT -> null
            }
        }
        return zrcClientService.listZaakobjecten(zaakobjectListParameters).results().isEmpty()
    }
}
