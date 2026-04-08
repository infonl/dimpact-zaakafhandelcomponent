/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.detacheddocuments.converter;

import net.atos.zac.app.detacheddocuments.model.RestDetachedDocumentListParameters;
import net.atos.zac.app.shared.RESTListParametersConverter;
import nl.info.zac.document.detacheddocument.model.DetachedDocumentListParameters;
import nl.info.zac.search.model.DatumRange;

public class RestDetachedDocumentListParametersConverter extends
                                                         RESTListParametersConverter<DetachedDocumentListParameters, RestDetachedDocumentListParameters> {

    @Override
    protected void doConvert(
            final DetachedDocumentListParameters listParameters,
            final RestDetachedDocumentListParameters restListParameters
    ) {
        listParameters.setReden(restListParameters.reden);
        listParameters.setTitel(restListParameters.titel);

        if (restListParameters.creatiedatum != null && restListParameters.creatiedatum.hasValue()) {
            listParameters.setCreatiedatum(new DatumRange(
                    restListParameters.creatiedatum.getVan(), restListParameters.creatiedatum
                            .getTot()
            ));
        }

        if (restListParameters.ontkoppeldDoor != null) {
            listParameters.setOntkoppeldDoor(restListParameters.ontkoppeldDoor.getId());
        }

        if (restListParameters.ontkoppeldOp != null && restListParameters.ontkoppeldOp.hasValue()) {
            listParameters.setOntkoppeldOp(new DatumRange(
                    restListParameters.ontkoppeldOp.getVan(), restListParameters.ontkoppeldOp
                            .getTot()
            ));
        }

        listParameters.setZaakID(restListParameters.zaakID);
    }

    @Override
    protected DetachedDocumentListParameters getListParameters() {
        return new DetachedDocumentListParameters();
    }
}
