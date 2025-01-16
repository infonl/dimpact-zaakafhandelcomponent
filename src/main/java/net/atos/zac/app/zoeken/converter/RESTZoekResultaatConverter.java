/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.zoeken.converter;

import static net.atos.zac.app.zoeken.converter.RESTDocumentZoekObjectConverterKt.convertDocumentZoekObject;
import static net.atos.zac.app.zoeken.converter.RESTTaakZoekObjectConverterKt.convertTaakZoekObject;
import static net.atos.zac.app.zoeken.converter.RESTZaakZoekObjectConverterKt.convertZaakZoekObject;

import java.util.ArrayList;
import java.util.List;

import jakarta.inject.Inject;

import net.atos.zac.app.zoeken.model.AbstractRESTZoekObject;
import net.atos.zac.app.zoeken.model.RESTZoekParameters;
import net.atos.zac.app.zoeken.model.RESTZoekResultaat;
import net.atos.zac.policy.PolicyService;
import net.atos.zac.zoeken.model.FilterResultaat;
import net.atos.zac.zoeken.model.ZoekResultaat;
import net.atos.zac.zoeken.model.zoekobject.DocumentZoekObject;
import net.atos.zac.zoeken.model.zoekobject.TaakZoekObject;
import net.atos.zac.zoeken.model.zoekobject.ZaakZoekObject;
import net.atos.zac.zoeken.model.zoekobject.ZoekObject;

public class RESTZoekResultaatConverter {

    @Inject
    private PolicyService policyService;

    public RESTZoekResultaat<? extends AbstractRESTZoekObject> convert(
            final ZoekResultaat<? extends ZoekObject> zoekResultaat,
            final RESTZoekParameters zoekParameters
    ) {

        final RESTZoekResultaat<? extends AbstractRESTZoekObject> restZoekResultaat = new RESTZoekResultaat<>(
                zoekResultaat.getItems().stream().map(this::convert).toList(), zoekResultaat.getCount()
        );
        restZoekResultaat.filters.putAll(zoekResultaat.getFilters());

        // indien geen resultaten, de huidige filters laten staan
        zoekParameters.filters.forEach((filterVeld, filters) -> {
            final List<FilterResultaat> filterResultaten = restZoekResultaat.filters.getOrDefault(filterVeld, new ArrayList<>());
            filters.getValues().forEach(filter -> {
                if (filterResultaten.stream().noneMatch(filterResultaat -> filterResultaat.getNaam().equals(filter))) {
                    filterResultaten.add(new FilterResultaat(filter, 0));
                }
            });
            restZoekResultaat.filters.put(filterVeld, filterResultaten);
        });
        return restZoekResultaat;
    }

    private AbstractRESTZoekObject convert(final ZoekObject zoekObject) {
        return switch (zoekObject.getType()) {
            case ZAAK -> convertZaakZoekObject((ZaakZoekObject) zoekObject, policyService.readZaakRechten((ZaakZoekObject) zoekObject));
            case TAAK -> convertTaakZoekObject((TaakZoekObject) zoekObject, policyService.readTaakRechten((TaakZoekObject) zoekObject));
            case DOCUMENT -> convertDocumentZoekObject((DocumentZoekObject) zoekObject, policyService.readDocumentRechten(
                    (DocumentZoekObject) zoekObject));
        };
    }
}
