package net.atos.zac.flowable.model;

public class ValueChangeData {
    public String oldValue;
    public String newValue;
    public String explanation;

    public ValueChangeData() {
    }

    public ValueChangeData(final String oldValue, final String newValue, final String explanation) {
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.explanation = explanation;
    }
}
