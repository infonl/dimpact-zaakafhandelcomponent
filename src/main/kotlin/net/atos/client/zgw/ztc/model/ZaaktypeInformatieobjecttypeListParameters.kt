/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.client.zgw.ztc.model

import jakarta.ws.rs.QueryParam
import java.net.URI

class ZaaktypeInformatieobjecttypeListParameters(@field:QueryParam("zaaktype") val zaaktype: URI) :
    AbstractZTCListParameters()
