/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { ButtonMenuItem } from "../../../shared/side-nav/menu-item/button-menu-item";
import { HeaderMenuItem } from "../../../shared/side-nav/menu-item/header-menu-item";
import { MenuItem } from "../../../shared/side-nav/menu-item/menu-item";
import { GeneratedType } from "../../../shared/utils/generated-types";
import { toI18nKey } from "../../../shared/utils/i18n-key";
import {
  allowedToAddBetrokkene,
  hasAfleidingswijzeBrondatumEigenschap,
  hasZaakData,
} from "./zaak-view.predicates";

type Zaak = GeneratedType<"RestZaak">;
type PlanItem = GeneratedType<"RestPlanItem">;

export interface ZaakMenuPlanItems {
  userEventListener: PlanItem[];
  humanTask: PlanItem[];
}

/** The menu actions that are the zaak view's own, rather than a dialog's. */
export interface ZaakMenuHandlers {
  /** Opens the side action panel the side nav itself switches to. */
  openSideAction(): void;
  startHumanTask(planItem: PlanItem): void;
}

/**
 * The dialogs the menu opens, shaped so that `ZaakActionDialogsService`
 * satisfies it as it stands.
 */
export interface ZaakMenuDialogs {
  openPlanItemStarten(zaak: Zaak, planItem: PlanItem): void;
  openHeropenen(zaak: Zaak): void;
  openOpschorten(zaak: Zaak): void;
  openVerlengen(zaak: Zaak): void;
  openHervatten(zaak: Zaak): void;
  openAfbreken(zaak: Zaak): void;
  openAfsluiten(zaak: Zaak): void;
  openBrondatumZetten(zaak: Zaak): void;
}

export function userEventListenerIcon(
  userEventListenerActie?: GeneratedType<"UserEventListenerActie"> | null,
) {
  switch (userEventListenerActie) {
    case "INTAKE_AFRONDEN":
      return "thumbs_up_down";
    case "ZAAK_AFHANDELEN":
      return "thumb_up_alt";
    default:
      return "fact_check";
  }
}

/**
 * Builds the zaak view side menu. Pass `null` for `planItems` while the plan
 * item calls are still in flight: the sections that depend on them, and the
 * koppelingen section that follows them, are then left out entirely.
 */
export function buildZaakMenu(
  zaak: Zaak,
  planItems: ZaakMenuPlanItems | null,
  handlers: ZaakMenuHandlers,
  dialogs: ZaakMenuDialogs,
  hasBrpSearchRight: boolean,
  isZaakdataGearchiveerd: boolean,
): MenuItem[] {
  const menu: MenuItem[] = [
    new HeaderMenuItem("zaak"),
    ...zaakMenuItems(zaak, handlers, isZaakdataGearchiveerd),
  ];

  if (!planItems) return menu;

  const actionMenuItems = createActionMenuItems(zaak, dialogs);

  if (zaak.rechten.canBehandelen) {
    if (planItems.userEventListener.length || actionMenuItems.length) {
      menu.push(new HeaderMenuItem("actie.zaak.acties"));
    }
    menu.push(
      ...planItems.userEventListener.map(
        (planItem) =>
          new ButtonMenuItem(
            toI18nKey("planitem." + planItem.userEventListenerActie),
            () => dialogs.openPlanItemStarten(zaak, planItem),
            userEventListenerIcon(planItem.userEventListenerActie),
          ),
      ),
    );
  }

  menu.push(...actionMenuItems);

  if (zaak.rechten.canBehandelen) {
    if (planItems.humanTask.length) {
      menu.push(new HeaderMenuItem("actie.taak.starten"));
    }
    menu.push(
      ...[...planItems.humanTask]
        .sort((humanTaskA, humanTaskB) =>
          (humanTaskA.naam ?? "").localeCompare(humanTaskB.naam ?? ""),
        )
        .map(
          (planItem) =>
            new ButtonMenuItem(
              planItem.naam,
              () => handlers.startHumanTask(planItem),
              "assignment",
            ),
        ),
    );
  }

  menu.push(...createKoppelingenMenuItems(zaak, handlers, hasBrpSearchRight));

  return menu;
}

function zaakMenuItems(
  zaak: Zaak,
  handlers: ZaakMenuHandlers,
  isZaakdataGearchiveerd: boolean,
) {
  const menu: MenuItem[] = [];
  const open = () => handlers.openSideAction();

  if (zaak.rechten.canBehandelen && !zaak.isProcesGestuurd) {
    if (
      zaak.rechten.canVersturenOntvangstbevestiging &&
      !zaak.isOntvangstbevestigingVerstuurd
    ) {
      menu.push(
        new ButtonMenuItem(
          "actie.ontvangstbevestiging.versturen",
          open,
          "mark_email_read",
        ),
      );
    }

    if (zaak.rechten.canVersturenEmail) {
      menu.push(new ButtonMenuItem("actie.mail.versturen", open, "mail"));
    }
  }

  if (zaak.rechten.canCreerenDocument) {
    const smartDocuments = zaak.zaaktype.zaakafhandelparameters?.smartDocuments;
    if (
      smartDocuments?.isEnabledForZaaktype &&
      smartDocuments.isEnabledGlobally
    ) {
      menu.push(new ButtonMenuItem("actie.document.maken", open, "note_add"));
    }

    menu.push(
      new ButtonMenuItem("actie.document.toevoegen", open, "upload_file"),
    );
    menu.push(
      new ButtonMenuItem("actie.document.verzenden", open, "local_post_office"),
    );
  }

  if (
    zaak.isOpen &&
    zaak.rechten.canBehandelen &&
    !zaak.isInIntakeFase &&
    zaak.isBesluittypeAanwezig &&
    !zaak.isProcesGestuurd
  ) {
    menu.push(new ButtonMenuItem("actie.besluit.vastleggen", open, "gavel"));
  }

  if (hasZaakData(zaak) && zaak.rechten.canBekijkenZaakdata) {
    menu.push(
      new ButtonMenuItem(
        isZaakdataGearchiveerd
          ? "actie.zaakdata.archief"
          : "actie.zaakdata.bekijken",
        open,
        "folder_copy",
      ),
    );
  }

  if (zaak.bpmnProcessDefinition) {
    menu.push(
      new ButtonMenuItem("actie.procesverloop.bekijken", open, "play_shapes"),
    );
  }

  return menu;
}

