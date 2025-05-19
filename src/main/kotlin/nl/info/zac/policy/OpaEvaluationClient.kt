/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.policy

import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import net.atos.client.opa.model.RuleQuery
import net.atos.client.opa.model.RuleResponse
import nl.info.zac.policy.input.DocumentInput
import nl.info.zac.policy.input.TaakInput
import nl.info.zac.policy.input.UserInput
import nl.info.zac.policy.input.ZaakInput
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
    @Path("notitie/notitie_rechten")
    fun readNotitieRechten(query: RuleQuery<UserInput>): RuleResponse<NotitieRechten>

    @POST
    @Path("overig/overige_rechten")
    fun readOverigeRechten(query: RuleQuery<UserInput>): RuleResponse<OverigeRechten>

    @POST
    @Path("werklijst/werklijst_rechten")
    fun readWerklijstRechten(query: RuleQuery<UserInput>): RuleResponse<WerklijstRechten>
}
