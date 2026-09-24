/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { NgIf } from "@angular/common";
import { Component, HostListener, input } from "@angular/core";
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
  readonly zoekObject =
    input.required<
      GeneratedType<"AbstractRestZoekObjectExtendsAbstractRestZoekObject">
    >();
  readonly sideNav = input.required<MatSidenav>();
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
    const zoekObject = this.zoekObject();
    switch (zoekObject.type) {
      case "ZAAK":
        return ["/zaken/", (zoekObject as ZaakZoekObject).identificatie];
      case "TAAK":
        return ["/taken/", zoekObject.id];
      case "DOCUMENT":
        return ["/informatie-objecten/", zoekObject.id];
      default:
        throw new Error(
          `Search object type ${zoekObject.type} is not supported`,
        );
    }
  }

  protected getName() {
    const zoekObject = this.zoekObject();
    switch (zoekObject.type) {
      case "ZAAK":
        return (zoekObject as ZaakZoekObject).identificatie;
      case "TAAK":
        return (zoekObject as TaakZoekObject).naam;
      case "DOCUMENT":
        return (zoekObject as DocumentZoekObject).titel;
      default:
        throw new Error(
          `Search object type ${zoekObject.type} is not supported`,
        );
    }
  }

  protected navigate(event: Event) {
    if (!this._newtab) {
      this.sideNav().close();
    }
    event.stopPropagation();
  }
}
