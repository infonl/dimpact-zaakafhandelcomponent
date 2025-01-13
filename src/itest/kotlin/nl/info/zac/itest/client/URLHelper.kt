/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.itest.client

import java.net.URLEncoder

fun String.urlEncode() = URLEncoder.encode(this, Charsets.UTF_8.name())
