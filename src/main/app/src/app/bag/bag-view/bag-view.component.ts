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
  bagObject!: GeneratedType<"RESTBAGObject">;
  adres?: GeneratedType<"RESTBAGAdres">;
  openbareRuimte?: GeneratedType<"RESTOpenbareRuimte">;
  woonplaats?: GeneratedType<"RESTWoonplaats">;
  pand?: GeneratedType<"RESTPand">;
  nummeraanduiding?: GeneratedType<"RESTNummeraanduiding">;
  geometrie?: GeneratedType<"RestGeometry">;

  constructor(
    private utilService: UtilService,
    private _route: ActivatedRoute,
  ) {}

  ngOnInit(): void {
    this.utilService.setTitle("bagobjectgegevens");
    this._route.data.subscribe((data) => {
      this.bagObject = data.bagObject;
      switch (this.bagObject.bagObjectType) {
        case "ADRES":
          this.adres = this.bagObject;
          this.geometrie = this.adres.geometry;
          break;
        case "ADRESSEERBAAR_OBJECT":
          break; // (Nog) geen zelfstandige entiteit
        case "WOONPLAATS":
          this.woonplaats = this.bagObject;
          break;
        case "PAND":
          this.pand = this.bagObject;
          this.geometrie = this.pand.geometry;
          break;
        case "OPENBARE_RUIMTE":
          this.openbareRuimte = this.bagObject;
          break;
        case "NUMMERAANDUIDING":
          this.nummeraanduiding = this.bagObject;
          break;
      }
    });
  }
}
