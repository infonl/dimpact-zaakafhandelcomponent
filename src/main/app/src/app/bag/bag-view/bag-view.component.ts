/*
 * SPDX-FileCopyrightText: 2023 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { NgIf } from "@angular/common";
import { Component, OnInit } from "@angular/core";
import { MatCardModule } from "@angular/material/card";
import { MatSidenavModule } from "@angular/material/sidenav";
import { ActivatedRoute } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { UtilService } from "../../core/service/util.service";
import { StaticTextComponent } from "../../shared/static-text/static-text.component";
import { GeneratedType } from "../../shared/utils/generated-types";
import { BagLocatieComponent } from "../bag-locatie/bag-locatie.component";
import { BagZakenTabelComponent } from "../bag-zaken-tabel/bag-zaken-tabel.component";

@Component({
  templateUrl: "./bag-view.component.html",
  styleUrls: ["./bag-view.component.less"],
  standalone: true,
  imports: [
    NgIf,
    MatCardModule,
    MatSidenavModule,
    TranslateModule,
    StaticTextComponent,
    BagZakenTabelComponent,
    BagLocatieComponent,
  ],
})
export class BAGViewComponent implements OnInit {
  protected bagIdentificatie!: string;
  protected adres?: GeneratedType<"RESTBAGAdres">;
  protected openbareRuimte?: GeneratedType<"RESTOpenbareRuimte">;
  protected woonplaats?: GeneratedType<"RESTWoonplaats">;
  protected pand?: GeneratedType<"RESTPand">;
  protected nummeraanduiding?: GeneratedType<"RESTNummeraanduiding">;
  protected geometrie?: GeneratedType<"RestGeometry">;

  constructor(
    private readonly utilService: UtilService,
    private readonly activatedRoute: ActivatedRoute,
  ) {}

  ngOnInit() {
    this.utilService.setTitle("bagobjectgegevens");
    this.activatedRoute.data.subscribe((data) => {
      const bagObject: GeneratedType<"RESTBAGObject"> = data.bagObject;
      this.bagIdentificatie = bagObject.identificatie!;
      switch (bagObject.bagObjectType) {
        case "ADRES":
          this.adres = bagObject;
          this.geometrie = this.adres.geometry ?? undefined;
          break;
        case "ADRESSEERBAAR_OBJECT":
          break; // (Nog) geen zelfstandige entiteit
        case "WOONPLAATS":
          this.woonplaats = bagObject;
          break;
        case "PAND":
          this.pand = bagObject;
          this.geometrie = this.pand.geometry ?? undefined;
          break;
        case "OPENBARE_RUIMTE":
          this.openbareRuimte = bagObject;
          break;
        case "NUMMERAANDUIDING":
          this.nummeraanduiding = bagObject;
          break;
      }
    });
  }
}
