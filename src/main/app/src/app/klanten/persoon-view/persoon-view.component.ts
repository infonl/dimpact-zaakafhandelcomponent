/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, OnInit } from "@angular/core";
import { ActivatedRoute } from "@angular/router";
import { UtilService } from "../../core/service/util.service";
import { GeneratedType } from "../../shared/utils/generated-types";

@Component({
  templateUrl: "./persoon-view.component.html",
  styleUrls: ["./persoon-view.component.less"],
})
export class PersoonViewComponent implements OnInit {
  persoon: GeneratedType<"RestPersoon">;

  constructor(
    private utilService: UtilService,
    private _route: ActivatedRoute,
  ) {}

  ngOnInit(): void {
    this.utilService.setTitle("persoonsgegevens");
    this._route.data.subscribe((data) => {
      this.persoon = data.persoon;
    });
  }
}
