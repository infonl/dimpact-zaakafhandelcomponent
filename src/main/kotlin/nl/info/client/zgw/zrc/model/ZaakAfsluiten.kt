package nl.info.client.zgw.zrc.model

import jakarta.json.bind.annotation.JsonbCreator
import jakarta.json.bind.annotation.JsonbProperty
import nl.info.client.zgw.zrc.model.generated.Zaak
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import java.net.URI
import java.time.Instant
import java.time.format.DateTimeFormatter

@NoArgConstructor
@AllOpen
data class ZaakAfsluiten @JsonbCreator constructor(
    @param:JsonbProperty("zaak") val zaak: Zaak,
    @param:JsonbProperty("resultaat") val resultaat: ResultaatSubRequest,
    @param:JsonbProperty("status") val status: StatusSubRequest
)

@NoArgConstructor
@AllOpen
data class ResultaatSubRequest @JsonbCreator constructor(
    /**
     * URL-referentie naar het RESULTAATTYPE (in de Catalogi API).
     */
    @param:JsonbProperty("resultaattype") val resultaattype: URI,
    /**
     * Een toelichting op wat het resultaat van de zaak inhoudt.
     */
    @param:JsonbProperty("toelichting") val toelichting: String?,
)

@NoArgConstructor
@AllOpen
data class StatusSubRequest @JsonbCreator constructor(
    /**
     * URL-referentie naar het STATUSTYPE (in de Catalogi API).
     */
    @param:JsonbProperty("statustype") val statustype: URI,
    /**
     * De datum waarop de ZAAK de status heeft verkregen.
     *
     * <date-time> e.g., "2019-08-24T14:15:22Z"
     */
    @param:JsonbProperty(
        "datumStatusGezet"
    ) val datumStatusGezet: String? = DateTimeFormatter.ISO_INSTANT.format(Instant.now()),
    /**
     * Een, voor de initiator van de zaak relevante, toelichting op de status van een zaak.
     */
    @param:JsonbProperty("statustoelichting") val statustoelichting: String?,
    /**
     * De BETROKKENE die in zijn/haar ROL in een ZAAK heeft geregistreerd dat STATUSsen in die ZAAK bereikt zijn.
     */
    @param:JsonbProperty("gezetdoor") val gezetdoor: URI? = null,

)
