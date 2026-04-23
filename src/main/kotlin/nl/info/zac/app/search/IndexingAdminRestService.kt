/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
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
import nl.info.zac.authentication.InternalEndpoint
import nl.info.zac.search.IndexingService
import nl.info.zac.search.model.zoekobject.ZoekObjectType
import nl.info.zac.solr.SolrDeployerService
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
    private val solrDeployerService: SolrDeployerService
) {
    @GET
    @Path("herindexeren/{type}")
    fun reindex(@PathParam("type") type: ZoekObjectType) = indexingService.reindex(type)

    @GET
    @Path("herindexeren/{type}/{targetCollection}")
    fun reindexToCollection(
        @PathParam("type") type: ZoekObjectType,
        @PathParam("targetCollection") targetCollection: String
    ) = indexingService.reindex(type, targetCollection)

    @GET
    @Path("schema/{collection}")
    fun applySchema(@PathParam("collection") collection: String) =
        solrDeployerService.applySchemaToCollection(collection)
}
