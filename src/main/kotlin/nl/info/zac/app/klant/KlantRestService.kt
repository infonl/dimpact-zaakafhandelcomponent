/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.klant

import jakarta.enterprise.inject.Instance
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
import net.atos.zac.app.shared.RESTResultaat
import nl.info.client.brp.BrpClientService
import nl.info.client.brp.exception.BrpPersonNotFoundException
import nl.info.client.klant.KlantClientService
import nl.info.client.klanten.model.generated.ExpandBetrokkene
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
import nl.info.zac.app.klant.model.contactdetails.toContactDetails
import nl.info.zac.app.klant.model.contactmoment.RestContactmoment
import nl.info.zac.app.klant.model.contactmoment.RestListContactmomentenParameters
import nl.info.zac.app.klant.model.contactmoment.toRestContactMoment
import nl.info.zac.app.klant.model.klant.RestContactDetails
import nl.info.zac.app.klant.model.klant.RestRoltype
import nl.info.zac.app.klant.model.klant.toRestRoltypes
import nl.info.zac.app.klant.model.personen.RestListPersonenParameters
import nl.info.zac.app.klant.model.personen.RestPersonenParameters
import nl.info.zac.app.klant.model.personen.RestPersoon
import nl.info.zac.app.klant.model.personen.VALID_PERSONEN_QUERIES
import nl.info.zac.app.klant.model.personen.toPersonenQuery
import nl.info.zac.app.klant.model.personen.toRestPersonen
import nl.info.zac.app.klant.model.personen.toRestPersoon
import nl.info.zac.app.klant.model.personen.toRestResultaat
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.identification.IdentificationService
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import nl.info.zac.zaak.model.Betrokkenen.BETROKKENEN_ENUMSET
import org.hibernate.validator.constraints.Length
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
    val klantClientService: KlantClientService,
    val identificationService: IdentificationService,
    val loggedInUserInstance: Instance<LoggedInUser>
) {
    companion object {
        const val ZAAKTYPE_UUID_HEADER = "X-ZAAKTYPE-UUID"
    }

    @GET
    @Path("person/{temporaryPersonId}")
    fun readPersoon(
        @PathParam("temporaryPersonId") requestedTemporaryPersonId: UUID,
        @HeaderParam(ZAAKTYPE_UUID_HEADER) zaaktypeUuid: UUID? = null,
    ) = loggedInUserInstance.get()?.id.let { userName ->
        runBlocking {
            val bsn = identificationService.replaceKeyWithBsn(requestedTemporaryPersonId)
            // run the two client calls concurrently in a coroutine scope,
            // so we do not need to wait for the first call to complete
            withContext(Dispatchers.IO) {
                val klantPersoonDigitalAddresses =
                    async { klantClientService.findDigitalAddressesForNaturalPerson(bsn) }
                val brpPersoon = async {
                    brpClientService.retrievePersoon(bsn, zaaktypeUuid, userName)
                }
                klantPersoonDigitalAddresses.await().toContactDetails().let { contactDetails ->
                    brpPersoon.await()?.toRestPersoon()?.apply {
                        telefoonnummer = contactDetails.telephoneNumber
                        emailadres = contactDetails.emailAddress
                        temporaryPersonId = requestedTemporaryPersonId
                    } ?: throw BrpPersonNotFoundException("Geen persoon gevonden voor BSN '$bsn'")
                }
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

    @GET
    @Path("vestigingsprofiel/{vestigingsnummer}")
    fun readVestigingsprofiel(@PathParam("vestigingsnummer") vestigingsnummer: String): RestVestigingsprofiel =
        kvkClientService.findVestigingsprofiel(vestigingsnummer)
            ?.toRestVestigingsProfiel()
            ?: throw VestigingNotFoundException(
                "Geen vestigingsprofiel gevonden voor vestiging met vestigingsnummer '$vestigingsnummer'"
            )

    /**
     * Read a rechtspersoon by RSIN.
     *
     * This endpoint is provided for legacy reasons.
     * Prefer using the KVK number for retrieving rechtspersonen using [readRechtspersoonByKvkNummer] where possible.
     */
    @GET
    @Path("rechtspersoon/rsin/{rsin}")
    fun readRechtspersoonByRsin(@PathParam("rsin") @Length(min = 9, max = 9) rsin: String): RestBedrijf =
        kvkClientService.findRechtspersoonByRsin(rsin)
            ?.toRestBedrijf()
            ?.copy(kvkNummer = null)
            ?: throw RechtspersoonNotFoundException("Geen rechtspersoon gevonden voor RSIN '$rsin'")

    /**
     * Read a rechtspersoon by KVK number.
     */
    @GET
    @Path("rechtspersoon/kvknummer/{kvkNummer}")
    fun readRechtspersoonByKvkNummer(@PathParam("kvkNummer") @Length(min = 8, max = 8) kvkNummer: String): RestBedrijf =
        kvkClientService.findRechtspersoonByKvkNummer(kvkNummer)
            ?.toRestBedrijf()
            ?: throw RechtspersoonNotFoundException("Geen rechtspersoon gevonden voor KVK nummer '$kvkNummer'")

    @GET
    @Path("personen/parameters")
    fun personenParameters(): List<RestPersonenParameters> = VALID_PERSONEN_QUERIES

    @PUT
    @Path("personen")
    fun listPersonen(
        restListPersonenParameters: RestListPersonenParameters,
        @HeaderParam(ZAAKTYPE_UUID_HEADER) zaaktypeUuid: UUID? = null
    ): RESTResultaat<RestPersoon> =
        restListPersonenParameters.bsn
            ?.takeIf { it.isNotBlank() }
            ?.let { bsn ->
                listOfNotNull(brpClientService.retrievePersoon(bsn, zaaktypeUuid))
                    .map { it.toRestPersoon() }
                    .map { it.apply { temporaryPersonId = identificationService.replaceBsnWithKey(bsn) } }
                    .toRestResultaat()
            }
            ?: brpClientService.queryPersonen(restListPersonenParameters.toPersonenQuery(), zaaktypeUuid)
                .toRestPersonen()
                .map { it.apply { temporaryPersonId = bsn?.let(identificationService::replaceBsnWithKey) } }
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
    @Path("contactdetails/person/{temporaryPersonId}")
    fun getContactDetailsForPerson(
        @PathParam("temporaryPersonId") temporaryPersonId: UUID
    ): RestContactDetails =
        identificationService.replaceKeyWithBsn(temporaryPersonId).let { bsn ->
            klantClientService.findDigitalAddressesForNaturalPerson(bsn).toContactDetails().let {
                RestContactDetails(
                    telefoonnummer = it.telephoneNumber,
                    emailadres = it.emailAddress
                )
            }
        }

    @PUT
    @Path("contactmomenten")
    fun listContactmomenten(parameters: RestListContactmomentenParameters): RESTResultaat<RestContactmoment> {
        val number = if (parameters.bsn != null) parameters.bsn else parameters.vestigingsnummer
        // OpenKlant 2.x pages start from 1 (not 0-based). Page 0 is considered invalid number
        // we currently assume that `number` is always non-null here; this will be refactored in a future PR
        val betrokkenenWithKlantcontactList = klantClientService.listExpandBetrokkenen(number!!, parameters.page + 1)
        val klantcontactListPage = betrokkenenWithKlantcontactList
            .mapNotNull { it.expand?.hadKlantcontact }
            .map { it.toRestContactMoment(betrokkenenWithKlantcontactList.toInitiatorAsUuidStringMap()) }
        return RESTResultaat(klantcontactListPage, klantcontactListPage.size.toLong())
    }

    private fun getVestiging(vestigingsnummer: String, kvkNummer: String? = null) = runBlocking {
        // run the two client calls concurrently in a coroutine scope,
        // so we do not need to wait for the first call to complete
        withContext(Dispatchers.IO) {
            val klantVestigingDigitalAddresses =
                async {
                    // we do not support retrieving contact details for a vestiging if no KVK number was provided
                    kvkNummer?.let { klantClientService.findDigitalAddressesForVestiging(vestigingsnummer, it) }
                        ?: emptyList()
                }
            val vestiging = async { kvkClientService.findVestiging(vestigingsnummer, kvkNummer) }
            klantVestigingDigitalAddresses.await().toContactDetails().let { contactDetails ->
                vestiging.await()?.toRestBedrijf()?.apply {
                    if (kvkNummer == null) this.kvkNummer = null
                    emailadres = contactDetails.emailAddress
                    telefoonnummer = contactDetails.telephoneNumber
                } ?: throw VestigingNotFoundException(
                    "Geen vestiging gevonden voor vestiging met vestigingsnummer '$vestigingsnummer'" +
                        (kvkNummer?.let { " en KVK nummer '$it'" } ?: "")
                )
            }
        }
    }

    private fun ResultaatItem.isKoppelbaar() = this.vestigingsnummer != null || this.kvkNummer != null

    private fun List<ExpandBetrokkene>.toInitiatorAsUuidStringMap(): Map<UUID, String> =
        this.filter { it.initiator && it.expand != null && it.expand.hadKlantcontact != null }
            .associate { it.expand.hadKlantcontact.uuid to it.volledigeNaam }
}
