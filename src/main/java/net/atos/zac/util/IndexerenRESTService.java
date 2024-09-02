/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.util;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import net.atos.zac.zoeken.IndexingService;
import net.atos.zac.zoeken.model.index.ZoekObjectType;

@Path("indexeren")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class IndexerenRESTService {

    @Inject
    private IndexingService indexingService;

    @GET
    @Path("herindexeren/{type}")
    public void herindexeren(@PathParam("type") ZoekObjectType type) {
        indexingService.herindexeren(type);
    }

    @POST
    @Path("commit-pending-changes-to-search-index")
    public void commit() {
        indexingService.commit();
    }
}
