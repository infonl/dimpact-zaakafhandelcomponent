/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.klant

import jakarta.inject.Inject
import jakarta.inject.Singleton
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import net.atos.client.klant.KlantClientService
import net.atos.client.klant.model.ExpandBetrokkene
import net.atos.zac.app.shared.RESTResultaat
import nl.info.client.brp.BrpClientService
import nl.info.client.brp.exception.BrpPersonNotFoundException
import nl.info.client.kvk.KvkClientService
import nl.info.client.kvk.zoeken.model.generated.ResultaatItem
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.zac.app.klant.exception.RechtspersoonNotFoundException
import nl.info.zac.app.klant.exception.VestigingNotFoundException
import nl.info.zac.app.klant.model.bedrijven.RestBedrijf
import nl.info.zac.app.klant.model.bedrijven.RestListBedrijvenParameters
import nl.info.zac.app.klant.model.bedrijven.RestVestigingsprofiel
import nl.info.zac.app.klant.model.bedrijven.toKvkZoekenParameters
import nl.info.zac.app.klant.model.bedrijven.toRestBedrijf
import nl.info.zac.app.klant.model.bedrijven.toRestResultaat
import nl.info.zac.app.klant.model.bedrijven.toRestVestigingsProfiel
import nl.info.zac.app.klant.model.contactmoment.RestContactmoment
import nl.info.zac.app.klant.model.contactmoment.RestListContactmomentenParameters
import nl.info.zac.app.klant.model.contactmoment.toRestContactMoment
import nl.info.zac.app.klant.model.klant.RestContactGegevens
import nl.info.zac.app.klant.model.klant.RestRoltype
import nl.info.zac.app.klant.model.klant.toRestRoltypes
import nl.info.zac.app.klant.model.personen.RestListPersonenParameters
import nl.info.zac.app.klant.model.personen.RestPersonenParameters
import nl.info.zac.app.klant.model.personen.RestPersoon
import nl.info.zac.app.klant.model.personen.VALID_PERSONEN_QUERIES
import nl.info.zac.app.klant.model.personen.toPersonenQuery
import nl.info.zac.app.klant.model.personen.toRechtsPersonen
import nl.info.zac.app.klant.model.personen.toRestPersoon
import nl.info.zac.app.klant.model.personen.toRestResultaat
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import nl.info.zac.zaak.model.Betrokkenen.BETROKKENEN_ENUMSET
import org.hibernate.validator.constraints.Length
import java.util.UUID
import kotlin.jvm.optionals.getOrNull

