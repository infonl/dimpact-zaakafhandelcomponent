/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.configuratie;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.UriBuilder;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import net.atos.client.zgw.ztc.ZtcClientService;
import net.atos.client.zgw.ztc.model.CatalogusListParameters;
import net.atos.zac.configuratie.model.Taal;

@ApplicationScoped
@Transactional
public class ConfiguratieService {
    //TODO zaakafhandelcomponent#1468 vervangen van onderstaande placeholders
    public static final String BRON_ORGANISATIE = "123443210";

    public static final String VERANTWOORDELIJKE_ORGANISATIE = "316245124";

    public static final String CATALOGUS_DOMEIN = "ALG";

    public static final String OMSCHRIJVING_TAAK_DOCUMENT = "taak-document";

    public static final String OMSCHRIJVING_VOORWAARDEN_GEBRUIKSRECHTEN = "geen";

    public static final String TAAL_NEDERLANDS = "dut"; // ISO 639-2/B

    public static final String STATUSTYPE_OMSCHRIJVING_HEROPEND = "Heropend";

    public static final String STATUSTYPE_OMSCHRIJVING_INTAKE = "Intake";

    public static final String STATUSTYPE_OMSCHRIJVING_IN_BEHANDELING = "In behandeling";

    public static final String STATUSTYPE_OMSCHRIJVING_AFGEROND = "Afgerond";

    /**
     * Zaak communicatiekanaal used when creating zaken from Dimpact productaanvragen.
     * This communicatiekanaal always needs to be available.
     */
    public static final String COMMUNICATIEKANAAL_EFORMULIER = "E-formulier";

    public static final String INFORMATIEOBJECTTYPE_OMSCHRIJVING_EMAIL = "e-mail";

    public static final String INFORMATIEOBJECTTYPE_OMSCHRIJVING_BIJLAGE = "bijlage";
    // ~TODO

    public static final String ENV_VAR_ZGW_API_CLIENT_MP_REST_URL = "ZGW_API_CLIENT_MP_REST_URL";

    // Note that WildFly / RESTEasy also defines a max file upload size.
    // The value used in our WildFly configuration should be set higher to account for overhead. (e.g. 80MB -> 120MB).
    // We use the Base2 system to calculate the max file size in bytes.
    public static final Integer MAX_FILE_SIZE_MB = 80;

    private static final String NONE = "<NONE>";

    @PersistenceContext(unitName = "ZaakafhandelcomponentPU")
    private EntityManager entityManager;

    @Inject
    @ConfigProperty(name = "ADDITIONAL_ALLOWED_FILE_TYPES", defaultValue = NONE)
    private String additionalAllowedFileTypes;

    @Inject
    @ConfigProperty(name = ENV_VAR_ZGW_API_CLIENT_MP_REST_URL)
    private String zgwApiClientMpRestUrl;

    public List<Taal> listTalen() {
        final CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        final CriteriaQuery<Taal> query = builder.createQuery(Taal.class);
        final Root<Taal> root = query.from(Taal.class);
        query.orderBy(builder.asc(root.get("naam")));
        final TypedQuery<Taal> emQuery = entityManager.createQuery(query);
        return emQuery.getResultList();
    }

    public Optional<Taal> findDefaultTaal() {
        return findTaal(TAAL_NEDERLANDS);
    }

    public Optional<Taal> findTaal(final String code) {
        final CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        final CriteriaQuery<Taal> query = builder.createQuery(Taal.class);
        final Root<Taal> root = query.from(Taal.class);
        query.where(builder.equal(root.get("code"), code));
        final TypedQuery<Taal> emQuery = entityManager.createQuery(query);
        final List<Taal> talen = emQuery.getResultList();
        return talen.isEmpty() ? Optional.empty() : Optional.of(talen.getFirst());
    }

    public long readMaxFileSizeMB() {
        return Long.valueOf(MAX_FILE_SIZE_MB);
    }

    public List<String> readAdditionalAllowedFileTypes() {
        return additionalAllowedFileTypes.equals(NONE) ? Collections.emptyList() :
                List.of(additionalAllowedFileTypes.split(","));
    }

    /**
     * Base URL of the zaakafhandelcomponent: protocol, host, port and context (no trailing slash)
     */
    @Inject
    @ConfigProperty(name = "CONTEXT_URL")
    private String contextUrl;

    @Inject
    @ConfigProperty(name = "GEMEENTE_CODE")
    private String gemeenteCode;

    @Inject
    @ConfigProperty(name = "GEMEENTE_NAAM")
    private String gemeenteNaam;

    @Inject
    @ConfigProperty(name = "GEMEENTE_MAIL")
    private String gemeenteMail;

    @Inject
    @ConfigProperty(name = "AUTH_RESOURCE")
    private String authResource;

    @Inject
    private ZtcClientService ztcClientService;

    private URI catalogusURI;

    public URI readDefaultCatalogusURI() {
        if (catalogusURI == null) {
            final CatalogusListParameters catalogusListParameters = new CatalogusListParameters();
            catalogusListParameters.setDomein(CATALOGUS_DOMEIN);
            catalogusURI = ztcClientService.readCatalogus(catalogusListParameters).getUrl();
        }
        return catalogusURI;
    }

    public boolean isLocalDevelopment() {
        return authResource.contains("localhost");
    }

    public URI zaakTonenUrl(final String zaakIdentificatie) {
        return UriBuilder.fromUri(contextUrl).path("zaken/{zaakIdentificatie}").build(zaakIdentificatie);
    }

    public URI taakTonenUrl(final String taakId) {
        return UriBuilder.fromUri(contextUrl).path("taken/{taakId}").build(taakId);
    }

    public URI informatieobjectTonenUrl(final UUID enkelvoudigInformatieobjectUUID) {
        return UriBuilder.fromUri(contextUrl).path("informatie-objecten/{enkelvoudigInformatieobjectUUID}")
                .build(enkelvoudigInformatieobjectUUID.toString());
    }

    public String readGemeenteCode() {
        return gemeenteCode;
    }

    public String readGemeenteNaam() {
        return gemeenteNaam;
    }

    public String readGemeenteMail() {
        return gemeenteMail;
    }

    public String readZgwApiClientMpRestUrl() {
        return zgwApiClientMpRestUrl;
    }
}
