/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { NgIf } from "@angular/common";
import {
  AfterViewInit,
  Component,
  DestroyRef,
  effect,
  inject,
  input,
  OnDestroy,
  OnInit,
  ViewChild,
} from "@angular/core";
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";
import { FormControl, ReactiveFormsModule } from "@angular/forms";
import { MatIconAnchor, MatIconButton } from "@angular/material/button";
import {
  MatCard,
  MatCardContent,
  MatCardHeader,
  MatCardTitle,
} from "@angular/material/card";
import { MatChip } from "@angular/material/chips";
import { MatIcon } from "@angular/material/icon";
import { MatSlideToggle } from "@angular/material/slide-toggle";
import { MatSort, MatSortHeader, MatSortModule } from "@angular/material/sort";
import { MatTableDataSource, MatTableModule } from "@angular/material/table";
import { RouterLink } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { injectQuery, QueryClient } from "@tanstack/angular-query-experimental";
import moment from "moment";
import { finalize } from "rxjs";
import { DateConditionals } from "src/app/shared/utils/date-conditionals";
import { UtilService } from "../../core/service/util.service";
import { ObjectType } from "../../core/websocket/model/object-type";
import { Opcode } from "../../core/websocket/model/opcode";
import { WebsocketListener } from "../../core/websocket/model/websocket-listener";
import { WebsocketService } from "../../core/websocket/websocket.service";
import { IdentityService } from "../../identity/identity.service";
import { detailExpand } from "../../shared/animations/animations";
import { ExpandableTableData } from "../../shared/dynamic-table/model/expandable-table-data";
import { DatumPipe } from "../../shared/pipes/datum.pipe";
import { EmptyPipe } from "../../shared/pipes/empty.pipe";
import { StaticTextComponent } from "../../shared/static-text/static-text.component";
import { SessionStorageUtil } from "../../shared/storage/session-storage.util";
import { GeneratedType } from "../../shared/utils/generated-types";
import { TakenService } from "../../taken/taken.service";

@Component({
  selector: "zac-zaak-taken",
  templateUrl: "./zaak-taken.component.html",
  styleUrls: ["./zaak-taken.component.less"],
  animations: [detailExpand],
  standalone: true,
  imports: [
    NgIf,
    MatCard,
    MatCardHeader,
    MatCardTitle,
    MatCardContent,
    MatChip,
    MatIcon,
    MatIconAnchor,
    MatIconButton,
    MatSlideToggle,
    MatSort,
    MatSortHeader,
    MatSortModule,
    MatTableModule,
    ReactiveFormsModule,
    RouterLink,
    TranslateModule,
    DatumPipe,
    EmptyPipe,
    StaticTextComponent,
  ],
})
export class ZaakTakenComponent implements OnInit, AfterViewInit, OnDestroy {
  private readonly takenService = inject(TakenService);
  private readonly websocketService = inject(WebsocketService);
  private readonly utilService = inject(UtilService);
  private readonly identityService = inject(IdentityService);
  private readonly queryClient = inject(QueryClient);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly loggedInUser = injectQuery(() =>
    this.identityService.readLoggedInUser(),
  );

  readonly zaak = input.required<GeneratedType<"RestZaak">>();

  protected readonly takenQuery = injectQuery(() =>
    this.takenService.listTakenVoorZaakQuery(this.zaak().uuid),
  );

  protected takenDataSource = new MatTableDataSource<
    ExpandableTableData<GeneratedType<"RestTask">>
  >();
  protected allTakenExpanded = false;
  protected toonAfgerondeTaken = new FormControl(false);
  protected takenStatusFilter: GeneratedType<"TaakStatus"> | "" = "";
  protected readonly takenColumnsToDisplay = [
    "naam",
    "status",
    "creatiedatumTijd",
    "fataledatum",
    "groep",
    "behandelaar",
    "id",
  ] as const;

  @ViewChild("takenSort") private takenSort!: MatSort;

  private zaakTakenListener!: WebsocketListener;

  constructor() {
    this.toonAfgerondeTaken.setValue(
      Boolean(SessionStorageUtil.getItem("toonAfgerondeTaken")),
      { emitEvent: false },
    );

    effect(() => {
      const data = this.takenQuery.data();
      if (!data) return;
      const taken = [...data]
        .map((value) => new ExpandableTableData(value))
        .sort(
          (a, b) =>
            (a.data.fataledatum?.localeCompare(b.data.fataledatum ?? "") ||
              a.data.creatiedatumTijd?.localeCompare(
                b.data.creatiedatumTijd ?? "",
              )) ??
            0,
        );
      this.takenDataSource.data = taken;
      this.filterTakenOpStatus();
    });
  }

