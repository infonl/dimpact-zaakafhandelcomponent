/*
 * SPDX-FileCopyrightText: 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac

import org.junit.runner.Description
import org.junit.runners.model.Statement
import org.testcontainers.containers.Network

const val DOCKER_IMAGE_ZAC_DEV = "ghcr.io/infonl/zaakafhandelcomponent:dev"

fun getTestContainersDockerNetwork(dockerNetworkName: String) = object : Network {
    override fun apply(base: Statement, description: Description): Statement = base

    override fun getId() = dockerNetworkName

    override fun close() {
        // noop
    }
}
