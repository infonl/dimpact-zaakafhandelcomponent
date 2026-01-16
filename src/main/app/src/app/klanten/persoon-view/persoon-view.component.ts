/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component } from "@angular/core";
import { ActivatedRoute } from "@angular/router";
import { UtilService } from "../../core/service/util.service";
import { GeneratedType } from "../../shared/utils/generated-types";

@Component({
  templateUrl: "./persoon-view.component.html",
  styleUrls: ["./persoon-view.component.less"],
  standalone: false,
})
export class PersoonViewComponent {
  protected persoon: GeneratedType<"RestPersoon"> | null = null;

  constructor(
    private readonly utilService: UtilService,
    private readonly route: ActivatedRoute,
  ) {
    this.utilService.setTitle("persoonsgegevens");
    this.route.data.subscribe((data) => {
      this.persoon = data.persoon;
    });
  }
}
