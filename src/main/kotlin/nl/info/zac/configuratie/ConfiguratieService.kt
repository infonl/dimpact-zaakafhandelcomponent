/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.configuratie

import jakarta.enterprise.context.ApplicationScoped
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

@ApplicationScoped
@Transactional
@AllOpen
@NoArgConstructor
@Suppress("LongParameterList", "TooManyFunctions")
class ConfiguratieService @Inject constructor(
    private val entityManager: EntityManager,

    ztcClientService: ZtcClientService,

    @ConfigProperty(name = "ADDITIONAL_ALLOWED_FILE_TYPES")
    private val additionalAllowedFileTypes: Optional<String>,

    @ConfigProperty(name = ENV_VAR_ZGW_API_CLIENT_MP_REST_URL)
    private val zgwApiClientMpRestUrl: String,

    /**
     * Base URL of the zaakafhandelcomponent: protocol, host, port and context (no trailing slash)
     */
    @ConfigProperty(name = "CONTEXT_URL")
    private val contextUrl: String,

    @ConfigProperty(name = "GEMEENTE_CODE")
    private val gemeenteCode: String,

    @ConfigProperty(name = "GEMEENTE_NAAM")
    private val gemeenteNaam: String,

    @ConfigProperty(name = "GEMEENTE_MAIL")
    private val gemeenteMail: String,

    @ConfigProperty(name = "FEATURE_FLAG_BPMN_SUPPORT")
    private val bpmnSupport: Boolean,

    @ConfigProperty(name = "FEATURE_FLAG_PABC_INTEGRATION")
    private val pabcIntegration: Boolean,

    @ConfigProperty(name = "BRON_ORGANISATIE_RSIN")
    private val bronOrganisatie: String,

    @ConfigProperty(name = "VERANTWOORDELIJKE_ORGANISATIE_RSIN")
    private val verantwoordelijkeOrganisatie: String,

    @ConfigProperty(name = "CATALOGUS_DOMEIN", defaultValue = "ALG")
    private val catalogusDomein: String,

    private val brpConfiguration: BrpConfiguration
) {
    companion object {
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

        const val ENV_VAR_ZGW_API_CLIENT_MP_REST_URL = "ZGW_API_CLIENT_MP_REST_URL"

        /**
         * Maximum file size in MB for file uploads.
         *
         * Note that WildFly / RESTEasy also defines a max file upload size.
         * The value used in WildFly configuration should be set higher to account for overhead. (e.g. 80MB -> 120MB).
         * We use the Base2 system to calculate the max file size in bytes.
         */
        const val MAX_FILE_SIZE_MB: Int = 80
    }

    init {
        bronOrganisatie.validateRSIN("BRON_ORGANISATIE_RSIN")
        verantwoordelijkeOrganisatie.validateRSIN("VERANTWOORDELIJKE_ORGANISATIE_RSIN")
    }

    private var catalogusURI: URI =
        ztcClientService.readCatalogus(CatalogusListParameters().apply { domein = catalogusDomein }).url

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

    fun featureFlagBpmnSupport(): Boolean = bpmnSupport

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
