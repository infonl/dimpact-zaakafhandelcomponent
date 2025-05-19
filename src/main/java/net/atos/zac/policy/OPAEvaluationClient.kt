/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.policy

import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import net.atos.client.opa.model.RuleQuery
import net.atos.client.opa.model.RuleResponse
import net.atos.zac.policy.input.DocumentInput
import net.atos.zac.policy.input.TaakInput
import net.atos.zac.policy.input.UserInput
import net.atos.zac.policy.input.ZaakInput
import net.atos.zac.policy.output.DocumentRechten
import net.atos.zac.policy.output.OverigeRechten
import net.atos.zac.policy.output.TaakRechten
import net.atos.zac.policy.output.WerklijstRechten
import net.atos.zac.policy.output.ZaakRechten
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient

@RegisterRestClient(configKey = "OPA-Api-Client")
@Path("v1/data/net/atos/zac")
@Produces(MediaType.APPLICATION_JSON)
interface OPAEvaluationClient {
    @POST
    @Path("zaak/zaak_rechten")
    fun readZaakRechten(query: RuleQuery<ZaakInput>): RuleResponse<ZaakRechten>

    @POST
    @Path("taak/taak_rechten")
    fun readTaakRechten(query: RuleQuery<TaakInput>): RuleResponse<TaakRechten>

    @POST
    @Path("document/document_rechten")
    fun readDocumentRechten(query: RuleQuery<DocumentInput>): RuleResponse<DocumentRechten>

    @POST
    @Path("overig/overige_rechten")
    fun readOverigeRechten(query: RuleQuery<UserInput>): RuleResponse<OverigeRechten>

    @POST
    @Path("werklijst/werklijst_rechten")
    fun readWerklijstRechten(query: RuleQuery<UserInput>): RuleResponse<WerklijstRechten>
}
