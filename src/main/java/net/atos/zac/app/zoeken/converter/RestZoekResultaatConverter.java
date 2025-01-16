/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.zoeken.converter;

import static net.atos.zac.app.zoeken.converter.RestDocumentZoekObjectConverterKt.toRestDocumentZoekObject;
import static net.atos.zac.app.zoeken.converter.RestTaakZoekObjectConverterKt.toRestTaakZoekObject;
import static net.atos.zac.app.zoeken.converter.RestZaakZoekObjectConverterKt.toRestZaakZoekObject;

import java.util.ArrayList;
import java.util.List;

import jakarta.inject.Inject;

import net.atos.zac.app.zoeken.model.AbstractRestZoekObject;
import net.atos.zac.app.zoeken.model.RestZoekParameters;
import net.atos.zac.app.zoeken.model.RestZoekResultaat;
import net.atos.zac.policy.PolicyService;
import net.atos.zac.zoeken.model.FilterResultaat;
import net.atos.zac.zoeken.model.ZoekResultaat;
import net.atos.zac.zoeken.model.zoekobject.DocumentZoekObject;
import net.atos.zac.zoeken.model.zoekobject.TaakZoekObject;
import net.atos.zac.zoeken.model.zoekobject.ZaakZoekObject;
import net.atos.zac.zoeken.model.zoekobject.ZoekObject;

public class RestZoekResultaatConverter {

    @Inject
    private PolicyService policyService;

    public RestZoekResultaat<? extends AbstractRestZoekObject> convert(
            final ZoekResultaat<? extends ZoekObject> zoekResultaat,
            final RestZoekParameters zoekParameters
    ) {

        final RestZoekResultaat<? extends AbstractRestZoekObject> restZoekResultaat = new RestZoekResultaat<>(
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

    private AbstractRestZoekObject convert(final ZoekObject zoekObject) {
        return switch (zoekObject.getType()) {
            case ZAAK -> toRestZaakZoekObject((ZaakZoekObject) zoekObject, policyService.readZaakRechten((ZaakZoekObject) zoekObject));
            case TAAK -> toRestTaakZoekObject((TaakZoekObject) zoekObject, policyService.readTaakRechten((TaakZoekObject) zoekObject));
            case DOCUMENT -> toRestDocumentZoekObject((DocumentZoekObject) zoekObject, policyService.readDocumentRechten(
                    (DocumentZoekObject) zoekObject));
        };
    }
}
