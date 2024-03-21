/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.zaken.converter

import jakarta.inject.Inject
import net.atos.client.zgw.zrc.ZRCClientService
import net.atos.zac.app.zaken.model.RESTZaakEigenschap
import java.net.URI
import java.util.stream.Collectors

// TODO: not used at the moment
class RESTZaakEigenschappenConverter {
    @Inject
    private lateinit var zrcClientService: ZRCClientService

    fun convert(uri: URI): RESTZaakEigenschap =
        zrcClientService.readZaakeigenschap(uri).let { zaakeigenschap ->
            RESTZaakEigenschap(
                naam = zaakeigenschap.naam,
                waarde = zaakeigenschap.waarde
            )
        }

    fun convert(eigenschappen: Collection<URI>): List<RESTZaakEigenschap> {
        return eigenschappen.stream().map { uri -> this.convert(uri) }.collect(Collectors.toList())
    }
}
