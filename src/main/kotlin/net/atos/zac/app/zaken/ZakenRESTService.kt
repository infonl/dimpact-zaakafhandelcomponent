/*
 * SPDX-FileCopyrightText: 2021 Atos, 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.zaken

import jakarta.enterprise.inject.Instance
import jakarta.inject.Inject
import jakarta.inject.Singleton
import jakarta.validation.Valid
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.NotFoundException
import jakarta.ws.rs.PATCH
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.atos.client.or.`object`.ObjectsClientService
import net.atos.client.vrl.VRLClientService
import net.atos.client.zgw.brc.BRCClientService
import net.atos.client.zgw.brc.model.generated.Besluit
import net.atos.client.zgw.brc.model.generated.Besluit.VervalredenEnum
import net.atos.client.zgw.brc.model.generated.BesluitInformatieObject
import net.atos.client.zgw.drc.DRCClientService
import net.atos.client.zgw.drc.model.generated.EnkelvoudigInformatieObject
import net.atos.client.zgw.shared.ZGWApiService
import net.atos.client.zgw.shared.model.audit.AuditTrailRegel
import net.atos.client.zgw.shared.util.URIUtil
import net.atos.client.zgw.zrc.ZRCClientService
import net.atos.client.zgw.zrc.model.AardRelatie
import net.atos.client.zgw.zrc.model.BetrokkeneType
import net.atos.client.zgw.zrc.model.HoofdzaakZaakPatch
import net.atos.client.zgw.zrc.model.LocatieZaakPatch
import net.atos.client.zgw.zrc.model.NatuurlijkPersoon
import net.atos.client.zgw.zrc.model.NietNatuurlijkPersoon
import net.atos.client.zgw.zrc.model.RelevanteZaak
import net.atos.client.zgw.zrc.model.RelevantezaakZaakPatch
import net.atos.client.zgw.zrc.model.Rol
import net.atos.client.zgw.zrc.model.RolMedewerker
import net.atos.client.zgw.zrc.model.RolNatuurlijkPersoon
import net.atos.client.zgw.zrc.model.RolNietNatuurlijkPersoon
import net.atos.client.zgw.zrc.model.RolVestiging
import net.atos.client.zgw.zrc.model.Vestiging
import net.atos.client.zgw.zrc.model.Zaak
import net.atos.client.zgw.zrc.model.ZaakInformatieobject
import net.atos.client.zgw.zrc.model.ZaakInformatieobjectListParameters
import net.atos.client.zgw.zrc.model.ZaakListParameters
import net.atos.client.zgw.zrc.model.zaakobjecten.Zaakobject
import net.atos.client.zgw.zrc.util.StatusTypeUtil
import net.atos.client.zgw.ztc.ZTCClientService
import net.atos.client.zgw.ztc.model.generated.BesluitType
import net.atos.client.zgw.ztc.model.generated.RolType
import net.atos.client.zgw.ztc.model.generated.ZaakType
import net.atos.client.zgw.ztc.util.isNuGeldig
import net.atos.zac.aanvraag.InboxProductaanvraagService
import net.atos.zac.aanvraag.ProductaanvraagService
import net.atos.zac.app.admin.converter.RESTZaakAfzenderConverter
import net.atos.zac.app.admin.model.RESTZaakAfzender
import net.atos.zac.app.audit.converter.RESTHistorieRegelConverter
import net.atos.zac.app.audit.model.RESTHistorieRegel
import net.atos.zac.app.bag.converter.RESTBAGConverter
import net.atos.zac.app.klanten.KlantenRESTService
import net.atos.zac.app.klanten.model.klant.IdentificatieType
import net.atos.zac.app.productaanvragen.model.RESTInboxProductaanvraag
import net.atos.zac.app.zaken.converter.RESTBesluitConverter
import net.atos.zac.app.zaken.converter.RESTBesluittypeConverter
import net.atos.zac.app.zaken.converter.RESTGeometryConverter
import net.atos.zac.app.zaken.converter.RESTResultaattypeConverter
import net.atos.zac.app.zaken.converter.RESTZaakConverter
import net.atos.zac.app.zaken.converter.RESTZaakOverzichtConverter
import net.atos.zac.app.zaken.converter.RESTZaaktypeConverter
import net.atos.zac.app.zaken.converter.convertToRESTCommunicatiekanalen
import net.atos.zac.app.zaken.converter.convertToRESTZaakBetrokkenen
import net.atos.zac.app.zaken.model.RESTBesluit
import net.atos.zac.app.zaken.model.RESTBesluitIntrekkenGegevens
import net.atos.zac.app.zaken.model.RESTBesluitVastleggenGegevens
import net.atos.zac.app.zaken.model.RESTBesluitWijzigenGegevens
import net.atos.zac.app.zaken.model.RESTBesluittype
import net.atos.zac.app.zaken.model.RESTCommunicatiekanaal
import net.atos.zac.app.zaken.model.RESTDocumentOntkoppelGegevens
import net.atos.zac.app.zaken.model.RESTReden
import net.atos.zac.app.zaken.model.RESTResultaattype
import net.atos.zac.app.zaken.model.RESTZaak
import net.atos.zac.app.zaken.model.RESTZaakAanmaakGegevens
import net.atos.zac.app.zaken.model.RESTZaakAfbrekenGegevens
import net.atos.zac.app.zaken.model.RESTZaakAfsluitenGegevens
import net.atos.zac.app.zaken.model.RESTZaakBetrokkene
import net.atos.zac.app.zaken.model.RESTZaakBetrokkeneGegevens
import net.atos.zac.app.zaken.model.RESTZaakEditMetRedenGegevens
import net.atos.zac.app.zaken.model.RESTZaakHeropenenGegevens
import net.atos.zac.app.zaken.model.RESTZaakKoppelGegevens
import net.atos.zac.app.zaken.model.RESTZaakLocatieGegevens
import net.atos.zac.app.zaken.model.RESTZaakOntkoppelGegevens
import net.atos.zac.app.zaken.model.RESTZaakOpschortGegevens
import net.atos.zac.app.zaken.model.RESTZaakOpschorting
import net.atos.zac.app.zaken.model.RESTZaakOverzicht
import net.atos.zac.app.zaken.model.RESTZaakToekennenGegevens
import net.atos.zac.app.zaken.model.RESTZaakVerlengGegevens
import net.atos.zac.app.zaken.model.RESTZaaktype
import net.atos.zac.app.zaken.model.RESTZakenVerdeelGegevens
import net.atos.zac.app.zaken.model.RESTZakenVrijgevenGegevens
import net.atos.zac.app.zaken.model.RelatieType
import net.atos.zac.authentication.LoggedInUser
import net.atos.zac.configuratie.ConfiguratieService
import net.atos.zac.documenten.OntkoppeldeDocumentenService
import net.atos.zac.event.EventingService
import net.atos.zac.flowable.BPMNService
import net.atos.zac.flowable.CMMNService
import net.atos.zac.flowable.FlowableTaskService
import net.atos.zac.flowable.ZaakVariabelenService
import net.atos.zac.healthcheck.HealthCheckService
import net.atos.zac.identity.IdentityService
import net.atos.zac.policy.PolicyService
import net.atos.zac.policy.PolicyService.assertPolicy
import net.atos.zac.shared.helper.OpschortenZaakHelper
import net.atos.zac.signalering.SignaleringenService
import net.atos.zac.signalering.model.SignaleringType
import net.atos.zac.signalering.model.SignaleringZoekParameters
import net.atos.zac.util.DateTimeConverterUtil
import net.atos.zac.util.LocalDateUtil
import net.atos.zac.util.UriUtil
import net.atos.zac.websocket.event.ScreenEventType
import net.atos.zac.zaaksturing.ZaakafhandelParameterService
import net.atos.zac.zaaksturing.model.ZaakAfzender
import net.atos.zac.zaaksturing.model.ZaakAfzender.Speciaal
import net.atos.zac.zaaksturing.model.ZaakafhandelParameters
import net.atos.zac.zaaksturing.model.ZaakbeeindigParameter
import net.atos.zac.zaken.ZakenService
import net.atos.zac.zoeken.IndexeerService
import net.atos.zac.zoeken.model.index.ZoekObjectType
import nl.lifely.zac.util.AllOpen
import nl.lifely.zac.util.NoArgConstructor
import org.apache.commons.collections4.CollectionUtils
import org.apache.commons.lang3.StringUtils
import java.net.URI
import java.time.LocalDate
import java.util.Locale
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.Consumer
import java.util.logging.Logger
import java.util.stream.Collectors
import java.util.stream.Stream

@Path("zaken")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Singleton
@Suppress("TooManyFunctions", "LargeClass", "LongParameterList")
@NoArgConstructor
@AllOpen
class ZakenRESTService @Inject constructor(
    private val zgwApiService: ZGWApiService,
    private val productaanvraagService: ProductaanvraagService,
    private val brcClientService: BRCClientService,
    private val drcClientService: DRCClientService,
    private val ztcClientService: ZTCClientService,
    private val zrcClientService: ZRCClientService,
    private val vrlClientService: VRLClientService,
    private val eventingService: EventingService,
    private val identityService: IdentityService,
    private val signaleringenService: SignaleringenService,
    private val ontkoppeldeDocumentenService: OntkoppeldeDocumentenService,
    private val indexeerService: IndexeerService,
    private val policyService: PolicyService,
    private val cmmnService: CMMNService,
    private val bpmnService: BPMNService,
    private val flowableTaskService: FlowableTaskService,
    private val objectsClientService: ObjectsClientService,
    private val inboxProductaanvraagService: InboxProductaanvraagService,
    private val zaakVariabelenService: ZaakVariabelenService,
    private val configuratieService: ConfiguratieService,
    private val loggedInUserInstance: Instance<LoggedInUser>,
    private val restZaakConverter: RESTZaakConverter,
    private val restZaaktypeConverter: RESTZaaktypeConverter,
    private val restBesluitConverter: RESTBesluitConverter,
    private val restBesluittypeConverter: RESTBesluittypeConverter,
    private val restResultaattypeConverter: RESTResultaattypeConverter,
    private val restZaakOverzichtConverter: RESTZaakOverzichtConverter,
    private val restBAGConverter: RESTBAGConverter,
    private val restHistorieRegelConverter: RESTHistorieRegelConverter,
    private val zaakafhandelParameterService: ZaakafhandelParameterService,
    private val restGeometryConverter: RESTGeometryConverter,
    private val healthCheckService: HealthCheckService,
    private val opschortenZaakHelper: OpschortenZaakHelper,
    private val restZaakAfzenderConverter: RESTZaakAfzenderConverter,
    private val zakenService: ZakenService
) {
    companion object {
        private val LOG = Logger.getLogger(ZakenRESTService::class.java.name)
        private val ioCoroutineScope = CoroutineScope(Dispatchers.IO)

        private const val ROL_VERWIJDER_REDEN = "Verwijderd door de medewerker tijdens het behandelen van de zaak"
        private const val ROL_TOEVOEGEN_REDEN = "Toegekend door de medewerker tijdens het behandelen van de zaak"
        private const val AANMAKEN_ZAAK_REDEN = "Aanmaken zaak"
        private const val VERLENGING = "Verlenging"
        private const val AANMAKEN_BESLUIT_TOELICHTING = "Aanmaken besluit"
        private const val WIJZIGEN_BESLUIT_TOELICHTING = "Wijzigen besluit"
    }

    @GET
    @Path("zaak/{uuid}")
    fun readZaak(@PathParam("uuid") zaakUUID: UUID): RESTZaak {
        val zaak = zrcClientService.readZaak(zaakUUID)
        val restZaak = restZaakConverter.convert(zaak)
        assertPolicy(restZaak.rechten.lezen)
        deleteSignaleringen(zaak)
        return restZaak
    }

    @GET
    @Path("zaak/id/{identificatie}")
    fun readZaakById(@PathParam("identificatie") identificatie: String): RESTZaak {
        val zaak = zrcClientService.readZaakByID(identificatie)
        val restZaak: RESTZaak = restZaakConverter.convert(zaak)
        assertPolicy(restZaak.rechten.lezen)
        deleteSignaleringen(zaak)
        return restZaak
    }

    @PUT
    @Path("initiator")
    fun updateInitiator(gegevens: RESTZaakBetrokkeneGegevens): RESTZaak {
        val zaak: Zaak = zrcClientService.readZaak(gegevens.zaakUUID)
        zgwApiService.findInitiatorForZaak(zaak)
            .ifPresent { initiator: Rol<*> -> removeInitiator(zaak, initiator, ROL_VERWIJDER_REDEN) }
        addInitiator(gegevens.betrokkeneIdentificatieType, gegevens.betrokkeneIdentificatie, zaak)
        return restZaakConverter.convert(zaak)
    }

    @DELETE
    @Path("{uuid}/initiator")
    fun deleteInitiator(@PathParam("uuid") zaakUUID: UUID, reden: RESTReden): RESTZaak {
        val zaak = zrcClientService.readZaak(zaakUUID)
        zgwApiService.findInitiatorForZaak(zaak)
            .ifPresent { initiator: Rol<*> -> removeInitiator(zaak, initiator, reden.reden) }
        return restZaakConverter.convert(zaak)
    }

    @POST
    @Path("betrokkene")
    fun createBetrokken(@Valid gegevens: RESTZaakBetrokkeneGegevens): RESTZaak {
        val zaak = zrcClientService.readZaak(gegevens.zaakUUID)
        addBetrokkene(
            gegevens.roltypeUUID,
            gegevens.betrokkeneIdentificatieType,
            gegevens.betrokkeneIdentificatie,
            gegevens.roltoelichting,
            zaak
        )
        return restZaakConverter.convert(zaak)
    }

    @DELETE
    @Path("betrokkene/{uuid}")
    fun deleteBetrokkene(
        @PathParam("uuid") betrokkeneUUID: UUID?,
        reden: RESTReden
    ): RESTZaak {
        val betrokkene = zrcClientService.readRol(betrokkeneUUID)
        val zaak = zrcClientService.readZaak(betrokkene.zaak)
        removeBetrokkene(zaak, betrokkene, reden.reden)
        return restZaakConverter.convert(zaak)
    }

    @POST
    @Path("zaak")
    fun createZaak(@Valid restZaakAanmaakGegevens: RESTZaakAanmaakGegevens): RESTZaak {
        val restZaak = restZaakAanmaakGegevens.zaak
        val zaaktype = ztcClientService.readZaaktype(restZaak.zaaktype.uuid)

        // make sure to use the omschrijving of the zaaktype that was retrieved to perform
        // authorisation on zaaktype
        assertPolicy(
            policyService.readOverigeRechten().startenZaak &&
                loggedInUserInstance.get().isGeautoriseerdZaaktype(zaaktype.omschrijving)
        )

        val zaak = zgwApiService.createZaak(restZaakConverter.convert(restZaak, zaaktype))
        if (StringUtils.isNotEmpty(restZaak.initiatorIdentificatie)) {
            addInitiator(
                restZaak.initiatorIdentificatieType!!,
                restZaak.initiatorIdentificatie!!,
                zaak
            )
        }
        restZaak.groep?.let {
            val group = identityService.readGroup(it.id)
            zrcClientService.updateRol(zaak, zakenService.bepaalRolGroep(group, zaak), AANMAKEN_ZAAK_REDEN)
        }
        restZaak.behandelaar?.let {
            val user = identityService.readUser(it.id)
            zrcClientService.updateRol(zaak, zakenService.bepaalRolMedewerker(user, zaak), AANMAKEN_ZAAK_REDEN)
        }
        cmmnService.startCase(
            zaak,
            zaaktype,
            zaakafhandelParameterService.readZaakafhandelParameters(
                URIUtil.parseUUIDFromResourceURI(zaaktype.url)
            ),
            null
        )

        restZaakAanmaakGegevens.inboxProductaanvraag?.let { inboxProductaanvraag ->
            koppelInboxProductaanvraag(zaak, inboxProductaanvraag)
        }

        restZaakAanmaakGegevens.bagObjecten?.let { restbagObjecten ->
            for (restbagObject in restbagObjecten) {
                val zaakobject: Zaakobject = restBAGConverter.convertToZaakobject(restbagObject, zaak)
                zrcClientService.createZaakobject(zaakobject)
            }
        }

        return restZaakConverter.convert(zaak)
    }

    @PATCH
    @Path("zaak/{uuid}")
    fun updateZaak(
        @PathParam("uuid") zaakUUID: UUID,
        restZaakEditMetRedenGegevens: RESTZaakEditMetRedenGegevens
    ): RESTZaak {
        assertPolicy(policyService.readZaakRechten(zrcClientService.readZaak(zaakUUID)).wijzigen)
        val updatedZaak = zrcClientService.patchZaak(
            zaakUUID,
            restZaakConverter.convertToPatch(restZaakEditMetRedenGegevens.zaak),
            restZaakEditMetRedenGegevens.reden
        )
        return restZaakConverter.convert(updatedZaak)
    }

    @PATCH
    @Path("{uuid}/zaaklocatie")
    fun updateZaakLocatie(
        @PathParam("uuid") zaakUUID: UUID?,
        locatieGegevens: RESTZaakLocatieGegevens
    ): RESTZaak {
        assertPolicy(policyService.readZaakRechten(zrcClientService.readZaak(zaakUUID)).wijzigen)
        val locatieZaakPatch = LocatieZaakPatch(
            locatieGegevens.geometrie?.let {
                restGeometryConverter.convert(it)
            }
        )
        val updatedZaak = zrcClientService.patchZaak(
            zaakUUID,
            locatieZaakPatch,
            locatieGegevens.reden
        )
        return restZaakConverter.convert(updatedZaak)
    }

    @PATCH
    @Path("zaak/{uuid}/opschorting")
    fun opschortenZaak(
        @PathParam("uuid") zaakUUID: UUID,
        opschortGegevens: RESTZaakOpschortGegevens
    ): RESTZaak {
        val zaak = zrcClientService.readZaak(zaakUUID)
        return if (opschortGegevens.indicatieOpschorting) {
            restZaakConverter.convert(
                opschortenZaakHelper.opschortenZaak(
                    zaak,
                    opschortGegevens.duurDagen,
                    opschortGegevens.redenOpschorting
                )
            )
        } else {
            restZaakConverter.convert(
                opschortenZaakHelper.hervattenZaak(zaak, opschortGegevens.redenOpschorting)
            )
        }
    }

    @GET
    @Path("zaak/{uuid}/opschorting")
    fun readOpschortingZaak(@PathParam("uuid") zaakUUID: UUID): RESTZaakOpschorting {
        assertPolicy(policyService.readZaakRechten(zrcClientService.readZaak(zaakUUID)).lezen)
        val zaakOpschorting = RESTZaakOpschorting()
        zaakVariabelenService.findDatumtijdOpgeschort(zaakUUID)
            .ifPresent { datumtijdOpgeschort -> zaakOpschorting.vanafDatumTijd = datumtijdOpgeschort }
        zaakVariabelenService.findVerwachteDagenOpgeschort(zaakUUID)
            .ifPresent { verwachteDagenOpgeschort -> zaakOpschorting.duurDagen = verwachteDagenOpgeschort }
        return zaakOpschorting
    }

    @PATCH
    @Path("zaak/{uuid}/verlenging")
    fun verlengenZaak(
        @PathParam("uuid") zaakUUID: UUID,
        restZaakVerlengGegevens: RESTZaakVerlengGegevens
    ): RESTZaak {
        val zaak = zrcClientService.readZaak(zaakUUID)
        assertPolicy(policyService.readZaakRechten(zaak).verlengen)
        val toelichting = "$VERLENGING: ${restZaakVerlengGegevens.redenVerlenging}"
        val updatedZaak = zrcClientService.patchZaak(
            zaakUUID,
            restZaakConverter.convertToPatch(
                zaakUUID,
                restZaakVerlengGegevens
            ),
            toelichting
        )
        if (restZaakVerlengGegevens.takenVerlengen) {
            val aantalTakenVerlengd = verlengOpenTaken(zaakUUID, restZaakVerlengGegevens.duurDagen)
            if (aantalTakenVerlengd > 0) {
                eventingService.send(ScreenEventType.ZAAK_TAKEN.updated(updatedZaak))
            }
        }
        val status = if (zaak.status != null) {
            zrcClientService.readStatus(zaak.status)
        } else {
            null
        }
        val statustype = if (status != null) {
            ztcClientService.readStatustype(status.statustype)
        } else {
            null
        }
        return restZaakConverter.convert(updatedZaak, status, statustype)
    }

    @PUT
    @Path("zaakinformatieobjecten/ontkoppel")
    fun ontkoppelInformatieObject(ontkoppelGegevens: RESTDocumentOntkoppelGegevens) {
        val zaak: Zaak = zrcClientService.readZaak(ontkoppelGegevens.zaakUUID)
        val informatieobject: EnkelvoudigInformatieObject = drcClientService.readEnkelvoudigInformatieobject(
            ontkoppelGegevens.documentUUID
        )
        assertPolicy(policyService.readDocumentRechten(informatieobject, zaak).ontkoppelen)
        val parameters = ZaakInformatieobjectListParameters()
        parameters.informatieobject = informatieobject.url
        parameters.zaak = zaak.url
        val zaakInformatieobjecten: List<ZaakInformatieobject> = zrcClientService.listZaakinformatieobjecten(
            parameters
        )
        if (zaakInformatieobjecten.isEmpty()) {
            throw NotFoundException(
                "Geen ZaakInformatieobject gevonden voor Zaak: '${ontkoppelGegevens.zaakUUID}' " +
                    "en InformatieObject: '${ontkoppelGegevens.documentUUID}'"
            )
        }
        zaakInformatieobjecten.forEach(
            Consumer { zaakInformatieobject: ZaakInformatieobject ->
                zrcClientService.deleteZaakInformatieobject(
                    zaakInformatieobject.uuid,
                    ontkoppelGegevens.reden,
                    "Ontkoppeld"
                )
            }
        )
        if (zrcClientService.listZaakinformatieobjecten(informatieobject).isEmpty()) {
            indexeerService.removeInformatieobject(URIUtil.parseUUIDFromResourceURI(informatieobject.url))
            ontkoppeldeDocumentenService.create(informatieobject, zaak, ontkoppelGegevens.reden)
        }
    }

    @GET
    @Path("waarschuwing")
    fun listZaakWaarschuwingen(): List<RESTZaakOverzicht> {
        val vandaag = LocalDate.now()
        val einddatumGeplandWaarschuwing: MutableMap<UUID, LocalDate> = HashMap()
        val uiterlijkeEinddatumAfdoeningWaarschuwing: MutableMap<UUID, LocalDate> = HashMap()
        zaakafhandelParameterService.listZaakafhandelParameters().forEach(
            Consumer { parameters: ZaakafhandelParameters ->
                if (parameters.einddatumGeplandWaarschuwing != null) {
                    einddatumGeplandWaarschuwing[parameters.zaakTypeUUID] = datumWaarschuwing(
                        vandaag,
                        parameters.einddatumGeplandWaarschuwing
                    )
                }
                if (parameters.uiterlijkeEinddatumAfdoeningWaarschuwing != null) {
                    uiterlijkeEinddatumAfdoeningWaarschuwing[parameters.zaakTypeUUID] = datumWaarschuwing(
                        vandaag,
                        parameters.uiterlijkeEinddatumAfdoeningWaarschuwing
                    )
                }
            }
        )
        val zaakListParameters = ZaakListParameters()
        zaakListParameters.rolBetrokkeneIdentificatieMedewerkerIdentificatie = loggedInUserInstance.get().id
        return zrcClientService.listZaken(zaakListParameters).results.stream()
            .filter { obj -> obj.isOpen }
            .filter { zaak ->
                isWaarschuwing(
                    zaak,
                    vandaag,
                    einddatumGeplandWaarschuwing,
                    uiterlijkeEinddatumAfdoeningWaarschuwing
                )
            }
            .map { zaak -> restZaakOverzichtConverter.convert(zaak) }
            .toList()
    }

    @GET
    @Path("zaaktypes")
    fun listZaaktypes(): List<RESTZaaktype> =
        ztcClientService.listZaaktypen(configuratieService.readDefaultCatalogusURI())
            .stream()
            .filter { zaaktype -> loggedInUserInstance.get().isGeautoriseerdZaaktype(zaaktype.omschrijving) }
            .filter { zaaktype -> !zaaktype.concept }
            .filter { zaakType -> isNuGeldig(zaakType) }
            .filter { zaaktype -> healthCheckService.controleerZaaktype(zaaktype.url).isValide }
            .map { zaaktype: ZaakType -> restZaaktypeConverter.convert(zaaktype) }
            .toList()

    @PUT
    @Path("zaakdata")
    fun updateZaakdata(restZaak: RESTZaak): RESTZaak {
        val zaak = zrcClientService.readZaak(restZaak.uuid)
        assertPolicy(policyService.readZaakRechten(zaak).wijzigen)

        zaakVariabelenService.setZaakdata(restZaak.uuid, restZaak.zaakdata)
        return restZaak
    }

    @PATCH
    @Path("toekennen")
    fun toekennen(@Valid toekennenGegevens: RESTZaakToekennenGegevens): RESTZaak {
        val zaak: Zaak = zrcClientService.readZaak(toekennenGegevens.zaakUUID)
        assertPolicy(policyService.readZaakRechten(zaak).toekennen)

        val behandelaar = zgwApiService.findBehandelaarForZaak(zaak)
            .map { rolMedewerker: RolMedewerker -> rolMedewerker.betrokkeneIdentificatie.identificatie }
            .orElse(null)

        val isUpdated = AtomicBoolean(false)
        if (!StringUtils.equals(behandelaar, toekennenGegevens.behandelaarGebruikersnaam)) {
            if (StringUtils.isNotEmpty(toekennenGegevens.behandelaarGebruikersnaam)) {
                // Toekennen of overdragen
                val user = identityService.readUser(
                    toekennenGegevens.behandelaarGebruikersnaam
                )
                zrcClientService.updateRol(
                    zaak,
                    zakenService.bepaalRolMedewerker(user, zaak),
                    toekennenGegevens.reden
                )
            } else {
                // Vrijgeven
                zrcClientService.deleteRol(
                    zaak,
                    BetrokkeneType.MEDEWERKER,
                    toekennenGegevens.reden
                )
            }
            isUpdated.set(true)
        }

        zgwApiService.findGroepForZaak(zaak)
            .ifPresent { groep ->
                if (!StringUtils.equals(
                        groep.betrokkeneIdentificatie.identificatie,
                        toekennenGegevens.groepId
                    )
                ) {
                    val group = identityService.readGroup(toekennenGegevens.groepId)
                    zrcClientService.updateRol(
                        zaak,
                        zakenService.bepaalRolGroep(group, zaak),
                        toekennenGegevens.reden
                    )
                    isUpdated.set(true)
                }
            }

        if (isUpdated.get()) {
            indexeerService.indexeerDirect(zaak.uuid.toString(), ZoekObjectType.ZAAK, false)
        }

        return restZaakConverter.convert(zaak)
    }

    @PUT
    @Path("lijst/toekennen/mij")
    fun toekennenAanIngelogdeMedewerkerVanuitLijst(
        @Valid toekennenGegevens: RESTZaakToekennenGegevens
    ): RESTZaakOverzicht {
        assertPolicy(policyService.readWerklijstRechten().zakenTaken)
        val zaak = ingelogdeMedewerkerToekennenAanZaak(toekennenGegevens)
        indexeerService.indexeerDirect(zaak.uuid.toString(), ZoekObjectType.ZAAK, false)
        return restZaakOverzichtConverter.convert(zaak)
    }

    @PUT
    @Path("lijst/verdelen")
    fun verdelenVanuitLijst(@Valid restZakenVerdeelGegevens: RESTZakenVerdeelGegevens) {
        assertPolicy(policyService.readWerklijstRechten().zakenTakenVerdelen)
        // this can be a long-running operation so run it asynchronously
        ioCoroutineScope.launch {
            zakenService.assignZakenAsync(
                zaakUUIDs = restZakenVerdeelGegevens.uuids,
                explanation = restZakenVerdeelGegevens.reden,
                group = restZakenVerdeelGegevens.groepId.let {
                    identityService.readGroup(
                        restZakenVerdeelGegevens.groepId
                    )
                },
                user = restZakenVerdeelGegevens.behandelaarGebruikersnaam?.let {
                    identityService.readUser(restZakenVerdeelGegevens.behandelaarGebruikersnaam)
                },
                screenEventResourceId = restZakenVerdeelGegevens.screenEventResourceId
            )
        }
    }

    @PUT
    @Path("lijst/vrijgeven")
    fun vrijgevenVanuitLijst(@Valid restZakenVrijgevenGegevens: RESTZakenVrijgevenGegevens) {
        assertPolicy(policyService.readWerklijstRechten().zakenTakenVerdelen)
        // this can be a long-running operation so run it asynchronously
        ioCoroutineScope.launch {
            zakenService.releaseZaken(
                zaakUUIDs = restZakenVrijgevenGegevens.uuids,
                explanation = restZakenVrijgevenGegevens.reden,
                screenEventResourceId = restZakenVrijgevenGegevens.screenEventResourceId
            )
        }
    }

    @PATCH
    @Path("/zaak/{uuid}/afbreken")
    fun afbreken(
        @PathParam("uuid") zaakUUID: UUID,
        afbrekenGegevens: RESTZaakAfbrekenGegevens
    ) {
        val zaak = zrcClientService.readZaak(zaakUUID)
        val statustype = if (zaak.status != null) {
            ztcClientService.readStatustype(
                zrcClientService.readStatus(zaak.status).statustype
            )
        } else {
            null
        }
        assertPolicy(policyService.readZaakRechten(zaak).afbreken)
        assertPolicy(zaak.isOpen && !StatusTypeUtil.isHeropend(statustype))
        policyService.checkZaakAfsluitbaar(zaak)
        val zaakafhandelParameters = zaakafhandelParameterService.readZaakafhandelParameters(
            UriUtil.uuidFromURI(zaak.zaaktype)
        )
        val zaakbeeindigParameter: ZaakbeeindigParameter = zaakafhandelParameters.readZaakbeeindigParameter(
            afbrekenGegevens.zaakbeeindigRedenId
        )
        zgwApiService.createResultaatForZaak(
            zaak,
            zaakbeeindigParameter.resultaattype,
            zaakbeeindigParameter.zaakbeeindigReden.naam
        )
        zgwApiService.endZaak(zaak, zaakbeeindigParameter.zaakbeeindigReden.naam)
        // Terminate case after the zaak is ended in order to prevent the EndCaseLifecycleListener from ending the zaak.
        cmmnService.terminateCase(zaakUUID)
    }

    @PATCH
    @Path("/zaak/{uuid}/heropenen")
    fun heropenen(
        @PathParam("uuid") zaakUUID: UUID?,
        heropenenGegevens: RESTZaakHeropenenGegevens
    ) {
        val zaak = zrcClientService.readZaak(zaakUUID)
        assertPolicy(!zaak.isOpen && policyService.readZaakRechten(zaak).heropenen)
        zgwApiService.createStatusForZaak(
            zaak,
            ConfiguratieService.STATUSTYPE_OMSCHRIJVING_HEROPEND,
            heropenenGegevens.reden
        )
        zaak.resultaat?.let {
            zrcClientService.deleteResultaat(UriUtil.uuidFromURI(it))
        }
    }

    @PATCH
    @Path("/zaak/{uuid}/afsluiten")
    fun afsluiten(
        @PathParam("uuid") zaakUUID: UUID?,
        afsluitenGegevens: RESTZaakAfsluitenGegevens
    ) {
        val zaak = zrcClientService.readZaak(zaakUUID)
        assertPolicy(zaak.isOpen && policyService.readZaakRechten(zaak).behandelen)
        policyService.checkZaakAfsluitbaar(zaak)
        zgwApiService.updateResultaatForZaak(
            zaak,
            afsluitenGegevens.resultaattypeUuid,
            afsluitenGegevens.reden
        )
        zgwApiService.closeZaak(zaak, afsluitenGegevens.reden)
    }

    @PATCH
    @Path("/zaak/koppel")
    fun koppelZaak(gegevens: RESTZaakKoppelGegevens) {
        val zaak: Zaak = zrcClientService.readZaak(gegevens.zaakUuid)
        val teKoppelenZaak: Zaak = zrcClientService.readZaak(gegevens.teKoppelenZaakUuid)
        assertPolicy(policyService.readZaakRechten(zaak).koppelen)
        assertPolicy(policyService.readZaakRechten(teKoppelenZaak).koppelen)

        when (gegevens.relatieType) {
            RelatieType.HOOFDZAAK -> koppelHoofdEnDeelzaak(teKoppelenZaak, zaak)
            RelatieType.DEELZAAK -> koppelHoofdEnDeelzaak(zaak, teKoppelenZaak)
            RelatieType.VERVOLG -> koppelRelevantezaken(zaak, teKoppelenZaak, AardRelatie.VERVOLG)
            RelatieType.ONDERWERP -> koppelRelevantezaken(zaak, teKoppelenZaak, AardRelatie.ONDERWERP)
            RelatieType.BIJDRAGE -> koppelRelevantezaken(zaak, teKoppelenZaak, AardRelatie.BIJDRAGE)
        }
        gegevens.reverseRelatieType?.let { reverseRelatieType ->
            when (reverseRelatieType) {
                RelatieType.VERVOLG -> koppelRelevantezaken(teKoppelenZaak, zaak, AardRelatie.VERVOLG)
                RelatieType.ONDERWERP -> koppelRelevantezaken(teKoppelenZaak, zaak, AardRelatie.ONDERWERP)
                RelatieType.BIJDRAGE -> koppelRelevantezaken(teKoppelenZaak, zaak, AardRelatie.BIJDRAGE)
                else -> error("Reverse relatie type $reverseRelatieType is not supported")
            }
        }
    }

    @PATCH
    @Path("/zaak/ontkoppel")
    fun ontkoppelZaak(gegevens: RESTZaakOntkoppelGegevens) {
        val zaak: Zaak = zrcClientService.readZaak(gegevens.zaakUuid)
        val gekoppeldeZaak = zrcClientService.readZaakByID(gegevens.gekoppeldeZaakIdentificatie)
        assertPolicy(policyService.readZaakRechten(zaak).wijzigen)
        assertPolicy(policyService.readZaakRechten(gekoppeldeZaak).wijzigen)

        when (gegevens.relatietype) {
            RelatieType.HOOFDZAAK -> ontkoppelHoofdEnDeelzaak(gekoppeldeZaak, zaak, gegevens.reden)
            RelatieType.DEELZAAK -> ontkoppelHoofdEnDeelzaak(zaak, gekoppeldeZaak, gegevens.reden)
            RelatieType.VERVOLG -> ontkoppelRelevantezaken(
                zaak,
                gekoppeldeZaak,
                AardRelatie.VERVOLG,
                gegevens.reden
            )
            RelatieType.ONDERWERP -> ontkoppelRelevantezaken(
                zaak,
                gekoppeldeZaak,
                AardRelatie.ONDERWERP,
                gegevens.reden
            )
            RelatieType.BIJDRAGE -> ontkoppelRelevantezaken(
                zaak,
                gekoppeldeZaak,
                AardRelatie.BIJDRAGE,
                gegevens.reden
            )
        }
    }

    @PUT
    @Path("toekennen/mij")
    fun toekennenAanIngelogdeMedewerker(
        @Valid toekennenGegevens: RESTZaakToekennenGegevens
    ): RESTZaak {
        val zaak = ingelogdeMedewerkerToekennenAanZaak(toekennenGegevens)
        return restZaakConverter.convert(zaak)
    }

    @GET
    @Path("zaak/{uuid}/historie")
    fun listHistorie(@PathParam("uuid") zaakUUID: UUID?): List<RESTHistorieRegel> {
        assertPolicy(policyService.readZaakRechten(zrcClientService.readZaak(zaakUUID)).lezen)
        val auditTrail: List<AuditTrailRegel> = zrcClientService.listAuditTrail(zaakUUID)
        return restHistorieRegelConverter.convert(auditTrail)
    }

    @GET
    @Path("zaak/{uuid}/betrokkene")
    fun listBetrokkenenVoorZaak(
        @PathParam("uuid") zaakUUID: UUID?
    ): List<RESTZaakBetrokkene> {
        val zaak = zrcClientService.readZaak(zaakUUID)
        assertPolicy(policyService.readZaakRechten(zaak).lezen)
        return convertToRESTZaakBetrokkenen(
            zrcClientService.listRollen(zaak).stream()
                .filter { rol ->
                    KlantenRESTService.betrokkenen.contains(
                        RolType.OmschrijvingGeneriekEnum.valueOf(
                            rol.omschrijvingGeneriek.uppercase(Locale.getDefault())
                        )
                    )
                }
        )
    }

    /**
     * Retrieve all possible afzenders for a zaak
     *
     * @param zaakUUID the id of the zaak
     * @return list of afzenders
     */
    @GET
    @Path("zaak/{uuid}/afzender")
    fun listAfzendersVoorZaak(@PathParam("uuid") zaakUUID: UUID): List<RESTZaakAfzender> {
        val zaak = zrcClientService.readZaak(zaakUUID)
        return sortAndRemoveDuplicates(
            resolveZaakAfzenderMail(
                restZaakAfzenderConverter.convertZaakAfzenders(
                    zaakafhandelParameterService.readZaakafhandelParameters(
                        UriUtil.uuidFromURI(zaak.zaaktype)
                    ).zaakAfzenders
                ).stream()
            )
        )
    }

    /**
     * Retrieve the default afzender for a zaak
     *
     * @param zaakUUID the id of the zaak
     * @return the default zaakafzender or null if no default is available
     */
    @GET
    @Path("zaak/{uuid}/afzender/default")
    fun readDefaultAfzenderVoorZaak(@PathParam("uuid") zaakUUID: UUID?): RESTZaakAfzender? {
        val zaak = zrcClientService.readZaak(zaakUUID)
        return resolveZaakAfzenderMail(
            zaakafhandelParameterService.readZaakafhandelParameters(
                UriUtil.uuidFromURI(zaak.zaaktype)
            )
                .zaakAfzenders.stream()
                .filter { obj -> obj.isDefault }
                .map { zaakAfzender -> restZaakAfzenderConverter.convertZaakAfzender(zaakAfzender) }
        )
            .findAny()
            .orElse(null)
    }

    @GET
    @Path("communicatiekanalen/{inclusiefEFormulier}")
    fun listCommunicatiekanalen(
        @PathParam("inclusiefEFormulier") inclusiefEFormulier: Boolean
    ): List<RESTCommunicatiekanaal> {
        val communicatieKanalen = vrlClientService.listCommunicatiekanalen()
        if (!inclusiefEFormulier) {
            communicatieKanalen.removeIf { communicatieKanaal ->
                (communicatieKanaal.naam == ConfiguratieService.COMMUNICATIEKANAAL_EFORMULIER)
            }
        }
        return convertToRESTCommunicatiekanalen(communicatieKanalen)
    }

    @GET
    @Path("besluit/zaakUuid/{zaakUuid}")
    fun listBesluitenForZaakUUID(@PathParam("zaakUuid") zaakUuid: UUID): List<RESTBesluit> {
        return brcClientService.listBesluiten(zrcClientService.readZaak(zaakUuid))
            .map { besluiten -> restBesluitConverter.convertToRESTBesluit(besluiten) }
            .orElse(null)
    }

    @POST
    @Path("besluit")
    fun createBesluit(besluitToevoegenGegevens: RESTBesluitVastleggenGegevens): RESTBesluit {
        val zaak = zrcClientService.readZaak(besluitToevoegenGegevens.zaakUuid)
        val zaaktype = ztcClientService.readZaaktype(zaak.zaaktype)
        assertPolicy(policyService.readZaakRechten(zaak, zaaktype).vastleggenBesluit)
        assertPolicy(CollectionUtils.isNotEmpty(zaaktype.besluittypen))

        val besluit = restBesluitConverter.convertToBesluit(zaak, besluitToevoegenGegevens)
        if (zaak.resultaat != null) {
            zgwApiService.updateResultaatForZaak(zaak, besluitToevoegenGegevens.resultaattypeUuid, null)
        } else {
            zgwApiService.createResultaatForZaak(zaak, besluitToevoegenGegevens.resultaattypeUuid, null)
        }
        val restBesluit = restBesluitConverter.convertToRESTBesluit(brcClientService.createBesluit(besluit))
        besluitToevoegenGegevens.informatieobjecten!!.forEach(
            Consumer { documentUri: UUID? ->
                val informatieobject = drcClientService.readEnkelvoudigInformatieobject(documentUri)
                val besluitInformatieobject = BesluitInformatieObject()
                besluitInformatieobject.informatieobject = informatieobject.url
                besluitInformatieobject.besluit = restBesluit.url
                brcClientService.createBesluitInformatieobject(besluitInformatieobject, AANMAKEN_BESLUIT_TOELICHTING)
            }
        )
        // This event should result from a ZAAKBESLUIT CREATED notification on the ZAKEN channel
        // but open_zaak does not send that one, so emulate it here.
        eventingService.send(ScreenEventType.ZAAK_BESLUITEN.updated(zaak))
        return restBesluit
    }

    @PUT
    @Path("besluit")
    fun updateBesluit(
        restBesluitWijzigenGegevens: RESTBesluitWijzigenGegevens
    ): RESTBesluit {
        var besluit = brcClientService.readBesluit(restBesluitWijzigenGegevens.besluitUuid)
        val zaak = zrcClientService.readZaak(besluit!!.zaak)
        assertPolicy(policyService.readZaakRechten(zaak).vastleggenBesluit)

        besluit = restBesluitConverter.convertToBesluit(besluit, restBesluitWijzigenGegevens)
        besluit = brcClientService.updateBesluit(besluit, restBesluitWijzigenGegevens.reden)
        if (zaak.resultaat != null) {
            val zaakResultaat = zrcClientService.readResultaat(zaak.resultaat)
            val resultaattype = ztcClientService.readResultaattype(
                restBesluitWijzigenGegevens.resultaattypeUuid!!
            )
            if (!UriUtil.isEqual(zaakResultaat.resultaattype, resultaattype.url)) {
                zrcClientService.deleteResultaat(zaakResultaat.uuid)
                zgwApiService.createResultaatForZaak(
                    zaak,
                    restBesluitWijzigenGegevens.resultaattypeUuid,
                    null
                )
            }
        }
        updateBesluitInformatieobjecten(besluit, restBesluitWijzigenGegevens.informatieobjecten)
        // This event should result from a ZAAKBESLUIT CREATED notification on the ZAKEN channel
        // but open_zaak does not send that one, so emulate it here.
        eventingService.send(ScreenEventType.ZAAK_BESLUITEN.updated(zaak))
        return restBesluitConverter.convertToRESTBesluit(besluit)
    }

    private fun updateBesluitInformatieobjecten(
        besluit: Besluit?,
        nieuweDocumenten: List<UUID?>?
    ) {
        val besluitInformatieobjecten = brcClientService.listBesluitInformatieobjecten(
            besluit!!.url
        )
        val huidigeDocumenten = besluitInformatieobjecten.stream()
            .map { besluitInformatieobject ->
                UriUtil.uuidFromURI(besluitInformatieobject.informatieobject)
            }
            .toList()

        val verwijderen = CollectionUtils.subtract(huidigeDocumenten, nieuweDocumenten)
        val toevoegen = CollectionUtils.subtract(nieuweDocumenten, huidigeDocumenten)

        verwijderen.forEach(
            Consumer { teVerwijderenInformatieobject ->
                besluitInformatieobjecten.stream()
                    .filter { besluitInformatieobject ->
                        UriUtil.uuidFromURI(besluitInformatieobject.informatieobject) == teVerwijderenInformatieobject
                    }
                    .forEach { besluitInformatieobject ->
                        brcClientService.deleteBesluitinformatieobject(UriUtil.uuidFromURI(besluitInformatieobject.url))
                    }
            }
        )

        toevoegen.forEach(
            Consumer { documentUri ->
                val informatieobject = drcClientService.readEnkelvoudigInformatieobject(documentUri)
                val besluitInformatieobject = BesluitInformatieObject()
                besluitInformatieobject.informatieobject = informatieobject.url
                besluitInformatieobject.besluit = besluit.url
                brcClientService.createBesluitInformatieobject(besluitInformatieobject, WIJZIGEN_BESLUIT_TOELICHTING)
            }
        )
    }

    @PUT
    @Path("besluit/intrekken")
    fun intrekkenBesluit(
        restBesluitIntrekkenGegevens: RESTBesluitIntrekkenGegevens
    ): RESTBesluit {
        var besluit = brcClientService.readBesluit(restBesluitIntrekkenGegevens.besluitUuid)
        val zaak = zrcClientService.readZaak(besluit!!.zaak)
        assertPolicy(zaak.isOpen && policyService.readZaakRechten(zaak).behandelen)
        besluit = restBesluitConverter.convertToBesluit(besluit, restBesluitIntrekkenGegevens)
        val intrekToelichting = getIntrekToelichting(besluit.vervalreden)
        besluit = brcClientService.updateBesluit(
            besluit,
            intrekToelichting?.let { String.format(it, restBesluitIntrekkenGegevens.reden) }
        )
        // This event should result from a ZAAKBESLUIT UPDATED notification on the ZAKEN channel
        // but open_zaak does not send that one, so emulate it here.
        eventingService.send(ScreenEventType.ZAAK_BESLUITEN.updated(zaak))
        return restBesluitConverter.convertToRESTBesluit(besluit)
    }

    @GET
    @Path("besluit/{uuid}/historie")
    fun listBesluitHistorie(@PathParam("uuid") uuid: UUID?): List<RESTHistorieRegel> {
        val auditTrail: List<AuditTrailRegel> = brcClientService.listAuditTrail(uuid)
        return restHistorieRegelConverter.convert(auditTrail)
    }

    @GET
    @Path("besluittypes/{zaaktypeUUID}")
    fun listBesluittypes(
        @PathParam("zaaktypeUUID") zaaktypeUUID: UUID?
    ): List<RESTBesluittype> {
        assertPolicy(policyService.readWerklijstRechten().zakenTaken)
        val besluittypen = ztcClientService.readBesluittypen(
            ztcClientService.readZaaktype(zaaktypeUUID!!).url
        ).stream()
            .filter { besluittype: BesluitType? -> LocalDateUtil.dateNowIsBetween(besluittype) }
            .toList()
        return restBesluittypeConverter.convertToRESTBesluittypes(besluittypen)
    }

    @GET
    @Path("resultaattypes/{zaaktypeUUID}")
    fun listResultaattypes(
        @PathParam("zaaktypeUUID") zaaktypeUUID: UUID?
    ): List<RESTResultaattype> {
        assertPolicy(policyService.readWerklijstRechten().zakenTaken)
        return restResultaattypeConverter.convertResultaattypes(
            ztcClientService.readResultaattypen(ztcClientService.readZaaktype(zaaktypeUUID!!).url)
        )
    }

    @GET
    @Path("{uuid}/procesdiagram")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    fun downloadProcessDiagram(@PathParam("uuid") uuid: UUID?): Response {
        return Response.ok(bpmnService.getProcessDiagram(uuid))
            .header(
                "Content-Disposition",
                "attachment; filename=\"procesdiagram.gif\""
            )
            .build()
    }

    @GET
    @Path("procesvariabelen")
    fun listProcesVariabelen(): List<String> = ZaakVariabelenService.VARS

    private fun addBetrokkene(
        roltype: UUID,
        identificatieType: IdentificatieType,
        identificatie: String,
        toelichting: String,
        zaak: Zaak
    ) {
        val zaakRechten = policyService.readZaakRechten(zaak)
        val betrokkene = ztcClientService.readRoltype(roltype)
        when (identificatieType) {
            IdentificatieType.BSN -> {
                assertPolicy(zaakRechten.toevoegenBetrokkenePersoon)
                addBetrokkenNatuurlijkPersoon(betrokkene, identificatie, zaak, toelichting)
            }
            IdentificatieType.VN -> {
                assertPolicy(zaakRechten.toevoegenBetrokkeneBedrijf)
                addBetrokkenVestiging(betrokkene, identificatie, zaak, toelichting)
            }
            IdentificatieType.RSIN -> {
                assertPolicy(zaakRechten.toevoegenBetrokkeneBedrijf)
                addBetrokkenNietNatuurlijkPersoon(betrokkene, identificatie, zaak, toelichting)
            }
        }
    }

    private fun addBetrokkenNatuurlijkPersoon(
        roltype: RolType,
        bsn: String,
        zaak: Zaak,
        toelichting: String
    ) {
        zrcClientService.createRol(
            RolNatuurlijkPersoon(
                zaak.url,
                roltype,
                toelichting,
                NatuurlijkPersoon(bsn)
            ),
            toelichting
        )
    }

    private fun addBetrokkenVestiging(
        roltype: RolType,
        vestigingsnummer: String,
        zaak: Zaak,
        toelichting: String
    ) {
        zrcClientService.createRol(
            RolVestiging(
                zaak.url,
                roltype,
                toelichting,
                Vestiging(vestigingsnummer)
            ),
            toelichting
        )
    }

    private fun addBetrokkenNietNatuurlijkPersoon(
        roltype: RolType,
        rsin: String,
        zaak: Zaak,
        toelichting: String
    ) {
        zrcClientService.createRol(
            RolNietNatuurlijkPersoon(
                zaak.url,
                roltype,
                toelichting,
                NietNatuurlijkPersoon(rsin)
            ),
            toelichting
        )
    }

    private fun addInitiator(
        identificatieType: IdentificatieType,
        identificatie: String,
        zaak: Zaak
    ) {
        val initiator = ztcClientService.readRoltype(RolType.OmschrijvingGeneriekEnum.INITIATOR, zaak.zaaktype)
        val zaakRechten = policyService.readZaakRechten(zaak)
        when (identificatieType) {
            IdentificatieType.BSN -> {
                assertPolicy(zaakRechten.toevoegenInitiatorPersoon)
                addBetrokkenNatuurlijkPersoon(initiator, identificatie, zaak, ROL_TOEVOEGEN_REDEN)
            }
            IdentificatieType.VN -> {
                assertPolicy(zaakRechten.toevoegenBetrokkeneBedrijf)
                addBetrokkenVestiging(initiator, identificatie, zaak, ROL_TOEVOEGEN_REDEN)
            }
            IdentificatieType.RSIN -> {
                assertPolicy(zaakRechten.toevoegenBetrokkeneBedrijf)
                addBetrokkenNietNatuurlijkPersoon(initiator, identificatie, zaak, ROL_TOEVOEGEN_REDEN)
            }
        }
    }

    private fun addRelevanteZaak(
        relevanteZaken: MutableList<RelevanteZaak>?,
        andereZaak: URI,
        aardRelatie: AardRelatie
    ): List<RelevanteZaak> {
        val relevanteZaak = RelevanteZaak(andereZaak, aardRelatie)
        if (relevanteZaken != null) {
            if (relevanteZaken.stream().noneMatch { zaak -> zaak.`is`(andereZaak, aardRelatie) }) {
                relevanteZaken.add(relevanteZaak)
            }
            return relevanteZaken
        } else {
            return listOf(relevanteZaak)
        }
    }

    private fun datumWaarschuwing(vandaag: LocalDate, dagen: Int): LocalDate {
        return vandaag.plusDays(dagen + 1L)
    }

    private fun deleteSignaleringen(zaak: Zaak) {
        signaleringenService.deleteSignaleringen(
            SignaleringZoekParameters(loggedInUserInstance.get())
                .types(SignaleringType.Type.ZAAK_OP_NAAM, SignaleringType.Type.ZAAK_DOCUMENT_TOEGEVOEGD)
                .subject(zaak)
        )
    }

    private fun getIntrekToelichting(vervalreden: VervalredenEnum): String? {
        return when (vervalreden) {
            VervalredenEnum.INGETROKKEN_OVERHEID -> "Overheid: %s"
            VervalredenEnum.INGETROKKEN_BELANGHEBBENDE -> "Belanghebbende: %s"
            else -> {
                LOG.info("Unknown vervalreden: '$vervalreden'. Returning 'null'.")
                null
            }
        }
    }

    private fun ingelogdeMedewerkerToekennenAanZaak(
        @Valid toekennenGegevens: RESTZaakToekennenGegevens
    ): Zaak {
        val zaak = zrcClientService.readZaak(toekennenGegevens.zaakUUID)
        assertPolicy(zaak.isOpen && policyService.readZaakRechten(zaak).toekennen)
        zrcClientService.updateRol(
            zaak,
            zakenService.bepaalRolMedewerker(identityService.readUser(loggedInUserInstance.get().id), zaak),
            toekennenGegevens.reden
        )
        return zaak
    }

    private fun isWaarschuwing(
        zaak: Zaak,
        vandaag: LocalDate,
        einddatumGeplandWaarschuwing: Map<UUID, LocalDate>,
        uiterlijkeEinddatumAfdoeningWaarschuwing: Map<UUID, LocalDate>
    ): Boolean {
        val zaaktypeUUID = UriUtil.uuidFromURI(zaak.zaaktype)
        return (
            zaak.einddatumGepland != null &&
                isWaarschuwing(
                    vandaag,
                    zaak.einddatumGepland,
                    einddatumGeplandWaarschuwing[zaaktypeUUID]
                )
            ) ||
            isWaarschuwing(
                vandaag,
                zaak.uiterlijkeEinddatumAfdoening,
                uiterlijkeEinddatumAfdoeningWaarschuwing[zaaktypeUUID]
            )
    }

    private fun isWaarschuwing(
        vandaag: LocalDate,
        datum: LocalDate,
        datumWaarschuwing: LocalDate?
    ) = datumWaarschuwing != null &&
        !datum.isBefore(vandaag) &&
        datum.isBefore(datumWaarschuwing)

    private fun koppelHoofdEnDeelzaak(hoofdZaak: Zaak, deelZaak: Zaak) {
        zrcClientService.patchZaak(deelZaak.uuid, HoofdzaakZaakPatch(hoofdZaak.url))
        // Hiervoor wordt door open zaak alleen voor de deelzaak een notificatie verstuurd.
        // Dus zelf het ScreenEvent versturen voor de hoofdzaak!
        indexeerService.addOrUpdateZaak(hoofdZaak.uuid, false)
        eventingService.send(ScreenEventType.ZAAK.updated(hoofdZaak.uuid))
    }

    private fun koppelInboxProductaanvraag(
        zaak: Zaak,
        inboxProductaanvraag: RESTInboxProductaanvraag
    ) {
        val productaanvraagObject = objectsClientService.readObject(
            inboxProductaanvraag.productaanvraagObjectUUID
        )
        val productaanvraag = productaanvraagService.getProductaanvraag(
            productaanvraagObject
        )
        productaanvraagService.pairProductaanvraagWithZaak(productaanvraagObject, zaak.url)
        productaanvraagService.pairAanvraagPDFWithZaak(productaanvraag, zaak.url)
        productaanvraagService.pairBijlagenWithZaak(
            productaanvraag.bijlagen,
            zaak.url
        )

        // verwijder het verwerkte inbox productaanvraag item
        inboxProductaanvraagService.delete(inboxProductaanvraag.id)
        zaakVariabelenService.setZaakdata(
            zaak.uuid,
            productaanvraagService.getFormulierData(productaanvraagObject)
        )
    }

    private fun koppelRelevantezaken(
        zaak: Zaak,
        andereZaak: Zaak,
        aardRelatie: AardRelatie
    ) {
        assertPolicy(policyService.readZaakRechten(zaak).koppelenGerelateerd)
        zrcClientService.patchZaak(
            zaak.uuid,
            RelevantezaakZaakPatch(
                addRelevanteZaak(
                    zaak.relevanteAndereZaken,
                    andereZaak.url,
                    aardRelatie
                )
            )
        )
    }

    private fun ontkoppelHoofdEnDeelzaak(
        hoofdZaak: Zaak,
        deelZaak: Zaak,
        reden: String
    ) {
        zrcClientService.patchZaak(deelZaak.uuid, HoofdzaakZaakPatch(null), reden)
        // Hiervoor wordt door open zaak alleen voor de deelzaak een notificatie verstuurd.
        // Dus zelf het ScreenEvent versturen voor de hoofdzaak!
        indexeerService.addOrUpdateZaak(hoofdZaak.uuid, false)
        eventingService.send(ScreenEventType.ZAAK.updated(hoofdZaak.uuid))
    }

    private fun ontkoppelRelevantezaken(
        zaak: Zaak,
        andereZaak: Zaak,
        aardRelatie: AardRelatie,
        reden: String
    ) {
        zrcClientService.patchZaak(
            zaak.uuid,
            RelevantezaakZaakPatch(
                removeRelevanteZaak(zaak.relevanteAndereZaken, andereZaak.url, aardRelatie)
            ),
            reden
        )
    }

    private fun removeBetrokkene(zaak: Zaak, betrokkene: Rol<*>, reden: String) {
        assertPolicy(policyService.readZaakRechten(zaak).verwijderenBetrokkene)
        zrcClientService.deleteRol(betrokkene, reden)
    }

    private fun removeInitiator(zaak: Zaak, initiator: Rol<*>, reden: String) {
        assertPolicy(policyService.readZaakRechten(zaak).verwijderenInitiator)
        zrcClientService.deleteRol(initiator, reden)
    }

    private fun removeRelevanteZaak(
        relevanteZaken: MutableList<RelevanteZaak>?,
        andereZaak: URI,
        aardRelatie: AardRelatie
    ): List<RelevanteZaak>? {
        relevanteZaken?.removeAll(
            relevanteZaken.stream()
                .filter { it.`is`(andereZaak, aardRelatie) }
                .toList()
        )
        return relevanteZaken
    }

    private fun resolveZaakAfzenderMail(
        afzenders: Stream<RESTZaakAfzender>
    ): Stream<RESTZaakAfzender> {
        return afzenders.peek { afzender ->
            val speciaal = speciaalMail(afzender.mail)
            if (speciaal != null) {
                afzender.suffix = "gegevens.mail.afzender.$speciaal"
                afzender.mail = resolveMail(speciaal)
            }
            afzender.replyTo = afzender.replyTo?.let {
                speciaalMail(afzender.replyTo)?.let { speciaalReplyTo ->
                    resolveMail(speciaalReplyTo)
                } ?: afzender.replyTo
            }
        }.filter { afzender: RESTZaakAfzender -> afzender.mail != null }
    }

    private fun resolveMail(speciaal: Speciaal): String {
        return when (speciaal) {
            Speciaal.GEMEENTE -> configuratieService.readGemeenteMail()
            Speciaal.MEDEWERKER -> loggedInUserInstance.get().email
        }
    }

    private fun speciaalMail(mail: String): Speciaal? {
        if (!mail.contains("@")) {
            return ZaakAfzender.Speciaal.valueOf(mail)
        }
        return null
    }

    private fun verlengOpenTaken(zaakUUID: UUID, duurDagen: Int): Int {
        val count = IntArray(1)
        flowableTaskService.listOpenTasksForZaak(zaakUUID).stream()
            .filter { task -> task.dueDate != null }
            .forEach { task ->
                task.dueDate = DateTimeConverterUtil.convertToDate(
                    DateTimeConverterUtil.convertToLocalDate(task.dueDate).plusDays(duurDagen.toLong())
                )
                flowableTaskService.updateTask(task)
                eventingService.send(ScreenEventType.TAAK.updated(task))
                count[0]++
            }
        return count[0]
    }
}

private fun sortAndRemoveDuplicates(
    afzenders: Stream<RESTZaakAfzender>
): List<RESTZaakAfzender> {
    val list = afzenders.sorted { a, b ->
        val result: Int = a.mail.compareTo(b.mail)
        if (result == 0) if (a.defaultMail) -1 else 0 else result
    }.collect(Collectors.toList())
    val i = list.iterator()
    var previous: String? = null
    while (i.hasNext()) {
        val afzender: RESTZaakAfzender = i.next()
        if (afzender.mail == previous) {
            i.remove()
        } else {
            previous = afzender.mail
        }
    }
    return list
}
