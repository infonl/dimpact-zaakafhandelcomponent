/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

/**
 * klantinteracties
 * **Warning: Difference between `PUT` and `PATCH`** Both `PUT` and `PATCH` methods can be used to update the fields in a resource, but
 * there is a key difference in how they handle required fields: * The `PUT` method requires you to specify **all mandatory fields** when
 * updating a resource. If any mandatory field is missing, the update will fail. Optional fields are left unchanged if they are not
 * specified. * The `PATCH` method, on the other hand, allows you to update only the fields you specify. Some mandatory fields can be left
 * out, and the resource will only be updated with the provided data, leaving other fields unchanged.
 *
 * The version of the OpenAPI document: 0.1.2 (1)
 * Contact: standaarden.ondersteuning@vng.nl
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */

package net.atos.client.klant.model;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import jakarta.json.bind.annotation.JsonbProperty;


public class PaginatedPartijList {

    @JsonbProperty("count")
    protected Integer count;

    @JsonbProperty("next")
    protected URI next;

    @JsonbProperty("previous")
    protected URI previous;

    @JsonbProperty("results")
    protected List<Partij> results = new ArrayList<>();

    /**
     * Get count
     * 
     * @return count
     **/
    public Integer getCount() {
        return count;
    }

    /**
     * Set count
     */
    public void setCount(Integer count) {
        this.count = count;
    }

    public PaginatedPartijList count(Integer count) {
        this.count = count;
        return this;
    }

    /**
     * Get next
     * 
     * @return next
     **/
    public URI getNext() {
        return next;
    }

    /**
     * Set next
     */
    public void setNext(URI next) {
        this.next = next;
    }

    public PaginatedPartijList next(URI next) {
        this.next = next;
        return this;
    }

    /**
     * Get previous
     * 
     * @return previous
     **/
    public URI getPrevious() {
        return previous;
    }

    /**
     * Set previous
     */
    public void setPrevious(URI previous) {
        this.previous = previous;
    }

    public PaginatedPartijList previous(URI previous) {
        this.previous = previous;
        return this;
    }

    /**
     * Get results
     * 
     * @return results
     **/
    public List<Partij> getResults() {
        return results;
    }

    /**
     * Set results
     */
    public void setResults(List<Partij> results) {
        this.results = results;
    }

    public PaginatedPartijList results(List<Partij> results) {
        this.results = results;
        return this;
    }

    public PaginatedPartijList addResultsItem(Partij resultsItem) {
        if (this.results == null) {
            this.results = new ArrayList<>();
        }
        this.results.add(resultsItem);
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PaginatedPartijList paginatedPartijList = (PaginatedPartijList) o;
        return Objects.equals(this.count, paginatedPartijList.count) &&
               Objects.equals(this.next, paginatedPartijList.next) &&
               Objects.equals(this.previous, paginatedPartijList.previous) &&
               Objects.equals(this.results, paginatedPartijList.results);
    }

    @Override
    public int hashCode() {
        return Objects.hash(count, next, previous, results);
    }

    /**
     * Create a string representation of this pojo.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class PaginatedPartijList {\n");

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
