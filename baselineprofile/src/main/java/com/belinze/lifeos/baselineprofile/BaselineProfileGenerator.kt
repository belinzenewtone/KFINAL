package com.belinze.lifeos.baselineprofile

import androidx.benchmark.macro.ExperimentalBaselineProfilesApi
import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Generates the Baseline Profile for LifeOS.
 *
 * Run with:
 *   ./gradlew :app:generateBaselineProfile
 *
 * This drives the app through its critical user journeys so ART can
 * AOT-compile the hot paths before first run. Expected cold-start gain:
 * ~30–40% on the splash → home transition.
 *
 * Profiles are written to app/src/main/baseline-prof.txt and should be
 * committed to source control.
 */
@OptIn(ExperimentalBaselineProfilesApi::class)
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() {
        rule.collect(packageName = "com.belinze.lifeos.compose") {
            // ── 1. Cold start ─────────────────────────────────────────────
            pressHome()
            startActivityAndWait()
            device.waitForIdle(3_000)

            // ── 2. Home screen visible ─────────────────────────────────────
            device.wait(Until.hasObject(By.res("lifeos:id/home_root")), 5_000)

            // ── 3. Finance tab ─────────────────────────────────────────────
            // Tap the Finance bottom-nav item (content description "Finance").
            val financeTab = device.findObject(By.desc("Finance"))
            if (financeTab != null) {
                financeTab.click()
                device.waitForIdle(2_000)
                // Scroll down to prefetch paging items
                device.swipe(540, 1500, 540, 500, 10)
                device.waitForIdle(1_000)
            }

            // ── 4. Tasks tab ───────────────────────────────────────────────
            val tasksTab = device.findObject(By.desc("Tasks"))
            if (tasksTab != null) {
                tasksTab.click()
                device.waitForIdle(2_000)
            }

            // ── 5. Planner tab ─────────────────────────────────────────────
            val plannerTab = device.findObject(By.desc("Planner"))
            if (plannerTab != null) {
                plannerTab.click()
                device.waitForIdle(2_000)
            }

            // ── 6. Insights tab ────────────────────────────────────────────
            val insightsTab = device.findObject(By.desc("Insights"))
            if (insightsTab != null) {
                insightsTab.click()
                device.waitForIdle(2_000)
            }

            // ── 7. Navigate back home ──────────────────────────────────────
            val homeTab = device.findObject(By.desc("Home"))
            if (homeTab != null) {
                homeTab.click()
                device.waitForIdle(1_000)
            }
        }
    }
}
