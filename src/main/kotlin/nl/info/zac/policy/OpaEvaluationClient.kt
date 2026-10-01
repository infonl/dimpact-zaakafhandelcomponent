/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.policy

import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import nl.info.client.opa.model.RoleNamesResponse
import nl.info.client.opa.model.RuleQuery
import nl.info.client.opa.model.RuleResponse
import nl.info.zac.policy.input.BrpInput
import nl.info.zac.policy.input.DocumentInput
import nl.info.zac.policy.input.TaakInput
import nl.info.zac.policy.input.UserInput
import nl.info.zac.policy.input.ZaakInput
import nl.info.zac.policy.output.BrpRechten
import nl.info.zac.policy.output.DocumentRechten
import nl.info.zac.policy.output.NotitieRechten
import nl.info.zac.policy.output.OverigeRechten
import nl.info.zac.policy.output.TaakRechten
import nl.info.zac.policy.output.WerklijstRechten
import nl.info.zac.policy.output.ZaakRechten
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient

@RegisterRestClient(configKey = "OPA-Api-Client")
@Path("v1/data/net/atos/zac")
@Produces(MediaType.APPLICATION_JSON)
interface OpaEvaluationClient {
    companion object {
        const val ZAAK_RECHTEN_PATH = "zaak/zaak_rechten"
        const val TAAK_RECHTEN_PATH = "taak/taak_rechten"
        const val DOCUMENT_RECHTEN_PATH = "document/document_rechten"
        const val NOTITIE_RECHTEN_PATH = "notitie/notitie_rechten"
        const val OVERIGE_RECHTEN_PATH = "overig/overige_rechten"
        const val WERKLIJST_RECHTEN_PATH = "werklijst/werklijst_rechten"
        const val BRP_RECHTEN_PATH = "brp/brp_rechten"
        const val LEESROLLEN_PATH = "rol/leesrollen"
    }

    @POST
    @Path(ZAAK_RECHTEN_PATH)
    fun readZaakRechten(query: RuleQuery<ZaakInput>): RuleResponse<ZaakRechten>

    @POST
    @Path(TAAK_RECHTEN_PATH)
    fun readTaakRechten(query: RuleQuery<TaakInput>): RuleResponse<TaakRechten>

    @POST
    @Path(DOCUMENT_RECHTEN_PATH)
    fun readDocumentRechten(query: RuleQuery<DocumentInput>): RuleResponse<DocumentRechten>

    @POST
    @Path(NOTITIE_RECHTEN_PATH)
    fun readNotitieRechten(query: RuleQuery<UserInput>): RuleResponse<NotitieRechten>

    @POST
    @Path(OVERIGE_RECHTEN_PATH)
    fun readOverigeRechten(query: RuleQuery<UserInput>): RuleResponse<OverigeRechten>

    @POST
    @Path(WERKLIJST_RECHTEN_PATH)
    fun readWerklijstRechten(query: RuleQuery<UserInput>): RuleResponse<WerklijstRechten>

    @POST
    @Path(BRP_RECHTEN_PATH)
    fun readBrpRechten(query: RuleQuery<BrpInput>): RuleResponse<BrpRechten>

    @GET
    @Path(LEESROLLEN_PATH)
    fun readLeesrollen(): RoleNamesResponse
}
