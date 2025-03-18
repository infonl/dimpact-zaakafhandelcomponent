/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.client.zgw.drc;

import static java.lang.String.format;
import static net.atos.zac.configuratie.ConfiguratieService.ENV_VAR_ZGW_API_CLIENT_MP_REST_URL;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.inject.RestClient;

import net.atos.client.util.JAXRSClientFactory;
import net.atos.client.zgw.drc.model.EnkelvoudigInformatieobjectListParameters;
import net.atos.client.zgw.drc.model.Lock;
import net.atos.client.zgw.shared.model.Results;
import net.atos.client.zgw.shared.model.audit.AuditTrailRegel;
import net.atos.client.zgw.shared.util.ZGWClientHeadersFactory;
import net.atos.zac.configuratie.ConfiguratieService;
import nl.info.client.zgw.drc.model.generated.EnkelvoudigInformatieObject;
import nl.info.client.zgw.drc.model.generated.EnkelvoudigInformatieObjectCreateLockRequest;
import nl.info.client.zgw.drc.model.generated.EnkelvoudigInformatieObjectWithLockRequest;
import nl.info.client.zgw.drc.model.generated.Gebruiksrechten;


@ApplicationScoped
public class DrcClientService {

    @Inject
    @RestClient
    private DrcClient drcClient;

    @Inject
    private ZGWClientHeadersFactory zgwClientHeadersFactory;

    @Inject
    private ConfiguratieService configuratieService;

    /**
     * Read {@link EnkelvoudigInformatieObject} via its UUID.
     * Throws a RuntimeException if the {@link EnkelvoudigInformatieObject} can not be read.
     *
     * @param uuid UUID of the {@link EnkelvoudigInformatieObject}.
     * @return {@link EnkelvoudigInformatieObject}. Never 'null'!
     */
    public EnkelvoudigInformatieObject readEnkelvoudigInformatieobject(final UUID uuid) {
        return drcClient.enkelvoudigInformatieobjectRead(uuid);
    }

    /**
     * Read {@link EnkelvoudigInformatieObject} via its UUID and version.
     * Throws a RuntimeException if the {@link EnkelvoudigInformatieObject} can not be read.
     *
     * @param uuid   UUID of the {@link EnkelvoudigInformatieObject}.
     * @param versie Required version
     * @return {@link EnkelvoudigInformatieObject}. Never 'null'!
     */
    public EnkelvoudigInformatieObject readEnkelvoudigInformatieobjectVersie(
            final UUID uuid,
            final int versie
    ) {
        return drcClient.enkelvoudigInformatieobjectReadVersie(uuid, versie);
    }

    /**
     * DELETE {@link EnkelvoudigInformatieObject} via its UUID.
     * Throws a RuntimeException if the {@link EnkelvoudigInformatieObject} can not be deleted.
     *
     * @param uuid UUID of the {@link EnkelvoudigInformatieObject}.
     */
    public void deleteEnkelvoudigInformatieobject(final UUID uuid) {
        drcClient.enkelvoudigInformatieobjectDelete(uuid);
    }

    /**
     * Read {@link EnkelvoudigInformatieObject} via its URI.
     * Throws a RuntimeException if the {@link EnkelvoudigInformatieObject} can not be read.
     *
     * @param enkelvoudigInformatieobjectURI URI of the {@link EnkelvoudigInformatieObject}.
     * @return {@link EnkelvoudigInformatieObject}. Never 'null'!
     */
    public EnkelvoudigInformatieObject readEnkelvoudigInformatieobject(final URI enkelvoudigInformatieobjectURI) {
        return createInvocationBuilder(enkelvoudigInformatieobjectURI).get(EnkelvoudigInformatieObject.class);
    }

    public EnkelvoudigInformatieObject updateEnkelvoudigInformatieobject(
            final UUID uuid,
            final EnkelvoudigInformatieObjectWithLockRequest enkelvoudigInformatieObjectWithLockRequest,
            final String toelichting
    ) {
        if (toelichting != null) {
            zgwClientHeadersFactory.setAuditToelichting(toelichting);
        }
        return drcClient.enkelvoudigInformatieobjectPartialUpdate(uuid, enkelvoudigInformatieObjectWithLockRequest);
    }

    /**
     * Lock a {@link EnkelvoudigInformatieObject}.
     *
     * @param enkelvoudigInformatieobjectUUID {@link EnkelvoudigInformatieObject}
     */
    public String lockEnkelvoudigInformatieobject(final UUID enkelvoudigInformatieobjectUUID) {
        // If the EnkelvoudigInformatieobject is already locked a ValidationException is thrown.
        return drcClient.enkelvoudigInformatieobjectLock(enkelvoudigInformatieobjectUUID, new Lock()).getLock();
    }

