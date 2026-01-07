/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.admin.converter;

import java.util.Collection;
import java.util.List;

import jakarta.inject.Inject;

import net.atos.zac.app.admin.model.RESTHumanTaskParameters;
import net.atos.zac.app.admin.model.RESTPlanItemDefinition;
import net.atos.zac.app.admin.model.RestHumanTaskReferenceTable;
import nl.info.zac.admin.model.ZaaktypeCmmnHumantaskParameters;
import nl.info.zac.app.planitems.converter.FormulierKoppelingConverterKt;

public class RESTHumanTaskParametersConverter {
    private RestHumanTaskReferenceTableConverter restHumanTaskReferenceTableConverter;

    /**
     * No-arg constructor for CDI..
     */
    public RESTHumanTaskParametersConverter() {
    }

    @Inject
    public RESTHumanTaskParametersConverter(
            final RestHumanTaskReferenceTableConverter restHumanTaskReferenceTableConverter
    ) {
        this.restHumanTaskReferenceTableConverter = restHumanTaskReferenceTableConverter;
    }

    public List<RESTHumanTaskParameters> convertHumanTaskParametersCollection(
            final Collection<ZaaktypeCmmnHumantaskParameters> zaaktypeCmmnHumantaskParametersCollection,
            final List<RESTPlanItemDefinition> humanTaskDefinitions
    ) {
        return humanTaskDefinitions.stream()
                .map(humanTaskDefinition -> convertHumanTaskDefinition(humanTaskDefinition, zaaktypeCmmnHumantaskParametersCollection))
                .toList();
    }

    public List<ZaaktypeCmmnHumantaskParameters> convertRESTHumanTaskParameters(
            final List<RESTHumanTaskParameters> restHumanTaskParametersList
    ) {
        return restHumanTaskParametersList.stream()
                .map(this::convertRESTHumanTaskParameters)
                .toList();
    }

    private RESTHumanTaskParameters convertHumanTaskDefinition(
            final RESTPlanItemDefinition humanTaskDefinition,
            final Collection<ZaaktypeCmmnHumantaskParameters> zaaktypeCmmnHumantaskParametersCollection
    ) {
        return zaaktypeCmmnHumantaskParametersCollection.stream()
                .filter(zaaktypeCmmnHumantaskParameters -> zaaktypeCmmnHumantaskParameters.getPlanItemDefinitionID()
                        .equals(humanTaskDefinition.id))
                .findAny()
                .map(zaaktypeCmmnHumantaskParameters -> convertToRESTHumanTaskParameters(zaaktypeCmmnHumantaskParameters,
                        humanTaskDefinition))
                .orElseGet(() -> convertToRESTHumanTaskParameters(humanTaskDefinition));
    }

    private RESTHumanTaskParameters convertToRESTHumanTaskParameters(
            final ZaaktypeCmmnHumantaskParameters zaaktypeCmmnHumantaskParameters,
            final RESTPlanItemDefinition humanTaskDefinition
    ) {
        final RESTHumanTaskParameters restHumanTaskParameters = new RESTHumanTaskParameters();
        restHumanTaskParameters.id = zaaktypeCmmnHumantaskParameters.getId();
        restHumanTaskParameters.actief = zaaktypeCmmnHumantaskParameters.getActief();
        restHumanTaskParameters.defaultGroepId = zaaktypeCmmnHumantaskParameters.getGroepID();
        restHumanTaskParameters.planItemDefinition = humanTaskDefinition;
        restHumanTaskParameters.formulierDefinitieId = zaaktypeCmmnHumantaskParameters.getFormulierDefinitieID();
        restHumanTaskParameters.doorlooptijd = zaaktypeCmmnHumantaskParameters.getDoorlooptijd();
        restHumanTaskParameters.referentieTabellen = convertReferentieTabellen(zaaktypeCmmnHumantaskParameters,
                humanTaskDefinition);
        return restHumanTaskParameters;
    }

    private List<RestHumanTaskReferenceTable> convertReferentieTabellen(
            final ZaaktypeCmmnHumantaskParameters zaaktypeCmmnHumantaskParameters,
            final RESTPlanItemDefinition humanTaskDefinition
    ) {
        final List<RestHumanTaskReferenceTable> referentieTabellen = restHumanTaskReferenceTableConverter.convert(
                zaaktypeCmmnHumantaskParameters.getReferentieTabellen());
        FormulierKoppelingConverterKt.readFormulierVeldDefinities(humanTaskDefinition.id).stream()
                .filter(veldDefinitie -> referentieTabellen.stream()
                        .noneMatch(referentieTabel -> veldDefinitie.name().equals(referentieTabel.veld)))
                .map(restHumanTaskReferenceTableConverter::convertDefault)
                .forEach(referentieTabellen::add);
        return referentieTabellen;
    }

    private ZaaktypeCmmnHumantaskParameters convertRESTHumanTaskParameters(final RESTHumanTaskParameters restHumanTaskParameters) {
        ZaaktypeCmmnHumantaskParameters zaaktypeCmmnHumantaskParameters = new ZaaktypeCmmnHumantaskParameters();
        zaaktypeCmmnHumantaskParameters.setId(restHumanTaskParameters.id);
        zaaktypeCmmnHumantaskParameters.setActief(restHumanTaskParameters.actief);
        zaaktypeCmmnHumantaskParameters.setDoorlooptijd(restHumanTaskParameters.doorlooptijd);
        zaaktypeCmmnHumantaskParameters.setPlanItemDefinitionID(restHumanTaskParameters.planItemDefinition.id);
        zaaktypeCmmnHumantaskParameters.setGroepID(restHumanTaskParameters.defaultGroepId);
        zaaktypeCmmnHumantaskParameters.setFormulierDefinitieID(restHumanTaskParameters.formulierDefinitieId);
        zaaktypeCmmnHumantaskParameters.setReferentieTabellen(
                restHumanTaskReferenceTableConverter.convert(restHumanTaskParameters.referentieTabellen));
        return zaaktypeCmmnHumantaskParameters;
    }

    private static RESTHumanTaskParameters convertToRESTHumanTaskParameters(final RESTPlanItemDefinition humanTaskDefinition) {
        final RESTHumanTaskParameters restHumanTaskParameters = new RESTHumanTaskParameters();
        restHumanTaskParameters.planItemDefinition = humanTaskDefinition;
        restHumanTaskParameters.actief = false;
        restHumanTaskParameters.formulierDefinitieId = FormulierKoppelingConverterKt.toFormulierDefinitie(
                humanTaskDefinition.id).name();
        return restHumanTaskParameters;
    }
}
