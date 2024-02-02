/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.client.zgw.ztc;

import static java.lang.String.format;
import static net.atos.zac.configuratie.ConfiguratieService.ENV_VAR_ZGW_API_CLIENT_MP_REST_URL;

import java.net.URI;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.cache.annotation.CacheRemoveAll;
import javax.cache.annotation.CacheResult;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.inject.RestClient;

import net.atos.client.util.JAXRSClientFactory;
import net.atos.client.zgw.shared.cache.Caching;
import net.atos.client.zgw.shared.model.Results;
import net.atos.client.zgw.shared.util.ZGWClientHeadersFactory;
import net.atos.client.zgw.ztc.model.AardVanRol;
import net.atos.client.zgw.ztc.model.BesluittypeListParameters;
import net.atos.client.zgw.ztc.model.CatalogusListParameters;
import net.atos.client.zgw.ztc.model.ResultaattypeListParameters;
import net.atos.client.zgw.ztc.model.RoltypeListParameters;
import net.atos.client.zgw.ztc.model.StatustypeListParameters;
import net.atos.client.zgw.ztc.model.ZaaktypeInformatieobjecttypeListParameters;
import net.atos.client.zgw.ztc.model.ZaaktypeListParameters;
import net.atos.client.zgw.ztc.model.generated.BesluitType;
import net.atos.client.zgw.ztc.model.generated.Catalogus;
import net.atos.client.zgw.ztc.model.generated.InformatieObjectType;
import net.atos.client.zgw.ztc.model.generated.ResultaatType;
import net.atos.client.zgw.ztc.model.generated.RolType;
import net.atos.client.zgw.ztc.model.generated.StatusType;
import net.atos.client.zgw.ztc.model.generated.ZaakType;
import net.atos.client.zgw.ztc.model.generated.ZaakTypeInformatieObjectType;
import net.atos.zac.configuratie.ConfiguratieService;

/**
 * Encapsulates the {@link ZTCClient} by providing caching and authentication.
 * <p>
 * Never call methods with caching annotations from within the service (or it will not work).
 * Do not introduce caches with keys other than URI and UUID.
 * Use Optional for caches that need to hold nulls (Infinispan does not cache nulls).
 */
@ApplicationScoped
public class ZTCClientService implements Caching {
    private static final List<String> CACHES = List.of(
            ZTC_BESLUITTYPE,
            ZTC_CACHE_TIME,
            ZTC_RESULTAATTYPE,
            ZTC_STATUSTYPE,
            ZTC_INFORMATIEOBJECTTYPE,
            ZTC_ZAAKTYPE_INFORMATIEOBJECTTYPE,
            ZTC_ZAAKTYPE);

    @Inject
    @RestClient
    private ZTCClient ztcClient;

    @Inject
    private ZGWClientHeadersFactory zgwClientHeadersFactory;

    @Inject
    private ConfiguratieService configuratieService;

    public Results<Catalogus> listCatalogus(final CatalogusListParameters catalogusListParameters) {
        return ztcClient.catalogusList(catalogusListParameters);
    }

    /**
     * Read {@link Catalogus} filtered by {@link CatalogusListParameters}.
     * Throws a RuntimeException if the {@link Catalogus} can not be read.
     *
     * @param filter {@link CatalogusListParameters}.
     * @return {@link Catalogus}. Never 'null'!
     */
    public Catalogus readCatalogus(final CatalogusListParameters filter) {
        return ztcClient.catalogusList(filter)
                .getSingleResult()
                .orElseThrow(() -> new RuntimeException("Catalogus not found."));
    }

    @CacheResult(cacheName = ZTC_CACHE_TIME)
    public ZonedDateTime readCacheTime() {
        return ZonedDateTime.now();
    }

    /**
     * Read {@link ZaakType} via URI.
     * Throws a RuntimeException if the {@link ZaakType} can not be read.
     *
     * @param zaaktypeURI URI of {@link ZaakType}.
     * @return {@link ZaakType}. Never 'null'!
     */
    @CacheResult(cacheName = ZTC_ZAAKTYPE)
    public ZaakType readZaaktype(final URI zaaktypeURI) {
        return createInvocationBuilder(zaaktypeURI).get(ZaakType.class);
    }

