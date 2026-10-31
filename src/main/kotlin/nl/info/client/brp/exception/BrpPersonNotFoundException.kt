/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.client.brp.exception

import jakarta.ws.rs.NotFoundException

open class BrpPersonNotFoundException(message: String) : NotFoundException(message)
