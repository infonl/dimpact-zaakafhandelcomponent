/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.configuratie.exception

import jakarta.ws.rs.NotFoundException

class BrpProtocolleringProviderNotFound(message: String) : NotFoundException(message)
