/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.admin.converter;

import static nl.info.zac.app.zaak.model.RestResultaattypeKt.toRestResultaatType;

import java.util.List;
import java.util.Set;

import jakarta.inject.Inject;

import net.atos.zac.admin.model.ZaaktypeCmmnCompletionParameters;
import net.atos.zac.app.admin.model.RESTZaakbeeindigParameter;
import nl.info.client.zgw.ztc.ZtcClientService;

public class RESTZaakbeeindigParameterConverter {

    @Inject
    private ZtcClientService ztcClientService;

    public List<RESTZaakbeeindigParameter> convertZaakbeeindigParameters(final Set<ZaaktypeCmmnCompletionParameters> zaakbeeindigRedenen) {
        return zaakbeeindigRedenen.stream()
                .map(this::convertZaakbeeindigParameter)
                .toList();
    }

    public static List<ZaaktypeCmmnCompletionParameters> convertRESTZaakbeeindigParameters(
            final List<RESTZaakbeeindigParameter> restZaakbeeindigParameters
    ) {
        return restZaakbeeindigParameters.stream()
                .map(RESTZaakbeeindigParameterConverter::convertRESTZaakbeeindigParameter)
                .toList();
    }

    private RESTZaakbeeindigParameter convertZaakbeeindigParameter(
            final ZaaktypeCmmnCompletionParameters zaaktypeCmmnCompletionParameters
    ) {
        final RESTZaakbeeindigParameter restZaakbeeindigParameter = new RESTZaakbeeindigParameter();
        restZaakbeeindigParameter.id = zaaktypeCmmnCompletionParameters.getId();
        restZaakbeeindigParameter.zaakbeeindigReden = RESTZaakbeeindigRedenConverter.convertZaakbeeindigReden(
                zaaktypeCmmnCompletionParameters
                        .getZaakbeeindigReden()
        );
        restZaakbeeindigParameter.resultaattype = toRestResultaatType(ztcClientService.readResultaattype(zaaktypeCmmnCompletionParameters
                .getResultaattype()));
        return restZaakbeeindigParameter;
    }

    private static ZaaktypeCmmnCompletionParameters convertRESTZaakbeeindigParameter(
            final RESTZaakbeeindigParameter restZaakbeeindigParameter
    ) {
        final ZaaktypeCmmnCompletionParameters zaaktypeCmmnCompletionParameters = new ZaaktypeCmmnCompletionParameters();
        zaaktypeCmmnCompletionParameters.setId(restZaakbeeindigParameter.id);
        zaaktypeCmmnCompletionParameters.setZaakbeeindigReden(
                RESTZaakbeeindigRedenConverter.convertRESTZaakbeeindigReden(restZaakbeeindigParameter.zaakbeeindigReden)
        );
        zaaktypeCmmnCompletionParameters.setResultaattype(restZaakbeeindigParameter.resultaattype.getId());
        return zaaktypeCmmnCompletionParameters;
    }
}
