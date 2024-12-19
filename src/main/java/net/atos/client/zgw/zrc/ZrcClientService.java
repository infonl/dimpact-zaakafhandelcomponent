/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.client.zgw.zrc;

import static java.lang.String.format;
import static net.atos.client.zgw.util.UriUtilsKt.extractUuid;
import static net.atos.zac.configuratie.ConfiguratieService.ENV_VAR_ZGW_API_CLIENT_MP_REST_URL;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriBuilder;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import net.atos.client.util.JAXRSClientFactory;
import net.atos.client.zgw.drc.model.generated.EnkelvoudigInformatieObject;
import net.atos.client.zgw.shared.exception.ZgwFoutExceptionMapper;
import net.atos.client.zgw.shared.exception.ZgwValidatieFoutResponseExceptionMapper;
import net.atos.client.zgw.shared.model.Results;
import net.atos.client.zgw.shared.model.audit.ZRCAuditTrailRegel;
import net.atos.client.zgw.shared.util.JsonbConfiguration;
import net.atos.client.zgw.shared.util.ZGWClientHeadersFactory;
import net.atos.client.zgw.zrc.exception.ZrcResponseExceptionMapper;
import net.atos.client.zgw.zrc.model.BetrokkeneType;
import net.atos.client.zgw.zrc.model.Rol;
import net.atos.client.zgw.zrc.model.RolListParameters;
import net.atos.client.zgw.zrc.model.Status;
import net.atos.client.zgw.zrc.model.Zaak;
import net.atos.client.zgw.zrc.model.ZaakInformatieobject;
import net.atos.client.zgw.zrc.model.ZaakInformatieobjectListParameters;
import net.atos.client.zgw.zrc.model.ZaakListParameters;
import net.atos.client.zgw.zrc.model.ZaakUuid;
import net.atos.client.zgw.zrc.model.generated.Resultaat;
import net.atos.client.zgw.zrc.model.generated.ZaakEigenschap;
import net.atos.client.zgw.zrc.model.zaakobjecten.Zaakobject;
import net.atos.client.zgw.zrc.model.zaakobjecten.ZaakobjectListParameters;
import net.atos.zac.configuratie.ConfiguratieService;

/**
 * Careful!
 * <p>
 * Never call methods with caching annotations from within the service (or it will not work).
 * Do not introduce caches with keys other than URI and UUID.
 * Use Optional for caches that need to hold nulls (Infinispan does not cache nulls).
 */
@ApplicationScoped
public class ZrcClientService {

    @Inject
    @ConfigProperty(name = "ZGW_API_URL_EXTERN")
    private String zgwApiUrlExtern;

    @Inject
    @RestClient
    private ZrcClient zrcClient;

    @Inject
    private ZGWClientHeadersFactory zgwClientHeadersFactory;

    @Inject
    private ConfiguratieService configuratieService;

    /**
     * Create {@link Rol}.
     *
     * @param rol {@link Rol}/
     */
    public void createRol(final Rol<?> rol) {
        createRol(rol, null);
    }

    /**
     * Create {@link Rol}.
     *
     * @param rol         {@link Rol}/
     * @param toelichting de toelichting
     * @return Created {@link Rol}.
     */
    public Rol<?> createRol(final Rol<?> rol, final String toelichting) {
        if (toelichting != null) {
            zgwClientHeadersFactory.setAuditToelichting(toelichting);
        }
        return zrcClient.rolCreate(rol);
    }

    /**
     * Delete {@link Rol}.
     *
     * @param rol         de betreffende rol {@link Rol}/
     * @param toelichting de toelichting
     */
    public void deleteRol(final Rol<?> rol, final String toelichting) {
        if (toelichting != null) {
            zgwClientHeadersFactory.setAuditToelichting(toelichting);
        }
        zrcClient.rolDelete(rol.getUuid());
    }

    /**
     * Create {@link Zaakobject}.
     *
     * @param zaakobject {@link Zaakobject}.
     * @return Created {@link Zaakobject}.
     */
    public Zaakobject createZaakobject(final Zaakobject zaakobject) {
        return zrcClient.zaakobjectCreate(zaakobject);
    }

    /**
     * Delete {@link Zaakobject}.
     *
     * @param zaakobject {@link Zaakobject}.
     */
    public void deleteZaakobject(final Zaakobject zaakobject, final String toelichting) {
        if (toelichting != null) {
            zgwClientHeadersFactory.setAuditToelichting(toelichting);
        }
        zrcClient.zaakobjectDelete(zaakobject.getUuid());
    }

