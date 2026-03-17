/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { ComponentFixture, TestBed } from "@angular/core/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule } from "@ngx-translate/core";
import { of } from "rxjs";
import { UtilService } from "../../core/service/util.service";
import { CsvService } from "../../csv/csv.service";
import { GeneratedType } from "../utils/generated-types";
import { ExportButtonComponent } from "./export-button.component";

describe(ExportButtonComponent.name, () => {
  let fixture: ComponentFixture<ExportButtonComponent>;
  let csvServiceMock: Pick<CsvService, "exportToCSV">;
  let utilServiceMock: Pick<UtilService, "downloadBlobResponse">;

  beforeEach(async () => {
    csvServiceMock = { exportToCSV: jest.fn() };
    utilServiceMock = { downloadBlobResponse: jest.fn() };

    await TestBed.configureTestingModule({
      imports: [ExportButtonComponent, NoopAnimationsModule, TranslateModule.forRoot()],
      providers: [
        { provide: CsvService, useValue: csvServiceMock },
        { provide: UtilService, useValue: utilServiceMock },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ExportButtonComponent);
  });

  it("should render export button with download icon", () => {
    fixture.componentRef.setInput("zoekParameters", {});
    fixture.componentRef.setInput("filename", "export.csv");
    fixture.detectChanges();

    const button = fixture.nativeElement.querySelector("#export_button");
    const icon = fixture.nativeElement.querySelector("mat-icon");

    expect(button).toBeTruthy();
    expect(icon.textContent.trim()).toBe("download_for_offline");
  });

  it("should call csvService and download blob on click", () => {
    const zoekParameters =
      {} as GeneratedType<"RestZoekParameters">;
    const blob = new Blob(["data"], { type: "text/csv" });
    (csvServiceMock.exportToCSV as jest.Mock).mockReturnValue(of(blob));

    fixture.componentRef.setInput("zoekParameters", zoekParameters);
    fixture.componentRef.setInput("filename", "results.csv");
    fixture.detectChanges();

    fixture.nativeElement.querySelector("#export_button").click();

    expect(csvServiceMock.exportToCSV).toHaveBeenCalledWith(zoekParameters);
    expect(utilServiceMock.downloadBlobResponse).toHaveBeenCalledWith(
      blob,
      "results.csv",
    );
  });
});
