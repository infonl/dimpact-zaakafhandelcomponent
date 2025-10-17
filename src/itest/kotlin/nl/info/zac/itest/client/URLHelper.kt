/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.itest.client

import java.net.URLEncoder

fun String.urlEncode(): String = URLEncoder.encode(this, Charsets.UTF_8.name())
