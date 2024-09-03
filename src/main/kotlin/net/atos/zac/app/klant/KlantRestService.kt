/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.klant

import jakarta.inject.Inject
import jakarta.inject.Singleton
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import net.atos.client.brp.BrpClientService
import net.atos.client.brp.exception.BrpPersonNotFoundException
import net.atos.client.klant.KlantClientService
import net.atos.client.klant.model.ExpandBetrokkene
import net.atos.client.kvk.KvkClientService
import net.atos.client.kvk.zoeken.model.generated.ResultaatItem
import net.atos.client.zgw.ztc.ZtcClientService
import net.atos.client.zgw.ztc.model.generated.OmschrijvingGeneriekEnum
import net.atos.zac.app.klant.exception.RechtspersoonNotFoundException
import net.atos.zac.app.klant.exception.VestigingNotFoundException
import net.atos.zac.app.klant.model.bedrijven.RestBedrijf
import net.atos.zac.app.klant.model.bedrijven.RestListBedrijvenParameters
import net.atos.zac.app.klant.model.bedrijven.RestVestigingsprofiel
import net.atos.zac.app.klant.model.bedrijven.toKvkZoekenParameters
import net.atos.zac.app.klant.model.bedrijven.toRestBedrijf
import net.atos.zac.app.klant.model.bedrijven.toRestResultaat
import net.atos.zac.app.klant.model.bedrijven.toRestVestigingsProfiel
import net.atos.zac.app.klant.model.contactmoment.RestContactmoment
import net.atos.zac.app.klant.model.contactmoment.RestListContactmomentenParameters
import net.atos.zac.app.klant.model.contactmoment.toRestContactMoment
import net.atos.zac.app.klant.model.klant.RestContactGegevens
import net.atos.zac.app.klant.model.klant.RestRoltype
import net.atos.zac.app.klant.model.klant.toRestRoltypes
import net.atos.zac.app.klant.model.personen.RestListPersonenParameters
import net.atos.zac.app.klant.model.personen.RestPersonenParameters
import net.atos.zac.app.klant.model.personen.RestPersoon
import net.atos.zac.app.klant.model.personen.VALID_PERSONEN_QUERIES
import net.atos.zac.app.klant.model.personen.toPersonenQuery
import net.atos.zac.app.klant.model.personen.toRechtsPersonen
import net.atos.zac.app.klant.model.personen.toRestPersoon
import net.atos.zac.app.klant.model.personen.toRestResultaat
import net.atos.zac.app.shared.RESTResultaat
import nl.lifely.zac.util.AllOpen
import nl.lifely.zac.util.NoArgConstructor
import org.hibernate.validator.constraints.Length
import java.util.EnumSet
import java.util.Objects
import java.util.UUID

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
        val betrokkenen: EnumSet<OmschrijvingGeneriekEnum> =
            EnumSet.allOf(OmschrijvingGeneriekEnum::class.java).apply {
                this.removeAll(
                    listOf(
                        OmschrijvingGeneriekEnum.INITIATOR,
                        OmschrijvingGeneriekEnum.BEHANDELAAR
                    )
                )
            }

        const val TELEFOON_SOORT_DIGITAAL_ADRES = "telefoon"
        const val EMAIL_SOORT_DIGITAAL_ADRES = "email"
    }

    @GET
    @Path("persoon/{bsn}")
    fun readPersoon(
        @PathParam("bsn") @Length(min = 8, max = 9) bsn: String
    ) = runBlocking {
        withContext(Dispatchers.IO) {
            // run the two client calls concurrently in a coroutine scope so we do not need to wait for the first call to complete
            val digitalAddresses = async { klantClientService.findDigitalAddressesByNumber(bsn) }
            val brpPersoon = async { brpClientService.retrievePersoon(bsn) }
            digitalAddresses.await().toRestPersoon().let { klantPersoon ->
                brpPersoon.await()?.toRestPersoon()?.apply {
                    telefoonnummer = klantPersoon.telefoonnummer
                    emailadres = klantPersoon.emailadres
                } ?: throw BrpPersonNotFoundException("Geen persoon gevonden voor BSN '$bsn'")
            }
        }
    }

    @GET
    @Path("vestiging/{vestigingsnummer}")
    fun readVestiging(
        @PathParam("vestigingsnummer") vestigingsnummer: String
    ): RestBedrijf = klantClientService.findDigitalAddressesByNumber(vestigingsnummer)
        .toRestPersoon().let { klantRestPersoon ->
            // note that we currently explicitly wait here for the asynchronous client invocation to complete
            // thereby blocking the request thread
            kvkClientService.findVestigingAsync(vestigingsnummer).toCompletableFuture().get()
                .map {
                    it.toRestBedrijf().apply {
                        emailadres = klantRestPersoon.emailadres
                        telefoonnummer = klantRestPersoon.telefoonnummer
                    }
                }.orElseThrow {
                    VestigingNotFoundException(
                        "Geen vestiging gevonden voor vestiging met vestigingsnummer '$vestigingsnummer'"
                    )
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
    fun listPersonen(restListPersonenParameters: RestListPersonenParameters): RESTResultaat<RestPersoon> =
        brpClientService.queryPersonen(restListPersonenParameters.toPersonenQuery())
            .toRechtsPersonen()
            .toRestResultaat()

    @PUT
    @Path("bedrijven")
    fun listBedrijven(restParameters: RestListBedrijvenParameters): RESTResultaat<RestBedrijf> =
        kvkClientService.list(restParameters.toKvkZoekenParameters()).resultaten
            .filter { it.isKoppelbaar() }
            .map { it.toRestBedrijf() }
            .toRestResultaat()

    @GET
    @Path("roltype/{zaaktypeUuid}/betrokkene")
    fun listBetrokkeneRoltypen(@PathParam("zaaktypeUuid") zaaktype: UUID): List<RestRoltype> =
        ztcClientService.listRoltypen(ztcClientService.readZaaktype(zaaktype).url)
            .filter { betrokkenen.contains(it.omschrijvingGeneriek) }
            .sortedBy { it.omschrijving }
            .toRestRoltypes()

    @GET
    @Path("roltype")
    fun listRoltypen(): List<RestRoltype> = ztcClientService.listRoltypen().sortedBy { it.omschrijving }.toRestRoltypes()

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
        val klantcontactListPage = betrokkenenWithKlantcontactList.toInitiatorAsUuidStringMap().let { map ->
            betrokkenenWithKlantcontactList
                .map { it.expand }
                .filter { Objects.nonNull(it) }
                .map { it.hadKlantcontact }
                .map { it.toRestContactMoment(map) }
                .toList()
        }
        return RESTResultaat(klantcontactListPage, klantcontactListPage.size.toLong())
    }

    private fun ResultaatItem.isKoppelbaar() = this.vestigingsnummer != null || this.rsin != null

    private fun List<ExpandBetrokkene>.toInitiatorAsUuidStringMap(): Map<UUID, String> =
        this.filter { it.initiator }
            .associate { it.expand.hadKlantcontact.uuid to it.volledigeNaam }
}
