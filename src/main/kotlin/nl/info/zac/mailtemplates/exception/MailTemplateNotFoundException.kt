/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.mailtemplates.exception

import jakarta.ws.rs.NotFoundException

class MailTemplateNotFoundException(string: String) : NotFoundException(string) {
    constructor(id: Long) : this("Mail template not found for ID: '$id'")
}