  ngOnInit() {
    const zaak = this.zaak();

    this.zaakTakenListener = this.websocketService.addListener(
      Opcode.UPDATED,
      ObjectType.ZAAK_TAKEN,
      zaak.uuid,
      () => this.invalidate(),
    );

    this.takenQuery.refetch();

    this.takenDataSource.filterPredicate = (data, filter) => {
      if (!filter) return true;
      return !this.toonAfgerondeTaken.value
        ? data.data.status !== filter
        : true;
    };
  }

  ngAfterViewInit() {
    this.takenDataSource.sortingDataAccessor = (item, property) => {
      switch (property) {
        case "groep":
          return item.data.groep?.naam ?? "";
        case "behandelaar":
          return item.data.behandelaar?.naam ?? "";
        default:
          return String(item.data[property as keyof typeof item.data]);
      }
    };
    this.takenDataSource.sort = this.takenSort;
  }

  ngOnDestroy() {
    this.websocketService.removeListener(this.zaakTakenListener);
  }

  reload() {
    this.invalidate();
  }

  private invalidate() {
    this.queryClient.invalidateQueries({
      queryKey: this.takenService.listTakenVoorZaakQuery(this.zaak().uuid)
        .queryKey,
    });
  }

  protected expandTaken(expand: boolean) {
    this.takenDataSource.data.forEach((value) => (value.expanded = expand));
    this.checkAllTakenExpanded();
  }

  protected expandTaak(taak: ExpandableTableData<GeneratedType<"RestTask">>) {
    taak.expanded = !taak.expanded;
    this.checkAllTakenExpanded();
  }

  private checkAllTakenExpanded() {
    const filter = this.toonAfgerondeTaken.value
      ? this.takenDataSource.data.filter((value) => !value.expanded)
      : this.takenDataSource.data.filter(
          (value) => value.data.status !== "AFGEROND" && !value.expanded,
        );
    this.allTakenExpanded = filter.length === 0;
  }

  private readonly assigningTaakIds = new Set<string>();

  protected isAssigningTaakToMe(taak: GeneratedType<"RestTask">) {
    return !!taak.id && this.assigningTaakIds.has(taak.id);
  }

  protected showAssignTaakToMe(taak: GeneratedType<"RestTask">) {
    if (taak.status === "AFGEROND") return false;
    if (!taak.rechten.toekennen) return false;
    if (!taak.groep?.id) return false;
    const loggedInUser = this.loggedInUser.data();
    if (!loggedInUser) return false;
    if (loggedInUser.id === taak.behandelaar?.id) return false;
    return loggedInUser.groupIds?.includes(taak.groep.id) ?? false;
  }

  protected assignTaakToMe(
    taak: GeneratedType<"RestTask">,
    $event: MouseEvent,
  ) {
    $event.stopPropagation();
    if (!taak.id || this.assigningTaakIds.has(taak.id)) return;

    this.assigningTaakIds.add(taak.id);
    this.websocketService.suspendListener(this.zaakTakenListener);
    this.takenService
      .toekennenAanIngelogdeMedewerker({
        taakId: taak.id,
        zaakUuid: taak.zaakUuid,
        groepId: taak.groep!.id!,
      })
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.assigningTaakIds.delete(taak.id!)),
      )
      .subscribe((returnTaak) => {
        taak.behandelaar = returnTaak.behandelaar;
        taak.status = returnTaak.status;
        this.utilService.openSnackbar("msg.taak.toegekend", {
          behandelaar: taak.behandelaar?.naam,
        });
      });
  }

  protected filterTakenOpStatus() {
    this.takenStatusFilter = this.toonAfgerondeTaken.value ? "" : "AFGEROND";
    this.takenDataSource.filter = this.takenStatusFilter;
    SessionStorageUtil.setItem(
      "toonAfgerondeTaken",
      this.toonAfgerondeTaken.value,
    );
  }

  protected isAfterDate(datum: Date | moment.Moment | string) {
    return DateConditionals.isExceeded(datum);
  }

  protected taskStatusChipColor(status: GeneratedType<"TaakStatus">) {
    switch (status) {
      case "AFGEROND":
        return "success";
      case "TOEGEKEND":
        return "primary";
      default:
        return "";
    }
  }
}
