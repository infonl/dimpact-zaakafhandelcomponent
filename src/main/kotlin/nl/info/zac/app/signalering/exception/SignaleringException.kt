/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.signalering.exception

import jakarta.ws.rs.BadRequestException

class SignaleringException(message: String) : BadRequestException(message)
