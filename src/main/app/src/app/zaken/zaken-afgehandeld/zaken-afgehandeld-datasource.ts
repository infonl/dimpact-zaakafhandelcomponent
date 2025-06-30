/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { UtilService } from "../../core/service/util.service";
import { Werklijst } from "../../gebruikersvoorkeuren/model/werklijst";
import { ZoekenDataSource } from "../../shared/dynamic-table/datasource/zoeken-data-source";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ZaakZoekObject } from "../../zoeken/model/zaken/zaak-zoek-object";
import { ZoekenService } from "../../zoeken/zoeken.service";

export class ZakenAfgehandeldDatasource extends ZoekenDataSource<ZaakZoekObject> {
  constructor(zoekenService: ZoekenService, utilService: UtilService) {
    super(Werklijst.AFGEHANDELDE_ZAKEN, zoekenService, utilService);
  }

  protected initZoekparameters(
    zoekParameters: GeneratedType<"RestZoekParameters">,
  ) {
    return {
      ...zoekParameters,
      type: "ZAAK",
      alleenAfgeslotenZaken: true,
    } satisfies GeneratedType<"RestZoekParameters">;
  }
}
