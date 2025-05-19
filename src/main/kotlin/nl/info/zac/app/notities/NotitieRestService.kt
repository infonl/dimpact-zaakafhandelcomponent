/*
 * SPDX-FileCopyrightText: 2021 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.notities

import jakarta.inject.Inject
import jakarta.inject.Singleton
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.PATCH
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import nl.info.zac.app.notities.converter.NotitieConverter
import nl.info.zac.app.notities.model.RestNotitie
import nl.info.zac.app.notities.model.toNotitie
import nl.info.zac.notities.NotitieService
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import java.util.UUID

@Singleton
@Path("notities")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@NoArgConstructor
@AllOpen
class NotitieRestService @Inject constructor(
    private val notitieService: NotitieService,
    private val notitieConverter: NotitieConverter
) {
    @GET
    @Path("{type}/{uuid}")
    fun listNotities(@PathParam("uuid") zaakUUID: UUID): List<RestNotitie> =
        notitieService.listNotitiesForZaak(zaakUUID)
            .map(notitieConverter::toRestNotitie)

    @POST
    fun createNotitie(restNotitie: RestNotitie): RestNotitie {
        val notitie = notitieService.createNotitie(restNotitie.toNotitie())
        return notitieConverter.toRestNotitie(notitie)
    }

    @PATCH
    fun updateNotitie(restNotitie: RestNotitie): RestNotitie {
        val updatedNotitie = notitieService.updateNotitie(restNotitie.toNotitie())
        return notitieConverter.toRestNotitie(updatedNotitie)
    }

    @DELETE
    @Path("{id}")
    fun deleteNotitie(@PathParam("id") id: Long) =
        notitieService.deleteNotitie(id)
}