    /**
     * Read {@link ZaakType} via UUID.
     * Throws a RuntimeException if the {@link ZaakType} can not be read.
     *
     * @param zaaktypeUuid UUID of {@link ZaakType}.
     * @return {@link ZaakType}. Never 'null'!
     */
    @CacheResult(cacheName = ZTC_ZAAKTYPE)
    public ZaakType readZaaktype(final UUID zaaktypeUuid) {
        return ztcClient.zaaktypeRead(zaaktypeUuid);
    }

    /**
     * List instances of {@link ZaakType} in {@link Catalogus}.
     *
     * @param catalogusURI URI of {@link Catalogus}.
     * @return List of {@link ZaakType} instances
     */
    @CacheResult(cacheName = ZTC_ZAAKTYPE)
    public List<ZaakType> listZaaktypen(final URI catalogusURI) {
        return ztcClient.zaaktypeList(new ZaaktypeListParameters(catalogusURI)).getResults();
    }

    /**
     * Read {@link StatusType} via URI.
     * Throws a RuntimeException if the {@link StatusType} can not be read.
     *
     * @param statustypeURI URI of {@link StatusType}.
     * @return {@link StatusType}. Never 'null'!
     */
    @CacheResult(cacheName = ZTC_STATUSTYPE)
    public StatusType readStatustype(final URI statustypeURI) {
        return createInvocationBuilder(statustypeURI).get(StatusType.class);
    }

    /**
     * Read {@link StatusType} via its UUID.
     * Throws a RuntimeException if the {@link StatusType} can not be read.
     *
     * @param statustypeUUID UUID of {@link StatusType}.
     * @return {@link StatusType}. Never 'null'!
     */
    @CacheResult(cacheName = ZTC_STATUSTYPE)
    public StatusType readStatustype(final UUID statustypeUUID) {
        return ztcClient.statustypeRead(statustypeUUID);
    }

    /**
     * Read the {@link StatusType} of {@link ZaakType}.
     *
     * @param zaaktypeURI URI of {@link ZaakType}.
     * @return list of {@link StatusType}.
     */
    @CacheResult(cacheName = ZTC_STATUSTYPE)
    public List<StatusType> readStatustypen(final URI zaaktypeURI) {
        return ztcClient.statustypeList(new StatustypeListParameters(zaaktypeURI)).getSinglePageResults();
    }

    /**
     * Read the {@link ZaakTypeInformatieObjectType} of {@link ZaakType}.
     *
     * @param zaaktypeURI URI of {@link ZaakType}.
     * @return list of {@link ZaakTypeInformatieObjectType}.
     */
    @CacheResult(cacheName = ZTC_ZAAKTYPE_INFORMATIEOBJECTTYPE)
    public List<ZaakTypeInformatieObjectType> readZaaktypeInformatieobjecttypen(final URI zaaktypeURI) {
        return ztcClient.zaaktypeinformatieobjecttypeList(new ZaaktypeInformatieobjecttypeListParameters(zaaktypeURI)).getSinglePageResults();
    }

    /**
     * Read the {@link InformatieObjectType} of {@link ZaakType}.
     *
     * @param zaaktypeURI URI of {@link ZaakType}.
     * @return list of {@link InformatieObjectType}.
     */
    @CacheResult(cacheName = ZTC_INFORMATIEOBJECTTYPE)
    public List<InformatieObjectType> readInformatieobjecttypen(final URI zaaktypeURI) {
        return readZaaktypeInformatieobjecttypen(zaaktypeURI).stream()
                .map(zaaktypeInformatieobjecttype -> readInformatieobjecttype(zaaktypeInformatieobjecttype.getInformatieobjecttype())).toList();
    }

    /**
     * Read {@link ResultaatType} via its URI.
     * Throws a RuntimeException if the {@link ResultaatType} can not be read.
     *
     * @param resultaattypeURI URI of {@link ResultaatType}.
     * @return {@link ResultaatType}. Never 'null'!
     */
    @CacheResult(cacheName = ZTC_RESULTAATTYPE)
    public ResultaatType readResultaattype(final URI resultaattypeURI) {
        return createInvocationBuilder(resultaattypeURI).get(ResultaatType.class);
    }

