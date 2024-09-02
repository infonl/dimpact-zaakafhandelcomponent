/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.admin

import jakarta.inject.Inject
import jakarta.inject.Singleton
import jakarta.validation.Valid
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
import net.atos.zac.admin.model.ReferenceTable
import net.atos.zac.admin.model.ReferenceTable.Systeem
import net.atos.zac.admin.model.ReferenceTableValue
import net.atos.zac.admin.model.toRestReferenceTable
import net.atos.zac.app.admin.model.RestReferenceTable
import net.atos.zac.app.admin.model.RestReferenceTableUpdate
import net.atos.zac.app.admin.model.toReferenceTable
import net.atos.zac.app.admin.model.toReferenceTableValue
import net.atos.zac.app.exception.ERROR_CODE_REFERENCE_TABLE_SYSTEM_VALUES_CANNOT_BE_CHANGED
import net.atos.zac.app.exception.InputValidationFailedException
import net.atos.zac.configuratie.ConfiguratieService
import net.atos.zac.policy.PolicyService
import nl.lifely.zac.util.AllOpen
import nl.lifely.zac.util.NoArgConstructor

@Singleton
@Path("referentietabellen")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Suppress("TooManyFunctions")
@NoArgConstructor
@AllOpen // required because we use Jakarta Validation using @Valid annotation in some functions
class ReferenceTableRestService @Inject constructor(
    private val referenceTableService: ReferenceTableService,
    private val referenceTableAdminService: ReferenceTableAdminService,
    private val policyService: PolicyService
) {
    @GET
    fun listReferenceTables(): List<RestReferenceTable> {
        PolicyService.assertPolicy(policyService.readOverigeRechten().beheren)
        return referenceTableService.listReferenceTables()
            .map { it.toRestReferenceTable(false) }
            .toList()
    }

    /**
     * This endpoint should be removed and this logic should be moved to the frontend.
     */
    @GET
    @Path("new")
    fun newReferenceTable() =
        RestReferenceTable(
            code = "VUL SVP EEN UNIEKE TABEL CODE IN",
            naam = "Nieuwe referentietabel",
            systeem = false,
        )

    @POST
    fun createReferenceTable(restReferenceTable: RestReferenceTable): RestReferenceTable {
        PolicyService.assertPolicy(policyService.readOverigeRechten().beheren)
        return referenceTableAdminService.createReferenceTable(
            restReferenceTable.toReferenceTable()
        ).toRestReferenceTable(
            true
        )
    }

    @GET
    @Path("{id}")
    fun readReferenceTable(@PathParam("id") id: Long): RestReferenceTable {
        PolicyService.assertPolicy(policyService.readOverigeRechten().beheren)
        return referenceTableService.readReferenceTable(id).toRestReferenceTable(
            true
        )
    }

    @PUT
    @Path("{id}")
    fun updateReferenceTable(
        @PathParam("id") id: Long,
        @Valid restReferenceTableUpdate: RestReferenceTableUpdate
    ): RestReferenceTable {
        PolicyService.assertPolicy(policyService.readOverigeRechten().beheren)
        return referenceTableService.readReferenceTable(id).let { existingReferenceTable ->
            val systemValueNames = existingReferenceTable.values.filter { it.isSystemValue }.map { it.name }
            existingReferenceTable.updateExistingReferenceTable(
                restReferenceTableUpdate
            ).let { updatedReferenceTable ->
                if (!updatedReferenceTable.values
                        .filter { it.isSystemValue }
                        .map { it.name }
                        .containsAll(systemValueNames)
                ) { throw InputValidationFailedException(ERROR_CODE_REFERENCE_TABLE_SYSTEM_VALUES_CANNOT_BE_CHANGED) }
                referenceTableAdminService.updateReferenceTable(updatedReferenceTable)
                    .toRestReferenceTable(true)
            }
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
            getReferenceTableValueNames(it)
        }
    }

    @GET
    @Path("communicatiekanaal/{inclusiefEFormulier}")
    fun listCommunicationChannels(
        @PathParam("inclusiefEFormulier") includingEFormulier: Boolean
    ) = getReferenceTableValueNames(
        referenceTableService.readReferenceTable(Systeem.COMMUNICATIEKANAAL.name).values
    )
        .filter { communicationChannel -> includingEFormulier || communicationChannel != ConfiguratieService.COMMUNICATIEKANAAL_EFORMULIER }
        .toList()

    @GET
    @Path("domein")
    fun listDomains(): List<String> {
        PolicyService.assertPolicy(policyService.readOverigeRechten().beheren)
        return referenceTableService.readReferenceTable(Systeem.DOMEIN.name).values.let {
            getReferenceTableValueNames(it)
        }
    }

    @GET
    @Path("server-error-text")
    fun listServerErrorPageTexts(): List<String> {
        return referenceTableService.readReferenceTable(Systeem.SERVER_ERROR_ERROR_PAGINA_TEKST.name).values.let {
            getReferenceTableValueNames(it)
        }
    }

    private fun getReferenceTableValueNames(referenceTableValues: List<ReferenceTableValue>) =
        referenceTableValues
            .map(ReferenceTableValue::name)
            .toList()

    fun ReferenceTable.updateExistingReferenceTable(
        restReferenceTableUpdate: RestReferenceTableUpdate
    ) = this.apply {
        if (!isSystemReferenceTable) {
            restReferenceTableUpdate.code?.let {
                // code can only be updated for non-system reference tables
                // the data model only supports uppercase codes so convert it here to be sure
                code = it.uppercase()
            }
        }
        name = restReferenceTableUpdate.naam
        values = restReferenceTableUpdate.waarden
            .map { it.toReferenceTableValue(this) }
            .toMutableList()
    }
}
