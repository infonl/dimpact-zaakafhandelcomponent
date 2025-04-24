/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.client.kvk

import jakarta.ws.rs.DefaultValue
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import net.atos.client.kvk.exception.KvkRuntimeExceptionMapper
import net.atos.client.kvk.util.KvkClientHeadersFactory
import net.atos.zac.util.MediaTypes
import nl.info.client.kvk.basisprofiel.model.generated.Basisprofiel
import nl.info.client.kvk.basisprofiel.model.generated.Eigenaar
import nl.info.client.kvk.basisprofiel.model.generated.Vestiging
import nl.info.client.kvk.basisprofiel.model.generated.VestigingList
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient

/**
 * API Basisprofiel
 *
 *
 * Documentatie voor API Basisprofiel.
 */
@RegisterRestClient(configKey = "KVK-API-Client")
@RegisterClientHeaders(KvkClientHeadersFactory::class)
@RegisterProvider(KvkRuntimeExceptionMapper::class)
@Produces(MediaTypes.MEDIA_TYPE_HAL_JSON)
@Path("api/v1/basisprofielen/{kvkNummer}")
interface BasisprofielClient {
    /**
     * Voor een specifiek bedrijf basisinformatie opvragen.
     */
    @GET
    fun getBasisprofielByKvkNummer(
        @PathParam("kvkNummer") kvkNummer: String?,
        @QueryParam("geoData") @DefaultValue("false") geoData: Boolean?
    ): Basisprofiel?

    /**
     * Voor een specifiek bedrijf eigenaar informatie opvragen.
     */
    @GET
    @Path("/eigenaar")
    fun getEigenaar(@PathParam("kvkNummer") kvkNummer: String?, @QueryParam("geoData") @DefaultValue("false") geoData: Boolean?): Eigenaar?

    /**
     * Voor een specifiek bedrijf hoofdvestigingsinformatie opvragen.
     */
    @GET
    @Path("/hoofdvestiging")
    fun getHoofdvestiging(
        @PathParam("kvkNummer") kvkNummer: String?,
        @QueryParam("geoData") @DefaultValue("false") geoData: Boolean?
    ): Vestiging?

    /**
     * Voor een specifiek bedrijf een lijst met vestigingen opvragen.
     */
    @GET
    @Path("/vestigingen")
    fun getVestigingen(@PathParam("kvkNummer") kvkNummer: String?): VestigingList?
}
