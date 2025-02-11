/*
 * SPDX-FileCopyrightText: 2021 Atos, 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.flowable.cmmn

import net.atos.zac.flowable.processengine.ProcessEngineLookupImpl
import org.flowable.cdi.spi.CmmnEngineLookup
import org.flowable.cmmn.engine.CmmnEngine

class CmmnEngineLookupImpl : CmmnEngineLookup {
    override fun getPrecedence() = 0

    override fun getCmmnEngine(): CmmnEngine = ProcessEngineLookupImpl.getCmmnEngineConfiguration().buildCmmnEngine()

    override fun ungetCmmnEngine() {
        // no-op
    }
}
