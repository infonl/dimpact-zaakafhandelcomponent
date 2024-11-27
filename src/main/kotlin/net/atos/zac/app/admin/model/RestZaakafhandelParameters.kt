/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.admin.model

import jakarta.annotation.Nullable
import net.atos.zac.app.zaak.model.RESTZaakStatusmailOptie
import net.atos.zac.app.zaak.model.RestResultaattype
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
    var zaakNietOntvankelijkResultaattype: RestResultaattype? = null,
    var intakeMail: RESTZaakStatusmailOptie? = null,
    var afrondenMail: RESTZaakStatusmailOptie? = null,
    var productaanvraagtype: String? = null,
    var domein: String? = null,
    var valide: Boolean = false,
    /**
     * The frontend currently requires this field to be non-null
     */
    var humanTaskParameters: List<RESTHumanTaskParameters> = emptyList(),
    /**
     * The frontend currently requires this field to be non-null
     */
    var userEventListenerParameters: List<RESTUserEventListenerParameter> = emptyList(),
    /**
     * The frontend currently requires this field to be non-null
     */
    var mailtemplateKoppelingen: List<RESTMailtemplateKoppeling> = emptyList(),
    /**
     * The frontend currently requires this field to be non-null
     */
    var zaakbeeindigParameters: List<RESTZaakbeeindigParameter> = emptyList(),
    /**
     * The frontend currently requires this field to be non-null
     */
    var zaakAfzenders: List<RESTZaakAfzender> = emptyList(),

    var documentCreation: RestDocumentCreationParameters
)
