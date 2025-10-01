/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.admin.converter;

import java.util.List;
import java.util.Set;

import net.atos.zac.app.admin.model.RESTPlanItemDefinition;
import net.atos.zac.app.admin.model.RESTUserEventListenerParameter;
import nl.info.zac.admin.model.ZaaktypeCmmnUsereventlistenerParameters;

public final class RESTUserEventListenerParametersConverter {

    public static List<RESTUserEventListenerParameter> convertUserEventListenerParametersCollection(
            final Set<ZaaktypeCmmnUsereventlistenerParameters> zaaktypeCmmnUsereventlistenerParametersCollection,
            final List<RESTPlanItemDefinition> userEventListenerDefinitions
    ) {
        return userEventListenerDefinitions.stream()
                .map(
                        userEventListenerDefinition -> convertUserEventListenerDefinition(
                                userEventListenerDefinition,
                                zaaktypeCmmnUsereventlistenerParametersCollection
                        )
                )
                .toList();
    }

    public static List<ZaaktypeCmmnUsereventlistenerParameters> convertRESTUserEventListenerParameters(
            final List<RESTUserEventListenerParameter> restUserEventListenerParameters
    ) {
        return restUserEventListenerParameters.stream()
                .map(RESTUserEventListenerParametersConverter::convertRESTUserEventListenerParameter)
                .toList();
    }

    private static ZaaktypeCmmnUsereventlistenerParameters convertRESTUserEventListenerParameter(
            final RESTUserEventListenerParameter restUserEventListenerParameter
    ) {
        final ZaaktypeCmmnUsereventlistenerParameters zaaktypeCmmnUsereventlistenerParameters = new ZaaktypeCmmnUsereventlistenerParameters();
        zaaktypeCmmnUsereventlistenerParameters.setPlanItemDefinitionID(restUserEventListenerParameter.id);
        zaaktypeCmmnUsereventlistenerParameters.setToelichting(restUserEventListenerParameter.toelichting);
        return zaaktypeCmmnUsereventlistenerParameters;
    }

    private static RESTUserEventListenerParameter convertUserEventListenerDefinition(
            final RESTPlanItemDefinition userEventListenerDefinition,
            final Set<ZaaktypeCmmnUsereventlistenerParameters> zaaktypeCmmnUsereventlistenerParametersCollection
    ) {
        final RESTUserEventListenerParameter restUserEventListenerParameter = new RESTUserEventListenerParameter();
        restUserEventListenerParameter.id = userEventListenerDefinition.id;
        restUserEventListenerParameter.naam = userEventListenerDefinition.naam;
        zaaktypeCmmnUsereventlistenerParametersCollection.stream()
                .filter(zaaktypeCmmnUsereventlistenerParameters -> zaaktypeCmmnUsereventlistenerParameters.getPlanItemDefinitionID().equals(
                        userEventListenerDefinition.id))
                .findAny()
                .ifPresent(
                        zaaktypeCmmnUsereventlistenerParameters -> restUserEventListenerParameter.toelichting = zaaktypeCmmnUsereventlistenerParameters
                                .getToelichting());
        return restUserEventListenerParameter;
    }
}
