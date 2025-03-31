/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.admin.converter;

import static nl.info.zac.app.zaak.model.RestResultaattypeKt.toRestResultaatType;

import java.util.List;
import java.util.Set;

import jakarta.inject.Inject;

import net.atos.zac.admin.model.ZaakbeeindigParameter;
import net.atos.zac.app.admin.model.RESTZaakbeeindigParameter;
import nl.info.client.zgw.ztc.ZtcClientService;

public class RESTZaakbeeindigParameterConverter {

    @Inject
    private ZtcClientService ztcClientService;

    public List<RESTZaakbeeindigParameter> convertZaakbeeindigParameters(final Set<ZaakbeeindigParameter> zaakbeeindigRedenen) {
        return zaakbeeindigRedenen.stream()
                .map(this::convertZaakbeeindigParameter)
                .toList();
    }

    public static List<ZaakbeeindigParameter> convertRESTZaakbeeindigParameters(
            final List<RESTZaakbeeindigParameter> restZaakbeeindigParameters
    ) {
        return restZaakbeeindigParameters.stream()
                .map(RESTZaakbeeindigParameterConverter::convertRESTZaakbeeindigParameter)
                .toList();
    }

    private RESTZaakbeeindigParameter convertZaakbeeindigParameter(final ZaakbeeindigParameter zaakbeeindigParameter) {
        final RESTZaakbeeindigParameter restZaakbeeindigParameter = new RESTZaakbeeindigParameter();
        restZaakbeeindigParameter.id = zaakbeeindigParameter.getId();
        restZaakbeeindigParameter.zaakbeeindigReden = RESTZaakbeeindigRedenConverter.convertZaakbeeindigReden(zaakbeeindigParameter
                .getZaakbeeindigReden()
        );
        restZaakbeeindigParameter.resultaattype = toRestResultaatType(ztcClientService.readResultaattype(zaakbeeindigParameter
                .getResultaattype()));
        return restZaakbeeindigParameter;
    }

    private static ZaakbeeindigParameter convertRESTZaakbeeindigParameter(final RESTZaakbeeindigParameter restZaakbeeindigParameter) {
        final ZaakbeeindigParameter zaakbeeindigParameter = new ZaakbeeindigParameter();
        zaakbeeindigParameter.setId(restZaakbeeindigParameter.id);
        zaakbeeindigParameter.setZaakbeeindigReden(
                RESTZaakbeeindigRedenConverter.convertRESTZaakbeeindigReden(restZaakbeeindigParameter.zaakbeeindigReden)
        );
        zaakbeeindigParameter.setResultaattype(restZaakbeeindigParameter.resultaattype.getId());
        return zaakbeeindigParameter;
    }
}
