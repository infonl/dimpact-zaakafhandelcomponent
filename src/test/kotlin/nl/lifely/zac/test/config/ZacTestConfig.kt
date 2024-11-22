/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.lifely.zac.test.config

import io.kotest.core.config.AbstractProjectConfig
import io.kotest.core.extensions.Extension
import nl.lifely.zac.test.listener.MockkClearingTestListener

object ZacTestConfig : AbstractProjectConfig() {
    override fun extensions(): List<Extension> = listOf(MockkClearingTestListener())
}
