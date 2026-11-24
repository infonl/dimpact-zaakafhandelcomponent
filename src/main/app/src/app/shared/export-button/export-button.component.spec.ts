/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  provideHttpClient,
  withInterceptorsFromDi,
  withXhr,
} from "@angular/common/http";
import {
  HttpTestingController,
  provideHttpClientTesting,
} from "@angular/common/http/testing";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { provideTranslateService } from "@ngx-translate/core";
import { provideTanStackQuery } from "@tanstack/angular-query-experimental";
import { screen, within } from "@testing-library/angular";
import userEvent from "@testing-library/user-event";
import { EMPTY } from "rxjs";
import { fromPartial } from "src/test-helpers";
import { sleep, testQueryClient } from "../../../../setupJest";
import { UtilService } from "../../core/service/util.service";
import { FoutAfhandelingService } from "../../fout-afhandeling/fout-afhandeling.service";
import { GeneratedType } from "../utils/generated-types";
import { ExportButtonComponent } from "./export-button.component";

describe(ExportButtonComponent.name, () => {
  let fixture: ComponentFixture<ExportButtonComponent>;
  let httpTestingController: HttpTestingController;
  let utilServiceMock: Pick<UtilService, "downloadBlobResponse">;
  let foutAfhandelingService: FoutAfhandelingService;
  const blob = new Blob(["data"], { type: "text/csv" });
  const zoekParameters = fromPartial<GeneratedType<"RestZoekParameters">>({
    zoeken: { ALLE: "fakeZoekterm" },
  });

  const exportButton = () =>
    screen.getByRole("button", { name: "actie.export" });

  const expectExportRequest = () =>
    httpTestingController.expectOne({
      method: "POST",
      url: "/rest/csv/export",
    });

  beforeEach(async () => {
    utilServiceMock = { downloadBlobResponse: jest.fn() };

    await TestBed.configureTestingModule({
      imports: [ExportButtonComponent, NoopAnimationsModule],
      providers: [
        provideTranslateService(),
        provideHttpClient(withXhr(), withInterceptorsFromDi()),
        provideHttpClientTesting(),
        provideTanStackQuery(testQueryClient),
        { provide: UtilService, useValue: utilServiceMock },
      ],
    }).compileComponents();

    httpTestingController = TestBed.inject(HttpTestingController);
    foutAfhandelingService = TestBed.inject(FoutAfhandelingService);
    jest.spyOn(foutAfhandelingService, "foutAfhandelen").mockReturnValue(EMPTY);

    fixture = TestBed.createComponent(ExportButtonComponent);
    fixture.componentRef.setInput("zoekParameters", zoekParameters);
    fixture.componentRef.setInput("filename", "fakeFilename");
    fixture.detectChanges();
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it("shows an export button with a download icon", () => {
    expect(
      within(exportButton()).getByText("download_for_offline"),
    ).toBeVisible();
  });

  it("does not export before the button is clicked", () => {
    httpTestingController.expectNone("/rest/csv/export");
    expect(utilServiceMock.downloadBlobResponse).not.toHaveBeenCalled();
  });

  it("exports the zoekParameters as a blob and downloads it under the filename when clicked", async () => {
    const user = userEvent.setup({ delay: null });

    await user.click(exportButton());
    await sleep();

    const request = expectExportRequest();
    expect(request.request.body).toEqual(zoekParameters);
    expect(request.request.responseType).toBe("blob");

    request.flush(blob);
    await sleep();

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
    await sleep();

    const request = expectExportRequest();
    expect(request.request.body).toEqual(expect.objectContaining({ page: 3 }));
    request.flush(blob);
    await sleep();
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
    await sleep();

    const request = expectExportRequest();
    expect(request.request.body).toEqual(replacingZoekParameters);
    request.flush(blob);
    await sleep();

    expect(utilServiceMock.downloadBlobResponse).toHaveBeenCalledWith(
      blob,
      "fakeOtherFilename",
    );
  });

  it("still downloads the export when the button is destroyed while the export is running", async () => {
    const user = userEvent.setup({ delay: null });

    await user.click(exportButton());
    await sleep();
    const request = expectExportRequest();
    fixture.destroy();

    request.flush(blob);
    await sleep();

    expect(utilServiceMock.downloadBlobResponse).toHaveBeenCalledWith(
      blob,
      "fakeFilename",
    );
  });

  it("reports a failed export and downloads nothing", async () => {
    const user = userEvent.setup({ delay: null });

    await user.click(exportButton());
    await sleep();

    expectExportRequest().flush(null, {
      status: 400,
      statusText: "Bad Request",
    });
    await sleep();

    expect(foutAfhandelingService.foutAfhandelen).toHaveBeenCalled();
    expect(utilServiceMock.downloadBlobResponse).not.toHaveBeenCalled();
  });
});
