/*
 * SPDX-FileCopyrightText: 2022 Atos, 2023-2024 Lifely
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

import net.atos.zac.app.configuratie.converter.RESTTaalConverter;
import net.atos.zac.app.configuratie.model.RESTTaal;
import net.atos.zac.configuratie.ConfiguratieService;

/**
 *
 */
@Path("configuratie")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Singleton
public class ConfiguratieRESTService {

    @Inject private ConfiguratieService configuratieService;

    @Inject private RESTTaalConverter taalConverter;

    @GET
    @Path("talen")
    public List<RESTTaal> listTalen() {
        return taalConverter.convert(configuratieService.listTalen());
    }

    @GET
    @Path("talen/default")
    public RESTTaal readDefaultTaal() {
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
