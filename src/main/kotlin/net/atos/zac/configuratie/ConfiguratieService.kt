/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.configuratie

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.persistence.EntityManager
import jakarta.transaction.Transactional
import jakarta.ws.rs.core.UriBuilder
import net.atos.client.zgw.ztc.ZtcClientService
import net.atos.client.zgw.ztc.model.CatalogusListParameters
import net.atos.zac.configuratie.model.Taal
import nl.lifely.zac.util.AllOpen
import nl.lifely.zac.util.NoArgConstructor
import org.eclipse.microprofile.config.inject.ConfigProperty
import java.net.URI
import java.util.UUID

@ApplicationScoped
@Transactional
@AllOpen
@NoArgConstructor
@Suppress("LongParameterList", "TooManyFunctions")
class ConfiguratieService @Inject constructor(
    private val entityManager: EntityManager,

    ztcClientService: ZtcClientService,

    @ConfigProperty(name = "ADDITIONAL_ALLOWED_FILE_TYPES", defaultValue = NONE)
    private val additionalAllowedFileTypes: String,

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
    private val bpmnSupport: Boolean
) {
    companion object {
        // TODO zaakafhandelcomponent#1468 vervangen van onderstaande placeholders
        const val BRON_ORGANISATIE = "123443210"
        const val VERANTWOORDELIJKE_ORGANISATIE = "316245124"
        const val CATALOGUS_DOMEIN = "ALG"
        const val OMSCHRIJVING_TAAK_DOCUMENT = "taak-document"
        const val OMSCHRIJVING_VOORWAARDEN_GEBRUIKSRECHTEN = "geen"

        /**
         * ISO 639-2/B language code for Dutch.
         */
        const val TAAL_NEDERLANDS = "dut"

        const val STATUSTYPE_OMSCHRIJVING_HEROPEND = "Heropend"
        const val STATUSTYPE_OMSCHRIJVING_INTAKE = "Intake"
        const val STATUSTYPE_OMSCHRIJVING_IN_BEHANDELING = "In behandeling"
        const val STATUSTYPE_OMSCHRIJVING_AFGEROND = "Afgerond"

        /**
         * Zaak communicatiekanaal used when creating zaken from Dimpact productaanvragen.
         * This communicatiekanaal always needs to be available.
         */
        const val COMMUNICATIEKANAAL_EFORMULIER = "E-formulier"

        const val INFORMATIEOBJECTTYPE_OMSCHRIJVING_EMAIL = "e-mail"
        const val INFORMATIEOBJECTTYPE_OMSCHRIJVING_BIJLAGE = "bijlage"

        // ~TODO
        const val ENV_VAR_ZGW_API_CLIENT_MP_REST_URL = "ZGW_API_CLIENT_MP_REST_URL"

        /**
         * Maximum file size in MB for file uploads.
         *
         * Note that WildFly / RESTEasy also defines a max file upload size.
         * The value used in our WildFly configuration should be set higher to account for overhead. (e.g. 80MB -> 120MB).
         * We use the Base2 system to calculate the max file size in bytes.
         */
        const val MAX_FILE_SIZE_MB: Int = 80

        private const val NONE = "<NONE>"
    }

    private var catalogusURI: URI =
        ztcClientService.readCatalogus(CatalogusListParameters().apply { domein = CATALOGUS_DOMEIN }).url

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

    fun readMaxFileSizeMB() = MAX_FILE_SIZE_MB.toLong()

    fun readAdditionalAllowedFileTypes(): List<String> =
        if (additionalAllowedFileTypes == NONE) {
            emptyList()
        } else {
            additionalAllowedFileTypes.split(",").filter { it.isNotEmpty() }
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

    fun readGemeenteCode(): String = gemeenteCode

    fun readGemeenteNaam(): String = gemeenteNaam

    fun readGemeenteMail(): String = gemeenteMail

    fun readZgwApiClientMpRestUrl(): String = zgwApiClientMpRestUrl
}
