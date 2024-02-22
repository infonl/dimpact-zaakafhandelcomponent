/*
 * SPDX-FileCopyrightText: 2022 Atos, 2023-2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.audit.converter.documenten;

import java.util.stream.Stream;

import net.atos.client.zgw.drc.model.generated.Gebruiksrechten;
import net.atos.client.zgw.shared.model.ObjectType;
import net.atos.client.zgw.shared.model.audit.documenten.GebuiksrechtenWijziging;
import net.atos.zac.app.audit.converter.AbstractAuditWijzigingConverter;
import net.atos.zac.app.audit.model.RESTHistorieRegel;

public class AuditGebruiksrechtenWijzigingConverter
    extends AbstractAuditWijzigingConverter<GebuiksrechtenWijziging> {

  @Override
  public boolean supports(final ObjectType objectType) {
    return ObjectType.GEBRUIKSRECHTEN == objectType;
  }

  @Override
  protected Stream<RESTHistorieRegel> doConvert(final GebuiksrechtenWijziging wijziging) {
    return Stream.of(
        new RESTHistorieRegel(
            "indicatieGebruiksrecht",
            toWaarde(wijziging.getOud()),
            toWaarde(wijziging.getNieuw())));
  }

  private String toWaarde(final Gebruiksrechten gebruiksrechten) {
    return gebruiksrechten != null ? gebruiksrechten.getOmschrijvingVoorwaarden() : null;
  }
}
