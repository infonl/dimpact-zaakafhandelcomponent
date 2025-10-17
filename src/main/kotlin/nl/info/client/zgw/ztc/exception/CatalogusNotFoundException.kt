/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.ztc.exception

import jakarta.ws.rs.NotFoundException

class CatalogusNotFoundException(message: String) : NotFoundException(message)
