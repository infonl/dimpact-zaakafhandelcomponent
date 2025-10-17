/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { UtilService } from "../../core/service/util.service";
import { ZoekenDataSource } from "../../shared/dynamic-table/datasource/zoeken-data-source";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ZaakZoekObject } from "../../zoeken/model/zaken/zaak-zoek-object";
import { ZoekenService } from "../../zoeken/zoeken.service";

/**
 * Datasource voor de mijn zaken. Via deze class wordt de data voor de tabel opgehaald
 */
export class ZakenMijnDatasource extends ZoekenDataSource<ZaakZoekObject> {
  constructor(zoekenService: ZoekenService, utilService: UtilService) {
    super("MIJN_ZAKEN", zoekenService, utilService);
  }

  protected initZoekparameters(
    zoekParameters: GeneratedType<"RestZoekParameters">,
  ) {
    return ZakenMijnDatasource.mijnLopendeZaken(zoekParameters);
  }

  public static mijnLopendeZaken(
    zoekParameters: GeneratedType<"RestZoekParameters">,
  ) {
    return {
      ...zoekParameters,
      type: "ZAAK",
      alleenOpenstaandeZaken: true,
      alleenMijnZaken: true,
    } satisfies GeneratedType<"RestZoekParameters">;
  }
}
