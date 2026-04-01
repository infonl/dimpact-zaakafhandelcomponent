/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { provideHttpClient } from "@angular/common/http";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { By } from "@angular/platform-browser";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { provideRouter } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { of, Subject } from "rxjs";
import { UtilService } from "../../core/service/util.service";
import { GeneratedType } from "../../shared/utils/generated-types";
import { SignaleringenSettingsService } from "../signaleringen-settings.service";
import { SignaleringenSettingsComponent } from "./signaleringen-settings.component";

const makeInstelling = (
  fields: Partial<GeneratedType<"RestSignaleringInstellingen">> = {},
): GeneratedType<"RestSignaleringInstellingen"> =>
  ({
    id: 1,
    type: "ZAAK_OP_NAAM",
    subjecttype: "ZAAK",
    dashboard: false,
    mail: false,
    ...fields,
  }) as Partial<
    GeneratedType<"RestSignaleringInstellingen">
  > as unknown as GeneratedType<"RestSignaleringInstellingen">;

describe(SignaleringenSettingsComponent.name, () => {
  let fixture: ComponentFixture<SignaleringenSettingsComponent>;
  let component: SignaleringenSettingsComponent;
  let utilService: UtilService;
  let signaleringenService: SignaleringenSettingsService;
  let listSubject: Subject<GeneratedType<"RestSignaleringInstellingen">[]>;

  beforeEach(async () => {
    listSubject = new Subject();

    await TestBed.configureTestingModule({
      imports: [
        SignaleringenSettingsComponent,
        NoopAnimationsModule,
        TranslateModule.forRoot(),
      ],
      providers: [provideHttpClient(), provideRouter([])],
    }).compileComponents();

    utilService = TestBed.inject(UtilService);
    signaleringenService = TestBed.inject(SignaleringenSettingsService);

    jest.spyOn(utilService, "setTitle").mockImplementation(() => {});
    jest.spyOn(utilService, "setLoading").mockImplementation(() => {});
    jest
      .spyOn(signaleringenService, "list")
      .mockReturnValue(listSubject.asObservable() as never);
    jest
      .spyOn(signaleringenService, "put")
      .mockReturnValue(of(makeInstelling()) as never);

    fixture = TestBed.createComponent(SignaleringenSettingsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it("calls setTitle with signaleringen settings key on init", () => {
    expect(utilService.setTitle).toHaveBeenCalledWith(
      "title.signaleringen.settings",
    );
  });

  it("calls service.list on init", () => {
    expect(signaleringenService.list).toHaveBeenCalled();
  });

  it("shows loading shade while data is loading", () => {
    const wrapper = fixture.debugElement.query(By.css(".table-wrapper"))
      .nativeElement as HTMLElement;
    expect(wrapper.classList).toContain("table-loading-shade");
  });

  it("removes loading shade after data arrives", () => {
    listSubject.next([]);
    listSubject.complete();
    fixture.detectChanges();

    const wrapper = fixture.debugElement.query(By.css(".table-wrapper"))
      .nativeElement as HTMLElement;
    expect(wrapper.classList).not.toContain("table-loading-shade");
  });

  it("populates dataSource after service emits", () => {
    const instellingen = [makeInstelling()];
    listSubject.next(instellingen);
    listSubject.complete();
    fixture.detectChanges();

    expect(component["dataSource"].data).toEqual(instellingen);
  });

  it("renders the four column headers", () => {
    listSubject.next([makeInstelling()]);
    listSubject.complete();
    fixture.detectChanges();

    const headers = fixture.debugElement
      .queryAll(By.css("th"))
      .map((el) => (el.nativeElement as HTMLElement).textContent?.trim());
    expect(headers).toEqual(
      expect.arrayContaining([
        "signalering.subjecttype",
        "signalering.type",
        "signalering.dashboard",
        "signalering.mail",
      ]),
    );
  });

  it("renders translated text for subjecttype and type columns", () => {
    listSubject.next([
      makeInstelling({ type: "ZAAK_OP_NAAM", subjecttype: "ZAAK" }),
    ]);
    listSubject.complete();
    fixture.detectChanges();

    const cells = fixture.debugElement
      .queryAll(By.css("td"))
      .map((el) => (el.nativeElement as HTMLElement).textContent?.trim())
      .filter((t) => !!t);

    expect(cells).toEqual(
      expect.arrayContaining([
        "signalering.subjecttype.ZAAK",
        "signalering.type.ZAAK_OP_NAAM",
      ]),
    );
  });

  it("renders a checkbox for the dashboard column", () => {
    listSubject.next([makeInstelling({ dashboard: true })]);
    listSubject.complete();
    fixture.detectChanges();

    const checkbox = fixture.debugElement.query(
      By.css("[id='ZAAK_OP_NAAM_dashboard_checkbox']"),
    );
    expect(checkbox).not.toBeNull();
  });

  it("renders a checkbox for the mail column", () => {
    listSubject.next([makeInstelling({ mail: true })]);
    listSubject.complete();
    fixture.detectChanges();

    const checkbox = fixture.debugElement.query(
      By.css("[id='ZAAK_OP_NAAM_mail_checkbox']"),
    );
    expect(checkbox).not.toBeNull();
  });

  it("does not render a checkbox when column value is null", () => {
    listSubject.next([makeInstelling({ dashboard: null })]);
    listSubject.complete();
    fixture.detectChanges();

    const checkbox = fixture.debugElement.query(
      By.css("[id='ZAAK_OP_NAAM_dashboard_checkbox']"),
    );
    expect(checkbox).toBeNull();
  });

  it("calls changed with correct args when checkbox changes", () => {
    const row = makeInstelling({ dashboard: false });
    listSubject.next([row]);
    listSubject.complete();
    fixture.detectChanges();

    jest.spyOn(component as SignaleringenSettingsComponent, "changed" as never);

    const checkboxInput = fixture.debugElement.query(
      By.css("[id='ZAAK_OP_NAAM_dashboard_checkbox'] input"),
    ).nativeElement as HTMLInputElement;
    checkboxInput.click();
    fixture.detectChanges();

    expect(component["changed"]).toHaveBeenCalled();
  });

  it("calls setLoading(true) and put() when changed is invoked", () => {
    const row = makeInstelling({ dashboard: false });
    component["changed"](row, "dashboard", true);

    expect(utilService.setLoading).toHaveBeenCalledWith(true);
    expect(signaleringenService.put).toHaveBeenCalledWith(row);
  });

  it("calls setLoading(false) after put completes", () => {
    const row = makeInstelling({ dashboard: false });
    component["changed"](row, "dashboard", true);

    expect(utilService.setLoading).toHaveBeenCalledWith(false);
  });

  it("mutates row column value when changed is called", () => {
    const row = makeInstelling({ dashboard: false });
    component["changed"](row, "dashboard", true);

    expect((row as Record<string, unknown>)["dashboard"]).toBe(true);
  });

  it("shows no-data message when dataSource is empty", () => {
    listSubject.next([]);
    listSubject.complete();
    fixture.detectChanges();

    expect(component["dataSource"].data).toHaveLength(0);
    expect(signaleringenService.list).toHaveBeenCalled();
  });

  it("does not render checkboxes for subjecttype and type columns", () => {
    listSubject.next([
      makeInstelling({
        type: "ZAAK_OP_NAAM",
        subjecttype: "ZAAK",
        dashboard: true,
        mail: true,
      }),
    ]);
    listSubject.complete();
    fixture.detectChanges();

    const checkboxes = fixture.debugElement.queryAll(By.css("mat-checkbox"));
    expect(checkboxes).toHaveLength(2);
  });
});