@Path("klanten")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Singleton
@Suppress("TooManyFunctions")
@AllOpen
@NoArgConstructor
class KlantRestService @Inject constructor(
    val brpClientService: BrpClientService,
    val kvkClientService: KvkClientService,
    val ztcClientService: ZtcClientService,
    val klantClientService: KlantClientService
) {
    companion object {
        const val HEADER_VERWERKING = "X-Verwerking"
    }

    @GET
    @Path("persoon/{bsn}")
    fun readPersoon(
        @PathParam("bsn") @Length(min = 8, max = 9) bsn: String,
        @HeaderParam(HEADER_VERWERKING) auditEvent: String
    ) = runBlocking {
        // run the two client calls concurrently in a coroutine scope,
        // so we do not need to wait for the first call to complete
        withContext(Dispatchers.IO) {
            val klantPersoonDigitalAddresses = async { klantClientService.findDigitalAddressesByNumber(bsn) }
            val brpPersoon = async {
                brpClientService.retrievePersoon(bsn, auditEvent)
            }
            klantPersoonDigitalAddresses.await().toRestPersoon().let { klantPersoon ->
                brpPersoon.await()?.toRestPersoon()?.apply {
                    telefoonnummer = klantPersoon.telefoonnummer
                    emailadres = klantPersoon.emailadres
                } ?: throw BrpPersonNotFoundException("Geen persoon gevonden voor BSN '$bsn'")
            }
        }
    }

    @GET
    @Path("vestiging/{vestigingsnummer}")
    fun readVestigingByVestigingsnummer(
        @PathParam("vestigingsnummer") vestigingsnummer: String,
    ) = getVestiging(vestigingsnummer, null)

    @GET
    @Path("vestiging/{vestigingsnummer}/{kvkNummer}")
    fun readVestigingByVestigingsnummerAndKvkNummer(
        @PathParam("vestigingsnummer") vestigingsnummer: String,
        @PathParam("kvkNummer") kvkNummer: String
    ) = getVestiging(vestigingsnummer, kvkNummer)

    private fun getVestiging(vestigingsnummer: String, kvkNummer: String? = null) = runBlocking {
        // run the two client calls concurrently in a coroutine scope,
        // so we do not need to wait for the first call to complete
        withContext(Dispatchers.IO) {
            val klantVestigingDigitalAddresses =
                async { klantClientService.findDigitalAddressesByNumber(vestigingsnummer) }
            val vestiging = async { kvkClientService.findVestiging(vestigingsnummer, kvkNummer) }
            klantVestigingDigitalAddresses.await().toRestPersoon().let { klantVestigingRestPersoon ->
                vestiging.await().getOrNull()?.toRestBedrijf()?.apply {
                    emailadres = klantVestigingRestPersoon.emailadres
                    telefoonnummer = klantVestigingRestPersoon.telefoonnummer
                } ?: throw VestigingNotFoundException(
                    "Geen vestiging gevonden voor vestiging met vestigingsnummer '$vestigingsnummer'"
                )
            }
        }
    }

    @GET
    @Path("vestigingsprofiel/{vestigingsnummer}")
    fun readVestigingsprofiel(@PathParam("vestigingsnummer") vestigingsnummer: String): RestVestigingsprofiel =
        kvkClientService.findVestigingsprofiel(vestigingsnummer).let {
            if (it.isPresent) {
                it.get().toRestVestigingsProfiel()
            } else {
                throw VestigingNotFoundException(
                    "Geen vestigingsprofiel gevonden voor vestiging met vestigingsnummer '$vestigingsnummer'"
                )
            }
        }

    @GET
    @Path("rechtspersoon/{rsin}")
    fun readRechtspersoon(@PathParam("rsin") @Length(min = 9, max = 9) rsin: String): RestBedrijf =
        kvkClientService.findRechtspersoon(rsin)
            .map { it.toRestBedrijf() }
            .orElseThrow { RechtspersoonNotFoundException("Geen rechtspersoon gevonden voor RSIN '$rsin'") }

    @GET
    @Path("personen/parameters")
    fun personenParameters(): List<RestPersonenParameters> = VALID_PERSONEN_QUERIES

    @PUT
    @Path("personen")
    fun listPersonen(
        @HeaderParam(HEADER_VERWERKING) auditEvent: String,
        restListPersonenParameters: RestListPersonenParameters
    ): RESTResultaat<RestPersoon> =
        restListPersonenParameters.bsn
            ?.takeIf { it.isNotBlank() }
            ?.let { bsn ->
                listOfNotNull(brpClientService.retrievePersoon(bsn, auditEvent))
                    .map { it.toRestPersoon() }
                    .toRestResultaat()
            }
            ?: brpClientService.queryPersonen(
                restListPersonenParameters.toPersonenQuery(),
                auditEvent
            ).toRechtsPersonen()
                .toRestResultaat()

    @PUT
    @Path("bedrijven")
    fun listBedrijven(restListBedrijvenParameters: RestListBedrijvenParameters): RESTResultaat<RestBedrijf> =
        kvkClientService.search(restListBedrijvenParameters.toKvkZoekenParameters()).resultaten
            .filter { it.isKoppelbaar() }
            .map { it.toRestBedrijf() }
            .toRestResultaat()

    @GET
    @Path("roltype/{zaaktypeUuid}/betrokkene")
    fun listBetrokkeneRoltypen(@PathParam("zaaktypeUuid") zaaktype: UUID): List<RestRoltype> =
        ztcClientService.listRoltypen(ztcClientService.readZaaktype(zaaktype).url)
            .filter { BETROKKENEN_ENUMSET.contains(it.omschrijvingGeneriek) }
            .sortedBy { it.omschrijving }
            .toRestRoltypes()

    @GET
    @Path("roltype")
    fun listRoltypen(): List<RestRoltype> =
        ztcClientService.listRoltypen().sortedBy { it.omschrijving }.toRestRoltypes()

    @GET
    @Path("contactgegevens/{initiatorIdentificatie}")
    fun ophalenContactGegevens(
        @PathParam("initiatorIdentificatie") initiatorIdentificatie: String
    ): RestContactGegevens =
        klantClientService.findDigitalAddressesByNumber(initiatorIdentificatie).toRestPersoon().let {
            RestContactGegevens(
                telefoonnummer = it.telefoonnummer,
                emailadres = it.emailadres
            )
        }

    @PUT
    @Path("contactmomenten")
    fun listContactmomenten(parameters: RestListContactmomentenParameters): RESTResultaat<RestContactmoment> {
        val nummer = if (parameters.bsn != null) parameters.bsn else parameters.vestigingsnummer
        // OpenKlant 2.1 pages start from 1 (not 0-based). Page 0 is considered invalid number
        val pageNumber = parameters.page!! + 1
        val betrokkenenWithKlantcontactList = klantClientService.listBetrokkenenByNumber(nummer, pageNumber)
        val klantcontactListPage = betrokkenenWithKlantcontactList.mapNotNull { it.expand?.hadKlantcontact }
            .map { it.toRestContactMoment(betrokkenenWithKlantcontactList.toInitiatorAsUuidStringMap()) }
        return RESTResultaat(klantcontactListPage, klantcontactListPage.size.toLong())
    }

    private fun ResultaatItem.isKoppelbaar() = this.vestigingsnummer != null || this.rsin != null

    private fun List<ExpandBetrokkene>.toInitiatorAsUuidStringMap(): Map<UUID, String> =
        this.filter { it.initiator && it.expand != null && it.expand.hadKlantcontact != null }
            .associate { it.expand.hadKlantcontact.uuid to it.volledigeNaam }
}
