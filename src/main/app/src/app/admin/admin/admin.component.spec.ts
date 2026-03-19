/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component } from "@angular/core";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { MatSidenav, MatSidenavContainer } from "@angular/material/sidenav";
import { Subject } from "rxjs";
import { ConfiguratieService } from "../../configuratie/configuratie.service";
import { UtilService } from "../../core/service/util.service";
import { HeaderMenuItem } from "../../shared/side-nav/menu-item/header-menu-item";
import { LinkMenuItem } from "../../shared/side-nav/menu-item/link-menu-item";
import { MenuItem } from "../../shared/side-nav/menu-item/menu-item";
import { AdminComponent } from "./admin.component";

@Component({ template: "", standalone: true })
class TestAdminComponent extends AdminComponent {
  private readonly openedStart = new Subject<void>();

  readonly sideNavContainer = {
    hasBackdrop: true,
    updateContentMargins: jest.fn(),
  } as unknown as MatSidenavContainer;

  readonly menuSidenav = {
    openedStart: this.openedStart.asObservable(),
  } as unknown as MatSidenav;

  public constructor(
    utilService: UtilService,
    configuratieService: ConfiguratieService,
  ) {
    super(utilService, configuratieService);
  }

  callSetupMenu(title: string, params?: Record<string, unknown>): void {
    this.setupMenu(title, params);
  }

  get testMenu(): MenuItem[] {
    return this.menu;
  }

  get testActiveMenu(): string {
    return this.activeMenu;
  }
}

describe(AdminComponent.name, () => {
  let fixture: ComponentFixture<TestAdminComponent>;
  let component: TestAdminComponent;
  let utilServiceMock: Pick<UtilService, "setTitle">;

  beforeEach(async () => {
    utilServiceMock = { setTitle: jest.fn() };

    await TestBed.configureTestingModule({
      imports: [TestAdminComponent],
      providers: [
        { provide: UtilService, useValue: utilServiceMock },
        {
          provide: ConfiguratieService,
          useValue: {} satisfies Partial<ConfiguratieService>,
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(TestAdminComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it("should call setTitle with the provided title on setupMenu", () => {
    component.callSetupMenu("title.mailtemplates");
    expect(utilServiceMock.setTitle).toHaveBeenCalledWith(
      "title.mailtemplates",
      undefined,
    );
  });

  it("should set activeMenu to the provided title on setupMenu", () => {
    component.callSetupMenu("title.parameters");
    expect(component.testActiveMenu).toBe("title.parameters");
  });

  it("should populate menu with a header item followed by link items", () => {
    component.callSetupMenu("title.mailtemplates");
    expect(component.testMenu[0]).toBeInstanceOf(HeaderMenuItem);
    expect(
      component.testMenu.slice(1).every((i) => i instanceof LinkMenuItem),
    ).toBe(true);
  });

  it("should mark the active menu link as activated", () => {
    component.callSetupMenu("title.mailtemplates");
    const activeLinks = component.testMenu.filter(
      (i) => i instanceof LinkMenuItem && (i as LinkMenuItem).activated,
    );
    expect(activeLinks).toHaveLength(1);
    expect((activeLinks[0] as LinkMenuItem).title).toBe("title.mailtemplates");
  });
});
