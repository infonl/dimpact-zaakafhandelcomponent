/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.admin.converter;

import static nl.info.zac.app.zaak.model.RestResultaattypeKt.toRestResultaatType;

import java.util.List;
import java.util.Set;

import jakarta.inject.Inject;

import net.atos.zac.app.admin.model.RESTZaakbeeindigParameter;
import nl.info.client.zgw.ztc.ZtcClientService;
import nl.info.zac.admin.model.ZaaktypeCompletionParameters;

public class RESTZaakbeeindigParameterConverter {

    @Inject
    private ZtcClientService ztcClientService;

    public List<RESTZaakbeeindigParameter> convertZaakbeeindigParameters(final Set<ZaaktypeCompletionParameters> zaakbeeindigRedenen) {
        return zaakbeeindigRedenen.stream()
                .map(this::convertZaakbeeindigParameter)
                .toList();
    }

    public static List<ZaaktypeCompletionParameters> convertRESTZaakbeeindigParameters(
            final List<RESTZaakbeeindigParameter> restZaakbeeindigParameters
    ) {
        return restZaakbeeindigParameters.stream()
                .map(RESTZaakbeeindigParameterConverter::convertRESTZaakbeeindigParameter)
                .toList();
    }

    private RESTZaakbeeindigParameter convertZaakbeeindigParameter(
            final ZaaktypeCompletionParameters zaaktypeCompletionParameters
    ) {
        final RESTZaakbeeindigParameter restZaakbeeindigParameter = new RESTZaakbeeindigParameter();
        restZaakbeeindigParameter.id = zaaktypeCompletionParameters.getId();
        restZaakbeeindigParameter.zaakbeeindigReden = RESTZaakbeeindigRedenConverter.convertZaakbeeindigReden(
                zaaktypeCompletionParameters.zaakbeeindigReden
        );
        restZaakbeeindigParameter.resultaattype = toRestResultaatType(ztcClientService.readResultaattype(zaaktypeCompletionParameters
                .getResultaattype()));
        return restZaakbeeindigParameter;
    }

    private static ZaaktypeCompletionParameters convertRESTZaakbeeindigParameter(
            final RESTZaakbeeindigParameter restZaakbeeindigParameter
    ) {
        final ZaaktypeCompletionParameters zaaktypeCompletionParameters = new ZaaktypeCompletionParameters();
        zaaktypeCompletionParameters.setId(restZaakbeeindigParameter.id);
        zaaktypeCompletionParameters.setZaakbeeindigReden(
                RESTZaakbeeindigRedenConverter.convertRESTZaakbeeindigReden(restZaakbeeindigParameter.zaakbeeindigReden)
        );
        zaaktypeCompletionParameters.setResultaattype(restZaakbeeindigParameter.resultaattype.getId());
        return zaaktypeCompletionParameters;
    }
}