    /**
     * Read {@link BesluitType} via its URI.
     * Throws a RuntimeException if the {@link BesluitType} can not be read.
     *
     * @param besluittypeURI URI of {@link BesluitType}.
     * @return {@link BesluitType}. Never 'null'!
     */
    @CacheResult(cacheName = ZTC_BESLUITTYPE)
    public BesluitType readBesluittype(final URI besluittypeURI) {
        return createInvocationBuilder(besluittypeURI).get(BesluitType.class);
    }

    /**
     * Read {@link BesluitType} via its UUID.
     * Throws a RuntimeException if the {@link BesluitType} can not be read.
     *
     * @param besluittypeUUID UUID of {@link BesluitType}.
     * @return {@link BesluitType}. Never 'null'!
     */
    @CacheResult(cacheName = ZTC_BESLUITTYPE)
    public BesluitType readBesluittype(final UUID besluittypeUUID) {
        return ztcClient.besluittypeRead(besluittypeUUID);
    }

    /**
     * Read the {@link BesluitType} of {@link ZaakType}.
     *
     * @param zaaktypeURI URI of {@link ZaakType}.
     * @return list of {@link BesluitType}.
     */
    @CacheResult(cacheName = ZTC_BESLUITTYPE)
    public List<BesluitType> readBesluittypen(final URI zaaktypeURI) {
        return ztcClient.besluittypeList(new BesluittypeListParameters(zaaktypeURI)).getSinglePageResults();
    }

    /**
     * Read {@link ResultaatType} via its UUID.
     * Throws a RuntimeException if the {@link ResultaatType} can not be read.
     *
     * @param resultaattypeUUID UUID of {@link ResultaatType}.
     * @return {@link ResultaatType}. Never 'null'!
     */
    @CacheResult(cacheName = ZTC_RESULTAATTYPE)
    public ResultaatType readResultaattype(final UUID resultaattypeUUID) {
        return ztcClient.resultaattypeRead(resultaattypeUUID);
    }

    /**
     * Read the {@link ResultaatType} of {@link ZaakType}.
     *
     * @param zaaktypeURI URI of {@link ZaakType}.
     * @return list of {@link ResultaatType}.
     */
    @CacheResult(cacheName = ZTC_RESULTAATTYPE)
    public List<ResultaatType> readResultaattypen(final URI zaaktypeURI) {
        return ztcClient.resultaattypeList(new ResultaattypeListParameters(zaaktypeURI)).getSinglePageResults();
    }

    /**
     * Find {@link RolType} of {@link ZaakType} and {@link AardVanRol}.
     * returns null if the {@link ResultaatType} can not be found
     *
     * @param zaaktypeURI URI of {@link ZaakType}.
     * @param aardVanRol  {@link AardVanRol}.
     * @return {@link RolType} or NULL
     */
    @CacheResult(cacheName = ZTC_ROLTYPE)
    public Optional<RolType> findRoltype(final URI zaaktypeURI, final AardVanRol aardVanRol) {
        return ztcClient.roltypeList(new RoltypeListParameters(zaaktypeURI, aardVanRol)).getSingleResult();
    }

    /**
     * Read {@link RolType} of {@link ZaakType} and {@link AardVanRol}.
     * Throws a RuntimeException if the {@link ResultaatType} can not be read.
     *
     * @param zaaktypeURI URI of {@link ZaakType}.
     * @param aardVanRol  {@link AardVanRol}.
     * @return {@link RolType}. Never 'null'!
     */
    @CacheResult(cacheName = ZTC_ROLTYPE)
    public RolType readRoltype(final AardVanRol aardVanRol, final URI zaaktypeURI) {
        return ztcClient.roltypeList(new RoltypeListParameters(zaaktypeURI, aardVanRol)).getSingleResult()
                .orElseThrow(
                        () -> new RuntimeException(format("Zaaktype '%s': Roltype with aard '%s' not found.", zaaktypeURI.toString(), aardVanRol.toString())));
    }

    /**
     * Read {@link RolType}s of {@link ZaakType}.
     *
     * @param zaaktypeURI URI of {@link ZaakType}.
     * @return list of {@link RolType}s.
     */
    @CacheResult(cacheName = ZTC_ROLTYPE)
    public List<RolType> listRoltypen(final URI zaaktypeURI) {
        return ztcClient.roltypeList(new RoltypeListParameters(zaaktypeURI)).getResults();
    }

