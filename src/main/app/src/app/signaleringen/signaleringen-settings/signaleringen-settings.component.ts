/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { NgClass, NgFor, NgIf } from "@angular/common";
import { AfterViewInit, Component, OnInit } from "@angular/core";
import { MatCheckboxModule } from "@angular/material/checkbox";
import { MatTableDataSource, MatTableModule } from "@angular/material/table";
import { TranslateModule } from "@ngx-translate/core";
import { UtilService } from "../../core/service/util.service";
import { GeneratedType } from "../../shared/utils/generated-types";
import { SignaleringenSettingsService } from "../signaleringen-settings.service";

@Component({
  templateUrl: "./signaleringen-settings.component.html",
  styleUrls: ["./signaleringen-settings.component.less"],
  standalone: true,
  imports: [NgClass, NgFor, NgIf, MatTableModule, MatCheckboxModule, TranslateModule],
})
export class SignaleringenSettingsComponent implements OnInit, AfterViewInit {
  protected isLoadingResults = true;
  protected readonly columns = ["subjecttype", "type", "dashboard", "mail"] as const;
  protected dataSource = new MatTableDataSource<
    GeneratedType<"RestSignaleringInstellingen">
  >();

  constructor(
    private readonly service: SignaleringenSettingsService,
    private readonly utilService: UtilService,
  ) {}

  ngOnInit(): void {
    this.utilService.setTitle("title.signaleringen.settings");
  }

  ngAfterViewInit(): void {
    this.service.list().subscribe((instellingen) => {
      this.dataSource.data = instellingen;
      this.isLoadingResults = false;
    });
  }

  protected changed(
    row: GeneratedType<"RestSignaleringInstellingen">,
    column: keyof Pick<
      GeneratedType<"RestSignaleringInstellingen">,
      "dashboard" | "mail"
    >,
    checked: boolean,
  ): void {
    this.utilService.setLoading(true);
    row[column] = checked;
    this.service.put(row).subscribe(() => {
      this.utilService.setLoading(false);
    });
  }
}
