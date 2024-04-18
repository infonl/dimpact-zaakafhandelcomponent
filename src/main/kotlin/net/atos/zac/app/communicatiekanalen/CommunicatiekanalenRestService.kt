package net.atos.zac.app.communicatiekanalen

import jakarta.inject.Inject
import jakarta.inject.Singleton
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import net.atos.zac.app.communicatiekanalen.converter.RestCommunicatiekanaalConverter
import net.atos.zac.app.communicatiekanalen.model.RESTCommunicatiekanaal
import net.atos.zac.zaaksturing.ReferentieTabelService
import net.atos.zac.zaaksturing.model.ReferentieTabel.Systeem
import nl.lifely.zac.util.AllOpen
import nl.lifely.zac.util.NoArgConstructor

@Path("communicatiekanalen")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Singleton
@NoArgConstructor
@AllOpen
class CommunicatiekanalenRestService @Inject constructor(
    private val referentieTabelService: ReferentieTabelService,
    private val restCommunicatiekanaalConverter: RestCommunicatiekanaalConverter
) {

    @GET
    fun listCommunicatiekanalen(): List<RESTCommunicatiekanaal> {
        val communicatieKanalen = referentieTabelService.readReferentieTabel(
            Systeem.COMMUNICATIEKANAAL.name
        ).waarden
        return restCommunicatiekanaalConverter.convertToRESTCommunicatiekanalen(communicatieKanalen)
    }

    @GET
    @Path("{id}")
    fun readCommunicatiekanaal(
        @PathParam("id") id: Long
    ): RESTCommunicatiekanaal {
        val kanaal = referentieTabelService.readReferentieTabel(
            Systeem.COMMUNICATIEKANAAL.name
        ).waarden.first { x -> x.id == id }
        return restCommunicatiekanaalConverter.convertToRESTCommunicatiekanaal(kanaal)
    }
}
