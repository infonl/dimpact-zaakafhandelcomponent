/*
 * SPDX-FileCopyrightText: 2022 Atos, 2023-2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.client.klanten.model;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import jakarta.json.bind.annotation.JsonbProperty;

public class KlantList200Response {

    @JsonbProperty("count")
    private Integer count;

    @JsonbProperty("next")
    private URI next;

    @JsonbProperty("previous")
    private URI previous;

    @JsonbProperty("results")
    private List<Klant> results = new ArrayList<>();

    /**
     * Get count
     * @return count
     **/
    public Integer getCount() {
        return count;
    }

    /**
     * Set count
     **/
    public void setCount(Integer count) {
        this.count = count;
    }

    public KlantList200Response count(Integer count) {
        this.count = count;
        return this;
    }

    /**
     * Get next
     * @return next
     **/
    public URI getNext() {
        return next;
    }

    /**
     * Set next
     **/
    public void setNext(URI next) {
        this.next = next;
    }

    public KlantList200Response next(URI next) {
        this.next = next;
        return this;
    }

    /**
     * Get previous
     * @return previous
     **/
    public URI getPrevious() {
        return previous;
    }

    /**
     * Set previous
     **/
    public void setPrevious(URI previous) {
        this.previous = previous;
    }

    public KlantList200Response previous(URI previous) {
        this.previous = previous;
        return this;
    }

    /**
     * Get results
     * @return results
     **/
    public List<Klant> getResults() {
        return results;
    }

    /**
     * Set results
     **/
    public void setResults(List<Klant> results) {
        this.results = results;
    }

    public KlantList200Response results(List<Klant> results) {
        this.results = results;
        return this;
    }

    public KlantList200Response addResultsItem(Klant resultsItem) {
        this.results.add(resultsItem);
        return this;
    }

    /**
     * Create a string representation of this pojo.
     **/
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class KlantList200Response {\n");

        sb.append("    count: ").append(toIndentedString(count)).append("\n");
        sb.append("    next: ").append(toIndentedString(next)).append("\n");
        sb.append("    previous: ").append(toIndentedString(previous)).append("\n");
        sb.append("    results: ").append(toIndentedString(results)).append("\n");
        sb.append("}");
        return sb.toString();
    }

    /**
     * Convert the given object to string with each line indented by 4 spaces
     * (except the first line).
     */
    private static String toIndentedString(Object o) {
        if (o == null) {
            return "null";
        }
        return o.toString().replace("\n", "\n    ");
    }
}
