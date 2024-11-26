/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.lifely.zac.itest.config

import org.testcontainers.containers.startupcheck.OneShotStartupCheckStrategy
import org.testcontainers.containers.startupcheck.StartupCheckStrategy.StartupStatus
import org.testcontainers.containers.wait.strategy.AbstractWaitStrategy

class OneShotStartupWaitStrategy : AbstractWaitStrategy() {
    override fun waitUntilReady() =
        check(
            OneShotStartupCheckStrategy().checkStartupState(
                waitStrategyTarget.dockerClient,
                waitStrategyTarget.containerId
            ) == StartupStatus.SUCCESSFUL
        ) { "Startup of container ${waitStrategyTarget.containerInfo.name} failed!" }
}
