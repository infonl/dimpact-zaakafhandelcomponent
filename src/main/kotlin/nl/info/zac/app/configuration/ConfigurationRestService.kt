/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.configuration

import jakarta.inject.Inject
import jakarta.inject.Singleton
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import net.atos.zac.util.JsonbUtil
import nl.info.zac.app.configuration.model.RestTaal
import nl.info.zac.app.configuration.model.toRestTaal
import nl.info.zac.app.configuration.model.toRestTalen
import nl.info.zac.configuration.ConfigurationService
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor

/**
 * Provides specific configuration items to a ZAC client.
 */
@Path("configuratie")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Singleton
@AllOpen
@NoArgConstructor
class ConfigurationRestService @Inject constructor(
    private val configurationService: ConfigurationService
) {
    @GET
    @Path("talen")
    fun listTalen(): List<RestTaal> = configurationService.listTalen().toRestTalen()

    @GET
    @Path("talen/default")
    fun readDefaultTaal(): RestTaal? = configurationService.findDefaultTaal()?.toRestTaal()

    @GET
    @Path("max-file-size-mb")
    fun readMaxFileSizeMB(): Long = configurationService.readMaxFileSizeMB()

    @GET
    @Path("additional-allowed-file-types")
    fun readAdditionalAllowedFileTypes(): List<String> = configurationService.readAdditionalAllowedFileTypes()

    @GET
    @Path("gemeente/code")
    fun readGemeenteCode(): String = JsonbUtil.JSONB.toJson(configurationService.readGemeenteCode())

    @GET
    @Path("gemeente")
    fun readGemeenteNaam(): String = JsonbUtil.JSONB.toJson(configurationService.readGemeenteNaam())

    /**
     * Returns whether doelbinding values must be configured per zaaktype in the admin UI.
     */
    @GET
    @Path("brp/doelbinding-setup-enabled")
    fun readBrpDoelbindingSetupEnabled(): Boolean =
        configurationService.readBrpConfiguration().isDoelbindingPerZaaktypeEnabled()
}
