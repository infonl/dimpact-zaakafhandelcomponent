/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { NgIf } from "@angular/common";
import { Component, HostListener, Input } from "@angular/core";
import { MatIconModule } from "@angular/material/icon";
import { MatSidenav } from "@angular/material/sidenav";
import { RouterLink } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { IndicatiesLayout } from "../../../shared/indicaties/indicaties.component";
import { InformatieObjectIndicatiesComponent } from "../../../shared/indicaties/informatie-object-indicaties/informatie-object-indicaties.component";
import { ZaakIndicatiesComponent } from "../../../shared/indicaties/zaak-indicaties/zaak-indicaties.component";
import { ReadMoreComponent } from "../../../shared/read-more/read-more.component";
import { GeneratedType } from "../../../shared/utils/generated-types";
import { DocumentZoekObject } from "../../model/documenten/document-zoek-object";
import { TaakZoekObject } from "../../model/taken/taak-zoek-object";
import { ZaakZoekObject } from "../../model/zaken/zaak-zoek-object";

type AbstractRestZoekObject =
  GeneratedType<"AbstractRestZoekObjectExtendsAbstractRestZoekObject">;

@Component({
  selector: "zac-zoek-object-link",
  styleUrls: ["./zoek-object-link.component.less"],
  templateUrl: "./zoek-object-link.component.html",
  standalone: true,
  imports: [
    NgIf,
    RouterLink,
    MatIconModule,
    TranslateModule,
    ReadMoreComponent,
    ZaakIndicatiesComponent,
    InformatieObjectIndicatiesComponent,
  ],
})
export class ZoekObjectLinkComponent {
  @Input({ required: true })
  zoekObject!: AbstractRestZoekObject;
  @Input({ required: true }) sideNav!: MatSidenav;
  protected _newtab = false;
  protected indicatiesLayout = IndicatiesLayout;

  @HostListener("document:keydown", ["$event"])
  protected handleKeydown(event: KeyboardEvent) {
    if (event.key === "Control") {
      this._newtab = true;
    }
  }

  @HostListener("document:keyup", ["$event"])
  protected handleKeyup(event: KeyboardEvent) {
    if (event.key === "Control") {
      this._newtab = false;
    }
  }

  protected getLink() {
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

  protected getName() {
    switch (true) {
      case this.isZaak(this.zoekObject):
        return this.zoekObject.identificatie;
      case this.isTaak(this.zoekObject):
        return this.zoekObject.naam;
      case this.isDocument(this.zoekObject):
        return this.zoekObject.titel;
      default:
        throw new Error(
          `Search object type ${this.zoekObject.type} is not supported`,
        );
    }
  }

  protected navigate(event: Event) {
    if (!this._newtab) {
      this.sideNav.close();
    }
    event.stopPropagation();
  }

  protected isZaak(
    zoekObject: AbstractRestZoekObject,
  ): zoekObject is ZaakZoekObject {
    return zoekObject.type === "ZAAK";
  }

  protected isDocument(
    zoekObject: AbstractRestZoekObject,
  ): zoekObject is DocumentZoekObject {
    return zoekObject.type === "DOCUMENT";
  }

  protected isTaak(
    zoekObject: AbstractRestZoekObject,
  ): zoekObject is TaakZoekObject {
    return zoekObject.type === "TAAK";
  }
}
