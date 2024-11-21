/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.client.zgw.ztc.exception

import jakarta.ws.rs.NotFoundException

class RoltypeNotFoundException(message: String) : NotFoundException(message)
