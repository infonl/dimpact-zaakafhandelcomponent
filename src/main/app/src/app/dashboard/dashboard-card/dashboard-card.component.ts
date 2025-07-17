/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  AfterViewInit,
  Component,
  Input,
  OnDestroy,
  OnInit,
  ViewChild,
} from "@angular/core";
import { MatPaginator } from "@angular/material/paginator";
import { MatSort } from "@angular/material/sort";
import { MatTableDataSource } from "@angular/material/table";
import { Observable, Subject, Subscription, interval } from "rxjs";
import { ObjectType } from "../../core/websocket/model/object-type";
import { Opcode } from "../../core/websocket/model/opcode";
import { ScreenEvent } from "../../core/websocket/model/screen-event";
import { WebsocketService } from "../../core/websocket/websocket.service";
import { IdentityService } from "../../identity/identity.service";
import { GeneratedType } from "../../shared/utils/generated-types";
import { DashboardCard } from "../model/dashboard-card";

@Component({
  template: "",
  styleUrls: ["./dashboard-card.component.less"],
})
export abstract class DashboardCardComponent<
    T extends
      GeneratedType<"AbstractRestZoekObjectExtendsAbstractRestZoekObject"> = GeneratedType<"AbstractRestZoekObjectExtendsAbstractRestZoekObject">,
    C extends readonly string[] = readonly string[],
  >
  implements OnInit, AfterViewInit, OnDestroy
{
  private readonly RELOAD_INTERVAL: number = 60; // 1 min.

  @Input({ required: true }) data!: DashboardCard;

  @ViewChild(MatPaginator) paginator?: MatPaginator;
  @ViewChild(MatSort) sort?: MatSort;
  dataSource: MatTableDataSource<T> = new MatTableDataSource<T>();

  protected reload: Observable<unknown> | null = null;
  private reloader?: Subscription;

  abstract readonly columns: C;

  constructor(
    protected identityService: IdentityService,
    protected websocketService: WebsocketService,
  ) {}

  ngOnInit(): void {
    this.onLoad(this.afterLoad);
  }

  ngAfterViewInit(): void {
    if (this.reload == null) {
      if (this.data.signaleringType != null) {
        this.reload = this.refreshOnSignalering(this.data.signaleringType);
      } else {
        this.reload = this.refreshTimed(this.RELOAD_INTERVAL);
      }
    }
    this.reloader = this.reload.subscribe(() => {
      this.onLoad(this.afterLoad);
    });
  }

  ngOnDestroy(): void {
    this.reloader?.unsubscribe();
  }

  protected abstract onLoad(afterLoad: () => void): void;

  private readonly afterLoad = () => {
    if (this.paginator) this.dataSource.paginator = this.paginator;
    if (this.sort) this.dataSource.sort = this.sort;
  };

  protected refreshTimed(seconds: number) {
    return interval(seconds * 1000);
  }

  protected refreshOnSignalering(
    signaleringType: GeneratedType<"RestSignaleringInstellingen">["type"],
  ) {
    const reload$ = new Subject<void>();
    this.identityService.readLoggedInUser().subscribe((medewerker) => {
      this.websocketService.addListener(
        Opcode.UPDATED,
        ObjectType.SIGNALERINGEN,
        medewerker.id,
        (event: ScreenEvent) => {
          if (event.objectId.detail === signaleringType) {
            reload$.next();
          }
        },
      );
    });
    return reload$;
  }
}
