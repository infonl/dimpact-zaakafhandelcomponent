/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.configuratie;

import static net.atos.zac.util.JsonbUtil.JSONB;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import net.atos.zac.app.configuratie.converter.RestTaalConverter;
import net.atos.zac.app.configuratie.model.RestTaal;
import net.atos.zac.configuratie.ConfiguratieService;

/**
 * Provides specific configuration items to a ZAC client.
 */
@Path("configuratie")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Singleton
public class ConfiguratieRestService {

    @Inject
    private ConfiguratieService configuratieService;

    @Inject
    private RestTaalConverter taalConverter;

    @GET
    @Path("talen")
    public List<RestTaal> listTalen() {
        return taalConverter.convert(configuratieService.listTalen());
    }

    @GET
    @Path("talen/default")
    public RestTaal readDefaultTaal() {
        return configuratieService.findDefaultTaal().map(taalConverter::convert).orElse(null);
    }

    @GET
    @Path("maxFileSizeMB")
    public long readMaxFileSizeMB() {
        return configuratieService.readMaxFileSizeMB();
    }

    @GET
    @Path("additionalAllowedFileTypes")
    public List<String> readAdditionalAllowedFileTypes() {
        return configuratieService.readAdditionalAllowedFileTypes();
    }

    @GET
    @Path("gemeente/code")
    public String readGemeenteCode() {
        return JSONB.toJson(configuratieService.readGemeenteCode());
    }

    @GET
    @Path("gemeente")
    public String readGemeenteNaam() {
        return JSONB.toJson(configuratieService.readGemeenteNaam());
    }
}
