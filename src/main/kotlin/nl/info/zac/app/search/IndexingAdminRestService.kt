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
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
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
    private val indexingService: IndexingService,

    /**
     * Declare a Kotlin coroutine dispatcher here so that it can be overridden in unit tests with a test dispatcher
     * while in normal operation it will be injected using [nl.info.zac.util.CoroutineDispatcherProducer].
     */
    private val dispatcher: CoroutineDispatcher
) {
    /**
     * Reindexing can be a long-running operation, so it is run asynchronously.
     */
    @GET
    @Path("herindexeren/{type}")
    fun reindex(@PathParam("type") type: ZoekObjectType): Response {
        CoroutineScope(dispatcher).launch {
            indexingService.reindex(type)
        }
        return Response.accepted().build()
    }

    /**
     * Reindexing can be a long-running operation, so it is run asynchronously.
     */
    @GET
    @Path("herindexeren")
    fun reindexAll(): Response {
        CoroutineScope(dispatcher).launch {
            indexingService.reindexAll()
        }
        return Response.accepted().build()
    }
}
