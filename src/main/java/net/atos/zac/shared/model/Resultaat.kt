/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.shared.model

open class Resultaat<TYPE>(val items: List<TYPE?>?, val count: Long)
