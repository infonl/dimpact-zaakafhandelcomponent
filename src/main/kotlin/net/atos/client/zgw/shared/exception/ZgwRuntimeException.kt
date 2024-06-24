/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.client.zgw.shared.exception

import java.lang.RuntimeException

open class ZgwRuntimeException(message: String) : RuntimeException(message)
