/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.smartdocuments.exception

import jakarta.ws.rs.BadRequestException

class SmartDocumentsBadRequestException(message: String) : BadRequestException(message)
