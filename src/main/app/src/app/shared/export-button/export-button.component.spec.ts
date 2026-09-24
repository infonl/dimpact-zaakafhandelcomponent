/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { ComponentFixture, TestBed } from "@angular/core/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule } from "@ngx-translate/core";
import { screen, within } from "@testing-library/angular";
import userEvent from "@testing-library/user-event";
import { of } from "rxjs";
import { fromPartial } from "src/test-helpers";
import { UtilService } from "../../core/service/util.service";
import { CsvService } from "../../csv/csv.service";
import { GeneratedType } from "../utils/generated-types";
import { ExportButtonComponent } from "./export-button.component";

describe(ExportButtonComponent.name, () => {
  let fixture: ComponentFixture<ExportButtonComponent>;
  let csvServiceMock: Pick<CsvService, "exportToCSV">;
  let utilServiceMock: Pick<UtilService, "downloadBlobResponse">;
  const blob = new Blob(["data"], { type: "text/csv" });
  const zoekParameters = fromPartial<GeneratedType<"RestZoekParameters">>({
    zoeken: { ALLE: "fakeZoekterm" },
  });

  const exportButton = () =>
    screen.getByRole("button", { name: "actie.export" });

  beforeEach(async () => {
    csvServiceMock = { exportToCSV: jest.fn().mockReturnValue(of(blob)) };
    utilServiceMock = { downloadBlobResponse: jest.fn() };

    await TestBed.configureTestingModule({
      imports: [
        ExportButtonComponent,
        NoopAnimationsModule,
        TranslateModule.forRoot(),
      ],
      providers: [
        { provide: CsvService, useValue: csvServiceMock },
        { provide: UtilService, useValue: utilServiceMock },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ExportButtonComponent);
    fixture.componentRef.setInput("zoekParameters", zoekParameters);
    fixture.componentRef.setInput("filename", "fakeFilename");
    fixture.detectChanges();
  });

  it("shows an export button with a download icon", () => {
    expect(
      within(exportButton()).getByText("download_for_offline"),
    ).toBeVisible();
  });

  it("does not export before the button is clicked", () => {
    expect(csvServiceMock.exportToCSV).not.toHaveBeenCalled();
    expect(utilServiceMock.downloadBlobResponse).not.toHaveBeenCalled();
  });

  it("exports the zoekParameters and downloads the result under the filename when clicked", async () => {
    const user = userEvent.setup({ delay: null });

    await user.click(exportButton());

    expect(csvServiceMock.exportToCSV).toHaveBeenCalledWith(zoekParameters);
    expect(utilServiceMock.downloadBlobResponse).toHaveBeenCalledWith(
      blob,
      "fakeFilename",
    );
  });

  it("exports the zoekParameters with the changes the parent made to them in place since binding them", async () => {
    const user = userEvent.setup({ delay: null });
    const parentZoekParameters = fromPartial<
      GeneratedType<"RestZoekParameters">
    >({ page: 0 });
    fixture.componentRef.setInput("zoekParameters", parentZoekParameters);
    fixture.detectChanges();

    parentZoekParameters.page = 3;
    await user.click(exportButton());

    expect(csvServiceMock.exportToCSV).toHaveBeenCalledWith(
      expect.objectContaining({ page: 3 }),
    );
  });

  it("exports with the zoekParameters and filename that replaced the previous ones", async () => {
    const user = userEvent.setup({ delay: null });
    const replacingZoekParameters = fromPartial<
      GeneratedType<"RestZoekParameters">
    >({ zoeken: { ALLE: "fakeOtherZoekterm" } });

    fixture.componentRef.setInput("zoekParameters", replacingZoekParameters);
    fixture.componentRef.setInput("filename", "fakeOtherFilename");
    fixture.detectChanges();
    await user.click(exportButton());

    expect(csvServiceMock.exportToCSV).toHaveBeenCalledWith(
      replacingZoekParameters,
    );
    expect(utilServiceMock.downloadBlobResponse).toHaveBeenCalledWith(
      blob,
      "fakeOtherFilename",
    );
  });
});