    /**
     * Read {@link Zaakobject} via its UUID.
     * Throws a RuntimeException if the {@link Zaakobject} can not be read.
     *
     * @param zaakobjectUUID UUID of {@link Zaakobject}.
     * @return {@link Zaakobject}. Never NULL!
     */
    public Zaakobject readZaakobject(final UUID zaakobjectUUID) {
        return zrcClient.zaakobjectRead(zaakobjectUUID);
    }

    /**
     * Create {@link ZaakInformatieobject}
     *
     * @param zaakInformatieobject describes relation between ZAAK en INFORMATIEOBJECT
     * @return ZaakInformatieobject
     */
    public ZaakInformatieobject createZaakInformatieobject(final ZaakInformatieobject zaakInformatieobject, final String toelichting) {
        if (toelichting != null) {
            zgwClientHeadersFactory.setAuditToelichting(toelichting);
        }
        return zrcClient.zaakinformatieobjectCreate(zaakInformatieobject);
    }

    /**
     * delete {@link ZaakInformatieobject}
     *
     * @param zaakInformatieobjectUuid zaakInformatieobjectUuid
     */
    public void deleteZaakInformatieobject(
            final UUID zaakInformatieobjectUuid,
            final String toelichting,
            final String toelichtingPrefix
    ) {
        final String fullToelichting = StringUtils.isEmpty(toelichting) ?
                toelichtingPrefix :
                String.format("%s: %s", toelichtingPrefix, toelichting);
        zgwClientHeadersFactory.setAuditToelichting(fullToelichting);
        zrcClient.zaakinformatieobjectDelete(zaakInformatieobjectUuid);
    }

    /**
     * Read {@link Zaak} via its UUID.
     * Throws a RuntimeException if the {@link Zaak} can not be read.
     *
     * @param zaakUUID UUID of {@link Zaak}.
     * @return {@link Zaak}. Never NULL!
     */
    public Zaak readZaak(final UUID zaakUUID) {
        return zrcClient.zaakRead(zaakUUID);
    }

    /**
     * Read {@link Zaak} via its URI.
     * Throws a RuntimeException if the {@link Zaak} can not be read.
     *
     * @param zaakURI URI of {@link Zaak}.
     * @return {@link Zaak}. Never NULL!
     */
    public Zaak readZaak(final URI zaakURI) {
        return createInvocationBuilder(zaakURI).get(Zaak.class);
    }


    /**
     * Read {@link ZaakInformatieobject} via its UUID.
     * Throws a RuntimeException if the {@link ZaakInformatieobject} can not be read.
     *
     * @param zaakinformatieobjectUUID UUID of {@link ZaakInformatieobject}.
     * @return {@link ZaakInformatieobject}. Never NULL!
     */
    public ZaakInformatieobject readZaakinformatieobject(final UUID zaakinformatieobjectUUID) {
        return zrcClient.zaakinformatieobjectRead(zaakinformatieobjectUUID);
    }

    /**
     * Update all instances of {@link Rol} for {@link Zaak}.
     * Replaces all current instances of {@link Rol} with the suplied instances.
     *
     * @param zaak   de bij te werken zaak
     * @param rollen de gewenste rollen
     */
    private void updateRollen(final Zaak zaak, final Collection<Rol<?>> rollen, final String toelichting) {
        final Collection<Rol<?>> current = listRollen(zaak);
        deleteDeletedRollen(current, rollen, toelichting);
        deleteUpdatedRollen(current, rollen, toelichting);
        createUpdatedRollen(current, rollen, toelichting);
        createCreatedRollen(current, rollen, toelichting);
    }

    public void updateRol(final Zaak zaak, final Rol<?> rol, final String toelichting) {
        final List<Rol<?>> rollen = listRollen(zaak);
        rollen.add(rol);
        updateRollen(zaak, rollen, toelichting);
    }

    public void deleteRol(final Zaak zaak, final BetrokkeneType betrokkeneType, final String toelichting) {
        final List<Rol<?>> rollen = listRollen(zaak);
        final Optional<Rol<?>> rolMedewerker = rollen.stream().filter(rol -> rol.getBetrokkeneType() == betrokkeneType).findFirst();
        rolMedewerker.ifPresent(betrokkene -> rollen.removeIf(rol -> rol.equalBetrokkeneRol(betrokkene)));
        updateRollen(zaak, rollen, toelichting);
    }

