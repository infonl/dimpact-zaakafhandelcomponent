/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.client.vrl;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;

import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import net.atos.client.vrl.exception.VrlRuntimeExceptionMapper;
import net.atos.client.vrl.model.generated.ProcesType;

/**
 * Referentielijsten & Selectielijst API
 *
 * <p> Een API om referentielijstwaarden en de gemeentelijke selectielijst te benaderen. ## Selectielijst De [Gemeentelijke
 * Selectielijst](<a href="https://vng.nl/selectielijst">...</a>) is relevant in het kader van archivering. **Zaakgericht werken** Bij het
 * configureren van
 * zaaktypes (en resultaattypes) in de catalogus API refereren een aantal resources naar resources binnen de Selectielijst API. Het gaat dan
 * om de `ProcesType` en `Resultaat` resources. ## Referentielijsten Referentielijsten bevat een standaardset aan waarden. Deze waarden zijn
 * net té dynamisch om in een enum opgenomen te worden, maar er is wel behoefte om deze landelijk te standaardiseren. Een voorbeeld hiervan
 * is de set aan mogelijke communicatiekanalen. ## Autorisatie Deze APIs zijn alleen-lezen, en behoeven geen autorisatie. ## Inhoud De
 * inhoud wordt beheerd door VNG Realisatie. Om de inhoud van referentielijsten bij te werken, contacteer dan VNG Realisatie via e-mail of
 * op Github. De inhoud van de Gemeentelijke Selectielijst wordt geïmporteerd vanuit de gepubliceerde Excel-bestanden.
 *
 */
@RegisterRestClient(configKey = "VRL-API-Client")
@RegisterProvider(VrlRuntimeExceptionMapper.class)
@Produces({"application/json", "application/problem+json"})
@Path("api/v1/procestypen")
public interface ProcestypenApi {

    /**
     * Ontsluit de selectielijst procestypen.
     * <p>
     * Procestypen worden gerefereerd in zaaktypecatalogi - bij het configureren van een zaaktype wordt aangegeven welk procestype van
     * toepassing is, zodat het archiefregime van zaken bepaald kan worden. Zie
     * <a href="https://vng.nl/files/vng/20170706-selectielijst-gemeenten-intergemeentelijke-organen-2017.pdf">...</a> voor de bron van de
     * inhoud.
     */
    @GET
    List<ProcesType> procestypeList(@QueryParam("jaar") BigDecimal jaar);

    /**
     * Ontsluit de selectielijst procestypen.
     * <p>
     * Procestypen worden gerefereerd in zaaktypecatalogi - bij het configureren van een zaaktype wordt aangegeven welk procestype van
     * toepassing is, zodat het archiefregime van zaken bepaald kan worden. Zie
     * <a href="https://vng.nl/files/vng/20170706-selectielijst-gemeenten-intergemeentelijke-organen-2017.pdf">...</a> voor de bron van de
     * inhoud.
     */
    @GET
    @Path("/{uuid}")
    ProcesType procestypeRead(@PathParam("uuid") UUID uuid);
}
