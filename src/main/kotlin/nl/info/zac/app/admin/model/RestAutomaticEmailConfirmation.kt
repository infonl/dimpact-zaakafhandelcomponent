/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.app.admin.model

import nl.info.zac.admin.model.ZaaktypeCmmnConfiguration
import nl.info.zac.admin.model.ZaaktypeCmmnEmailParameters
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor

@NoArgConstructor
@AllOpen
data class RestAutomaticEmailConfirmation(
    var id: Long? = null,
    var enabled: Boolean = false,
    var templateName: String? = null,
    var emailSender: String? = null,
    var emailReply: String? = null,
)

fun ZaaktypeCmmnEmailParameters.toRestAutomaticEmailConfirmation(): RestAutomaticEmailConfirmation =
    RestAutomaticEmailConfirmation().apply {
        id = this@toRestAutomaticEmailConfirmation.id
        enabled = this@toRestAutomaticEmailConfirmation.enabled
        templateName = this@toRestAutomaticEmailConfirmation.templateName
        emailSender = this@toRestAutomaticEmailConfirmation.emailSender
        emailReply = this@toRestAutomaticEmailConfirmation.emailReply
    }

fun RestAutomaticEmailConfirmation.toAutomaticEmailConfirmation(
    zaaktypeCmmnConfiguration: ZaaktypeCmmnConfiguration
): ZaaktypeCmmnEmailParameters =
    ZaaktypeCmmnEmailParameters().apply {
        id = this@toAutomaticEmailConfirmation.id
        enabled = this@toAutomaticEmailConfirmation.enabled
        templateName = this@toAutomaticEmailConfirmation.templateName
        emailSender = this@toAutomaticEmailConfirmation.emailSender
        emailReply = this@toAutomaticEmailConfirmation.emailReply
        this.zaaktypeCmmnConfiguration = zaaktypeCmmnConfiguration
    }
