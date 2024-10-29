/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.admin.converter;

import java.util.Collection;
import java.util.List;

import jakarta.inject.Inject;

import net.atos.zac.admin.model.HumanTaskParameters;
import net.atos.zac.app.admin.model.RESTHumanTaskParameters;
import net.atos.zac.app.admin.model.RESTPlanItemDefinition;
import net.atos.zac.app.admin.model.RestHumanTaskReferenceTable;
import net.atos.zac.app.planitems.model.DefaultHumanTaskFormulierKoppeling;

public class RESTHumanTaskParametersConverter {

    @Inject
    private RestHumanTaskReferenceTableConverter restHumanTaskReferenceTableConverter;

    public List<RESTHumanTaskParameters> convertHumanTaskParametersCollection(
            final Collection<HumanTaskParameters> humanTaskParametersCollection,
            final List<RESTPlanItemDefinition> humanTaskDefinitions
    ) {
        return humanTaskDefinitions.stream()
                .map(humanTaskDefinition -> convertHumanTaskDefinition(humanTaskDefinition, humanTaskParametersCollection))
                .toList();
    }

    public List<HumanTaskParameters> convertRESTHumanTaskParameters(
            final List<RESTHumanTaskParameters> restHumanTaskParametersList
    ) {
        return restHumanTaskParametersList.stream()
                .map(this::convertRESTHumanTaskParameters)
                .toList();
    }

    private RESTHumanTaskParameters convertHumanTaskDefinition(
            final RESTPlanItemDefinition humanTaskDefinition,
            final Collection<HumanTaskParameters> humanTaskParametersCollection
    ) {
        return humanTaskParametersCollection.stream()
                .filter(humanTaskParameters -> humanTaskParameters.getPlanItemDefinitionID()
                        .equals(humanTaskDefinition.id))
                .findAny()
                .map(humanTaskParameters -> convertToRESTHumanTaskParameters(humanTaskParameters, humanTaskDefinition))
                .orElseGet(() -> convertToRESTHumanTaskParameters(humanTaskDefinition));
    }

    private RESTHumanTaskParameters convertToRESTHumanTaskParameters(
            final HumanTaskParameters humanTaskParameters,
            final RESTPlanItemDefinition humanTaskDefinition
    ) {
        final RESTHumanTaskParameters restHumanTaskParameters = new RESTHumanTaskParameters();
        restHumanTaskParameters.id = humanTaskParameters.getId();
        restHumanTaskParameters.actief = humanTaskParameters.isActief();
        restHumanTaskParameters.defaultGroepId = humanTaskParameters.getGroepID();
        restHumanTaskParameters.planItemDefinition = humanTaskDefinition;
        restHumanTaskParameters.formulierDefinitieId = humanTaskParameters.getFormulierDefinitieID();
        restHumanTaskParameters.doorlooptijd = humanTaskParameters.getDoorlooptijd();
        restHumanTaskParameters.referentieTabellen = convertReferentieTabellen(humanTaskParameters,
                humanTaskDefinition);
        return restHumanTaskParameters;
    }

    private List<RestHumanTaskReferenceTable> convertReferentieTabellen(
            final HumanTaskParameters humanTaskParameters,
            final RESTPlanItemDefinition humanTaskDefinition
    ) {
        final List<RestHumanTaskReferenceTable> referentieTabellen = restHumanTaskReferenceTableConverter.convert(
                humanTaskParameters.getReferentieTabellen());
        DefaultHumanTaskFormulierKoppeling.Companion.readFormulierVeldDefinities(humanTaskDefinition.id).stream()
                .filter(veldDefinitie -> referentieTabellen.stream()
                        .noneMatch(referentieTabel -> veldDefinitie.name().equals(referentieTabel.veld)))
                .map(restHumanTaskReferenceTableConverter::convertDefault)
                .forEach(referentieTabellen::add);
        return referentieTabellen;
    }

    private HumanTaskParameters convertRESTHumanTaskParameters(final RESTHumanTaskParameters restHumanTaskParameters) {
        HumanTaskParameters humanTaskParameters = new HumanTaskParameters();
        humanTaskParameters.setId(restHumanTaskParameters.id);
        humanTaskParameters.setActief(restHumanTaskParameters.actief);
        humanTaskParameters.setDoorlooptijd(restHumanTaskParameters.doorlooptijd);
        humanTaskParameters.setPlanItemDefinitionID(restHumanTaskParameters.planItemDefinition.id);
        humanTaskParameters.setGroepID(restHumanTaskParameters.defaultGroepId);
        humanTaskParameters.setFormulierDefinitieID(restHumanTaskParameters.formulierDefinitieId);
        humanTaskParameters.setReferentieTabellen(
                restHumanTaskReferenceTableConverter.convert(restHumanTaskParameters.referentieTabellen));
        return humanTaskParameters;
    }

    private static RESTHumanTaskParameters convertToRESTHumanTaskParameters(final RESTPlanItemDefinition humanTaskDefinition) {
        final RESTHumanTaskParameters restHumanTaskParameters = new RESTHumanTaskParameters();
        restHumanTaskParameters.planItemDefinition = humanTaskDefinition;
        restHumanTaskParameters.actief = false;
        restHumanTaskParameters.formulierDefinitieId = DefaultHumanTaskFormulierKoppeling.Companion.readFormulierDefinitie(
                humanTaskDefinition.id).name();
        return restHumanTaskParameters;
    }
}
