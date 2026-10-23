/*
 * SPDX-FileCopyrightText: 2023 Atos, 2023 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, inject, input } from "@angular/core";
import { MatCardModule } from "@angular/material/card";
import { MatChipsModule } from "@angular/material/chips";
import { MatIconModule } from "@angular/material/icon";
import { MatTooltipModule } from "@angular/material/tooltip";
import { TranslatePipe } from "@ngx-translate/core";
import { injectQuery } from "@tanstack/angular-query-experimental";
import { HealthCheckService } from "../../admin/health-check.service";
import { DatumPipe } from "../pipes/datum.pipe";

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
    MatChipsModule,
    MatTooltipModule,
    MatIconModule,
    MatCardModule,
    DatumPipe,
    TranslatePipe,
  ],
})
export class VersionComponent {
  private readonly healthCheckService = inject(HealthCheckService);

  protected readonly versionLayout = VersionLayout;
  protected readonly layout = input<VersionLayout>();
  protected readonly buildInformatieQuery = injectQuery(() =>
    this.healthCheckService.readBuildInformatie(),
  );
}
