/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.admin;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpSession;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import net.atos.zac.app.admin.model.RESTDeletedSignaleringenResponse;
import net.atos.zac.authentication.ActiveSession;
import net.atos.zac.authentication.SecurityUtil;
import net.atos.zac.event.EventingService;
import net.atos.zac.signalering.SignaleringenService;
import net.atos.zac.util.event.JobEvent;
import net.atos.zac.util.event.JobId;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Path("admin/signaleringen")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class SignaleringenAdminRESTService {

    @Inject
    private EventingService eventingService;

    @Inject
    private SignaleringenService signaleringenService;

    @Inject
    @ActiveSession
    private Instance<HttpSession> httpSession;

    @Inject
    @ConfigProperty(name = "SIGNALERINGEN_DELETE_OLDER_THAN_DAYS")
    private Long deleteOlderThanDays;

    @GET
    @Path("send-zaak-signaleringen")
    public String zaakSignaleringenVerzenden() {
        SecurityUtil.setFunctioneelGebruiker(httpSession.get());
        eventingService.send(new JobEvent(JobId.SIGNALERINGEN_JOB));
        return String.format("Started sending zaak signaleringen using job: '%s'", JobId.SIGNALERINGEN_JOB.getName());
    }

    @DELETE
    @Path("delete-old")
    public RESTDeletedSignaleringenResponse deleteOldSignaleringen() {
        SecurityUtil.setFunctioneelGebruiker(httpSession.get());
        final var deletedSignaleringenCount = signaleringenService.deleteOldSignaleringen(deleteOlderThanDays);
        return new RESTDeletedSignaleringenResponse(deletedSignaleringenCount);
    }
}
