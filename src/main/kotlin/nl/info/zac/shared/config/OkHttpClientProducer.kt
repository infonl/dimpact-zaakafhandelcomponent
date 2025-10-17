/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.shared.config

import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.inject.Produces
import nl.info.zac.util.AllOpen
import okhttp3.OkHttpClient

@AllOpen
@ApplicationScoped
class OkHttpClientProducer {
    @Produces
    fun produceOkHttpClient(): OkHttpClient = OkHttpClient()
}
