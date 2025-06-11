/*
 * SPDX-FileCopyrightText: 2023 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, OnInit } from "@angular/core";
import { ActivatedRoute } from "@angular/router";
import { UtilService } from "../../core/service/util.service";
import { GeneratedType } from "../../shared/utils/generated-types";

@Component({
  templateUrl: "./bag-view.component.html",
  styleUrls: ["./bag-view.component.less"],
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

  ngOnInit(): void {
    this.utilService.setTitle("bagobjectgegevens");
    this.activatedRoute.data.subscribe((data) => {
      const bagObject: GeneratedType<"RESTBAGObject"> = data.bagObject;
      this.bagIdentificatie = bagObject.identificatie!;
      switch (bagObject.bagObjectType) {
        case "ADRES":
          this.adres = bagObject;
          this.geometrie = this.adres.geometry;
          break;
        case "ADRESSEERBAAR_OBJECT":
          break; // (Nog) geen zelfstandige entiteit
        case "WOONPLAATS":
          this.woonplaats = bagObject;
          break;
        case "PAND":
          this.pand = bagObject;
          this.geometrie = this.pand.geometry;
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
