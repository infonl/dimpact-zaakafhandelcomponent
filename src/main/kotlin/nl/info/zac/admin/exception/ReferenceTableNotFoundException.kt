/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.admin.exception

import jakarta.ws.rs.NotFoundException

class ReferenceTableNotFoundException(message: String) : NotFoundException(message) {
    constructor(id: Long) : this("No reference table found with id '$id'")
}
