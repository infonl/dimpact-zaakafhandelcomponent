/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.flowable.cmmn;

import net.atos.client.zgw.zrc.model.Zaak;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.interceptor.DefaultCmmnIdentityLinkInterceptor;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;

import net.atos.zac.flowable.FlowableHelper;

import java.util.UUID;
import java.util.logging.Logger;

import static java.lang.String.format;
import static net.atos.zac.configuratie.ConfiguratieService.STATUSTYPE_OMSCHRIJVING_INTAKE;

public class CompleteTaskInterceptor extends DefaultCmmnIdentityLinkInterceptor {
    private static final Logger LOG = Logger.getLogger(CompleteTaskInterceptor.class.getName());
    private static final String STATUS_TOELICHTING = "Status gewijzigd";
    private static final String TASK_AANVULLENDE_INFORMATIE_ID = "AANVULLENDE_INFORMATIE";

    public CompleteTaskInterceptor(final CmmnEngineConfiguration cmmnEngineConfiguration) {
        super(cmmnEngineConfiguration);
    }

    @Override
    public void handleCompleteTask(final TaskEntity task) {
        super.handleCompleteTask(task);
        FlowableHelper.getInstance().getIndexeerService().removeTaak(task.getId());

        if (task.getTaskDefinitionKey().equals(TASK_AANVULLENDE_INFORMATIE_ID)) {
            // TODO: check if there are no other tasks with the same key still open
            PlanItemInstance planItemInstance = cmmnEngineConfiguration.getCmmnRuntimeService()
                    .createPlanItemInstanceQuery()
                    .planItemInstanceId(task.getSubScopeId())
                    .singleResult();
            updateZaak(planItemInstance, STATUSTYPE_OMSCHRIJVING_INTAKE);
        }
    }

    private void updateZaak(final PlanItemInstance planItemInstance, final String statustypeOmschrijving) {
        final UUID zaakUUID = FlowableHelper.getInstance().getZaakVariabelenService().readZaakUUID(planItemInstance);
        final Zaak zaak = FlowableHelper.getInstance().getZrcClientService().readZaak(zaakUUID);
        LOG.info(format("Zaak with UUID '%s': changing status to '%s'", zaakUUID, statustypeOmschrijving));
        FlowableHelper.getInstance().getZgwApiService().createStatusForZaak(zaak, statustypeOmschrijving, STATUS_TOELICHTING);
    }
}
