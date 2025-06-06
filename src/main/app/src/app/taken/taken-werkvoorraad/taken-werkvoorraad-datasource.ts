/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { UtilService } from "../../core/service/util.service";
import { ZoekenDataSource } from "../../shared/dynamic-table/datasource/zoeken-data-source";
import { TaakZoekObject } from "../../zoeken/model/taken/taak-zoek-object";
import { ZoekObjectType } from "../../zoeken/model/zoek-object-type";
import { ZoekParameters } from "../../zoeken/model/zoek-parameters";
import { ZoekenService } from "../../zoeken/zoeken.service";

/**
 * Datasource voor de werkvoorraad taken. Via deze class wordt de data voor de tabel opgehaald
 */
export class TakenWerkvoorraadDatasource extends ZoekenDataSource<TaakZoekObject> {
  constructor(zoekenService: ZoekenService, utilService: UtilService) {
    super("WERKVOORRAAD_TAKEN", zoekenService, utilService);
  }

  protected initZoekparameters(zoekParameters: ZoekParameters) {
    zoekParameters.type = ZoekObjectType.TAAK;
  }
}
