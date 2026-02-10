/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.configuratie

import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.context.Initialized
import jakarta.enterprise.event.Observes
import jakarta.inject.Inject
import jakarta.persistence.EntityManager
import jakarta.transaction.Transactional
import jakarta.ws.rs.core.UriBuilder
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.model.CatalogusListParameters
import nl.info.zac.configuratie.model.Taal
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import nl.info.zac.util.validateRSIN
import org.eclipse.microprofile.config.inject.ConfigProperty
import java.net.URI
import java.util.Optional
import java.util.UUID
import java.util.logging.Logger

@ApplicationScoped
@Transactional
@AllOpen
@NoArgConstructor
@Suppress("LongParameterList", "TooManyFunctions")
class ConfiguratieService @Inject constructor(
    private val brpConfiguration: BrpConfiguration,

    private val entityManager: EntityManager,

    private val ztcClientService: ZtcClientService,

    @ConfigProperty(name = ENV_VAR_ADDITIONAL_ALLOWED_FILE_TYPES)
    private val additionalAllowedFileTypes: Optional<String>,

    @ConfigProperty(name = ENV_VAR_BRON_ORGANISATIE_RSIN)
    private val bronOrganisatie: String,

    @ConfigProperty(name = ENV_VAR_CATALOGUS_DOMEIN, defaultValue = "ALG")
    private val catalogusDomein: String,

    /**
     * Base URL of the zaakafhandelcomponent: protocol, host, port and context (no trailing slash)
     */
    @ConfigProperty(name = ENV_VAR_CONTEXT_URL)
    private val contextUrl: String,

    @ConfigProperty(name = ENV_VAR_GEMEENTE_CODE)
    private val gemeenteCode: String,

    @ConfigProperty(name = ENV_VAR_GEMEENTE_NAAM)
    private val gemeenteNaam: String,

    @ConfigProperty(name = ENV_VAR_GEMEENTE_MAIL)
    private val gemeenteMail: String,

    @ConfigProperty(name = ENV_VAR_FEATURE_FLAG_PABC_INTEGRATION)
    private val pabcIntegration: Boolean,

    @ConfigProperty(name = ENV_VAR_VERANTWOORDELIJKE_ORGANISATIE_RSIN)
    private val verantwoordelijkeOrganisatie: String,

    @ConfigProperty(name = ENV_VAR_ZGW_API_CLIENT_MP_REST_URL)
    private val zgwApiClientMpRestUrl: String
) {
    companion object {
        const val ENV_VAR_ADDITIONAL_ALLOWED_FILE_TYPES = "ADDITIONAL_ALLOWED_FILE_TYPES"
        const val ENV_VAR_BRON_ORGANISATIE_RSIN = "BRON_ORGANISATIE_RSIN"
        const val ENV_VAR_VERANTWOORDELIJKE_ORGANISATIE_RSIN = "VERANTWOORDELIJKE_ORGANISATIE_RSIN"
        const val ENV_VAR_CATALOGUS_DOMEIN = "CATALOGUS_DOMEIN"
        const val ENV_VAR_CONTEXT_URL = "CONTEXT_URL"
        const val ENV_VAR_FEATURE_FLAG_PABC_INTEGRATION = "FEATURE_FLAG_PABC_INTEGRATION"
        const val ENV_VAR_GEMEENTE_MAIL = "GEMEENTE_MAIL"
        const val ENV_VAR_GEMEENTE_NAAM = "GEMEENTE_NAAM"
        const val ENV_VAR_GEMEENTE_CODE = "GEMEENTE_CODE"
        const val ENV_VAR_ZGW_API_CLIENT_MP_REST_URL = "ZGW_API_CLIENT_MP_REST_URL"

        const val OMSCHRIJVING_TAAK_DOCUMENT = "taak-document"
        const val OMSCHRIJVING_VOORWAARDEN_GEBRUIKSRECHTEN = "geen"

        /**
         * ISO 639-2/B language code for Dutch.
         */
        const val TAAL_NEDERLANDS = "dut"

        const val STATUSTYPE_OMSCHRIJVING_HEROPEND = "Heropend"
        const val STATUSTYPE_OMSCHRIJVING_INTAKE = "Intake"
        const val STATUSTYPE_OMSCHRIJVING_IN_BEHANDELING = "In behandeling"
        const val STATUSTYPE_OMSCHRIJVING_AANVULLENDE_INFORMATIE = "Wacht op aanvullende informatie"
        const val STATUSTYPE_OMSCHRIJVING_AFGEROND = "Afgerond"

        /**
         * Zaak communicatiekanaal used when creating zaken from Dimpact productaanvragen.
         * This communicatiekanaal always needs to be available.
         */
        const val COMMUNICATIEKANAAL_EFORMULIER = "E-formulier"

        const val INFORMATIEOBJECTTYPE_OMSCHRIJVING_EMAIL = "e-mail"

        /**
         * Maximum file size in MB for file uploads.
         *
         * Note that WildFly / RESTEasy also defines a max file upload size.
         * The value used in WildFly configuration should be set higher to account for overhead. (e.g. 80MB -> 120MB).
         * We use the Base2 system to calculate the max file size in bytes.
         */
        const val MAX_FILE_SIZE_MB: Int = 80

        private val LOG = Logger.getLogger(ConfiguratieService::class.java.name)
    }

    private lateinit var catalogusURI: URI

    fun onStartup(@Observes @Initialized(ApplicationScoped::class) @Suppress("UNUSED_PARAMETER") event: Any) {
        LOG.info {
            "ZAC configuration environment variables:\n" +
                "- $ENV_VAR_ADDITIONAL_ALLOWED_FILE_TYPES: '${additionalAllowedFileTypes.orElse("")}'\n" +
                "- $ENV_VAR_BRON_ORGANISATIE_RSIN: '$bronOrganisatie'\n" +
                "- $ENV_VAR_VERANTWOORDELIJKE_ORGANISATIE_RSIN: '$verantwoordelijkeOrganisatie'\n" +
                "- $ENV_VAR_CATALOGUS_DOMEIN: '$catalogusDomein'\n" +
                "- $ENV_VAR_CONTEXT_URL: '$contextUrl'\n" +
                "- $ENV_VAR_FEATURE_FLAG_PABC_INTEGRATION: '$pabcIntegration'\n" +
                "- $ENV_VAR_GEMEENTE_CODE: '$gemeenteCode'\n" +
                "- $ENV_VAR_GEMEENTE_MAIL: '$gemeenteMail'\n" +
                "- $ENV_VAR_GEMEENTE_NAAM: '$gemeenteNaam'\n" +
                "- $ENV_VAR_ZGW_API_CLIENT_MP_REST_URL: '$zgwApiClientMpRestUrl'\n" +
                "- BRP configuration: $brpConfiguration\n"
        }
        bronOrganisatie.validateRSIN(ENV_VAR_BRON_ORGANISATIE_RSIN)
        verantwoordelijkeOrganisatie.validateRSIN(ENV_VAR_VERANTWOORDELIJKE_ORGANISATIE_RSIN)
        catalogusURI = ztcClientService.readCatalogus(CatalogusListParameters().apply { domein = catalogusDomein }).url
    }

    fun listTalen(): List<Taal> {
        val query = entityManager.criteriaBuilder.createQuery(Taal::class.java)
        val root = query.from(Taal::class.java)
        query.orderBy(entityManager.criteriaBuilder.asc(root.get<Any>("naam")))
        return entityManager.createQuery(query).resultList
    }

    fun findDefaultTaal(): Taal? = findTaal(TAAL_NEDERLANDS)

    fun findTaal(code: String): Taal? {
        val query = entityManager.criteriaBuilder.createQuery(Taal::class.java)
        val root = query.from(Taal::class.java)
        query.where(entityManager.criteriaBuilder.equal(root.get<Any>("code"), code))
        val talen = entityManager.createQuery(query).resultList
        return talen.firstOrNull()
    }

    fun featureFlagPabcIntegration(): Boolean = pabcIntegration

    fun readMaxFileSizeMB() = MAX_FILE_SIZE_MB.toLong()

    fun readAdditionalAllowedFileTypes(): List<String> =
        if (additionalAllowedFileTypes.isEmpty) {
            emptyList()
        } else {
            additionalAllowedFileTypes.get().split(",").filter { it.isNotEmpty() }
        }

    fun readDefaultCatalogusURI(): URI = catalogusURI

    fun zaakTonenUrl(zaakIdentificatie: String): URI =
        UriBuilder.fromUri(contextUrl).path("zaken/{zaakIdentificatie}").build(zaakIdentificatie)

    fun taakTonenUrl(taakId: String): URI =
        UriBuilder.fromUri(contextUrl).path("taken/{taakId}").build(taakId)

    fun informatieobjectTonenUrl(enkelvoudigInformatieobjectUUID: UUID): URI =
        UriBuilder
            .fromUri(contextUrl)
            .path("informatie-objecten/{enkelvoudigInformatieobjectUUID}")
            .build(enkelvoudigInformatieobjectUUID.toString())

    fun readContextUrl(): String = contextUrl

    fun readGemeenteCode(): String = gemeenteCode

    fun readGemeenteNaam(): String = gemeenteNaam

    fun readGemeenteMail(): String = gemeenteMail

    fun readZgwApiClientMpRestUrl(): String = zgwApiClientMpRestUrl

    fun readBronOrganisatie(): String = bronOrganisatie

    fun readVerantwoordelijkeOrganisatie(): String = verantwoordelijkeOrganisatie

    fun readCatalogusDomein(): String = catalogusDomein

    fun readBrpConfiguration(): BrpConfiguration = brpConfiguration
}
