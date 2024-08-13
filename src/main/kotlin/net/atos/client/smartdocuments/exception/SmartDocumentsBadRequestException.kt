/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.client.smartdocuments.exception

import jakarta.ws.rs.BadRequestException

class SmartDocumentsBadRequestException(message: String) : BadRequestException(message)
