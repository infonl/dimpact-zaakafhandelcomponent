## Context

Material 22 marks the drawer content inert while a drawer that shows a backdrop is open, and checks this again only
when a drawer opens, closes or changes mode. A drawer container only manages the drawers whose nearest container it
is, so nested containers do not affect each other.

## Goals / Non-Goals

**Goals:**
- No code that switches the backdrop of a drawer container.
- No visible change for users.

**Non-Goals:**
- A shared layout component for the menu and the actions panel. The pages keep their own templates.
- Other changes to the menu or the actions panel.

## Decisions

- **Outer container for the menu, with the backdrop off in the template.** The menu never blocks the page, in any
  mode. The 8 admin pages only have a menu, so they only need this.
- **Inner container for the actions panel, inside the content of the outer container.** The actions panel keeps the
  default Material behaviour: a backdrop, inert content and a focus trap while it is open, and closing it through
  Escape or the backdrop.
- **Keep the outer container as it is.** It keeps its element, classes and height rules, so the page layout on
  tablet and phone does not change. Only the global styles that would now also hit the inner container change: the
  auto mode margin applies to the direct content only, the outer content gives its padding to the inner content,
  and the inner container keeps the full height on a phone.
- **One code pull request.** The shared base classes and all templates change together; a split would need the
  workaround in between.

## Risks / Trade-offs

- **Layout of 11 pages.** Padding, margins and heights could shift. → Check the zaak, taak and document view and the
  8 admin pages by hand in the modes open, closed and auto, on desktop, tablet and phone widths.
- **Less room for the actions panel next to an open menu.** The panel no longer covers the menu, so on a narrow
  screen it can get less than its 900px. → The panel shrinks to its container and its content scrolls, instead of
  being cut off.
- **Spec coverage.** The specs cannot check the layout. → A spec on the document view checks that the page stays
  usable when the menu switches to auto mode and after the actions panel closes.