    /**
     * Unlock a {@link EnkelvoudigInformatieObject}.
     *
     * @param enkelvoudigInformatieobjectUUID {@link EnkelvoudigInformatieObject}
     * @param lock                            The lock id
     */
    public void unlockEnkelvoudigInformatieobject(final UUID enkelvoudigInformatieobjectUUID, String lock) {
        drcClient.enkelvoudigInformatieobjectUnlock(enkelvoudigInformatieobjectUUID, new Lock(lock));
    }

    /**
     * Download content of {@link EnkelvoudigInformatieObject}.
     *
     * @param enkelvoudigInformatieobjectUUID UUID of {@link EnkelvoudigInformatieObject}
     * @return Content of {@link EnkelvoudigInformatieObject}.
     */
    public ByteArrayInputStream downloadEnkelvoudigInformatieobject(final UUID enkelvoudigInformatieobjectUUID) {
        final Response response = drcClient.enkelvoudigInformatieobjectDownload(enkelvoudigInformatieobjectUUID);
        if (!response.bufferEntity()) {
            throw new RuntimeException(
                    String.format("Content of enkelvoudig informatieobject with uuid '%s' could not be buffered.",
                            enkelvoudigInformatieobjectUUID.toString()));
        }
        return (ByteArrayInputStream) response.getEntity();
    }

    /**
     * Download content of {@link EnkelvoudigInformatieObject} of a specific version
     *
     * @param enkelvoudigInformatieobjectUUID UUID of {@link EnkelvoudigInformatieObject}
     * @param versie                          Required version
     * @return Content of {@link EnkelvoudigInformatieObject}.
     */
    public ByteArrayInputStream downloadEnkelvoudigInformatieobjectVersie(
            final UUID enkelvoudigInformatieobjectUUID,
            final Integer versie
    ) {
        final Response response = drcClient.enkelvoudigInformatieobjectDownloadVersie(enkelvoudigInformatieobjectUUID,
                versie);
        if (!response.bufferEntity()) {
            throw new RuntimeException(String.format(
                    "Content of enkelvoudig informatieobject with uuid '%s' and version '%d' could not be buffered.",
                    enkelvoudigInformatieobjectUUID.toString(), versie));
        }
        return (ByteArrayInputStream) response.getEntity();
    }

    /**
     * List all instances of {@link AuditTrailRegel} for a specific
     * {@link EnkelvoudigInformatieObject}.
     *
     * @param enkelvoudigInformatieobjectUUID UUID of {@link EnkelvoudigInformatieObject}.
     * @return List of {@link AuditTrailRegel} instances.
     */
    public List<AuditTrailRegel> listAuditTrail(final UUID enkelvoudigInformatieobjectUUID) {
        return drcClient.listAuditTrail(enkelvoudigInformatieobjectUUID);
    }

    /**
     * List instances of {@link EnkelvoudigInformatieObject} filtered by
     * {@link EnkelvoudigInformatieobjectListParameters}.
     *
     * @param filter {@link EnkelvoudigInformatieobjectListParameters}.
     * @return List of {@link EnkelvoudigInformatieObject} instances.
     */
    public Results<EnkelvoudigInformatieObject> listEnkelvoudigInformatieObjecten(
            final EnkelvoudigInformatieobjectListParameters filter
    ) {
        return drcClient.enkelvoudigInformatieobjectList(filter);
    }

    public EnkelvoudigInformatieObject createEnkelvoudigInformatieobject(
            final EnkelvoudigInformatieObjectCreateLockRequest enkelvoudigInformatieObjectCreateLockRequest
    ) {
        return drcClient.enkelvoudigInformatieobjectCreate(enkelvoudigInformatieObjectCreateLockRequest);
    }

    public void createGebruiksrechten(final Gebruiksrechten gebruiksrechten) {
        drcClient.gebruiksrechtenCreate(gebruiksrechten);
    }

    private Invocation.Builder createInvocationBuilder(final URI uri) {
        // for security reasons check if the provided URI starts with the value of the
        // environment variable that we use to configure the ztcClient
        if (!uri.toString().startsWith(configuratieService.readZgwApiClientMpRestUrl())) {
            throw new IllegalStateException(format(
                    "URI '%s' does not start with value for environment variable '%s': '%s'",
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
