/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.notities;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import net.atos.zac.app.notities.converter.NotitieConverter;
import net.atos.zac.app.notities.model.RESTNotitie;
import net.atos.zac.notities.NotitieService;
import net.atos.zac.notities.model.Notitie;

@Singleton
@Path("notities")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class NotitiesRESTService {

    @Inject
    private NotitieService notitieService;

    @Inject
    private NotitieConverter notitieConverter;

    @GET
    @Path("{type}/{uuid}")
    public List<RESTNotitie> listNotities(@PathParam("type") final String type, @PathParam("uuid") final String uuid) {
        final UUID notitieUUID = UUID.fromString(uuid);
        return notitieService.listNotitiesForZaak(notitieUUID).stream()
                .map(notitieConverter::convertToRESTNotitie)
                .collect(Collectors.toList());
    }

    @POST
    public RESTNotitie createNotitie(final RESTNotitie restNotitie) {
        final Notitie notitie = notitieConverter.convertToNotitie(restNotitie);
        return notitieConverter.convertToRESTNotitie(notitieService.createNotitie(notitie));
    }

    @PATCH
    public RESTNotitie updateNotitie(final RESTNotitie restNotitie) {
        final Notitie notitie = notitieConverter.convertToNotitie(restNotitie);
        return notitieConverter.convertToRESTNotitie(notitieService.updateNotitie(notitie));
    }

    @DELETE
    @Path("{id}")
    public void deleteNotitie(@PathParam("id") final Long id) {
        notitieService.deleteNotitie(id);
    }
}
