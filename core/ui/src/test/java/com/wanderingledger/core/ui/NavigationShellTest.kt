package com.wanderingledger.core.ui

import android.content.Context
import android.view.View
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NavigationShellTest {

    private lateinit var context: Context
    private lateinit var shell: NavigationShell
    private lateinit var initialView: View

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        initialView = View(context)
        shell = NavigationShell(context, initialView)
    }

    @Test
    fun `handleDestination dispatches callback`() {
        var dispatchedDestination: BottomNavBar.Destination? = null
        shell.onNavigateToDestination = { dispatchedDestination = it }

        shell.handleDestination(BottomNavBar.Destination.LEDGER)

        assertEquals(BottomNavBar.Destination.LEDGER, dispatchedDestination)
    }

    @Test
    fun `handleDestination fallback uses navigateTo`() {
        // No callback set
        shell.handleDestination(BottomNavBar.Destination.WORLD_MAP)

        assertEquals(NavigationShell.ScreenType.WORLD_MAP, shell.currentScreen)
    }

    @Test
    fun `navigateBack pops stack and restores screen`() {
        var restoredScreen: NavigationShell.ScreenType? = null
        shell.onRestoreScreen = { restoredScreen = it }

        // Start at WORLD_MAP (implicitly set by shell)
        
        // Navigate to JOURNEY
        shell.navigateTo(NavigationShell.ScreenType.JOURNEY)
        assertEquals(NavigationShell.ScreenType.JOURNEY, shell.currentScreen)

        // Go back
        shell.navigateBack()
        
        assertEquals(NavigationShell.ScreenType.WORLD_MAP, shell.currentScreen)
        assertEquals(NavigationShell.ScreenType.WORLD_MAP, restoredScreen)
    }

    @Test
    fun `navigateTo adds to stack only if screen changes`() {
        // Initial screen is WORLD_MAP
        assertEquals(NavigationShell.ScreenType.WORLD_MAP, shell.currentScreen)
        assertTrue(shell.screenStack.isEmpty())

        shell.navigateTo(NavigationShell.ScreenType.WORLD_MAP)
        assertTrue("Stack should be empty if navigating to same screen", shell.screenStack.isEmpty())
        
        shell.navigateTo(NavigationShell.ScreenType.TOWN)
        assertEquals(1, shell.screenStack.size)
        assertEquals(NavigationShell.ScreenType.WORLD_MAP, shell.screenStack.last())
    }

    @Test
    fun `replaceContent updates content view`() {
        val newView = View(context)
        shell.replaceContent(newView)
        
        // NavigationShell has TopBar, ContentView, BottomNavBar
        // Content view is at index 1
        assertEquals(newView, shell.getChildAt(1))
    }
}
