/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.app.klant.exception

import jakarta.ws.rs.NotFoundException

class VestigingNotFoundException(message: String) : NotFoundException(message)
