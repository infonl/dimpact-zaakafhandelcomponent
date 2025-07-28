/*
 * SPDX-FileCopyrightText: 2023-2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.util;

/**
 * A workaround for a bug in the Yasson 3 library which is included in WildFly related to using
 * generic types in JSON-B deserialisation. See:
 * <a href="https://github.com/eclipse-ee4j/yasson/issues/599">Issue with @JsonbCreator constructor with Generic Type in Yasson 3.0.0 and
 * higher</a>.
 * Details of the workaround: <a href="https://github.com/smallrye/smallrye-graphql/issues/1819#issuecomment-1549588537">issue comment</a>
 * The workaround is:
 * "T needs to implement an interface or extends an abstract class. A fake is fine, it is not added to the schema.
 * For java this means that T can not be a record, in kotlin a data class is possible."
 */
public interface SerializableByYasson {
}
