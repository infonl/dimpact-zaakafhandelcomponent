/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.klant.exception

import jakarta.ws.rs.NotFoundException

class RechtspersoonNotFoundException(message: String) : NotFoundException(message)
