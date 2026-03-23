/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { ComponentFixture, TestBed } from "@angular/core/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TekstFilterComponent } from "./tekst-filter.component";

describe("TekstFilterComponent", () => {
  let fixture: ComponentFixture<TekstFilterComponent>;
  let component: TekstFilterComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TekstFilterComponent, NoopAnimationsModule],
    }).compileComponents();

    fixture = TestBed.createComponent(TekstFilterComponent);
    component = fixture.componentInstance;
  });

  it("pre-fills the input with the given value", () => {
    component.value = "hello";
    fixture.detectChanges();

    const input = fixture.nativeElement.querySelector("input") as HTMLInputElement;
    expect(input.value).toBe("hello");
  });

  it("emits the new value when the input loses focus after a change", () => {
    component.value = "old";
    fixture.detectChanges();

    const emitted: (string | undefined)[] = [];
    component.changed.subscribe((v) => emitted.push(v));

    component["formControl"].setValue("new");
    fixture.nativeElement.querySelector("input").dispatchEvent(new Event("blur"));

    expect(emitted).toEqual(["new"]);
  });

  it("emits the new value when Enter is pressed after a change", () => {
    component.value = "old";
    fixture.detectChanges();

    const emitted: (string | undefined)[] = [];
    component.changed.subscribe((v) => emitted.push(v));

    component["formControl"].setValue("new");
    fixture.nativeElement
      .querySelector("input")
      .dispatchEvent(new KeyboardEvent("keyup", { key: "Enter" }));

    expect(emitted).toEqual(["new"]);
  });

  it("emits the new value when the search icon is clicked after a change", () => {
    component.value = "old";
    fixture.detectChanges();

    const emitted: (string | undefined)[] = [];
    component.changed.subscribe((v) => emitted.push(v));

    component["formControl"].setValue("new");
    fixture.nativeElement.querySelector("mat-icon").click();

    expect(emitted).toEqual(["new"]);
  });

  it("does not emit when the value has not changed", () => {
    component.value = "same";
    fixture.detectChanges();

    const emitted: (string | undefined)[] = [];
    component.changed.subscribe((v) => emitted.push(v));

    fixture.nativeElement.querySelector("input").dispatchEvent(new Event("blur"));

    expect(emitted).toEqual([]);
  });
});
