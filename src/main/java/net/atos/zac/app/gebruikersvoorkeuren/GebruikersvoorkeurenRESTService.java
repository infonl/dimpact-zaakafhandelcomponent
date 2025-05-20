/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.gebruikersvoorkeuren;

import static nl.info.zac.app.policy.model.RestWerklijstRechtenKt.toRestWerklijstRechten;

import java.util.List;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import net.atos.zac.app.gebruikersvoorkeuren.converter.RESTDashboardCardInstellingConverter;
import net.atos.zac.app.gebruikersvoorkeuren.converter.RESTZoekopdrachtConverter;
import net.atos.zac.app.gebruikersvoorkeuren.model.RESTDashboardCardInstelling;
import net.atos.zac.app.gebruikersvoorkeuren.model.RESTTabelGegevens;
import net.atos.zac.app.gebruikersvoorkeuren.model.RESTZoekopdracht;
import net.atos.zac.gebruikersvoorkeuren.GebruikersvoorkeurenService;
import net.atos.zac.gebruikersvoorkeuren.model.TabelInstellingen;
import net.atos.zac.gebruikersvoorkeuren.model.Werklijst;
import net.atos.zac.gebruikersvoorkeuren.model.Zoekopdracht;
import net.atos.zac.gebruikersvoorkeuren.model.ZoekopdrachtListParameters;
import nl.info.zac.authentication.LoggedInUser;
import nl.info.zac.policy.PolicyService;

@Singleton
@Path("gebruikersvoorkeuren")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class GebruikersvoorkeurenRESTService {

    @Inject
    private GebruikersvoorkeurenService gebruikersvoorkeurenService;

    @Inject
    private Instance<LoggedInUser> loggedInUserInstance;

    @Inject
    private PolicyService policyService;

    @GET
    @Path("zoekopdracht/{lijstID}")
    public List<RESTZoekopdracht> listZoekopdrachten(@PathParam("lijstID") final Werklijst lijstID) {
        final List<Zoekopdracht> zoekopdrachten = gebruikersvoorkeurenService.listZoekopdrachten(
                new ZoekopdrachtListParameters(lijstID, loggedInUserInstance.get().getId()));
        return RESTZoekopdrachtConverter.convert(zoekopdrachten);
    }

    @DELETE
    @Path("zoekopdracht/{id}")
    public void deleteZoekopdracht(@PathParam("id") final long id) {
        gebruikersvoorkeurenService.deleteZoekopdracht(id);
    }

    @POST
    @Path("zoekopdracht")
    public RESTZoekopdracht createOrUpdateZoekopdracht(final RESTZoekopdracht restZoekopdracht) {
        final Zoekopdracht zoekopdracht = RESTZoekopdrachtConverter.convert(restZoekopdracht, loggedInUserInstance);
        return RESTZoekopdrachtConverter.convert(gebruikersvoorkeurenService.createZoekopdracht(zoekopdracht));
    }

    @PUT
    @Path("zoekopdracht/actief")
    public void setZoekopdrachtActief(final RESTZoekopdracht restZoekopdracht) {
        final Zoekopdracht zoekopdracht = RESTZoekopdrachtConverter.convert(restZoekopdracht, loggedInUserInstance);
        gebruikersvoorkeurenService.setActief(zoekopdracht);
    }

    @DELETE
    @Path("zoekopdracht/{werklijst}/actief")
    public void removeZoekopdrachtActief(@PathParam("werklijst") final Werklijst werklijst) {
        gebruikersvoorkeurenService.removeActief(new ZoekopdrachtListParameters(werklijst, loggedInUserInstance.get().getId()));
    }

    @GET
    @Path("tabel-gegevens/{werklijst}")
    public RESTTabelGegevens readTabelGegevens(@PathParam("werklijst") final Werklijst werklijst) {
        final RESTTabelGegevens restTabelGegevens = new RESTTabelGegevens();
        final TabelInstellingen tabelInstellingen = gebruikersvoorkeurenService.readTabelInstellingen(werklijst,
                loggedInUserInstance.get().getId()
        );
        restTabelGegevens.aantalPerPagina = tabelInstellingen.getAantalPerPagina();
        restTabelGegevens.pageSizeOptions = TabelInstellingen.PAGE_SIZE_OPTIONS;
        restTabelGegevens.werklijstRechten = toRestWerklijstRechten(policyService.readWerklijstRechten());
        return restTabelGegevens;
    }

    @PUT
    @Path("aantal-per-pagina/{werklijst}/{aantal}")
    public void updateAantalItemsPerPagina(@PathParam("werklijst") final Werklijst werklijst, @PathParam("aantal") final int aantal) {
        if (aantal <= TabelInstellingen.AANTAL_PER_PAGINA_MAX && aantal >= TabelInstellingen.AANTAL_PER_PAGINA_MIN) {
            final TabelInstellingen tabelInstellingen = new TabelInstellingen();
            tabelInstellingen.setAantalPerPagina(aantal);
            tabelInstellingen.setLijstID(werklijst);
            tabelInstellingen.setMedewerkerID(loggedInUserInstance.get().getId());
            gebruikersvoorkeurenService.updateTabelInstellingen(tabelInstellingen);
        }
    }

    @GET
    @Path("dasboardcard/actief")
    public List<RESTDashboardCardInstelling> listDashboardCards() {
        return RESTDashboardCardInstellingConverter.convert(
                gebruikersvoorkeurenService.listDashboardCards(loggedInUserInstance.get().getId()));
    }

    @PUT
    @Path("dasboardcard/actief")
    public List<RESTDashboardCardInstelling> updateDashboardCards(final List<RESTDashboardCardInstelling> instellingen) {
        gebruikersvoorkeurenService.updateDashboardCards(
                loggedInUserInstance.get().getId(),
                instellingen.stream().map(RESTDashboardCardInstellingConverter::convert).toList());
        return listDashboardCards();
    }

    @PUT
    @Path("dasboardcard")
    public List<RESTDashboardCardInstelling> addDashboardCard(RESTDashboardCardInstelling instelling) {
        gebruikersvoorkeurenService.addDashboardCard(
                loggedInUserInstance.get().getId(),
                RESTDashboardCardInstellingConverter.convert(instelling));
        return listDashboardCards();
    }

    @DELETE
    @Path("dasboardcard")
    public List<RESTDashboardCardInstelling> deleteDashboardCard(final RESTDashboardCardInstelling instelling) {
        gebruikersvoorkeurenService.deleteDashboardCard(
                loggedInUserInstance.get().getId(),
                RESTDashboardCardInstellingConverter.convert(instelling));
        return listDashboardCards();
    }
}
