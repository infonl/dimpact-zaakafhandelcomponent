/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.besluit

import jakarta.ws.rs.BadRequestException

class BesluitException(message: String) : BadRequestException(message)
