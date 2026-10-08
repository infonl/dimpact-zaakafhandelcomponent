/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.app.admin.model

import jakarta.json.bind.annotation.JsonbProperty
import nl.info.zac.admin.model.ZaaktypeConfiguration
import nl.info.zac.admin.model.ZaaktypeEmailParameters
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor

@NoArgConstructor
@AllOpen
data class RestAutomaticEmailConfirmation(
    var id: Long? = null,

    @get:JsonbProperty("isEnabled")
    @set:JsonbProperty("isEnabled")
    var isEnabled: Boolean = false,

    var templateName: String? = null,
    var emailSender: String? = null,
    var emailReply: String? = null,
)

fun ZaaktypeEmailParameters.toRestAutomaticEmailConfirmation(): RestAutomaticEmailConfirmation =
    RestAutomaticEmailConfirmation().apply {
        id = this@toRestAutomaticEmailConfirmation.id
        isEnabled = this@toRestAutomaticEmailConfirmation.isEnabled
        templateName = this@toRestAutomaticEmailConfirmation.templateName
        emailSender = this@toRestAutomaticEmailConfirmation.emailSender
        emailReply = this@toRestAutomaticEmailConfirmation.emailReply
    }

fun RestAutomaticEmailConfirmation.toAutomaticEmailConfirmation(
    zaaktypeConfiguration: ZaaktypeConfiguration
): ZaaktypeEmailParameters =
    ZaaktypeEmailParameters().apply {
        id = this@toAutomaticEmailConfirmation.id
        isEnabled = this@toAutomaticEmailConfirmation.isEnabled
        templateName = this@toAutomaticEmailConfirmation.templateName
        emailSender = this@toAutomaticEmailConfirmation.emailSender
        emailReply = this@toAutomaticEmailConfirmation.emailReply
        this.zaaktypeConfiguration = zaaktypeConfiguration
    }
