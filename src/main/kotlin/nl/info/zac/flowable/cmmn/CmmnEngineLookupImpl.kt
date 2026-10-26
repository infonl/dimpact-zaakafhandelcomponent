/*
 * SPDX-FileCopyrightText: 2021 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.flowable.cmmn

import nl.info.zac.flowable.processengine.ProcessEngineLookupImpl
import org.flowable.cdi.spi.CmmnEngineLookup
import org.flowable.cmmn.engine.CmmnEngine

class CmmnEngineLookupImpl : CmmnEngineLookup {
    override fun getPrecedence() = 0

    override fun getCmmnEngine(): CmmnEngine = ProcessEngineLookupImpl.getCmmnEngineConfiguration().buildCmmnEngine()

    override fun ungetCmmnEngine() {
        // no-op
    }
}
