/*
 * SPDX-FileCopyrightText: 2023 Atos, 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, Input, OnInit } from "@angular/core";
import { HealthCheckService } from "../../admin/health-check.service";
import { BuildInformatie } from "../../admin/model/build-informatie";

export enum VersionLayout {
  VERBOSE = "VERBOSE",
  NORMAL = "NORMAL",
}

@Component({
  selector: "zac-version",
  templateUrl: "./version.component.html",
  styleUrls: ["./version.component.less"],
})
export class VersionComponent implements OnInit {
  versionLayout = VersionLayout;
  @Input() layout: VersionLayout;
  buildInformatie: BuildInformatie = new BuildInformatie();

  constructor(private healtCheckService: HealthCheckService) {}

  ngOnInit(): void {
    this.healtCheckService
      .readBuildInformatie()
      .subscribe((buildInformatie) => {
        this.buildInformatie = buildInformatie;
      });
  }
}