    /**
     * Read {@link Rol} via its URI.
     * Throws a RuntimeException if the {@link Rol} can not be read.
     *
     * @param rolURI URI of {@link Rol}.
     * @return {@link Rol}. Never NULL!
     */
    public Rol<?> readRol(final URI rolURI) {
        return createInvocationBuilder(rolURI).get(Rol.class);
    }

    /**
     * Read {@link Rol} via its UUID.
     * Throws a RuntimeException if the {@link Rol} can not be read.
     *
     * @param rolUUID UUID of {@link Rol}.
     * @return {@link Rol}. Never NULL!
     */
    public Rol<?> readRol(final UUID rolUUID) {
        return zrcClient.rolRead(rolUUID);
    }

    /**
     * Read {@link Resultaat} via its URI.
     * Throws a RuntimeException if the {@link Resultaat} can not be read.
     *
     * @param resultaatURI URI of {@link Resultaat}.
     * @return {@link Resultaat}. Never 'null'!
     */
    public Resultaat readResultaat(final URI resultaatURI) {
        return createInvocationBuilder(resultaatURI).get(Resultaat.class);
    }

    /**
     * Read {@link ZaakEigenschap} via its URI.
     * Throws a RuntimeException if the {@link ZaakEigenschap} can not be read.
     *
     * @param zaakeigenschapURI URI of {@link ZaakEigenschap}.
     * @return {@link ZaakEigenschap}. Never 'null'!
     */
    public ZaakEigenschap readZaakeigenschap(final URI zaakeigenschapURI) {
        return createInvocationBuilder(zaakeigenschapURI).get(ZaakEigenschap.class);
    }

    /**
     * Read {@link Status} via its URI.
     * Throws a RuntimeException if the {@link Status} can not be read.
     *
     * @param statusURI URI of {@link Status}.
     * @return {@link Status}. Never 'null'!
     */
    public Status readStatus(final URI statusURI) {
        return createInvocationBuilder(statusURI).get(Status.class);
    }

    /**
     * List instances of {@link Zaakobject} filtered by {@link ZaakobjectListParameters}.
     *
     * @param zaakobjectListParameters {@link ZaakobjectListParameters}.
     * @return List of {@link Zaakobject} instances.
     */
    public Results<Zaakobject> listZaakobjecten(final ZaakobjectListParameters zaakobjectListParameters) {
        return zrcClient.zaakobjectList(zaakobjectListParameters);
    }

    /**
     * Partially update {@link Zaak}.
     *
     * @param zaakUUID UUID of {@link Zaak}.
     * @param zaak     {@link Zaak} with parts that need to be updated.
     * @return Updated {@link Zaak}
     */
    public Zaak patchZaak(final UUID zaakUUID, final Zaak zaak, final String toelichting) {
        if (toelichting != null) {
            zgwClientHeadersFactory.setAuditToelichting(toelichting);
        }
        return patchZaak(zaakUUID, zaak);
    }

    /**
     * Partially update {@link Zaak}.
     *
     * @param zaakUUID UUID of {@link Zaak}.
     * @param zaak     {@link Zaak} with parts that need to be updated.
     * @return Updated {@link Zaak}
     */
    public Zaak patchZaak(final UUID zaakUUID, final Zaak zaak) {
        return zrcClient.zaakPartialUpdate(zaakUUID, zaak);
    }

    /**
     * List instances of {@link Zaak} filtered by {@link ZaakListParameters}.
     *
     * @param filter {@link ZaakListParameters}.
     * @return List of {@link Zaak} instances.
     */
    public Results<Zaak> listZaken(final ZaakListParameters filter) {
        return zrcClient.zaakList(filter);
    }

    /**
     * List instances of {@link Zaak} filtered by {@link ZaakListParameters}.
     *
     * @param filter {@link ZaakListParameters}.
     * @return List of {@link ZaakUuid} instances.
     */
    public Results<ZaakUuid> listZakenUuids(final ZaakListParameters filter) {
        return zrcClient.zaakListUuids(filter);
    }

    /**
     * List instances of {@link ZaakInformatieobject} filtered by {@link ZaakInformatieobjectListParameters}.
     *
     * @param filter {@link ZaakInformatieobjectListParameters}.
     * @return List of {@link ZaakInformatieobject} instances.
     */
    public List<ZaakInformatieobject> listZaakinformatieobjecten(final ZaakInformatieobjectListParameters filter) {
        return zrcClient.zaakinformatieobjectList(filter);
    }

