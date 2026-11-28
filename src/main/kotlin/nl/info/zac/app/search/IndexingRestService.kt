/*
 *  * SPDX-FileCopyrightText: 2025 Lifely
 *  * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.search

import jakarta.inject.Inject
import jakarta.inject.Singleton
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import nl.info.zac.search.IndexingService
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
class IndexingRestService @Inject constructor(
    private val indexingService: IndexingService
) {
    @POST
    @Path("commit-pending-changes-to-search-index")
    fun commit() = indexingService.commit()
}
