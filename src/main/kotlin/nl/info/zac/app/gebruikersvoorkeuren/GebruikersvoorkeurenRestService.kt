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
import net.atos.zac.app.gebruikersvoorkeuren.converter.RESTDashboardCardInstellingConverter
import net.atos.zac.app.gebruikersvoorkeuren.converter.RESTZoekopdrachtConverter
import net.atos.zac.app.gebruikersvoorkeuren.model.RESTDashboardCardInstelling
import net.atos.zac.app.gebruikersvoorkeuren.model.RESTTabelGegevens
import net.atos.zac.app.gebruikersvoorkeuren.model.RESTZoekopdracht
import net.atos.zac.gebruikersvoorkeuren.model.TabelInstellingen
import net.atos.zac.gebruikersvoorkeuren.model.Werklijst
import net.atos.zac.gebruikersvoorkeuren.model.ZoekopdrachtListParameters
import nl.info.zac.app.policy.model.toRestWerklijstRechten
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.gebruikersvoorkeuren.GebruikersvoorkeurenService
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
    fun listZoekopdrachten(@PathParam("lijstID") lijstID: Werklijst): List<RESTZoekopdracht> =
        gebruikersvoorkeurenService.listZoekopdrachten(
            ZoekopdrachtListParameters(lijstID, loggedInUserInstance.get().id)
        ).let(RESTZoekopdrachtConverter::convert)

    @DELETE
    @Path("zoekopdracht/{id}")
    fun deleteZoekopdracht(@PathParam("id") id: Long) = gebruikersvoorkeurenService.deleteZoekopdracht(id)

    @POST
    @Path("zoekopdracht")
    fun createOrUpdateZoekopdracht(restZoekopdracht: RESTZoekopdracht): RESTZoekopdracht =
        RESTZoekopdrachtConverter.convert(restZoekopdracht, loggedInUserInstance)
            .let(gebruikersvoorkeurenService::createZoekopdracht)
            .let(RESTZoekopdrachtConverter::convert)

    @PUT
    @Path("zoekopdracht/actief")
    fun setZoekopdrachtActief(restZoekopdracht: RESTZoekopdracht) =
        gebruikersvoorkeurenService.setActief(RESTZoekopdrachtConverter.convert(restZoekopdracht, loggedInUserInstance))

    @DELETE
    @Path("zoekopdracht/{werklijst}/actief")
    fun removeZoekopdrachtActief(@PathParam("werklijst") werklijst: Werklijst) =
        gebruikersvoorkeurenService.removeActief(ZoekopdrachtListParameters(werklijst, loggedInUserInstance.get().id))

    @GET
    @Path("tabel-gegevens/{werklijst}")
    fun readTabelGegevens(@PathParam("werklijst") werklijst: Werklijst): RESTTabelGegevens {
        val tabelInstellingen = gebruikersvoorkeurenService.readTabelInstellingen(
            werklijst,
            loggedInUserInstance.get().id
        )
        return RESTTabelGegevens().apply {
            aantalPerPagina = tabelInstellingen.aantalPerPagina
            pageSizeOptions = TabelInstellingen.PAGE_SIZE_OPTIONS
            werklijstRechten = policyService.readWerklijstRechten().toRestWerklijstRechten()
        }
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
                medewerkerID = loggedInUserInstance.get().id
            }.let(gebruikersvoorkeurenService::updateTabelInstellingen)
        }
    }

    @GET
    @Path("dasboardcard/actief")
    fun listDashboardCards(): List<RESTDashboardCardInstelling> =
        gebruikersvoorkeurenService.listDashboardCards(loggedInUserInstance.get().id)
            .let(RESTDashboardCardInstellingConverter::convert)

    @PUT
    @Path("dasboardcard/actief")
    fun updateDashboardCards(instellingen: List<RESTDashboardCardInstelling>): List<RESTDashboardCardInstelling> {
        gebruikersvoorkeurenService.updateDashboardCards(
            loggedInUserInstance.get().id,
            instellingen.map(RESTDashboardCardInstellingConverter::convert)
        )
        return listDashboardCards()
    }

    @PUT
    @Path("dasboardcard")
    fun addDashboardCard(instelling: RESTDashboardCardInstelling): List<RESTDashboardCardInstelling> {
        gebruikersvoorkeurenService.addDashboardCard(
            loggedInUserInstance.get().id,
            RESTDashboardCardInstellingConverter.convert(instelling)
        )
        return listDashboardCards()
    }

    @DELETE
    @Path("dasboardcard")
    fun deleteDashboardCard(instelling: RESTDashboardCardInstelling): List<RESTDashboardCardInstelling> {
        gebruikersvoorkeurenService.deleteDashboardCard(
            loggedInUserInstance.get().id,
            RESTDashboardCardInstellingConverter.convert(instelling)
        )
        return listDashboardCards()
    }
}
