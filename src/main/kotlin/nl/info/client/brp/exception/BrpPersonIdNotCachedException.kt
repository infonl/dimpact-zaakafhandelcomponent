/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.brp.exception

import jakarta.ws.rs.ClientErrorException
import jakarta.ws.rs.core.Response

open class BrpPersonIdNotCachedException(message: String) : ClientErrorException(message, Response.Status.GONE)
