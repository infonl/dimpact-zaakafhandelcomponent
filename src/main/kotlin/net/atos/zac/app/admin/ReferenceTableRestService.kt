/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.admin

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
import net.atos.zac.admin.ReferenceTableAdminService
import net.atos.zac.admin.ReferenceTableService
import net.atos.zac.admin.model.ReferenceTable.Systeem
import net.atos.zac.app.admin.converter.RestReferenceValueConverter
import net.atos.zac.app.admin.converter.convertToReferenceTable
import net.atos.zac.app.admin.converter.convertToRestReferenceTable
import net.atos.zac.app.admin.model.RestReferenceTable
import net.atos.zac.configuratie.ConfiguratieService
import net.atos.zac.policy.PolicyService

@Singleton
@Path("referentietabellen")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
class ReferenceTableRestService @Inject constructor(
    private val referenceTableService: ReferenceTableService,
    private val referenceTableAdminService: ReferenceTableAdminService,
    private val policyService: PolicyService
) {
    @GET
    fun listReferenceTables(): List<RestReferenceTable> {
        PolicyService.assertPolicy(policyService.readOverigeRechten().beheren)
        return referenceTableService.listReferenceTables()
            .map { convertToRestReferenceTable(it, false) }
            .toList()
    }

    @GET
    @Path("new")
    fun newReferenceTable(): RestReferenceTable {
        PolicyService.assertPolicy(policyService.readOverigeRechten().beheren)
        return convertToRestReferenceTable(
            referenceTableAdminService.newReferenceTable(),
            true
        )
    }

    @POST
    fun createReferenceTable(restReferenceTable: RestReferenceTable): RestReferenceTable {
        PolicyService.assertPolicy(policyService.readOverigeRechten().beheren)
        return convertToRestReferenceTable(
            referenceTableAdminService.createReferenceTable(
                convertToReferenceTable(restReferenceTable)
            ),
            true
        )
    }

    @GET
    @Path("{id}")
    fun readReferenceTable(@PathParam("id") id: Long): RestReferenceTable {
        PolicyService.assertPolicy(policyService.readOverigeRechten().beheren)
        return convertToRestReferenceTable(
            referenceTableService.readReferenceTable(id),
            true
        )
    }

    @PUT
    @Path("{id}")
    fun updateReferenceTable(
        @PathParam("id") id: Long,
        referentieTabel: RestReferenceTable
    ): RestReferenceTable {
        PolicyService.assertPolicy(policyService.readOverigeRechten().beheren)
        return convertToReferenceTable(
            referentieTabel,
            referenceTableService.readReferenceTable(id)
        ).let {
            convertToRestReferenceTable(
                referenceTableAdminService.updateReferenceTable(it),
                true
            )
        }
    }

    @DELETE
    @Path("{id}")
    fun deleteReferenceTable(@PathParam("id") id: Long) {
        PolicyService.assertPolicy(policyService.readOverigeRechten().beheren)
        referenceTableAdminService.deleteReferenceTable(id)
    }

    @GET
    @Path("afzender")
    fun listEmailSenders(): List<String> {
        PolicyService.assertPolicy(policyService.readOverigeRechten().beheren)
        return referenceTableService.readReferenceTable(Systeem.AFZENDER.name).values.let {
            RestReferenceValueConverter.convert(it)
        }
    }

    @GET
    @Path("communicatiekanaal/{inclusiefEFormulier}")
    fun listCommunicationChannels(
        @PathParam("inclusiefEFormulier") inclusiefEFormulier: Boolean
    ) =
        RestReferenceValueConverter.convert(
            referenceTableService.readReferenceTable(Systeem.COMMUNICATIEKANAAL.name).values
        )
            .filter { communicatiekanaal -> inclusiefEFormulier || communicatiekanaal != ConfiguratieService.COMMUNICATIEKANAAL_EFORMULIER }
            .toList()

    @GET
    @Path("domein")
    fun listDomains(): List<String> {
        PolicyService.assertPolicy(policyService.readOverigeRechten().beheren)
        return referenceTableService.readReferenceTable(Systeem.DOMEIN.name).values.let {
            RestReferenceValueConverter.convert(it)
        }
    }

    @GET
    @Path("server-error-text")
    fun listServerErrorPageTexts(): List<String> {
        return referenceTableService.readReferenceTable(Systeem.SERVER_ERROR_ERROR_PAGINA_TEKST.name).values.let {
            RestReferenceValueConverter.convert(it)
        }
    }
}
