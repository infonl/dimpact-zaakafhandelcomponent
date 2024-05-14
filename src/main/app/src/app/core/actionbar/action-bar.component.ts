/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  AfterViewInit,
  Component,
  OnDestroy,
  TemplateRef,
  ViewChild,
} from "@angular/core";
import {
  MatBottomSheet,
  MatBottomSheetRef,
} from "@angular/material/bottom-sheet";
import { Router } from "@angular/router";
import { Observable, Subject, takeUntil } from "rxjs";
import { UtilService } from "../service/util.service";
import { ActionBarAction } from "./model/action-bar-action";

@Component({
  selector: "zac-action-bar",
  templateUrl: "./action-bar.component.html",
  styleUrls: ["./action-bar.component.less"],
})
export class ActionBarComponent implements AfterViewInit, OnDestroy {
  @ViewChild(TemplateRef) template: TemplateRef<any>;
  addAction$: Observable<ActionBarAction>;
  disableActionBar$: Observable<boolean>;
  actions: ActionBarAction[] = [];
  actionBarDisabled = false;
  matBottomSheetRef: MatBottomSheetRef;
  destroy$ = new Subject<void>();

  constructor(
    readonly bottomSheet: MatBottomSheet,
    public utilService: UtilService,
    private router: Router,
  ) {}
  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  actionTextClicked(action: ActionBarAction): void {
    action.action.iconClicked.next(this.router.url);
    this.dismissAction(action);
  }

  dismissAction(action: ActionBarAction) {
    const index: number = this.actions.indexOf(action);
    if (index > -1) {
      this.actions.splice(index, 1);
      action.dissmis.next();
    }
    if (this.actions.length === 0) {
      this.matBottomSheetRef.dismiss();
    }
  }

  ngAfterViewInit(): void {
    this.addAction$ = this.utilService.addAction$.asObservable();
    this.addAction$.pipe(takeUntil(this.destroy$)).subscribe((action) => {
      this.actions.push(action);
      if (this.actions.length === 1) {
        this.matBottomSheetRef = this.bottomSheet.open(this.template, {
          closeOnNavigation: false,
          hasBackdrop: false,
        });
      }
    });
    this.disableActionBar$ = this.utilService.disableActionBar$.asObservable();
    this.disableActionBar$.pipe(takeUntil(this.destroy$)).subscribe((state) => {
      this.actionBarDisabled = state;
    });
  }
}
