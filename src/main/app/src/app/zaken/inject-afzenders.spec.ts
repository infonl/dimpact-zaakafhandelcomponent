/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component } from "@angular/core";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { FormControl } from "@angular/forms";
import { provideTranslateService } from "@ngx-translate/core";
import { fromPartial } from "src/test-helpers";
import { sleep, testQueryClient } from "../../../setupJest";
import { GeneratedType } from "../shared/utils/generated-types";
import { injectAfzenders } from "./inject-afzenders";

const afzendersKey = [
  "/rest/zaken/zaak/{uuid}/afzender",
  { path: { uuid: "fakeZaakUuid" } },
];

const defaultAfzender = fromPartial<GeneratedType<"RestZaakAfzender">>({
  mail: "fakeDefault@example.com",
  isDefaultMail: true,
});
const andereAfzender = fromPartial<GeneratedType<"RestZaakAfzender">>({
  mail: "fakeAndere@example.com",
  isDefaultMail: false,
});

@Component({ template: "" })
class TestHostComponent {
  readonly verzender =
    new FormControl<GeneratedType<"RestZaakAfzender"> | null>(null);
  readonly afzendersQuery = injectAfzenders(
    () => "fakeZaakUuid",
    () => this.verzender,
  );
}

describe(injectAfzenders.name, () => {
  let fixture: ComponentFixture<TestHostComponent>;

  async function settle() {
    for (let cycle = 0; cycle < 3; cycle++) {
      await sleep();
      fixture.detectChanges();
    }
  }

  async function setup(afzenders: GeneratedType<"RestZaakAfzender">[]) {
    TestBed.configureTestingModule({
      imports: [TestHostComponent],
      providers: [provideTranslateService()],
    });
    testQueryClient.setQueryData(afzendersKey, afzenders);
    fixture = TestBed.createComponent(TestHostComponent);
    await settle();
    return fixture.componentInstance;
  }

  async function arriveReRead(afzenders: GeneratedType<"RestZaakAfzender">[]) {
    testQueryClient.setQueryData(afzendersKey, afzenders);
    await settle();
  }

  it("selects the default from the afzenders it offers, so that the verzender is always one of the options", async () => {
    const host = await setup([andereAfzender, defaultAfzender]);

    expect(host.afzendersQuery.data()).toEqual([
      andereAfzender,
      defaultAfzender,
    ]);
    expect(host.verzender.value).toBe(host.afzendersQuery.data()?.[1]);
  });

  it("selects nothing when no afzender is the default", async () => {
    const host = await setup([andereAfzender]);

    expect(host.verzender.value).toBeNull();
  });

  it("follows a re-read of the afzenders while the user has not chosen a verzender", async () => {
    const host = await setup([defaultAfzender, andereAfzender]);

    await arriveReRead([
      { ...defaultAfzender, isDefaultMail: false },
      { ...andereAfzender, isDefaultMail: true },
    ]);

    expect(host.verzender.value?.mail).toBe(andereAfzender.mail);
  });

  it("keeps the verzender the user chose when a re-read of the afzenders arrives", async () => {
    const host = await setup([defaultAfzender, andereAfzender]);
    host.verzender.setValue(andereAfzender);
    host.verzender.markAsDirty();

    await arriveReRead([
      fromPartial<GeneratedType<"RestZaakAfzender">>({
        mail: "fakeNieuweDefault@example.com",
        isDefaultMail: true,
      }),
      andereAfzender,
    ]);

    expect(host.verzender.value).toBe(andereAfzender);
  });
});
