/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.csv

import jakarta.inject.Inject
import jakarta.inject.Singleton
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import net.atos.zac.app.zoeken.converter.RESTZoekParametersConverter
import net.atos.zac.app.zoeken.model.RESTZoekParameters
import net.atos.zac.csv.CsvService
import net.atos.zac.gebruikersvoorkeuren.model.TabelInstellingen
import net.atos.zac.policy.PolicyService
import net.atos.zac.policy.PolicyService.assertPolicy
import net.atos.zac.util.MediaTypes
import net.atos.zac.zoeken.ZoekenService
import nl.info.zac.util.NoArgConstructor

@Path("csv")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Singleton
@NoArgConstructor
class CsvRESTService @Inject constructor(
    private val zoekenService: ZoekenService,
    private val restZoekParametersConverter: RESTZoekParametersConverter,
    private val csvService: CsvService,
    private val policyService: PolicyService
) {
    @POST
    @Path("export")
    fun downloadCSV(restZoekParameters: RESTZoekParameters): Response {
        assertPolicy(policyService.readWerklijstRechten().zakenTakenExporteren)
        val zoekParameters = restZoekParametersConverter.convert(restZoekParameters).let {
            // if no max nr of result rows are specified, resort to the default value
            if (it.rows == 0) {
                it.rows = TabelInstellingen.AANTAL_PER_PAGINA_MAX
            }
            it
        }
        val streamingOutput = zoekenService.zoek(zoekParameters).let {
            csvService.exportToCsv(it)
        }
        return Response.ok(streamingOutput).header("Content-Type", MediaTypes.Text.CSV.mediaType).build()
    }
}
