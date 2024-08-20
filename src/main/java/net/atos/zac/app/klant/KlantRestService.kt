/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.klant

import jakarta.inject.Inject
import jakarta.inject.Singleton
import jakarta.validation.constraints.Size
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.NotFoundException
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import net.atos.client.brp.BrpClientService
import net.atos.client.brp.model.generated.Persoon
import net.atos.client.klant.KlantClientService
import net.atos.client.klant.model.DigitaalAdres
import net.atos.client.kvk.KvkClientService
import net.atos.client.kvk.model.KvkZoekenParameters
import net.atos.client.kvk.zoeken.model.generated.ResultaatItem
import net.atos.client.zgw.ztc.ZtcClientService
import net.atos.client.zgw.ztc.model.generated.OmschrijvingGeneriekEnum
import net.atos.zac.app.klant.converter.RestVestigingsprofielConverter
import net.atos.zac.app.klant.converter.RestVestigingsprofielConverter.convert
import net.atos.zac.app.klant.converter.VALID_PERSONEN_QUERIES
import net.atos.zac.app.klant.converter.convertFromPersonenQueryResponse
import net.atos.zac.app.klant.converter.convertPersoon
import net.atos.zac.app.klant.converter.convertToPersonenQuery
import net.atos.zac.app.klant.converter.mapContactToInitiatorFullName
import net.atos.zac.app.klant.converter.toKvkZoekenParameters
import net.atos.zac.app.klant.converter.toRestBedrijf
import net.atos.zac.app.klant.converter.toRestContactMoment
import net.atos.zac.app.klant.converter.toRestRoltypes
import net.atos.zac.app.klant.model.bedrijven.RestBedrijf
import net.atos.zac.app.klant.model.bedrijven.RestListBedrijvenParameters
import net.atos.zac.app.klant.model.bedrijven.RestVestigingsprofiel
import net.atos.zac.app.klant.model.contactmoment.RESTContactmoment
import net.atos.zac.app.klant.model.contactmoment.RESTListContactmomentenParameters
import net.atos.zac.app.klant.model.klant.IdentificatieType
import net.atos.zac.app.klant.model.klant.RestContactGegevens
import net.atos.zac.app.klant.model.klant.RestKlant
import net.atos.zac.app.klant.model.klant.RestRoltype
import net.atos.zac.app.klant.model.personen.RestListPersonenParameters
import net.atos.zac.app.klant.model.personen.RestPersonenParameters
import net.atos.zac.app.klant.model.personen.RestPersoon
import net.atos.zac.app.shared.RESTResultaat
import nl.lifely.zac.util.AllOpen
import nl.lifely.zac.util.NoArgConstructor
import java.util.EnumSet
import java.util.Objects
import java.util.Optional
import java.util.UUID
import java.util.concurrent.ExecutionException

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
                this.remove(OmschrijvingGeneriekEnum.INITIATOR)
                this.remove(OmschrijvingGeneriekEnum.BEHANDELAAR)
            }

        const val TELEFOON_SOORT_DIGITAAL_ADRES: String = "telefoon"
        const val EMAIL_SOORT_DIGITAAL_ADRES: String = "email"
    }

    @GET
    @Path("persoon/{bsn}")
    @Throws(ExecutionException::class, InterruptedException::class)
    fun readPersoon(
        @PathParam("bsn") bsn:
        @Size(min = 8, max = 9)
        String
    ): RestPersoon = convertToRestPersoon(
        // note that we currently explicitly wait here for the asynchronous client invocation to complete
        // thereby blocking the request thread
        brpClientService.retrievePersoonAsync(bsn).toCompletableFuture().get(),
        convertToRestPersoon(klantClientService.findDigitalAddressesByNumber(bsn))
    )

    @GET
    @Path("vestiging/{vestigingsnummer}")
    @Throws(ExecutionException::class, InterruptedException::class)
    fun readVestiging(
        @PathParam("vestigingsnummer") vestigingsnummer: String
    ): RestBedrijf = convertToRestBedrijf(
        // note that we currently explicitly wait here for the asynchronous client invocation to complete
        // thereby blocking the request thread
        kvkClientService.findVestigingAsync(vestigingsnummer).toCompletableFuture().get(),
        convertToRestPersoon(klantClientService.findDigitalAddressesByNumber(vestigingsnummer))
    )

    @GET
    @Path("vestigingsprofiel/{vestigingsnummer}")
    fun readVestigingsprofiel(@PathParam("vestigingsnummer") vestigingsnummer: String): RestVestigingsprofiel {
        val vestiging = kvkClientService.findVestigingsprofiel(vestigingsnummer)
        if (vestiging.isPresent) {
            return RestVestigingsprofielConverter.convert(vestiging.get())
        } else {
            throw NotFoundException(
                "Geen vestigingsprofiel gevonden voor vestiging met vestigingsnummer '$vestigingsnummer'"
            )
        }
    }

    @GET
    @Path("rechtspersoon/{rsin}")
    fun readRechtspersoon(@PathParam("rsin") rsin: String): RestBedrijf =
        kvkClientService.findRechtspersoon(rsin)
            .map { it.toRestBedrijf() }
            .orElseGet { RestBedrijf() }

    @GET
    @Path("personen/parameters")
    fun personenParameters(): List<RestPersonenParameters> = VALID_PERSONEN_QUERIES

    @PUT
    @Path("personen")
    fun listPersonen(restListPersonenParameters: RestListPersonenParameters): RESTResultaat<RestPersoon> {
        val query = convertToPersonenQuery(restListPersonenParameters)
        val response = brpClientService.queryPersonen(query)
        return RESTResultaat(convertFromPersonenQueryResponse(response))
    }

    @PUT
    @Path("bedrijven")
    fun listBedrijven(restParameters: RestListBedrijvenParameters): RESTResultaat<RestBedrijf> =
        RESTResultaat(
            kvkClientService.list(toKvkZoekenParameters(restParameters)).resultaten
                .filter { isKoppelbaar(it) }
                .map { it.toRestBedrijf() }
                .toList()
        )

    @GET
    @Path("roltype/{zaaktypeUuid}/betrokkene")
    fun listBetrokkeneRoltypen(@PathParam("zaaktypeUuid") zaaktype: UUID): List<RestRoltype> =
        toRestRoltypes(
            ztcClientService.listRoltypen(ztcClientService.readZaaktype(zaaktype).url)
                .filter { betrokkenen.contains(it.omschrijvingGeneriek) }
                .sortedBy { it.omschrijving }
        )

    @GET
    @Path("roltype")
    fun listRoltypen(): List<RestRoltype> =
        toRestRoltypes(ztcClientService.listRoltypen().sortedBy { it.omschrijving })

    @GET
    @Path("contactgegevens/{identificatieType}/{initiatorIdentificatie}")
    fun ophalenContactGegevens(
        @PathParam("identificatieType") identificatieType: IdentificatieType,
        @PathParam("initiatorIdentificatie") initiatorIdentificatie: String
    ): RestContactGegevens {
        val restContactGegevens = RestContactGegevens()
        val klantPersoon = convertToRestPersoon(
            klantClientService.findDigitalAddressesByNumber(initiatorIdentificatie)
        )
        restContactGegevens.telefoonnummer = klantPersoon.telefoonnummer
        restContactGegevens.emailadres = klantPersoon.emailadres
        return restContactGegevens
    }

    @PUT
    @Path("contactmomenten")
    fun listContactmomenten(parameters: RESTListContactmomentenParameters): RESTResultaat<RESTContactmoment> {
        val nummer = if (parameters.bsn != null) parameters.bsn else parameters.vestigingsnummer
        // OpenKlant 2.1 pages start from 1 (not 0-based). Page 0 is considered invalid number
        val pageNumber = parameters.page!! + 1
        val betrokkenenWithKlantcontactList = klantClientService.listBetrokkenenByNumber(nummer, pageNumber)
        val contactToFullNameMap = mapContactToInitiatorFullName(betrokkenenWithKlantcontactList)
        val klantcontactListPage = betrokkenenWithKlantcontactList
            .map { it.expand }
            .filter { Objects.nonNull(it) }
            .map { it.hadKlantcontact }
            .map { it.toRestContactMoment(contactToFullNameMap) }
            .toList()

        return RESTResultaat(klantcontactListPage, klantcontactListPage.size.toLong())
    }

    private fun addKlantData(restKlant: RestKlant, restPersoon: RestPersoon): RestKlant {
        restKlant.telefoonnummer = restPersoon.telefoonnummer
        restKlant.emailadres = restPersoon.emailadres
        return restKlant
    }

    private fun convertToRestBedrijf(vestiging: Optional<ResultaatItem>, klantPerson: RestPersoon): RestBedrijf {
        return vestiging
            .map { it.toRestBedrijf() }
            .map { addKlantData(it, klantPerson) as RestBedrijf }
            .orElseGet { RestBedrijf() }
    }

    private fun convertToRestPersoon(persoon: Persoon, klantPersoon: RestPersoon): RestPersoon {
        val restPersoon = convertPersoon(persoon)
        return addKlantData(restPersoon, klantPersoon) as RestPersoon
    }

    private fun convertToRestPersoon(digitalAddresses: List<DigitaalAdres>?): RestPersoon {
        val restPersoon = RestPersoon()
        if (digitalAddresses != null) {
            for (digitalAdress in digitalAddresses) {
                when (digitalAdress.soortDigitaalAdres) {
                    TELEFOON_SOORT_DIGITAAL_ADRES -> restPersoon.telefoonnummer = digitalAdress.adres
                    EMAIL_SOORT_DIGITAAL_ADRES -> restPersoon.emailadres = digitalAdress.adres
                }
            }
        }
        return restPersoon
    }

    private fun isKoppelbaar(item: ResultaatItem): Boolean {
        return item.vestigingsnummer != null || item.rsin != null
    }
}
