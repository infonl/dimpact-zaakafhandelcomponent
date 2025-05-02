package net.atos.zac.app.admin.converter;

import net.atos.zac.admin.model.BetrokkeneKoppelingen;
import nl.info.zac.app.admin.model.RestBetrokkeneKoppelingen;

public final class RestBetrokkeneKoppelingenConverter {
    public RestBetrokkeneKoppelingen convert(final BetrokkeneKoppelingen betrokkeneKoppelingen) {
        final RestBetrokkeneKoppelingen restBetrokkeneKoppelingen = new RestBetrokkeneKoppelingen();
        restBetrokkeneKoppelingen.setId(betrokkeneKoppelingen.getId());
        restBetrokkeneKoppelingen.setBrpKoppelen(betrokkeneKoppelingen.getBrpKoppelen());
        restBetrokkeneKoppelingen.setKvkKoppelen(betrokkeneKoppelingen.getKvkKoppelen());

        return restBetrokkeneKoppelingen;
    }

    public BetrokkeneKoppelingen convert(final RestBetrokkeneKoppelingen restBetrokkeneKoppelingen) {
        final BetrokkeneKoppelingen betrokkeneKoppelingen = new BetrokkeneKoppelingen();
        betrokkeneKoppelingen.setId(restBetrokkeneKoppelingen.getId());
        betrokkeneKoppelingen.setBrpKoppelen(restBetrokkeneKoppelingen.getBrpKoppelen());
        betrokkeneKoppelingen.setKvkKoppelen(restBetrokkeneKoppelingen.getKvkKoppelen());

        return betrokkeneKoppelingen;
    }
}
