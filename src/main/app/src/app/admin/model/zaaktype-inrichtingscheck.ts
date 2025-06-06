/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Zaaktype } from "../../zaken/model/zaaktype";

/**
 * @deprecated - use the `GeneratedType`
 */
export class ZaaktypeInrichtingscheck {
  zaaktype: Zaaktype;
  statustypeIntakeAanwezig: boolean;
  statustypeInBehandelingAanwezig: boolean;
  statustypeHeropendAanwezig: boolean;
  statustypeAanvullendeInformatieVereist: boolean;
  statustypeAfgerondAanwezig: boolean;
  statustypeAfgerondLaatsteVolgnummer: boolean;
  resultaattypeAanwezig: boolean;
  rolInitiatorAanwezig: boolean;
  rolBehandelaarAanwezig: boolean;
  rolOverigeAanwezig: boolean;
  informatieobjecttypeEmailAanwezig: boolean;
  besluittypeAanwezig: boolean;
  resultaattypesMetVerplichtBesluit: string[];
  zaakafhandelParametersValide: boolean;
  valide: boolean;
}
