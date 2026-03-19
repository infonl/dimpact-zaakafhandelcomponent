/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.bag

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import nl.info.client.bag.api.AdresApi
import nl.info.client.bag.api.NummeraanduidingApi
import nl.info.client.bag.api.OpenbareRuimteApi
import nl.info.client.bag.api.PandApi
import nl.info.client.bag.api.WoonplaatsApi
import nl.info.client.bag.model.BevraagAdressenParameters
import nl.info.client.bag.model.generated.AdresIOHal
import nl.info.client.bag.model.generated.NummeraanduidingIOHal
import nl.info.client.bag.model.generated.OpenbareRuimteIOHal
import nl.info.client.bag.model.generated.PandIOHal
import nl.info.client.bag.model.generated.WoonplaatsIOHal
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import org.eclipse.microprofile.rest.client.inject.RestClient

@ApplicationScoped
@NoArgConstructor
@AllOpen
class BagClientService @Inject constructor(
    @RestClient private val adresApi: AdresApi,
    @RestClient private val woonplaatsApi: WoonplaatsApi,
    @RestClient private val nummeraanduidingApi: NummeraanduidingApi,
    @RestClient private val pandApi: PandApi,
    @RestClient private val openbareRuimteApi: OpenbareRuimteApi
) {
    companion object {
        const val DEFAULT_CRS = "epsg:28992"
        private const val ADRES_EXPAND = "panden, adresseerbaarObject, nummeraanduiding, openbareRuimte, woonplaats"
        private const val NUMMERAANDUIDING_EXPAND = "ligtAanOpenbareRuimte, ligtInWoonplaats"
        private const val OPENBARE_RUIMTE_EXPAND = "ligtInWoonplaats"
    }

    fun readAdres(nummeraanduidingIdentificatie: String): AdresIOHal =
        adresApi.bevraagAdressenMetNumId(
            nummeraanduidingIdentificatie = nummeraanduidingIdentificatie,
            expand = ADRES_EXPAND,
            inclusiefEindStatus = null
        )

    fun readWoonplaats(woonplaatsIdentificatie: String): WoonplaatsIOHal =
        woonplaatsApi.woonplaatsIdentificatie(
            identificatie = woonplaatsIdentificatie,
            geldigOp = null,
            beschikbaarOp = null,
            expand = null,
            acceptCrs = null,
            huidig = null
        )

    fun readNummeraanduiding(nummeraanduidingIdentificatie: String): NummeraanduidingIOHal =
        nummeraanduidingApi.nummeraanduidingIdentificatie(
            nummeraanduidingIdentificatie = nummeraanduidingIdentificatie,
            geldigOp = null,
            beschikbaarOp = null,
            expand = NUMMERAANDUIDING_EXPAND,
            huidig = null
        )

    fun readPand(pandIdentificatie: String): PandIOHal =
        pandApi.pandIdentificatie(
            identificatie = pandIdentificatie,
            geldigOp = null,
            beschikbaarOp = null,
            acceptCrs = DEFAULT_CRS,
            huidig = null
        )

    fun readOpenbareRuimte(openbareRuimteIdentificatie: String): OpenbareRuimteIOHal =
        openbareRuimteApi.openbareruimteIdentificatie(
            openbareRuimteIdentificatie = openbareRuimteIdentificatie,
            geldigOp = null,
            beschikbaarOp = null,
            expand = OPENBARE_RUIMTE_EXPAND,
            huidig = null
        )

    fun listAdressen(parameters: BevraagAdressenParameters): List<AdresIOHal> {
        val embedded = adresApi.bevraagAdressen(parameters).getEmbedded()
        return embedded?.adressen ?: emptyList()
    }
}
