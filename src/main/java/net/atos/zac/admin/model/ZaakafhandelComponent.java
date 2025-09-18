package net.atos.zac.admin.model;

public interface ZaakafhandelComponent {
    public <T extends ZaakafhandelComponent> boolean isChanged(T original);

    public <T extends ZaakafhandelComponent> void modify(T changes);
}
