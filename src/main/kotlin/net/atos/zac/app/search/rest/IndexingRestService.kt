/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.search.rest

import jakarta.inject.Inject
import jakarta.inject.Singleton
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import net.atos.zac.zoeken.IndexingService
import net.atos.zac.zoeken.model.zoekobject.ZoekObjectType
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor

@Singleton
@Path("indexeren")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@NoArgConstructor
@AllOpen
class IndexingRestService @Inject constructor(
    private val indexingService: IndexingService
) {
    @GET
    @Path("herindexeren/{type}")
    fun reindex(@PathParam("type") type: ZoekObjectType) =
        indexingService.reindex(type)

    @POST
    @Path("commit-pending-changes-to-search-index")
    fun commit() =
        indexingService.commit()
}
