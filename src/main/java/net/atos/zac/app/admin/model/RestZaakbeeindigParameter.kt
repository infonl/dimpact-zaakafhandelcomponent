/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.admin.model;

import nl.info.zac.app.zaak.model.RestResultaattype;

data class RestZaakbeeindigParameter(
     val id: Long? = null,
     val zaakbeeindigReden: RESTZaakbeeindigReden,
     val resultaattype: RestResultaattype
)
