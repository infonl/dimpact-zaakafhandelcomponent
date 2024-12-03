/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 Dimpact
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.flowable.bpmn;

import static net.atos.zac.flowable.ZaakVariabelenService.*;
import static net.atos.zac.util.UriUtilsKt.extractUuid;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

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
                        processEngineConfiguration.isDrawSequenceFlowNameWithNoLabelDI()
                );
    }

    public boolean isProcesGestuurd(final UUID zaakUUID) {
        return findProcessInstance(zaakUUID) != null;
    }

    public ProcessDefinition findProcessDefinitionByprocessDefinitionKey(final String processDefinitionKey) {
        return repositoryService.createProcessDefinitionQuery()
                .processDefinitionKey(processDefinitionKey)
                .active()
                .latestVersion()
                .singleResult();
    }

    public ProcessDefinition readProcessDefinitionByprocessDefinitionKey(final String processDefinitionKey) {
        final ProcessDefinition processDefinition = findProcessDefinitionByprocessDefinitionKey(processDefinitionKey);
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
            Map<String, Object> zaakData
    ) {
        final var processDefinitionKey = zaaktype.getReferentieproces().getNaam();
        LOG.info(() -> String.format("Starting zaak '%s' using BPMN model '%s'", zaak.getUuid(), processDefinitionKey));
        if (zaakData == null) {
            zaakData = Collections.emptyMap();
        }
        runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey(processDefinitionKey)
                .businessKey(zaak.getUuid().toString())
                .variable(VAR_ZAAK_UUID, zaak.getUuid())
                .variable(VAR_ZAAK_IDENTIFICATIE, zaak.getIdentificatie())
                .variable(VAR_ZAAKTYPE_UUUID, extractUuid(zaaktype.getUrl()))
                .variable(VAR_ZAAKTYPE_OMSCHRIJVING, zaaktype.getOmschrijving())
                .variables(zaakData)
                .start();
    }

    public List<ProcessDefinition> listProcessDefinitions() {
        return repositoryService.createProcessDefinitionQuery()
                .latestVersion()
                .orderByProcessDefinitionName().asc()
                .list();
    }

    public void addProcessDefinition(final String filename, final String processDefinitionContent) {
        repositoryService.createDeployment()
                .addString(filename, processDefinitionContent)
                .name(filename)
                .enableDuplicateFiltering()
                .deploy();
    }

    public void deleteProcessDefinition(final String processDefinitionKey) {
        repositoryService.createDeploymentQuery()
                .processDefinitionKey(processDefinitionKey)
                .list()
                .forEach(deployment -> repositoryService.deleteDeployment(deployment.getId(), true));
    }

    private ProcessInstance findProcessInstance(final UUID zaakUUID) {
        return runtimeService.createProcessInstanceQuery()
                .processInstanceBusinessKey(zaakUUID.toString())
                .singleResult();
    }
}
