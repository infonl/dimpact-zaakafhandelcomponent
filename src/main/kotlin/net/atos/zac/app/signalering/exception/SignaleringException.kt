/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.signalering.exception

import jakarta.ws.rs.BadRequestException

class SignaleringException(message: String) : BadRequestException(message)
