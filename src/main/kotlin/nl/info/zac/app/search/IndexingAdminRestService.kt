/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.search

import jakarta.inject.Inject
import jakarta.inject.Singleton
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import net.atos.zac.search.IndexingService
import net.atos.zac.search.model.zoekobject.ZoekObjectType
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor

/**
 * Admin REST service for indexing.
 * Note that the endpoints in this service are currently not protected by any security mechanism.
 * See the 'web.xml' file for details.
 */
@Singleton
@Path("indexeren")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@NoArgConstructor
@AllOpen
class IndexingAdminRestService @Inject constructor(
    private val indexingService: IndexingService
) {
    @GET
    @Path("herindexeren/{type}")
    fun reindex(@PathParam("type") type: ZoekObjectType) = indexingService.reindex(type)

    @POST
    @Path("commit-pending-changes-to-search-index")
    fun commit() = indexingService.commit()
}
