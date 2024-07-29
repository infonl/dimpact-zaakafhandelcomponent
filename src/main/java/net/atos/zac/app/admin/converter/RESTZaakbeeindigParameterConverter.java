/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.admin.converter;

import java.util.List;
import java.util.Set;

import jakarta.inject.Inject;

import net.atos.client.zgw.ztc.ZtcClientService;
import net.atos.zac.admin.model.ZaakbeeindigParameter;
import net.atos.zac.app.admin.model.RESTZaakbeeindigParameter;
import net.atos.zac.app.zaak.converter.RESTResultaattypeConverter;

public class RESTZaakbeeindigParameterConverter {

    @Inject
    private RESTResultaattypeConverter restResultaattypeConverter;

    @Inject
    private ZtcClientService ztcClientService;

    public List<RESTZaakbeeindigParameter> convertZaakbeeindigParameters(final Set<ZaakbeeindigParameter> zaakbeeindigRedenen) {
        return zaakbeeindigRedenen.stream()
                .map(this::convertZaakbeeindigParameter)
                .toList();
    }

    public List<ZaakbeeindigParameter> convertRESTZaakbeeindigParameters(final List<RESTZaakbeeindigParameter> restZaakbeeindigParameters) {
        return restZaakbeeindigParameters.stream()
                .map(this::convertRESTZaakbeeindigParameter)
                .toList();
    }

    private RESTZaakbeeindigParameter convertZaakbeeindigParameter(final ZaakbeeindigParameter zaakbeeindigParameter) {
        final RESTZaakbeeindigParameter restZaakbeeindigParameter = new RESTZaakbeeindigParameter();
        restZaakbeeindigParameter.id = zaakbeeindigParameter.getId();
        restZaakbeeindigParameter.zaakbeeindigReden = RESTZaakbeeindigRedenConverter.convertZaakbeeindigReden(zaakbeeindigParameter
                .getZaakbeeindigReden()
        );
        restZaakbeeindigParameter.resultaattype = restResultaattypeConverter.convertResultaattype(
                ztcClientService.readResultaattype(zaakbeeindigParameter.getResultaattype())
        );
        return restZaakbeeindigParameter;
    }

    private ZaakbeeindigParameter convertRESTZaakbeeindigParameter(final RESTZaakbeeindigParameter restZaakbeeindigParameter) {
        final ZaakbeeindigParameter zaakbeeindigParameter = new ZaakbeeindigParameter();
        zaakbeeindigParameter.setId(restZaakbeeindigParameter.id);
        zaakbeeindigParameter.setZaakbeeindigReden(
                RESTZaakbeeindigRedenConverter.convertRESTZaakbeeindigReden(restZaakbeeindigParameter.zaakbeeindigReden)
        );
        zaakbeeindigParameter.setResultaattype(restZaakbeeindigParameter.resultaattype.getId());
        return zaakbeeindigParameter;
    }
}
