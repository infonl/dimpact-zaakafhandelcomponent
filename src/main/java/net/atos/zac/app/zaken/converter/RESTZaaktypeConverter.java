/*
 * SPDX-FileCopyrightText: 2022 Atos, 2023-2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.zaken.converter;

import static net.atos.client.zgw.ztc.util.ZaakTypeUtil.isNuGeldig;
import static net.atos.client.zgw.ztc.util.ZaakTypeUtil.isServicenormBeschikbaar;

import java.time.Period;
import java.util.ArrayList;

import jakarta.inject.Inject;

import net.atos.client.zgw.ztc.model.generated.ZaakType;
import net.atos.zac.app.admin.converter.RESTZaakafhandelParametersConverter;
import net.atos.zac.app.zaken.model.RESTZaaktype;
import net.atos.zac.app.zaken.model.RelatieType;
import net.atos.zac.util.PeriodUtil;
import net.atos.zac.util.UriUtil;
import net.atos.zac.zaaksturing.ZaakafhandelParameterService;
import net.atos.zac.zaaksturing.model.ZaakafhandelParameters;

public class RESTZaaktypeConverter {

  @Inject private RESTZaaktypeRelatieConverter zaaktypeRelatieConverter;

  @Inject private RESTZaakafhandelParametersConverter zaakafhandelParametersConverter;

  @Inject private ZaakafhandelParameterService zaakafhandelParameterService;

  public RESTZaaktype convert(final ZaakType zaaktype) {
    final RESTZaaktype restZaaktype = new RESTZaaktype();
    restZaaktype.uuid = UriUtil.uuidFromURI(zaaktype.getUrl());
    restZaaktype.identificatie = zaaktype.getIdentificatie();
    restZaaktype.doel = zaaktype.getDoel();
    restZaaktype.omschrijving = zaaktype.getOmschrijving();
    restZaaktype.servicenorm = isServicenormBeschikbaar(zaaktype);
    restZaaktype.versiedatum = zaaktype.getVersiedatum();
    restZaaktype.nuGeldig = isNuGeldig(zaaktype);
    restZaaktype.beginGeldigheid = zaaktype.getBeginGeldigheid();
    restZaaktype.eindeGeldigheid = zaaktype.getEindeGeldigheid();
    restZaaktype.vertrouwelijkheidaanduiding = zaaktype.getVertrouwelijkheidaanduiding();
    restZaaktype.opschortingMogelijk = zaaktype.getOpschortingEnAanhoudingMogelijk();
    restZaaktype.verlengingMogelijk = zaaktype.getVerlengingMogelijk();
    if (restZaaktype.verlengingMogelijk) {
      restZaaktype.verlengingstermijn =
          PeriodUtil.aantalDagenVanafHeden(Period.parse(zaaktype.getVerlengingstermijn()));
    }
    restZaaktype.zaaktypeRelaties = new ArrayList<>();
    if (zaaktype.getDeelzaaktypen() != null) {
      zaaktype.getDeelzaaktypen().stream()
          .map(
              deelzaaktype ->
                  zaaktypeRelatieConverter.convertToRESTZaaktypeRelatie(
                      deelzaaktype, RelatieType.DEELZAAK))
          .forEach(restZaaktype.zaaktypeRelaties::add);
    }
    if (zaaktype.getGerelateerdeZaaktypen() != null) {
      zaaktype.getGerelateerdeZaaktypen().stream()
          .map(zaaktypeRelatieConverter::convertToRESTZaaktypeRelatie)
          .forEach(restZaaktype.zaaktypeRelaties::add);
    }

    restZaaktype.informatieobjecttypes =
        zaaktype.getInformatieobjecttypen().stream().map(UriUtil::uuidFromURI).toList();

    if (zaaktype.getReferentieproces() != null) {
      restZaaktype.referentieproces = zaaktype.getReferentieproces().getNaam();
    }
    final ZaakafhandelParameters zaakafhandelParameters =
        zaakafhandelParameterService.readZaakafhandelParameters(restZaaktype.uuid);
    restZaaktype.zaakafhandelparameters =
        zaakafhandelParametersConverter.convertZaakafhandelParameters(zaakafhandelParameters, true);
    return restZaaktype;
  }
}
