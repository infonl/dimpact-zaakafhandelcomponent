/*
 * SPDX-FileCopyrightText: 2021 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.admin.model

import jakarta.annotation.Nullable
import jakarta.json.bind.annotation.JsonbProperty
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import net.atos.zac.app.admin.model.RESTMailtemplateKoppeling
import net.atos.zac.app.admin.model.RESTHumanTaskParameters
import net.atos.zac.app.admin.model.RESTUserEventListenerParameter
import nl.info.zac.admin.model.ZaakafhandelparametersStatusMailOption
import nl.info.zac.app.zaak.model.RestResultaattype
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import java.time.ZonedDateTime

/**
 * The configuration of a zaaktype version (zaakafhandelparameters).
 *
 * [processDefinition] names the process that the zaken of the zaaktype run with; a configuration that was never
 * saved has none. The plan item settings, [humanTaskParameters] and [userEventListenerParameters], configure the plan
 * items of that process definition. So does the mail on completing them: [intakeMail] and [afrondenMail]. A process
 * definition without such plan items has none of these settings.
 *
 * Currently, this class is used both for creating and updating as well as for reading
 * zaaktype configuration.
 * For this reason, most fields are nullable.
 */
@NoArgConstructor
@AllOpen
data class RestZaaktypeConfiguration(
    var id: Long? = null,
    var zaaktype: RestZaaktypeOverzicht,
    @field:Valid
    var processDefinition: RestProcessDefinition? = null,
    var defaultBehandelaarId: String? = null,
    @field:NotBlank
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

    @get:JsonbProperty("isValide")
    @set:JsonbProperty("isValide")
    var isValide: Boolean = false,
    /**
     * Indicates that zaken of this zaaktype can be authorised on an individual basis,
     * which is the case when the zaaktype defines the [ZAAK_GEAUTORISEERD_EIGENSCHAP_NAAM] eigenschap.
     */
    @get:JsonbProperty("isZaakspecifiekAutoriseerbaar")
    @set:JsonbProperty("isZaakspecifiekAutoriseerbaar")
    var isZaakspecifiekAutoriseerbaar: Boolean = false,
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
    @field:Valid
    var automaticEmailConfirmation: RestAutomaticEmailConfirmation = RestAutomaticEmailConfirmation(),
    /**
     * The frontend currently requires this field to be non-null
     */
    var betrokkeneKoppelingen: RestBetrokkeneKoppelingen = RestBetrokkeneKoppelingen(),
    /**
     * The frontend currently requires this field to be non-null
     */
    var zaakbeeindigParameters: List<RestZaakbeeindigParameter> = emptyList(),
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
