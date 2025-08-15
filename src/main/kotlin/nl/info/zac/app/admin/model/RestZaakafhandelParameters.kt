/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.admin.model

import jakarta.annotation.Nullable
import jakarta.validation.constraints.Size
import net.atos.zac.app.admin.model.RESTCaseDefinition
import net.atos.zac.app.admin.model.RESTHumanTaskParameters
import net.atos.zac.app.admin.model.RESTMailtemplateKoppeling
import net.atos.zac.app.admin.model.RESTUserEventListenerParameter
import net.atos.zac.app.admin.model.RESTZaakbeeindigParameter
import nl.info.zac.admin.model.ZaakafhandelparametersStatusMailOption
import nl.info.zac.app.zaak.model.RestResultaattype
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import java.time.ZonedDateTime

/**
 * Currently this class it used both for creating and updating as well as for reading
 * zaakafhandelparameter data.
 * For this reason, all fields are currently nullable.
 * In future, we should consider splitting this class into separate classes, depending
 * on the CRUD operation.
 */
@NoArgConstructor
@AllOpen
data class RestZaakafhandelParameters(
    var id: Long? = null,
    var zaaktype: RestZaaktypeOverzicht,
    var caseDefinition: RESTCaseDefinition? = null,
    var defaultBehandelaarId: String? = null,
    var defaultGroepId: String? = null,
    var einddatumGeplandWaarschuwing: Int? = null,
    var uiterlijkeEinddatumAfdoeningWaarschuwing: Int? = null,
    var creatiedatum: ZonedDateTime? = null,
    var zaakNietOntvankelijkResultaattype: RestResultaattype? = null,
    var intakeMail: ZaakafhandelparametersStatusMailOption? = null,
    var afrondenMail: ZaakafhandelparametersStatusMailOption? = null,
    @field:Nullable
    @field:Size(min = 1)
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
    var automaticEmailConfirmation: RestAutomaticEmailConfirmation = RestAutomaticEmailConfirmation(),
    /**
     * The frontend currently requires this field to be non-null
     */
    var betrokkeneKoppelingen: RestBetrokkeneKoppelingen = RestBetrokkeneKoppelingen(),
    /**
     * The frontend currently requires this field to be non-null
     */
    var zaakbeeindigParameters: List<RESTZaakbeeindigParameter> = emptyList(),
    /**
     * The frontend currently requires this field to be non-null
     */
    var brpDoelbindingen: RestBrpDoelbindingen = RestBrpDoelbindingen(),
    /**
     * The frontend currently requires this field to be non-null
     */
    var zaakAfzenders: List<RestZaakAfzender> = emptyList(),

    var smartDocuments: RestSmartDocuments
)
