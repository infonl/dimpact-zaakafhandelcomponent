/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.productaanvraag

import jakarta.inject.Inject
import jakarta.inject.Singleton
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import net.atos.zac.util.MediaTypes
import nl.info.client.zgw.drc.DrcClientService
import nl.info.zac.app.productaanvraag.converter.toInboxProductaanvraagListParameters
import nl.info.zac.app.productaanvraag.converter.toRestInboxProductaanvragen
import nl.info.zac.app.productaanvraag.model.RestInboxProductaanvraagListParameters
import nl.info.zac.app.productaanvraag.model.RestInboxProductaanvraagResultaat
import nl.info.zac.policy.PolicyService
import nl.info.zac.policy.assertPolicy
import nl.info.zac.productaanvraag.InboxProductaanvraagService
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import java.util.UUID

@Singleton
@Path("inbox-productaanvragen")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@NoArgConstructor
@AllOpen
class InboxProductaanvraagRestService @Inject constructor(
    private val drcClientService: DrcClientService,
    private val policyService: PolicyService,
    private val inboxProductaanvraagService: InboxProductaanvraagService
) {
    @PUT
    @Path("")
    fun listInboxProductaanvragen(
        restListParameters: RestInboxProductaanvraagListParameters
    ): RestInboxProductaanvraagResultaat {
        assertPolicy(policyService.readWerklijstRechten().inbox)
        val resultaat = inboxProductaanvraagService.list(restListParameters.toInboxProductaanvraagListParameters())
        val restResultaat = RestInboxProductaanvraagResultaat(
            resultaat.items.toRestInboxProductaanvragen(),
            resultaat.count
        )
        val types = resultaat.typeFilter
        restResultaat.filterType = types.ifEmpty {
            restListParameters.type?.let { listOf(it) } ?: emptyList()
        }
        return restResultaat
    }

    @GET
    @Path("/{uuid}/pdfPreview")
    fun pdfPreview(@PathParam("uuid") uuid: UUID): Response {
        assertPolicy(policyService.readWerklijstRechten().inbox)
        val enkelvoudigInformatieobject = drcClientService.readEnkelvoudigInformatieobject(uuid)
        return drcClientService.downloadEnkelvoudigInformatieobject(uuid).use { inputStream ->
            Response.ok(inputStream)
                .header("Content-Disposition", "inline; filename=\"${enkelvoudigInformatieobject.bestandsnaam}\"")
                .header("Content-Type", MediaTypes.Application.PDF.mediaType)
                .build()
        }
    }

    @DELETE
    @Path("{id}")
    fun deleteInboxProductaanvraag(@PathParam("id") id: Long) {
        assertPolicy(policyService.readWerklijstRechten().inboxProductaanvragenVerwijderen)
        inboxProductaanvraagService.delete(id)
    }
}
