/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { ButtonMenuItem } from "../../../shared/side-nav/menu-item/button-menu-item";
import { HeaderMenuItem } from "../../../shared/side-nav/menu-item/header-menu-item";
import { MenuItem } from "../../../shared/side-nav/menu-item/menu-item";
import { GeneratedType } from "../../../shared/utils/generated-types";
import {
  allowedToAddBetrokkene,
  hasAfleidingswijzeBrondatumEigenschap,
  hasZaakData,
} from "./zaak-view.predicates";

type Zaak = GeneratedType<"RestZaak">;
type PlanItem = GeneratedType<"RESTPlanItem">;

export interface ZaakMenuPlanItems {
  userEventListener: PlanItem[];
  humanTask: PlanItem[];
}

export interface ZaakMenuHandlers {
  /** Opens the side action panel the side nav itself switches to. */
  openSideAction(): void;
  startHumanTask(planItem: PlanItem): void;
  startUserEventListener(planItem: PlanItem): void;
  heropenen(): void;
  opschorten(): void;
  verlengen(): void;
  hervatten(): void;
  afbreken(): void;
  afsluiten(): void;
  brondatumZetten(): void;
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
  hasBrpSearchRight: boolean,
): MenuItem[] {
  const menu: MenuItem[] = [
    new HeaderMenuItem("zaak"),
    ...zaakMenuItems(zaak, handlers),
  ];

  if (!planItems) return menu;

  const actionMenuItems = createActionMenuItems(zaak, handlers);

  if (zaak.rechten.behandelen) {
    if (planItems.userEventListener.length || actionMenuItems.length) {
      menu.push(new HeaderMenuItem("actie.zaak.acties"));
    }
    menu.push(
      ...planItems.userEventListener.map(
        (planItem) =>
          new ButtonMenuItem(
            "planitem." + planItem.userEventListenerActie,
            () => handlers.startUserEventListener(planItem),
            userEventListenerIcon(planItem.userEventListenerActie),
          ),
      ),
    );
  }

  menu.push(...actionMenuItems);

  if (zaak.rechten.behandelen) {
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

function zaakMenuItems(zaak: Zaak, handlers: ZaakMenuHandlers) {
  const menu: MenuItem[] = [];
  const open = () => handlers.openSideAction();

  if (zaak.rechten.behandelen && !zaak.isProcesGestuurd) {
    if (
      zaak.rechten.versturenOntvangstbevestiging &&
      !zaak.heeftOntvangstbevestigingVerstuurd
    ) {
      menu.push(
        new ButtonMenuItem(
          "actie.ontvangstbevestiging.versturen",
          open,
          "mark_email_read",
        ),
      );
    }

    if (zaak.rechten.versturenEmail) {
      menu.push(new ButtonMenuItem("actie.mail.versturen", open, "mail"));
    }
  }

  if (zaak.rechten.creerenDocument) {
    const smartDocuments = zaak.zaaktype.zaakafhandelparameters?.smartDocuments;
    if (smartDocuments?.enabledForZaaktype && smartDocuments.enabledGlobally) {
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
    zaak.rechten.behandelen &&
    !zaak.isInIntakeFase &&
    zaak.isBesluittypeAanwezig &&
    !zaak.isProcesGestuurd
  ) {
    menu.push(new ButtonMenuItem("actie.besluit.vastleggen", open, "gavel"));
  }

  if (hasZaakData(zaak) && zaak.rechten.bekijkenZaakdata) {
    menu.push(
      new ButtonMenuItem("actie.zaakdata.bekijken", open, "folder_copy"),
    );
  }

  if (zaak.bpmnProcessDefinition) {
    menu.push(
      new ButtonMenuItem("actie.procesverloop.bekijken", open, "play_shapes"),
    );
  }

  return menu;
}

function createActionMenuItems(zaak: Zaak, handlers: ZaakMenuHandlers) {
  const actionMenuItems: MenuItem[] = [];

  if (!zaak.isOpen && zaak.rechten.heropenen) {
    actionMenuItems.push(
      new ButtonMenuItem(
        "actie.zaak.heropenen",
        () => handlers.heropenen(),
        "restart_alt",
      ),
    );
  }

  if (
    zaak.isOpen &&
    zaak.rechten.behandelen &&
    zaak.zaaktype.opschortingMogelijk &&
    !zaak.isHeropend &&
    !zaak.isOpgeschort &&
    !zaak.isProcesGestuurd &&
    !zaak.eerdereOpschorting
  ) {
    actionMenuItems.push(
      new ButtonMenuItem(
        "actie.zaak.opschorten",
        () => handlers.opschorten(),
        "pause",
      ),
    );
  }

  if (
    zaak.isOpen &&
    zaak.rechten.wijzigenDoorlooptijd &&
    zaak.zaaktype.verlengingMogelijk &&
    !zaak.duurVerlenging &&
    !zaak.isHeropend &&
    !zaak.isOpgeschort &&
    !zaak.isProcesGestuurd
  ) {
    actionMenuItems.push(
      new ButtonMenuItem(
        "actie.zaak.verlengen",
        () => handlers.verlengen(),
        "update",
      ),
    );
  }

  if (zaak.isOpgeschort && zaak.rechten.behandelen && !zaak.isProcesGestuurd) {
    actionMenuItems.push(
      new ButtonMenuItem(
        "actie.zaak.hervatten",
        () => handlers.hervatten(),
        "play_circle",
      ),
    );
  }

  if (zaak.isOpen && !zaak.isHeropend && zaak.rechten.afbreken) {
    actionMenuItems.push(
      new ButtonMenuItem(
        "actie.zaak.afbreken",
        () => handlers.afbreken(),
        "thumb_down_alt",
      ),
    );
  }

  if (zaak.isHeropend && zaak.rechten.behandelen) {
    actionMenuItems.push(
      new ButtonMenuItem(
        "actie.zaak.afsluiten",
        () => handlers.afsluiten(),
        "thumb_up_alt",
      ),
    );
  }

  if (
    zaak.rechten.brondatumZetten &&
    hasAfleidingswijzeBrondatumEigenschap(zaak)
  ) {
    actionMenuItems.push(
      new ButtonMenuItem(
        "actie.zaak.brondatumZetten",
        () => handlers.brondatumZetten(),
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
  if (!zaak.rechten.behandelen && !zaak.rechten.wijzigen) return [];

  const menu: MenuItem[] = [new HeaderMenuItem("koppelingen")];
  const open = () => handlers.openSideAction();

  if (allowedToAddBetrokkene(zaak, hasBrpSearchRight)) {
    menu.push(
      new ButtonMenuItem("actie.betrokkene.koppelen", open, "group_add"),
    );
  }

  if (zaak.rechten.toevoegenBagObject) {
    menu.push(
      new ButtonMenuItem("actie.bagObject.koppelen", open, "add_home_work"),
    );
  }

  if (zaak.rechten.wijzigenLocatie && !zaak.zaakgeometrie) {
    menu.push(
      new ButtonMenuItem(
        "actie.zaak.locatie.koppelen",
        open,
        "add_location_alt",
      ),
    );
  }

  if (zaak.rechten.wijzigen) {
    menu.push(new ButtonMenuItem("actie.zaak.koppelen", open, "account_tree"));
  }

  return menu;
}
