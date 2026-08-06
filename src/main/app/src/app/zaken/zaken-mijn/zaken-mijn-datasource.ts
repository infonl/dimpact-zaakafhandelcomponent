/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { UtilService } from "../../core/service/util.service";
import { ZoekenDataSource } from "../../shared/dynamic-table/datasource/zoeken-data-source";
import { ZaakZoekObject } from "../../zoeken/model/zaken/zaak-zoek-object";
import { ZoekParametersInternal } from "../../zoeken/model/zoek-parameters";
import { ZoekenService } from "../../zoeken/zoeken.service";

/**
 * Datasource voor de mijn zaken. Via deze class wordt de data voor de tabel opgehaald
 */
export class ZakenMijnDatasource extends ZoekenDataSource<ZaakZoekObject> {
  constructor(zoekenService: ZoekenService, utilService: UtilService) {
    super("MIJN_ZAKEN", zoekenService, utilService);
  }

  protected initZoekparameters(
    zoekParameters: ZoekParametersInternal,
  ): ZoekParametersInternal {
    return ZakenMijnDatasource.mijnLopendeZaken(zoekParameters);
  }

  public static mijnLopendeZaken(
    zoekParameters: ZoekParametersInternal,
  ): ZoekParametersInternal {
    return {
      ...zoekParameters,
      type: "ZAAK",
      alleenOpenstaandeZaken: true,
      alleenMijnZaken: true,
    };
  }
}
