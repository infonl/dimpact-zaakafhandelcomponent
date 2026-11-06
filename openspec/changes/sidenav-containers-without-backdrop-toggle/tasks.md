## 1. Containers

- [x] 1.1 Turn the backdrop off in the template of the menu container on the 8 admin pages
- [x] 1.2 Move the actions panel and the page content of the zaak, taak and document view into an inner container,
  and turn the backdrop of the outer container off
- [x] 1.3 Adjust the global styles so the padding, the auto mode margin and the phone height apply to one container

## 2. Clean up

- [x] 2.1 Remove the backdrop switching from the view base classes and the workaround of PR #7713
- [x] 2.2 Remove the menu drawer references that are no longer used

## 3. Verify

- [x] 3.1 A spec checks that the document view stays usable when the menu switches to auto mode and after the
  actions panel closes
- [x] 3.2 `npm run lint`, `npm test` and `npm run build` pass
- [ ] 3.3 On all 11 pages the menu works in the modes open, closed and auto, and the page is clickable after the
  actions panel closes through the close button, Escape and a click on the backdrop
