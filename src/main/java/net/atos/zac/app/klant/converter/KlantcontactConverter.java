/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.klant.converter;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import net.atos.client.klant.model.Actor;
import net.atos.client.klant.model.ExpandBetrokkene;
import net.atos.client.klant.model.Klantcontact;
import net.atos.zac.app.klant.model.contactmoment.RESTContactmoment;

public class KlantcontactConverter {

    public Map<UUID, String> mapContactToInitiatorFullName(List<ExpandBetrokkene> betrokkenenWithKlantcontactList) {
        return betrokkenenWithKlantcontactList.stream()
                .filter(ExpandBetrokkene::getInitiator)
                .collect(Collectors.toMap(
                        betrokkene -> betrokkene.getExpand().getHadKlantcontact().getUuid(),
                        ExpandBetrokkene::getVolledigeNaam,
                        (a, b) -> b
                ));
    }

    public RESTContactmoment convert(
            final Klantcontact klantcontact,
            final Map<UUID, String> contactToFullNameMap
    ) {
        final var restContactmoment = new RESTContactmoment();
        if (klantcontact.getPlaatsgevondenOp() != null) {
            restContactmoment.registratiedatum = klantcontact.getPlaatsgevondenOp().toZonedDateTime();
        }
        restContactmoment.initiatiefnemer = contactToFullNameMap.get(klantcontact.getUuid());
        restContactmoment.kanaal = klantcontact.getKanaal();
        restContactmoment.tekst = klantcontact.getOnderwerp();
        if (klantcontact.getHadBetrokkenActoren() != null) {
            restContactmoment.medewerker = convert(klantcontact.getHadBetrokkenActoren());
        }
        return restContactmoment;
    }

    private String convert(final List<Actor> actors) {
        if (actors.isEmpty()) {
            return null;
        }

        StringBuilder result = new StringBuilder();
        for (Actor actor : actors) {
            if (isNotBlank(result)) {
                result.append(",");
            }
            if (isNotBlank(actor.getNaam())) {
                result.append(actor.getNaam());
            } else {
                var actoridentificator = actor.getActoridentificator();
                result.append("%s %s %s".formatted(
                        actoridentificator.getCodeObjecttype(),
                        actoridentificator.getCodeRegister(),
                        actoridentificator.getObjectId()
                ));
            }
        }
        return result.toString();
    }
}