    public List<ZaakInformatieobject> listZaakinformatieobjecten(final Zaak zaak) {
        final ZaakInformatieobjectListParameters parameters = new ZaakInformatieobjectListParameters();
        parameters.setZaak(zaak.getUrl());
        return listZaakinformatieobjecten(parameters);
    }

    public List<ZaakInformatieobject> listZaakinformatieobjecten(final EnkelvoudigInformatieObject informatieobject) {
        final ZaakInformatieobjectListParameters parameters = new ZaakInformatieobjectListParameters();
        parameters.setInformatieobject(informatieobject.getUrl());
        return listZaakinformatieobjecten(parameters);
    }

    /**
     * List instances of {@link Rol} filtered by {@link RolListParameters}.
     *
     * @param filter {@link RolListParameters}.
     * @return List of {@link Rol} instances.
     */
    public Results<Rol<?>> listRollen(final RolListParameters filter) {
        return zrcClient.rolList(filter);
    }

    /**
     * List all instances of {@link Rol} for a specific {@link Zaak}.
     *
     * @param zaak {@link Zaak}
     * @return List of {@link Rol}
     */
    public List<Rol<?>> listRollen(final Zaak zaak) {
        return zrcClient.rolList(new RolListParameters(zaak.getUrl())).getResults();
    }

    public Zaak readZaakByID(final String identificatie) {
        final ZaakListParameters zaakListParameters = new ZaakListParameters();
        zaakListParameters.setIdentificatie(identificatie);
        final Results<Zaak> zaakResults = listZaken(zaakListParameters);
        if (zaakResults.getCount() == 0) {
            throw new NotFoundException(String.format("Zaak met identificatie '%s' niet gevonden", identificatie));
        } else if (zaakResults.getCount() > 1) {
            throw new IllegalStateException(String.format("Meerdere zaken met identificatie '%s' gevonden", identificatie));
        }
        return zaakResults.getResults().getFirst();
    }

    public void verplaatsInformatieobject(
            final EnkelvoudigInformatieObject informatieobject,
            final Zaak oudeZaak,
            final Zaak nieuweZaak
    ) {
        final ZaakInformatieobjectListParameters parameters = new ZaakInformatieobjectListParameters();
        parameters.setInformatieobject(informatieobject.getUrl());
        parameters.setZaak(oudeZaak.getUrl());
        List<ZaakInformatieobject> zaakInformatieobjecten = listZaakinformatieobjecten(parameters);
        if (zaakInformatieobjecten.isEmpty()) {
            throw new NotFoundException(String.format("Geen ZaakInformatieobject gevonden voor Zaak: '%s' en InformatieObject: '%s'",
                    oudeZaak.getIdentificatie(),
                    extractUuid(informatieobject.getInhoud())));
        }

        final ZaakInformatieobject oudeZaakInformatieobject = zaakInformatieobjecten.getFirst();
        final ZaakInformatieobject nieuweZaakInformatieObject = new ZaakInformatieobject();
        nieuweZaakInformatieObject.setZaak(nieuweZaak.getUrl());
        nieuweZaakInformatieObject.setInformatieobject(informatieobject.getUrl());
        nieuweZaakInformatieObject.setTitel(oudeZaakInformatieobject.getTitel());
        nieuweZaakInformatieObject.setBeschrijving(oudeZaakInformatieobject.getBeschrijving());


        final String toelichting = "%s -> %s".formatted(oudeZaak.getIdentificatie(), nieuweZaak.getIdentificatie());
        createZaakInformatieobject(nieuweZaakInformatieObject, toelichting);
        deleteZaakInformatieobject(oudeZaakInformatieobject.getUuid(), toelichting, "Verplaatst");
    }

    public void koppelInformatieobject(
            final EnkelvoudigInformatieObject informatieobject,
            final Zaak nieuweZaak,
            final String toelichting
    ) {
        List<ZaakInformatieobject> zaakInformatieobjecten = listZaakinformatieobjecten(informatieobject);
        if (!zaakInformatieobjecten.isEmpty()) {
            final UUID zaakUuid = extractUuid(zaakInformatieobjecten.getFirst().getZaak());
            throw new IllegalStateException(String.format("Informatieobject is reeds gekoppeld aan zaak '%s'", zaakUuid));
        }
        final ZaakInformatieobject nieuweZaakInformatieObject = new ZaakInformatieobject();
        nieuweZaakInformatieObject.setZaak(nieuweZaak.getUrl());
        nieuweZaakInformatieObject.setInformatieobject(informatieobject.getUrl());
        nieuweZaakInformatieObject.setTitel(informatieobject.getTitel());
        nieuweZaakInformatieObject.setBeschrijving(informatieobject.getBeschrijving());
        createZaakInformatieobject(nieuweZaakInformatieObject, toelichting);
    }

