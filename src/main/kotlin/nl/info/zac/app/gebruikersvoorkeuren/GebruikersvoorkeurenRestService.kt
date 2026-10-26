/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.gebruikersvoorkeuren

import jakarta.enterprise.inject.Instance
import jakarta.inject.Inject
import jakarta.inject.Singleton
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import nl.info.zac.app.gebruikersvoorkeuren.model.RestDashboardCardInstelling
import nl.info.zac.app.gebruikersvoorkeuren.model.RestTabelGegevens
import nl.info.zac.app.gebruikersvoorkeuren.model.RestZoekopdracht
import nl.info.zac.app.gebruikersvoorkeuren.model.toDashboardCardInstelling
import nl.info.zac.app.gebruikersvoorkeuren.model.toRestDashboardCardInstelling
import nl.info.zac.app.gebruikersvoorkeuren.model.toRestZoekopdracht
import nl.info.zac.app.gebruikersvoorkeuren.model.toZoekopdracht
import nl.info.zac.app.policy.model.toRestWerklijstRechten
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.gebruikersvoorkeuren.GebruikersvoorkeurenService
import nl.info.zac.gebruikersvoorkeuren.model.DashboardCardInstelling
import nl.info.zac.gebruikersvoorkeuren.model.TabelInstellingen
import nl.info.zac.gebruikersvoorkeuren.model.Werklijst
import nl.info.zac.gebruikersvoorkeuren.model.Zoekopdracht
import nl.info.zac.gebruikersvoorkeuren.model.ZoekopdrachtListParameters
import nl.info.zac.policy.PolicyService
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor

@Singleton
@Path("gebruikersvoorkeuren")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@NoArgConstructor
@AllOpen
class GebruikersvoorkeurenRestService @Inject constructor(
    private val gebruikersvoorkeurenService: GebruikersvoorkeurenService,
    private val loggedInUserInstance: Instance<LoggedInUser>,
    private val policyService: PolicyService
) {
    @GET
    @Path("zoekopdracht/{lijstID}")
    fun listZoekopdrachten(@PathParam("lijstID") lijstID: Werklijst): List<RestZoekopdracht> =
        gebruikersvoorkeurenService.listZoekopdrachten(
            ZoekopdrachtListParameters(lijstID, loggedInUserInstance.get().username)
        ).map(Zoekopdracht::toRestZoekopdracht)

    @DELETE
    @Path("zoekopdracht/{id}")
    fun deleteZoekopdracht(@PathParam("id") id: Long) = gebruikersvoorkeurenService.deleteZoekopdracht(id)

    @POST
    @Path("zoekopdracht")
    fun createOrUpdateZoekopdracht(restZoekopdracht: RestZoekopdracht): RestZoekopdracht =
        restZoekopdracht.toZoekopdracht(loggedInUserInstance.get())
            .let(gebruikersvoorkeurenService::createZoekopdracht)
            .toRestZoekopdracht()

    @PUT
    @Path("zoekopdracht/actief")
    fun setZoekopdrachtActief(restZoekopdracht: RestZoekopdracht) =
        gebruikersvoorkeurenService.setActief(restZoekopdracht.toZoekopdracht(loggedInUserInstance.get()))

    @DELETE
    @Path("zoekopdracht/{werklijst}/actief")
    fun removeZoekopdrachtActief(@PathParam("werklijst") werklijst: Werklijst) =
        gebruikersvoorkeurenService.removeActief(ZoekopdrachtListParameters(werklijst, loggedInUserInstance.get().username))

    @GET
    @Path("tabel-gegevens/{werklijst}")
    fun readTabelGegevens(@PathParam("werklijst") werklijst: Werklijst): RestTabelGegevens {
        val tabelInstellingen = gebruikersvoorkeurenService.readTabelInstellingen(
            werklijst,
            loggedInUserInstance.get().username
        )
        return RestTabelGegevens(
            aantalPerPagina = tabelInstellingen.aantalPerPagina,
            pageSizeOptions = TabelInstellingen.PAGE_SIZE_OPTIONS,
            werklijstRechten = policyService.readWerklijstRechten().toRestWerklijstRechten()
        )
    }

    @PUT
    @Path("aantal-per-pagina/{werklijst}/{aantal}")
    fun updateAantalItemsPerPagina(
        @PathParam("werklijst") werklijst: Werklijst,
        @PathParam("aantal") aantal: Int
    ) {
        if (aantal in TabelInstellingen.AANTAL_PER_PAGINA_MIN..TabelInstellingen.AANTAL_PER_PAGINA_MAX) {
            TabelInstellingen().apply {
                aantalPerPagina = aantal
                lijstID = werklijst
                medewerkerID = loggedInUserInstance.get().username
            }.let(gebruikersvoorkeurenService::updateTabelInstellingen)
        }
    }

    @GET
    @Path("dasboardcard/actief")
    fun listDashboardCards(): List<RestDashboardCardInstelling> =
        gebruikersvoorkeurenService.listDashboardCards(loggedInUserInstance.get().username)
            .map(DashboardCardInstelling::toRestDashboardCardInstelling)

    @PUT
    @Path("dasboardcard/actief")
    fun updateDashboardCards(instellingen: List<RestDashboardCardInstelling>): List<RestDashboardCardInstelling> {
        gebruikersvoorkeurenService.updateDashboardCards(
            loggedInUserInstance.get().username,
            instellingen.map(RestDashboardCardInstelling::toDashboardCardInstelling)
        )
        return listDashboardCards()
    }

    @PUT
    @Path("dasboardcard")
    fun addDashboardCard(instelling: RestDashboardCardInstelling): List<RestDashboardCardInstelling> {
        gebruikersvoorkeurenService.addDashboardCard(
            loggedInUserInstance.get().username,
            instelling.toDashboardCardInstelling()
        )
        return listDashboardCards()
    }

    @DELETE
    @Path("dasboardcard")
    fun deleteDashboardCard(instelling: RestDashboardCardInstelling): List<RestDashboardCardInstelling> {
        gebruikersvoorkeurenService.deleteDashboardCard(
            loggedInUserInstance.get().username,
            instelling.toDashboardCardInstelling()
        )
        return listDashboardCards()
    }
}
