/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 Lifely
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
    @GET
    @Path("herindexeren/{type}")
    fun reindex(@PathParam("type") type: ZoekObjectType) = indexingService.reindex(type)
}
