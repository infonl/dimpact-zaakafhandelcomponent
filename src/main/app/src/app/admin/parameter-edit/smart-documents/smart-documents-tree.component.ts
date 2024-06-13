import { NestedTreeControl } from "@angular/cdk/tree";
import { Component, Input, effect } from "@angular/core";
import { MatTreeNestedDataSource } from "@angular/material/tree";
import { injectQuery } from "@tanstack/angular-query-experimental";
import { Observable, firstValueFrom } from "rxjs";
import {
  DocumentsTemplate,
  DocumentsTemplateGroup,
  SmartDocumentsService,
} from "../../smart-documents.service";

type SelectableDocumentsTemplate = {
  id: string;
  name: string;
  selected: boolean;
};

type SelectableDocumentsTemplateGroup = {
  id: string;
  name: string;
  templates?: SelectableDocumentsTemplate[];
  groups?: SelectableDocumentsTemplateGroup[];
};

function getSelectableGroup(
  original: DocumentsTemplateGroup,
  selection?: DocumentsTemplateGroup,
): SelectableDocumentsTemplateGroup {
  return {
    ...original,
    groups:
      original.groups &&
      getSelectableGroups(original.groups, selection?.groups ?? []),
    templates:
      original.templates &&
      getSelectableTemplates(original.templates, selection?.templates ?? []),
  };
}

function getSelectableTemplates(
  left: DocumentsTemplate[],
  right: DocumentsTemplate[],
): SelectableDocumentsTemplate[] {
  return left.map((l) => ({
    ...l,
    selected: !!right.find((x) => x.id === l.id),
  }));
}

export function getSelectableGroups(
  all: DocumentsTemplateGroup[],
  selection: DocumentsTemplateGroup[],
): SelectableDocumentsTemplateGroup[] {
  return all.map((a) =>
    getSelectableGroup(
      a,
      selection.find((s) => s.id === a.id),
    ),
  );
}

export function filterOutUnselected(
  group: SelectableDocumentsTemplateGroup,
): DocumentsTemplateGroup | undefined {
  const groups = group.groups?.map(filterOutUnselected).filter(Boolean);
  const templates = group.templates?.filter(({ selected }) => selected);
  return groups?.length || templates?.length
    ? {
        ...group,
        groups,
        templates,
      }
    : undefined;
}

@Component({
  selector: "smart-documents-tree",
  styles: `
    .mat-tree {
      --_padding: 40px;
    }

    [data-hide="true"] {
      visibility: collapse;
      block-size: 0;
    }

    ul,
    li {
      margin-top: 0;
      margin-bottom: 0;
      list-style-type: none;
    }

    /*
 * This padding sets alignment of the nested nodes.
 */
    .mat-nested-tree-node [role="group"] {
      padding-left: var(--_padding);
    }

    /*
 * Padding for leaf nodes.
 * Leaf nodes need to have padding so as to align with other non-leaf nodes
 * under the same parent.
 */
    [role="group"] > .mat-tree-node {
      padding-left: var(--_padding);
    }

    .tree-grid {
      display: grid;
      grid-template-columns: repeat(2, auto);
      inline-size: fit-content;
      column-gap: 2rem;

      * {
        grid-column: 1 / -1;
      }

      .single-node {
        grid-template-columns: subgrid;
        display: grid;
        > * {
          grid-column: unset;
        }
      }
    }
  `,
  template: `
    <mat-tree [dataSource]="dataSource" [treeControl]="treeControl">
      <!-- This is the tree node template for leaf nodes -->
      <!-- There is inline padding applied to this node using styles.
    This padding value depends on the mat-icon-button width. -->
      <mat-tree-node
        *matTreeNodeDef="let node"
        matTreeNodeToggle
        class="single-node"
      >
        <span>{{ node.name }}</span>
        <mat-slide-toggle
          [(ngModel)]="node.selected"
          color="primary"
        ></mat-slide-toggle>
      </mat-tree-node>
      <!-- This is the tree node template for expandable nodes -->
      <mat-nested-tree-node *matTreeNodeDef="let node; when: hasChild">
        <div class="mat-tree-node">
          <button
            mat-icon-button
            matTreeNodeToggle
            [attr.aria-label]="'Toggle ' + node.name"
          >
            <mat-icon class="mat-icon-rtl-mirror">
              {{
                treeControl.isExpanded(node) ? "expand_more" : "chevron_right"
              }}
            </mat-icon>
          </button>
          {{ node.name }}
        </div>
        <!-- There is inline padding applied to this div using styles.
          This padding value depends on the mat-icon-button width.  -->
        <div
          [attr.data-hide]="!treeControl.isExpanded(node)"
          role="group"
          class="tree-grid"
        >
          <ng-container matTreeNodeOutlet></ng-container>
        </div>
      </mat-nested-tree-node>
    </mat-tree>
  `,
})
export class SmartDocumentsTreeComponent {
  @Input() zaaktypeUuid: string;
  treeControl = new NestedTreeControl<SelectableDocumentsTemplateGroup>(
    ({ groups = [], templates = [] }) => [...groups, ...templates],
  );
  allSmartDocumentTemplates = injectQuery(() => ({
    queryKey: ["all smart documents"],
    queryFn: () => firstValueFrom(this.smartDocumentsService.listTemplates()),
  }));
  templateMappings = injectQuery(() => ({
    queryKey: [
      "smart documents template mapping for zaaktype",
      this.zaaktypeUuid,
    ],
    queryFn: () =>
      firstValueFrom(
        this.smartDocumentsService.getTemplatesMapping(this.zaaktypeUuid),
      ),
  }));
  dataSource = new MatTreeNestedDataSource<SelectableDocumentsTemplateGroup>();
  parameters: any;

  constructor(private smartDocumentsService: SmartDocumentsService) {
    effect(() => {
      this.dataSource.data = getSelectableGroups(
        this.allSmartDocumentTemplates.data() || [],
        this.templateMappings.data() || [],
      );
    });
  }

  hasChild = (
    _: number,
    { groups = [], templates = [] }: SelectableDocumentsTemplateGroup,
  ) => !!groups.length || !!templates.length;

  save(): Observable<never> {
    return this.smartDocumentsService.storeTemplatesMapping(
      this.zaaktypeUuid,
      this.dataSource.data.map(filterOutUnselected).filter(Boolean),
    );
  }
}
