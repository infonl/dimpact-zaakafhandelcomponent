/*
 * SPDX-FileCopyrightText: 2023-2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.util;

/**
 * A workaround for a bug in the Yasson 3 library which is included in WildFly related to using
 * generic types in JSON-B deserialisation. See:
 * https://github.com/eclipse-ee4j/yasson/issues/599.
 * Details of the workaround: https://github.com/smallrye/smallrye-graphql/issues/1819#issuecomment-1549588537
 * The workaround is:
 * "T needs to implement an interface or extends an abstract class. A fake is fine, it is not added to the schema.
 * For java this means that T can not be a record, in kotlin a data class is possible."
 */
public interface SerializableByYasson {
}
