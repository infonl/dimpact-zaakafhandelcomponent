/*
 * SPDX-FileCopyrightText: 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.admin.converter;

import net.atos.zac.admin.model.BetrokkeneKoppelingen;
import net.atos.zac.admin.model.ZaakafhandelParameters;
import nl.info.zac.app.admin.model.RestBetrokkeneKoppelingen;

public final class RestBetrokkeneKoppelingenConverter {
    public RestBetrokkeneKoppelingen convert(final BetrokkeneKoppelingen betrokkeneKoppelingen) {
        final RestBetrokkeneKoppelingen restBetrokkeneKoppelingen = new RestBetrokkeneKoppelingen();
        restBetrokkeneKoppelingen.setId(betrokkeneKoppelingen.getId());
        restBetrokkeneKoppelingen.setBrpKoppelen(betrokkeneKoppelingen.getBrpKoppelen());
        restBetrokkeneKoppelingen.setKvkKoppelen(betrokkeneKoppelingen.getKvkKoppelen());

        return restBetrokkeneKoppelingen;
    }

    public BetrokkeneKoppelingen convert(
            final RestBetrokkeneKoppelingen restBetrokkeneKoppelingen,
            final ZaakafhandelParameters zaakafhandelParameters
    ) {
        final BetrokkeneKoppelingen betrokkeneKoppelingen = new BetrokkeneKoppelingen();
        betrokkeneKoppelingen.setId(restBetrokkeneKoppelingen.getId());
        betrokkeneKoppelingen.setKvkKoppelen(restBetrokkeneKoppelingen.getKvkKoppelen());
        betrokkeneKoppelingen.setBrpKoppelen(restBetrokkeneKoppelingen.getBrpKoppelen());
        betrokkeneKoppelingen.setZaakafhandelParameters(zaakafhandelParameters);

        return betrokkeneKoppelingen;
    }
}
