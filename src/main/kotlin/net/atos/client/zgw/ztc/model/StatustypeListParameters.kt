/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.client.zgw.ztc.model

import jakarta.ws.rs.QueryParam
import java.net.URI

/**
 *
 */
class StatustypeListParameters(
    /**
     * URL-referentie naar het ZAAKTYPE van ZAAKen waarin STATUSsen van dit STATUSTYPE bereikt kunnen worden.
     */
    @field:QueryParam("zaaktype") val zaaktype: URI
) : AbstractZTCListParameters()
