/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.zoeken.converter;

import java.util.Arrays;
import java.util.List;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import net.atos.zac.shared.model.SorteerRichtingKt;
import org.apache.commons.lang3.BooleanUtils;

import net.atos.zac.app.zoeken.model.RESTZoekParameters;
import net.atos.zac.authentication.LoggedInUser;
import net.atos.zac.zoeken.model.DatumRange;
import net.atos.zac.zoeken.model.ZoekParameters;
import net.atos.zac.zoeken.model.ZoekVeld;
import net.atos.zac.zoeken.model.zoekobject.TaakZoekObject;
import net.atos.zac.zoeken.model.zoekobject.ZaakZoekObject;

public class RESTZoekParametersConverter {
    private static final List<String> zoekvelden = Arrays.stream(ZoekVeld.values()).map(Enum::name).toList();

    @Inject
    private Instance<LoggedInUser> loggedInUserInstance;

    public ZoekParameters convert(final RESTZoekParameters restZoekParameters) {
        final ZoekParameters zoekParameters = new ZoekParameters(restZoekParameters.type);

        restZoekParameters.filters.forEach(zoekParameters::addFilter);

        restZoekParameters.datums.forEach((key, value) -> {
            if (value != null && value.hasValue()) {
                zoekParameters.addDatum(key, new DatumRange(value.van, value.tot));
            }
        });

        if (restZoekParameters.alleenOpenstaandeZaken) {
            zoekParameters.addFilterQuery(ZaakZoekObject.AFGEHANDELD_FIELD, BooleanUtils.FALSE);
        }

        if (restZoekParameters.alleenAfgeslotenZaken) {
            zoekParameters.addFilterQuery(ZaakZoekObject.EINDSTATUS_FIELD, BooleanUtils.TRUE);
        }

        if (restZoekParameters.alleenMijnZaken) {
            zoekParameters.addFilterQuery(ZaakZoekObject.BEHANDELAAR_ID_FIELD, loggedInUserInstance.get().getId());
        }

        if (restZoekParameters.alleenMijnTaken) {
            zoekParameters.addFilterQuery(TaakZoekObject.BEHANDELAAR_ID_FIELD, loggedInUserInstance.get().getId());
        }

        if (restZoekParameters.zoeken != null) {
            restZoekParameters.zoeken.forEach((key, value) -> {
                if (zoekvelden.contains(key)) {
                    zoekParameters.addZoekVeld(ZoekVeld.valueOf(key), value);
                } else if (key.startsWith(ZaakZoekObject.ZAAK_BETROKKENE_PREFIX)) {
                    zoekParameters.addFilterQuery(key, value);
                }
            });
        }

        if (restZoekParameters.sorteerVeld != null) {
            zoekParameters.setSortering(restZoekParameters.sorteerVeld, SorteerRichtingKt.fromValue(restZoekParameters.sorteerRichting));
        }

        zoekParameters.setStart(restZoekParameters.page * restZoekParameters.rows);
        zoekParameters.setRows(restZoekParameters.rows);
        return zoekParameters;
    }
}
