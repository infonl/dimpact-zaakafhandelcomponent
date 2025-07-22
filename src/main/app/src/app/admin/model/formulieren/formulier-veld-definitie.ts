/*
 * SPDX-FileCopyrightText: 2023 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { FormControl, FormGroup, Validators } from "@angular/forms";
import { GeneratedType } from "../../../shared/utils/generated-types";

/**
 * @deprecated - use the `GeneratedType`
 */
export class FormulierVeldDefinitie {
  id: number;
  systeemnaam: string;
  volgorde: number;
  label: string;
  veldtype: GeneratedType<"FormulierVeldtype">;
  beschrijving: string;
  helptekst: string;
  verplicht: boolean;
  defaultWaarde: string;
  meerkeuzeOpties: string;
  validaties: string[];

  static asFormGroup(
    veldDefinitie: GeneratedType<"RESTFormulierVeldDefinitie">,
  ) {
    return new FormGroup({
      id: new FormControl(veldDefinitie.id),
      label: new FormControl(veldDefinitie.label, Validators.required),
      systeemnaam: new FormControl(
        veldDefinitie.systeemnaam,
        Validators.required,
      ),
      beschrijving: new FormControl(veldDefinitie.beschrijving),
      helptekst: new FormControl(veldDefinitie.helptekst),
      veldtype: new FormControl(veldDefinitie.veldtype, Validators.required),
      defaultWaarde: new FormControl(veldDefinitie.defaultWaarde),
      verplicht: new FormControl(!!veldDefinitie.verplicht),
      meerkeuzeOpties: new FormControl(
        {
          value: veldDefinitie.meerkeuzeOpties,
          disabled: !this.isMeerkeuzeVeld(veldDefinitie.veldtype),
        },
        Validators.required,
      ),
      volgorde: new FormControl(veldDefinitie.volgorde, Validators.required),
    });
  }

  static asControl(veldDefinitie: GeneratedType<"RESTFormulierVeldDefinitie">) {
    let control = new FormControl<string | Date | boolean>(
      veldDefinitie.defaultWaarde ?? "",
      veldDefinitie.verplicht ? Validators.required : null,
    );
    switch (veldDefinitie.veldtype) {
      case "NUMMER":
        control = new FormControl<string>(
          veldDefinitie.defaultWaarde ?? "",
          veldDefinitie.verplicht
            ? [
                Validators.required,
                Validators.min(0),
                Validators.max(2147483647),
              ]
            : [Validators.min(0), Validators.max(2147483647)],
        );
        break;
      case "EMAIL":
        control = new FormControl<string>(
          veldDefinitie.defaultWaarde ?? "",
          veldDefinitie.verplicht
            ? [Validators.required, Validators.email]
            : Validators.email,
        );
        break;
      case "DATUM":
        control.setValue(this.toDate(veldDefinitie.defaultWaarde));
        break;
      default:
        break;
    }
    return control;
  }

  private static toDate(dateStr?: string | null) {
    if (!dateStr) return new Date();

    const [day, month, year] = dateStr.split("-");
    return new Date(Number(year), Number(month) - 1, Number(day));
  }

  static isMeerkeuzeVeld(veldtype: GeneratedType<"FormulierVeldtype">) {
    return ["CHECKBOXES", "RADIO", "KEUZELIJST", "DOCUMENTEN_LIJST"].includes(
      veldtype,
    );
  }

  static isFataldatum(
    formulierVeldDefinitie: GeneratedType<"RESTFormulierVeldDefinitie">,
  ) {
    return (
      formulierVeldDefinitie.veldtype === "DATUM" &&
      formulierVeldDefinitie.systeemnaam === "fatale-datum"
    );
  }

  static isOpschorten(
    formulierVeldDefinitie: GeneratedType<"RESTFormulierVeldDefinitie">,
  ) {
    return (
      formulierVeldDefinitie.veldtype === "CHECKBOX" &&
      formulierVeldDefinitie.systeemnaam === "zaak-opschorten"
    );
  }

  static isHervatten(
    formulierVeldDefinitie: GeneratedType<"RESTFormulierVeldDefinitie">,
  ) {
    return (
      formulierVeldDefinitie.veldtype === "CHECKBOX" &&
      formulierVeldDefinitie.systeemnaam === "zaak-hervatten"
    );
  }

  static isToekenningGroep(
    formulierVeldDefinitie: GeneratedType<"RESTFormulierVeldDefinitie">,
  ) {
    return (
      formulierVeldDefinitie.veldtype === "GROEP_KEUZELIJST" &&
      formulierVeldDefinitie.systeemnaam === "toekenning-groep"
    );
  }

  static isToekenningBehandelaar(
    formulierVeldDefinitie: GeneratedType<"RESTFormulierVeldDefinitie">,
  ) {
    return (
      formulierVeldDefinitie.veldtype === "MEDEWERKER_KEUZELIJST" &&
      formulierVeldDefinitie.systeemnaam === "toekenning-behandelaar"
    );
  }

  static isOndertekenen(
    formulierVeldDefinitie: GeneratedType<"RESTFormulierVeldDefinitie">,
  ) {
    // zou ook een eigen veldtype kunnen zijn
    return (
      formulierVeldDefinitie.veldtype === "DOCUMENTEN_LIJST" &&
      formulierVeldDefinitie.systeemnaam === "ondertekenen"
    );
  }
}
