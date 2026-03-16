/*
 * SPDX-FileCopyrightText: 2023 Atos, 2023 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { NgIf } from "@angular/common";
import { Component, Input, OnInit } from "@angular/core";
import { MatCardModule } from "@angular/material/card";
import { MatChipsModule } from "@angular/material/chips";
import { MatIconModule } from "@angular/material/icon";
import { MatTooltipModule } from "@angular/material/tooltip";
import { TranslateModule } from "@ngx-translate/core";
import { HealthCheckService } from "../../admin/health-check.service";
import { DatumPipe } from "../pipes/datum.pipe";
import { GeneratedType } from "../utils/generated-types";

export enum VersionLayout {
  VERBOSE = "VERBOSE",
  NORMAL = "NORMAL",
}

@Component({
  selector: "zac-version",
  templateUrl: "./version.component.html",
  styleUrls: ["./version.component.less"],
  standalone: true,
  imports: [
    NgIf,
    MatChipsModule,
    MatTooltipModule,
    MatIconModule,
    MatCardModule,
    DatumPipe,
    TranslateModule,
  ],
})
export class VersionComponent implements OnInit {
  protected readonly versionLayout = VersionLayout;
  @Input() protected layout?: VersionLayout;
  protected buildInformatie?: GeneratedType<"RESTBuildInformation">;

  constructor(private readonly healtCheckService: HealthCheckService) {}

  ngOnInit() {
    this.healtCheckService
      .readBuildInformatie()
      .subscribe((buildInformatie) => {
        this.buildInformatie = buildInformatie;
      });
  }
}
