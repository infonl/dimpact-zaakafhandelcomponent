/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.flowable.processengine

import net.atos.zac.flowable.bpmn.UserTaskCompletionListener
import net.atos.zac.flowable.bpmn.function.TaskFunctionsDelegate
import net.atos.zac.flowable.cmmn.CompleteTaskInterceptor
import net.atos.zac.flowable.cmmn.ZacCreateHumanTaskInterceptor
import nl.info.zac.flowable.task.ZacCreateUserTaskInterceptor
import org.flowable.cdi.CdiStandaloneProcessEngineConfiguration
import org.flowable.cdi.spi.ProcessEngineLookup
import org.flowable.cmmn.engine.CmmnEngineConfiguration
import org.flowable.cmmn.engine.configurator.CmmnEngineConfigurator
import org.flowable.common.engine.impl.AbstractEngineConfiguration.DATABASE_TYPE_POSTGRES
import org.flowable.common.engine.impl.AbstractEngineConfiguration.DB_SCHEMA_UPDATE_TRUE
import org.flowable.common.engine.impl.history.HistoryLevel.AUDIT
import org.flowable.common.engine.impl.interceptor.EngineConfigurationConstants
import org.flowable.engine.ProcessEngine
import org.flowable.cmmn.engine.impl.cfg.DelegateExpressionFieldInjectionMode as CmmnDelegateExpressionFieldInjectionMode
import org.flowable.engine.impl.cfg.DelegateExpressionFieldInjectionMode as BpmnDelegateExpressionFieldInjectionMode

class ProcessEngineLookupImpl : ProcessEngineLookup {
    companion object {
        const val DATABASE_SCHEMA = "flowable"
        const val DATA_SOURCE_JNDI_NAME = "java:comp/env/jdbc/FlowableDS"

        private val sharedProcessEngine: ProcessEngine by lazy { createProcessEngineConfiguration().buildProcessEngine() }

        fun getCmmnEngineConfiguration() =
            sharedProcessEngine.processEngineConfiguration
                .engineConfigurations[EngineConfigurationConstants.KEY_CMMN_ENGINE_CONFIG] as CmmnEngineConfiguration

        private fun createProcessEngineConfiguration() =
            CdiStandaloneProcessEngineConfiguration().apply {
                dataSourceJndiName = DATA_SOURCE_JNDI_NAME
                databaseType = DATABASE_TYPE_POSTGRES
                databaseSchema = DATABASE_SCHEMA
                databaseSchemaUpdate = DB_SCHEMA_UPDATE_TRUE
                historyLevel = AUDIT
                isEnableSafeBpmnXml = true
                delegateExpressionFieldInjectionMode = BpmnDelegateExpressionFieldInjectionMode.DISABLED
                isEnableHistoricTaskLogging = true
                isDisableIdmEngine = true
                isAsyncExecutorActivate = true
                createUserTaskInterceptor = ZacCreateUserTaskInterceptor()
                eventListeners = (eventListeners ?: mutableListOf()).apply { add(UserTaskCompletionListener()) }
                customFlowableFunctionDelegates = (customFlowableFunctionDelegates ?: mutableListOf()).apply {
                    add(TaskFunctionsDelegate())
                }
                configurators = listOf(
                    CmmnEngineConfigurator().apply { cmmnEngineConfiguration = createCmmnEngineConfiguration() }
                )
            }

        private fun createCmmnEngineConfiguration() =
            CmmnEngineConfiguration().apply {
                dataSourceJndiName = DATA_SOURCE_JNDI_NAME
                databaseType = DATABASE_TYPE_POSTGRES
                databaseSchema = DATABASE_SCHEMA
                databaseSchemaUpdate = DB_SCHEMA_UPDATE_TRUE
                historyLevel = AUDIT
                isEnableSafeCmmnXml = true
                delegateExpressionFieldInjectionMode = CmmnDelegateExpressionFieldInjectionMode.DISABLED
                isEnableHistoricTaskLogging = true
                createHumanTaskInterceptor = ZacCreateHumanTaskInterceptor()
                identityLinkInterceptor = CompleteTaskInterceptor(this)
                isDisableIdmEngine = true
            }
    }

    override fun getPrecedence() = 0

    override fun getProcessEngine(): ProcessEngine = sharedProcessEngine

    override fun ungetProcessEngine() {
        // no action needed
    }
}
