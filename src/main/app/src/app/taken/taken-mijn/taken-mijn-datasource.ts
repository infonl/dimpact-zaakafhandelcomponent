/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { UtilService } from "../../core/service/util.service";
import { ZoekenDataSource } from "../../shared/dynamic-table/datasource/zoeken-data-source";
import { GeneratedType } from "../../shared/utils/generated-types";
import { TaakZoekObject } from "../../zoeken/model/taken/taak-zoek-object";
import { ZoekenService } from "../../zoeken/zoeken.service";

export class TakenMijnDatasource extends ZoekenDataSource<TaakZoekObject> {
  constructor(zoekenService: ZoekenService, utilService: UtilService) {
    super("MIJN_TAKEN", zoekenService, utilService);
  }

  protected initZoekparameters(
    zoekParameters: GeneratedType<"RestZoekParameters">,
  ) {
    return TakenMijnDatasource.mijnLopendeTaken(zoekParameters);
  }

  public static mijnLopendeTaken(
    zoekParameters: GeneratedType<"RestZoekParameters">,
  ) {
    return {
      ...zoekParameters,
      type: "TAAK",
      alleenMijnTaken: true,
    } satisfies GeneratedType<"RestZoekParameters">;
  }
}
