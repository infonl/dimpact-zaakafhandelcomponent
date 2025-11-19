/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { AfterViewInit, Component, OnInit } from "@angular/core";
import { MatTableDataSource } from "@angular/material/table";
import { UtilService } from "../../core/service/util.service";
import { GeneratedType } from "../../shared/utils/generated-types";
import { SignaleringenSettingsService } from "../signaleringen-settings.service";

@Component({
  templateUrl: "./signaleringen-settings.component.html",
  styleUrls: ["./signaleringen-settings.component.less"],
})
export class SignaleringenSettingsComponent implements OnInit, AfterViewInit {
  isLoadingResults = true;
  columns = ["subjecttype", "type", "dashboard", "mail"] as const;
  dataSource = new MatTableDataSource<
    GeneratedType<"RestSignaleringInstellingen">
  >();

  constructor(
    private service: SignaleringenSettingsService,
    private utilService: UtilService,
  ) {}

  ngOnInit() {
    this.utilService.setTitle("title.signaleringen.settings");
  }

  ngAfterViewInit() {
    this.service.list().subscribe((instellingen) => {
      this.dataSource.data = instellingen;
      this.isLoadingResults = false;
    });
  }

  changed(
    row: GeneratedType<"RestSignaleringInstellingen">,
    column: keyof Pick<
      GeneratedType<"RestSignaleringInstellingen">,
      "dashboard" | "mail"
    >,
    checked: boolean,
  ) {
    this.utilService.setLoading(true);
    row[column] = checked;
    this.service.put(row).subscribe(() => {
      this.utilService.setLoading(false);
    });
  }
}
