/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, HostListener, Input } from "@angular/core";

import { MatSidenav } from "@angular/material/sidenav";
import { IndicatiesLayout } from "../../../shared/indicaties/indicaties.component";
import { GeneratedType } from "../../../shared/utils/generated-types";
import { DocumentZoekObject } from "../../model/documenten/document-zoek-object";
import { TaakZoekObject } from "../../model/taken/taak-zoek-object";
import { ZaakZoekObject } from "../../model/zaken/zaak-zoek-object";

@Component({
  selector: "zac-zoek-object-link",
  styleUrls: ["./zoek-object-link.component.less"],
  templateUrl: "./zoek-object-link.component.html",
})
export class ZoekObjectLinkComponent {
  @Input({ required: true })
  zoekObject!: GeneratedType<"AbstractRestZoekObjectExtendsAbstractRestZoekObject">;
  @Input({ required: true }) sideNav!: MatSidenav;
  _newtab = false;
  indicatiesLayout = IndicatiesLayout;

  @HostListener("document:keydown", ["$event"])
  handleKeydown(event: KeyboardEvent) {
    if (event.key === "Control") {
      this._newtab = true;
    }
  }

  @HostListener("document:keyup", ["$event"])
  handleKeyup(event: KeyboardEvent) {
    if (event.key === "Control") {
      this._newtab = false;
    }
  }

  getLink() {
    switch (this.zoekObject.type) {
      case "ZAAK":
        return ["/zaken/", (this.zoekObject as ZaakZoekObject).identificatie];
      case "TAAK":
        return ["/taken/", this.zoekObject.id];
      case "DOCUMENT":
        return ["/informatie-objecten/", this.zoekObject.id];
      default:
        throw new Error(
          `Search object type ${this.zoekObject.type} is not supported`,
        );
    }
  }

  getName() {
    switch (this.zoekObject.type) {
      case "ZAAK":
        return (this.zoekObject as ZaakZoekObject).identificatie;
      case "TAAK":
        return (this.zoekObject as TaakZoekObject).naam;
      case "DOCUMENT":
        return (this.zoekObject as DocumentZoekObject).titel;
      default:
        throw new Error(
          `Search object type ${this.zoekObject.type} is not supported`,
        );
    }
  }
}
