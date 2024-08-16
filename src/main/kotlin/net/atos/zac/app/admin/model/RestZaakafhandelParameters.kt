/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.admin.model

import net.atos.zac.app.zaak.model.RESTResultaattype
import net.atos.zac.app.zaak.model.RESTZaakStatusmailOptie
import nl.lifely.zac.util.AllOpen
import nl.lifely.zac.util.NoArgConstructor
import java.time.ZonedDateTime

@NoArgConstructor
@AllOpen
data class RestZaakafhandelParameters(
    var id: Long? = null,
    var zaaktype: RESTZaaktypeOverzicht,
    var caseDefinition: RESTCaseDefinition? = null,
    var defaultBehandelaarId: String? = null,
    var defaultGroepId: String? = null,
    var einddatumGeplandWaarschuwing: Int? = null,
    var uiterlijkeEinddatumAfdoeningWaarschuwing: Int? = null,
    var creatiedatum: ZonedDateTime? = null,
    var zaakNietOntvankelijkResultaattype: RESTResultaattype? = null,
    var intakeMail: RESTZaakStatusmailOptie? = null,
    var afrondenMail: RESTZaakStatusmailOptie? = null,
    var productaanvraagtype: String? = null,
    var domein: String? = null,
    var valide: Boolean = false,
    var humanTaskParameters: List<RESTHumanTaskParameters>? = null,
    var userEventListenerParameters: List<RESTUserEventListenerParameter>? = null,
    var mailtemplateKoppelingen: List<RESTMailtemplateKoppeling>? = null,
    var zaakbeeindigParameters: List<RESTZaakbeeindigParameter>? = null,
    var zaakAfzenders: List<RESTZaakAfzender>? = null
)
