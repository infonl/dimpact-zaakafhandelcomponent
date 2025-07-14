/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.mailtemplates.exception

import jakarta.ws.rs.NotFoundException
import net.atos.zac.mailtemplates.model.Mail

class MailTemplateNotFoundException(string: String) : NotFoundException(string) {
    constructor(mail: Mail) : this("Mail template not found for mail type: '${mail.name}'")

    constructor(id: Long) : this("Mail template not found for ID: '$id'")
}