    /**
     * List all instances of {@link ZRCAuditTrailRegel} for a specific {@link Zaak}.
     *
     * @param zaakUUID UUID of {@link Zaak}.
     * @return List of {@link ZRCAuditTrailRegel} instances.
     */
    public List<ZRCAuditTrailRegel> listAuditTrail(final UUID zaakUUID) {
        return zrcClient.listAuditTrail(zaakUUID);
    }

    public Resultaat createResultaat(final Resultaat resultaat) {
        if (resultaat.getToelichting() != null) {
            zgwClientHeadersFactory.setAuditToelichting(resultaat.getToelichting());
        }
        return zrcClient.resultaatCreate(resultaat);
    }

    public Resultaat updateResultaat(final Resultaat resultaat) {
        if (resultaat.getToelichting() != null) {
            zgwClientHeadersFactory.setAuditToelichting(resultaat.getToelichting());
        }
        return zrcClient.resultaatUpdate(resultaat.getUuid(), resultaat);
    }

    public void deleteResultaat(final UUID resultaatUUID) {
        zrcClient.resultaatDelete(resultaatUUID);
    }

    public Zaak createZaak(final Zaak zaak) {
        if (zaak.getToelichting() != null) {
            zgwClientHeadersFactory.setAuditToelichting(zaak.getToelichting());
        }
        return zrcClient.zaakCreate(zaak);
    }

    public Status createStatus(final Status status) {
        if (status.getStatustoelichting() != null) {
            zgwClientHeadersFactory.setAuditToelichting(status.getStatustoelichting());
        }
        return zrcClient.statusCreate(status);
    }

    public URI createUrlExternToZaak(final UUID zaakUUID) {
        return UriBuilder.fromUri(zgwApiUrlExtern).path(ZrcClient.class).path(ZrcClient.class, "zaakRead").build(zaakUUID);
    }

    public boolean heeftOpenDeelzaken(final Zaak zaak) {
        return zaak.getDeelzaken().stream()
                .map(this::readZaak).anyMatch(Zaak::isOpen);
    }

    private void deleteDeletedRollen(final Collection<Rol<?>> current, final Collection<Rol<?>> rollen, final String toelichting) {
        current.stream()
                .filter(oud -> rollen.stream()
                        .noneMatch(oud::equalBetrokkeneRol))
                .forEach(rol -> deleteRol(rol, toelichting));
    }

    private void deleteUpdatedRollen(final Collection<Rol<?>> current, final Collection<Rol<?>> rollen, final String toelichting) {
        current.stream()
                .filter(oud -> rollen.stream()
                        .filter(oud::equalBetrokkeneRol)
                        .anyMatch(nieuw -> !nieuw.equals(oud)))
                .forEach(rol -> deleteRol(rol, toelichting));
    }

    private void createUpdatedRollen(final Collection<Rol<?>> current, final Collection<Rol<?>> rollen, final String toelichting) {
        rollen.stream()
                .filter(nieuw -> current.stream()
                        .filter(nieuw::equalBetrokkeneRol)
                        .anyMatch(oud -> !oud.equals(nieuw)))
                .forEach(rol -> createRol(rol, toelichting));
    }

    private void createCreatedRollen(final Collection<Rol<?>> currentRollen, final Collection<Rol<?>> rollen, final String toelichting) {
        rollen.stream()
                .filter(nieuw -> currentRollen.stream()
                        .noneMatch(nieuw::equalBetrokkeneRol))
                .forEach(rol -> createRol(rol, toelichting));
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
                .register(ZgwFoutExceptionMapper.class)
                .register(ZgwValidatieFoutResponseExceptionMapper.class)
                .register(ZrcResponseExceptionMapper.class)
                .register(JsonbConfiguration.class)
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, zgwClientHeadersFactory.generateJWTToken())
                .header(ZrcClient.ACCEPT_CRS, ZrcClient.ACCEPT_CRS_VALUE)
                .header(ZrcClient.CONTENT_CRS, ZrcClient.ACCEPT_CRS_VALUE);
    }
}
