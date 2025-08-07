/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.test.org.flowable.cmmn.api.runtime

import org.flowable.cmmn.api.runtime.PlanItemInstance
import java.util.Date

@Suppress("LongParameterList")
fun createTestPlanItemInstance(
    id: String = "fakeId",
    name: String = "fakeName",
    state: String = "fakeState",
    caseDefinitionId: String = "fakeCaseDefinitionId",
    derivedCaseDefinitionId: String = "fakeDerivedCaseDefinitionId",
    caseInstanceId: String = "fakeCaseInstanceId",
    stageInstanceId: String = "fakeStageInstanceId",
    isStage: Boolean = false,
    elementId: String = "fakeElementId",
    planItemDefinitionId: String = "fakePlanItemDefinitionId",
    planItemDefinitionType: String = "fakePlanItemDefinitionType",
) = TestPlanItemInstance(
    id,
    name,
    state,
    caseDefinitionId,
    derivedCaseDefinitionId,
    caseInstanceId,
    stageInstanceId,
    isStage,
    elementId,
    planItemDefinitionId,
    planItemDefinitionType
)

data class TestPlanItemInstance(
    private val id: String,
    private val name: String,
    private val state: String,
    private val caseDefinitionId: String,
    private val derivedCaseDefinitionId: String,
    private val caseInstanceId: String,
    private val stageInstanceId: String,
    private val isStage: Boolean,
    private val elementId: String,
    private val planItemDefinitionId: String,
    private val planItemDefinitionType: String,
    private val createTime: Date? = null,
    private val lastAvailableTime: Date? = null,
    private val lastUnavailableTime: Date? = null,
    private val lastEnabledTime: Date? = null,
    private val lastDisabledTime: Date? = null,
    private val lastStartedTime: Date? = null,
    private val lastSuspendedTime: Date? = null,
    private val completedTime: Date? = null,
    private val occurredTime: Date? = null,
    private val terminatedTime: Date? = null,
    private val exitTime: Date? = null,
    private val endedTime: Date? = null,
    private val startUserId: String? = null,
    private val referenceId: String? = null,
    private val referenceType: String? = null,
    private val completable: Boolean = false,
    private val entryCriterionId: String? = null,
    private val exitCriterionId: String? = null,
    private val formKey: String? = null,
    private val extraValue: String? = null,
    private val tenantId: String? = null,
    private val planItemInstanceLocalVariables: Map<String, Any>? = null,
    private var localizedName: String? = null
) : PlanItemInstance {
    override fun getId() = id
    override fun getName() = name
    override fun getState() = state
    override fun getCaseDefinitionId() = caseDefinitionId
    override fun getDerivedCaseDefinitionId() = derivedCaseDefinitionId
    override fun getCaseInstanceId() = caseInstanceId
    override fun getStageInstanceId() = stageInstanceId
    override fun isStage() = isStage
    override fun getElementId() = elementId
    override fun getPlanItemDefinitionId() = planItemDefinitionId
    override fun getPlanItemDefinitionType() = planItemDefinitionType
    override fun getCreateTime() = createTime
    override fun getLastAvailableTime() = lastAvailableTime
    override fun getLastUnavailableTime() = lastUnavailableTime
    override fun getLastEnabledTime() = lastEnabledTime
    override fun getLastDisabledTime() = lastDisabledTime
    override fun getLastStartedTime() = lastStartedTime
    override fun getLastSuspendedTime() = lastSuspendedTime
    override fun getCompletedTime() = completedTime
    override fun getOccurredTime() = occurredTime
    override fun getTerminatedTime() = terminatedTime
    override fun getExitTime() = exitTime
    override fun getEndedTime() = endedTime
    override fun getStartUserId() = startUserId
    override fun getReferenceId() = referenceId
    override fun getReferenceType() = referenceType
    override fun isCompletable() = completable
    override fun getEntryCriterionId() = entryCriterionId
    override fun getExitCriterionId() = exitCriterionId
    override fun getFormKey() = formKey
    override fun getExtraValue() = extraValue
    override fun getTenantId() = tenantId
    override fun getPlanItemInstanceLocalVariables() = planItemInstanceLocalVariables
    override fun setLocalizedName(localizedName: String) {
        this.localizedName = localizedName
    }
}
