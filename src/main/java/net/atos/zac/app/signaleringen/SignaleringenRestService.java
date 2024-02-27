/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.signaleringen;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import net.atos.client.zgw.drc.DRCClientService;
import net.atos.client.zgw.zrc.ZRCClientService;
import net.atos.zac.app.informatieobjecten.converter.RESTInformatieobjectConverter;
import net.atos.zac.app.informatieobjecten.model.RESTEnkelvoudigInformatieobject;
import net.atos.zac.app.signaleringen.converter.RESTSignaleringInstellingenConverter;
import net.atos.zac.app.signaleringen.model.RESTSignaleringInstellingen;
import net.atos.zac.app.taken.converter.RESTTaakConverter;
import net.atos.zac.app.taken.model.RESTTaak;
import net.atos.zac.app.zaken.converter.RESTZaakOverzichtConverter;
import net.atos.zac.app.zaken.model.RESTZaakOverzicht;
import net.atos.zac.authentication.LoggedInUser;
import net.atos.zac.flowable.TakenService;
import net.atos.zac.identity.IdentityService;
import net.atos.zac.identity.model.Group;
import net.atos.zac.signalering.SignaleringenService;
import net.atos.zac.signalering.model.SignaleringInstellingenZoekParameters;
import net.atos.zac.signalering.model.SignaleringSubject;
import net.atos.zac.signalering.model.SignaleringType;
import net.atos.zac.signalering.model.SignaleringZoekParameters;

@Path("signaleringen")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Singleton
public class SignaleringenRestService {

    @Inject
    private SignaleringenService signaleringenService;

    @Inject
    private ZRCClientService zrcClientService;

    @Inject
    private TakenService takenService;

    @Inject
    private DRCClientService drcClientService;

    @Inject
    private IdentityService identityService;

    @Inject
    private RESTZaakOverzichtConverter restZaakOverzichtConverter;

    @Inject
    private RESTTaakConverter restTaakConverter;

    @Inject
    private RESTInformatieobjectConverter restInformatieobjectConverter;

    @Inject
    private RESTSignaleringInstellingenConverter restSignaleringInstellingenConverter;

    @Inject
    private Instance<LoggedInUser> loggedInUserInstance;

    @GET
    @Path("/latest")
    public ZonedDateTime latestSignaleringen() {
        final SignaleringZoekParameters parameters = new SignaleringZoekParameters(loggedInUserInstance.get());
        return signaleringenService.latestSignalering(parameters);
    }

    @GET
    @Path("/zaken/{type}")
    public List<RESTZaakOverzicht> listZakenSignaleringen(
            @PathParam("type") final SignaleringType.Type signaleringsType) {
        final SignaleringZoekParameters parameters = new SignaleringZoekParameters(loggedInUserInstance.get())
                .types(signaleringsType)
                .subjecttype(SignaleringSubject.ZAAK);
        return signaleringenService.listSignaleringen(parameters).stream()
                .map(signalering -> zrcClientService.readZaak(UUID.fromString(signalering.getSubject())))
                .map(restZaakOverzichtConverter::convert)
                .toList();
    }

    @GET
    @Path("/taken/{type}")
    public List<RESTTaak> listTakenSignaleringen(@PathParam("type") final SignaleringType.Type signaleringsType) {
        final SignaleringZoekParameters parameters = new SignaleringZoekParameters(loggedInUserInstance.get())
                .types(signaleringsType)
                .subjecttype(SignaleringSubject.TAAK);
        return signaleringenService.listSignaleringen(parameters).stream()
                .map(signalering -> takenService.readTask(signalering.getSubject()))
                .map(restTaakConverter::convert)
                .toList();
    }

    @GET
    @Path("/informatieobjecten/{type}")
    public List<RESTEnkelvoudigInformatieobject> listInformatieobjectenSignaleringen(
            @PathParam("type") final SignaleringType.Type signaleringsType) {
        final SignaleringZoekParameters parameters = new SignaleringZoekParameters(loggedInUserInstance.get())
                .types(signaleringsType)
                .subjecttype(SignaleringSubject.DOCUMENT);
        return signaleringenService.listSignaleringen(parameters).stream()
                .map(signalering -> drcClientService.readEnkelvoudigInformatieobject(
                        UUID.fromString(signalering.getSubject())))
                .map(restInformatieobjectConverter::convertToREST)
                .toList();
    }

    @GET
    @Path("/instellingen")
    public List<RESTSignaleringInstellingen> listUserSignaleringInstellingen() {
        final SignaleringInstellingenZoekParameters parameters = new SignaleringInstellingenZoekParameters(
                loggedInUserInstance.get());
        return restSignaleringInstellingenConverter.convert(
                signaleringenService.listInstellingenInclusiefMogelijke(parameters));
    }

    @PUT
    @Path("/instellingen")
    public void updateUserSignaleringInstellingen(final RESTSignaleringInstellingen restInstellingen) {
        signaleringenService.createUpdateOrDeleteInstellingen(
                restSignaleringInstellingenConverter.convert(restInstellingen, loggedInUserInstance.get()));
    }

    @GET
    @Path("group/{groupId}/instellingen")
    public List<RESTSignaleringInstellingen> listGroupSignaleringInstellingen(
            @PathParam("groupId") final String groupId) {
        final Group group = identityService.readGroup(groupId);
        final SignaleringInstellingenZoekParameters parameters = new SignaleringInstellingenZoekParameters(group);
        return restSignaleringInstellingenConverter.convert(
                signaleringenService.listInstellingenInclusiefMogelijke(parameters));
    }

    @PUT
    @Path("group/{groupId}/instellingen")
    public void updateGroupSignaleringInstellingen(@PathParam("groupId") final String groupId,
            final RESTSignaleringInstellingen restInstellingen) {
        final Group group = identityService.readGroup(groupId);
        signaleringenService.createUpdateOrDeleteInstellingen(
                restSignaleringInstellingenConverter.convert(restInstellingen, group));
    }

    @GET
    @Path("/typen/dashboard")
    public List<SignaleringType.Type> listDashboardSignaleringTypen() {
        final SignaleringInstellingenZoekParameters parameters = new SignaleringInstellingenZoekParameters(
                loggedInUserInstance.get())
                        .dashboard();
        return signaleringenService.listInstellingen(parameters).stream()
                .map(instellingen -> instellingen.getType().getType())
                .toList();
    }
}
