package com.factory.aquacoachsmarthydrationai.testutil

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * Swaps [Dispatchers.Main] for a [TestDispatcher] so ViewModel/manager code that
 * launches on `Dispatchers.Main` (immediate or not) can run under plain JUnit tests.
 *
 * Defaults to [UnconfinedTestDispatcher] (rather than [kotlinx.coroutines.test.StandardTestDispatcher])
 * so coroutines launched on secondary scopes (e.g. a manager's own `SupervisorJob` scope) run eagerly
 * to their next suspension point instead of requiring a manual `advanceUntilIdle()` at every call site.
 */
@ExperimentalCoroutinesApi
class MainDispatcherRule(
    val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()
) : TestWatcher() {

    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
