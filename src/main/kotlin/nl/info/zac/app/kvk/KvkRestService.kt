package nl.info.zac.app.kvk

import jakarta.inject.Inject
import jakarta.validation.Valid
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import nl.info.client.kvk.KvkSearchClient
import nl.info.client.kvk.model.KvkSearchParameters
import nl.info.client.kvk.zoeken.model.generated.Resultaat
import nl.info.zac.app.klant.model.klant.IdentificatieType
import nl.info.zac.app.zaak.model.BetrokkeneIdentificatie
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor

@AllOpen
@Path("kvk")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@NoArgConstructor
class KvkRestService @Inject constructor(
    private val kvkSearchClient: KvkSearchClient
) {
    @GET
    @Path("search/{kvkNummer}")
    fun getKvkInfoByKvkNumber(@PathParam("kvkNummer") kvkNumber: String): Resultaat? {
        val betrokkene = BetrokkeneIdentificatie(IdentificatieType.VN).apply {
            this.kvkNummer = kvkNumber
        }
        return searchBetrokkeneIdentificatieInKvK(betrokkene)
    }

    @GET
    @Path("search/{kvkNummer}/{vestigingsnummer}")
    fun getKvkInfo(
        @PathParam("kvkNummer") kvkNumber: String,
        @PathParam("vestigingsnummer") vestigingsnummer: String
    ): Resultaat? {
        val betrokkene = BetrokkeneIdentificatie(IdentificatieType.VN).apply {
            this.kvkNummer = kvkNumber
            this.vestigingsnummer = vestigingsnummer
        }
        return searchBetrokkeneIdentificatieInKvK(betrokkene)
    }

    @GET
    @Path("search")
    fun getKvkInfoByBetrokkene(@Valid involved: List<BetrokkeneIdentificatie>): List<Resultaat> {
        return involved.mapNotNull(this::searchBetrokkeneIdentificatieInKvK)
    }

    private fun searchBetrokkeneIdentificatieInKvK(betrokkene: BetrokkeneIdentificatie): Resultaat? {
        val searchParameter = KvkSearchParameters().apply {
            this.kvkNummer = betrokkene.kvkNummer
            this.vestigingsnummer = betrokkene.vestigingsnummer
            this.rsin = betrokkene.rsin // Legacy support for RSIN
        }
        val result = kvkSearchClient.getResults(searchParameter)
        return if (result.totaal != null && result.totaal!! > 0) result else null
    }
}
