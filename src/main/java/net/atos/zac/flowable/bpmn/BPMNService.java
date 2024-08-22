/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.flowable.bpmn;

import static net.atos.client.zgw.shared.util.URIUtil.parseUUIDFromResourceURI;
import static net.atos.zac.flowable.ZaakVariabelenService.VAR_ZAAKTYPE_OMSCHRIJVING;
import static net.atos.zac.flowable.ZaakVariabelenService.VAR_ZAAKTYPE_UUUID;
import static net.atos.zac.flowable.ZaakVariabelenService.VAR_ZAAK_IDENTIFICATIE;
import static net.atos.zac.flowable.ZaakVariabelenService.VAR_ZAAK_UUID;

import java.io.InputStream;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;

import net.atos.client.zgw.zrc.model.Zaak;
import net.atos.client.zgw.ztc.model.generated.ZaakType;

@ApplicationScoped
@Transactional
public class BPMNService {

    private static final Logger LOG = Logger.getLogger(BPMNService.class.getName());

    @Inject
    private RepositoryService repositoryService;

    @Inject
    private RuntimeService runtimeService;

    @Inject
    private ProcessEngine processEngine;

    public InputStream getProcessDiagram(final UUID zaakUUID) {
        final var processInstance = findProcessInstance(zaakUUID);
        final var processDefinition = repositoryService.getProcessDefinition(processInstance.getProcessDefinitionId());
        final var bpmnModel = repositoryService.getBpmnModel(processDefinition.getId());
        final var processEngineConfiguration = processEngine.getProcessEngineConfiguration();
        return processEngineConfiguration.getProcessDiagramGenerator()
                .generateDiagram(bpmnModel, "gif",
                        runtimeService.getActiveActivityIds(processInstance.getId()),
                        Collections.emptyList(),
                        processEngineConfiguration.getActivityFontName(),
                        processEngineConfiguration.getLabelFontName(),
                        processEngineConfiguration.getAnnotationFontName(),
                        processEngineConfiguration.getClassLoader(), 1.0,
                        processEngineConfiguration.isDrawSequenceFlowNameWithNoLabelDI());
    }

    public boolean isProcesGestuurd(final UUID zaakUUID) {
        return findProcessInstance(zaakUUID) != null;
    }

    public ProcessDefinition readProcessDefinitionByprocessDefinitionKey(final String processDefinitionKey) {
        final ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                .processDefinitionKey(processDefinitionKey)
                .latestVersion()
                .singleResult();
        if (processDefinition != null) {
            return processDefinition;
        } else {
            throw new RuntimeException(
                    "No process definition found with process definition key: '%s'".formatted(processDefinitionKey)
            );
        }
    }

    public void startProcess(
            final Zaak zaak,
            final ZaakType zaaktype,
            final Map<String, Object> zaakData,
            final String processDefinitionKey
    ) {
        try {
            runtimeService.createProcessInstanceBuilder()
                    .processDefinitionKey(processDefinitionKey)
                    .businessKey(zaak.getUuid().toString())
                    .variable(VAR_ZAAK_UUID, zaak.getUuid())
                    .variable(VAR_ZAAK_IDENTIFICATIE, zaak.getIdentificatie())
                    .variable(VAR_ZAAKTYPE_UUUID, parseUUIDFromResourceURI(zaaktype.getUrl()))
                    .variable(VAR_ZAAKTYPE_OMSCHRIJVING, zaaktype.getOmschrijving())
                    .variables(zaakData)
                    .start();
            LOG.info("Zaak %s gestart met BPMN model '%s'".formatted(zaak.getUuid(), processDefinitionKey));
        } catch (final FlowableObjectNotFoundException flowableObjectNotFoundException) {
            LOG.severe("Zaak %s niet gestart omdat BPMN model '%s' niet bestaat"
                    .formatted(zaak.getUuid(), processDefinitionKey));
        }
    }

    private ProcessInstance findProcessInstance(final UUID zaakUUID) {
        return runtimeService.createProcessInstanceQuery()
                .processInstanceBusinessKey(zaakUUID.toString())
                .singleResult();
    }
}
