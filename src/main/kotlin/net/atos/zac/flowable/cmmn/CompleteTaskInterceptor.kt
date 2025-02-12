/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.flowable.cmmn

import net.atos.zac.flowable.FlowableHelper
import org.flowable.cmmn.engine.CmmnEngineConfiguration
import org.flowable.cmmn.engine.impl.interceptor.DefaultCmmnIdentityLinkInterceptor
import org.flowable.task.service.impl.persistence.entity.TaskEntity


/**
 * Custom Flowable complete task interceptor.
 */
class CompleteTaskInterceptor(cmmnEngineConfiguration: CmmnEngineConfiguration) :
    DefaultCmmnIdentityLinkInterceptor(cmmnEngineConfiguration) {

    override fun handleCompleteTask(task: TaskEntity) {
        super.handleCompleteTask(task)
        FlowableHelper.getInstance().indexeerService.removeTaak(task.id)
    }
}
