/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.search

import jakarta.inject.Inject
import jakarta.inject.Singleton
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import nl.info.zac.authentication.InternalEndpoint
import nl.info.zac.search.IndexingService
import nl.info.zac.search.model.zoekobject.ZoekObjectType
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor

/**
 * Internal REST service to reindex data in ZAC's Solr search engine on demand.
 * Not meant to be called by the ZAC frontend.
 */
@Singleton
@Path("internal/indexeren")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@NoArgConstructor
@AllOpen
@InternalEndpoint
class IndexingAdminRestService @Inject constructor(
    private val indexingService: IndexingService
) {
    /**
     * Reindexing can be a long-running operation, so it is run asynchronously on
     * [IndexingService]'s own background coroutine scope.
     */
    @GET
    @Path("herindexeren/{type}")
    fun reindex(@PathParam("type") type: ZoekObjectType): Response =
        if (indexingService.reindexAsync(type)) {
            Response.accepted().build()
        } else {
            Response.status(Response.Status.CONFLICT).build()
        }

    /**
     * Reindexing can be a long-running operation, so it is run asynchronously on
     * [IndexingService]'s own background coroutine scope.
     */
    @GET
    @Path("herindexeren")
    fun reindexAll(): Response {
        indexingService.reindexAllAsync()
        return Response.accepted().build()
    }
}