    /**
     * Read {@link RolType} via its UUID.
     * Throws a RuntimeException if the {@link RolType} can not be read.
     *
     * @param roltypeUUID UUID of {@link RolType}.
     * @return {@link RolType}. Never 'null'!
     */
    @CacheResult(cacheName = ZTC_ROLTYPE)
    public RolType readRoltype(final UUID roltypeUUID) {
        return ztcClient.roltypeRead(roltypeUUID);
    }

    /**
     * Read {@link InformatieObjectType} via its URI.
     * Throws a RuntimeException if the {@link InformatieObjectType} can not be read.
     *
     * @param informatieobjecttypeURI URI of {@link InformatieObjectType}.
     * @return {@link InformatieObjectType}. Never 'null'!
     */
    @CacheResult(cacheName = ZTC_INFORMATIEOBJECTTYPE)
    public InformatieObjectType readInformatieobjecttype(final URI informatieobjecttypeURI) {
        return createInvocationBuilder(informatieobjecttypeURI).get(InformatieObjectType.class);
    }

    /**
     * Read {@link InformatieObjectType} via its UUID.
     * Throws a RuntimeException if the {@link InformatieObjectType} can not be read.
     *
     * @param informatieobjecttypeUUID UUID of {@link InformatieObjectType}.
     * @return {@link InformatieObjectType}.
     */
    @CacheResult(cacheName = ZTC_INFORMATIEOBJECTTYPE)
    public InformatieObjectType readInformatieobjecttype(final UUID informatieobjecttypeUUID) {
        return ztcClient.informatieObjectTypeRead(informatieobjecttypeUUID);
    }

    @CacheRemoveAll(cacheName = ZTC_ZAAKTYPE)
    public String clearZaaktypeCache() {
        return cleared(ZTC_ZAAKTYPE);
    }

    @CacheRemoveAll(cacheName = ZTC_STATUSTYPE)
    public String clearStatustypeCache() {
        return cleared(ZTC_STATUSTYPE);
    }

    @CacheRemoveAll(cacheName = ZTC_RESULTAATTYPE)
    public String clearResultaattypeCache() {
        return cleared(ZTC_RESULTAATTYPE);
    }

    @CacheRemoveAll(cacheName = ZTC_INFORMATIEOBJECTTYPE)
    public String clearInformatieobjecttypeCache() {
        return cleared(ZTC_INFORMATIEOBJECTTYPE);
    }

    @CacheRemoveAll(cacheName = ZTC_ZAAKTYPE_INFORMATIEOBJECTTYPE)
    public String clearZaaktypeInformatieobjecttypeCache() {
        return cleared(ZTC_ZAAKTYPE_INFORMATIEOBJECTTYPE);
    }

    @CacheRemoveAll(cacheName = ZTC_BESLUITTYPE)
    public String clearBesluittypeCache() {
        return cleared(ZTC_BESLUITTYPE);
    }

    @CacheRemoveAll(cacheName = ZTC_ROLTYPE)
    public String clearRoltypeCache() {
        return cleared(ZTC_ROLTYPE);
    }

    @CacheRemoveAll(cacheName = ZTC_CACHE_TIME)
    public String clearCacheTime() {
        return cleared(ZTC_CACHE_TIME);
    }

    @Override
    public List<String> cacheNames() {
        return CACHES;
    }

    private Invocation.Builder createInvocationBuilder(final URI uri) {
        // for security reasons check if the provided URI starts with the value of the
        // environment variable that we use to configure the ztcClient
        if (!uri.toString().startsWith(configuratieService.readZgwApiClientMpRestUrl())) {
            throw new RuntimeException(format(
                    "URI '%s' does not start with value for environment variable " +
                            "'%s': '%s'",
                    uri,
                    ENV_VAR_ZGW_API_CLIENT_MP_REST_URL,
                    configuratieService.readZgwApiClientMpRestUrl()
            ));
        }

        return JAXRSClientFactory.getOrCreateClient().target(uri)
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, zgwClientHeadersFactory.generateJWTToken());
    }
}