function createActionMenuItems(zaak: Zaak, dialogs: ZaakMenuDialogs) {
  const actionMenuItems: MenuItem[] = [];

  if (!zaak.isOpen && zaak.rechten.canHeropenen) {
    actionMenuItems.push(
      new ButtonMenuItem(
        "actie.zaak.heropenen",
        () => dialogs.openHeropenen(zaak),
        "restart_alt",
      ),
    );
  }

  if (
    zaak.isOpen &&
    zaak.rechten.canBehandelen &&
    zaak.zaaktype.isOpschortingMogelijk &&
    !zaak.isHeropend &&
    !zaak.isOpgeschort &&
    !zaak.isProcesGestuurd &&
    !zaak.hasEerdereOpschorting
  ) {
    actionMenuItems.push(
      new ButtonMenuItem(
        "actie.zaak.opschorten",
        () => dialogs.openOpschorten(zaak),
        "pause",
      ),
    );
  }

  if (
    zaak.isOpen &&
    zaak.rechten.canWijzigenDoorlooptijd &&
    zaak.zaaktype.isVerlengingMogelijk &&
    !zaak.duurVerlenging &&
    !zaak.isHeropend &&
    !zaak.isOpgeschort &&
    !zaak.isProcesGestuurd
  ) {
    actionMenuItems.push(
      new ButtonMenuItem(
        "actie.zaak.verlengen",
        () => dialogs.openVerlengen(zaak),
        "update",
      ),
    );
  }

  if (
    zaak.isOpgeschort &&
    zaak.rechten.canBehandelen &&
    !zaak.isProcesGestuurd
  ) {
    actionMenuItems.push(
      new ButtonMenuItem(
        "actie.zaak.hervatten",
        () => dialogs.openHervatten(zaak),
        "play_circle",
      ),
    );
  }

  if (zaak.isOpen && !zaak.isHeropend && zaak.rechten.canAfbreken) {
    actionMenuItems.push(
      new ButtonMenuItem(
        "actie.zaak.afbreken",
        () => dialogs.openAfbreken(zaak),
        "thumb_down_alt",
      ),
    );
  }

  if (zaak.isHeropend && zaak.rechten.canBehandelen) {
    actionMenuItems.push(
      new ButtonMenuItem(
        "actie.zaak.afsluiten",
        () => dialogs.openAfsluiten(zaak),
        "thumb_up_alt",
      ),
    );
  }

  if (
    zaak.rechten.canBrondatumZetten &&
    hasAfleidingswijzeBrondatumEigenschap(zaak)
  ) {
    actionMenuItems.push(
      new ButtonMenuItem(
        "actie.zaak.brondatum-zetten",
        () => dialogs.openBrondatumZetten(zaak),
        "calendar_today",
      ),
    );
  }

  return actionMenuItems;
}

function createKoppelingenMenuItems(
  zaak: Zaak,
  handlers: ZaakMenuHandlers,
  hasBrpSearchRight: boolean,
) {
  if (!zaak.rechten.canBehandelen && !zaak.rechten.canWijzigen) return [];

  const menu: MenuItem[] = [new HeaderMenuItem("koppelingen")];
  const open = () => handlers.openSideAction();

  if (allowedToAddBetrokkene(zaak, hasBrpSearchRight)) {
    menu.push(
      new ButtonMenuItem("actie.betrokkene.koppelen", open, "group_add"),
    );
  }

  if (zaak.rechten.canToevoegenBagObject) {
    menu.push(
      new ButtonMenuItem("actie.bag-object.koppelen", open, "add_home_work"),
    );
  }

  if (zaak.rechten.canWijzigenLocatie && !zaak.zaakgeometrie) {
    menu.push(
      new ButtonMenuItem(
        "actie.zaak.locatie.koppelen",
        open,
        "add_location_alt",
      ),
    );
  }

  if (zaak.rechten.canWijzigen) {
    menu.push(new ButtonMenuItem("actie.zaak.koppelen", open, "account_tree"));
  }

  if (zaak.isZaakspecifiekGeautoriseerd && zaak.rechten.canWijzigen) {
    menu.push(
      new ButtonMenuItem(
        "actie.zaakspecifiek-geautoriseerde-medewerker.toevoegen",
        open,
        "person_add",
      ),
    );
  }

  return menu;
}
