/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.client.zgw.ztc.model

import jakarta.json.bind.annotation.JsonbCreator
import jakarta.json.bind.annotation.JsonbProperty
import java.net.URI

/**
 * Het Referentieproces dat ten grondslag ligt aan dit ZAAKTYPE.
 */
class Referentieproces
/**
 * Constructor with required attributes for POST and PUT requests and GET response
 */
@JsonbCreator constructor(
    /**
     * De naam van het Referentieproces.
     * maxLength: [Referentieproces.NAAM_MAX_LENGTH]
     */
    @param:JsonbProperty(
        "naam"
    ) val naam: String
) {
    /**
     * De URL naar de beschrijving van het Referentieproces
     */
    var link: URI? = null

    companion object {
        const val NAAM_MAX_LENGTH: Int = 80
    }
}
