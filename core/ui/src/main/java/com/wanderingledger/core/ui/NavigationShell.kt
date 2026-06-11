package com.wanderingledger.core.ui

import android.content.Context
import android.view.View
import android.widget.LinearLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class NavigationShell(
    context: Context,
    private val initialContent: View,
) : LinearLayout(context) {
    enum class ScreenType {
        JOURNEY,
        WORLD_MAP,
        TOWN,
        TOWN_ARRIVAL,
        MARKET,
        INVENTORY,
        LEDGER,
        CHRONICLE,
        COMPANIONS,
        SETTINGS,
    }

    var onNavigateToDestination: ((BottomNavBar.Destination) -> Unit)? = null
    var onReselectTab: ((BottomNavBar.Destination) -> Unit)? = null
    var onRestoreScreen: ((ScreenType) -> Unit)? = null

    internal var currentScreen: ScreenType = ScreenType.WORLD_MAP
    private var currentDestination: BottomNavBar.Destination = BottomNavBar.Destination.WORLD_MAP
    private var contentView: View = initialContent
    private lateinit var topBar: TopBar
    private lateinit var bottomNav: BottomNavBar

    internal var screenStack = mutableListOf<ScreenType>()

    init {
        orientation = VERTICAL
        setupWindowInsets()
        setupNavigation(initialContent)
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(this) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(0, insets.top, 0, insets.bottom)
            WindowInsetsCompat.CONSUMED
        }
    }

    private fun setupNavigation(initialContent: View) {
        topBar =
            TopBar(
                context = context,
                title = "Wandering Ledger",
                subtitle = null,
                onBack = { navigateBack() },
            )

        bottomNav =
            BottomNavBar(
                context = context,
                onNavigate = { destination ->
                    if (destination == currentDestination) {
                        onReselectTab?.invoke(destination)
                    } else {
                        handleDestination(destination)
                    }
                },
            )

        addView(
            topBar,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT),
        )

        contentView.layoutParams =
            LayoutParams(
                LayoutParams.MATCH_PARENT,
                0,
                1f,
            )
        addView(contentView)

        addView(
            bottomNav,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT),
        )

        bottomNav.render(BottomNavBar.Destination.WORLD_MAP)
        topBar.render("Wandering Ledger", null, showBack = false)
    }

    internal fun handleDestination(destination: BottomNavBar.Destination) {
        onNavigateToDestination?.invoke(destination)
            ?: run {
                // Fallback: just update nav bar highlight (no content swap)
                val screenType = destination.toScreenType()
                navigateTo(screenType)
            }
    }

    private fun BottomNavBar.Destination.toScreenType() =
        when (this) {
            BottomNavBar.Destination.WORLD_MAP -> ScreenType.WORLD_MAP
            BottomNavBar.Destination.TOWN -> ScreenType.TOWN
            BottomNavBar.Destination.LEDGER -> ScreenType.LEDGER
            BottomNavBar.Destination.COMPANIONS -> ScreenType.COMPANIONS
        }

    fun navigateTo(
        screen: ScreenType,
        title: String? = null,
        subtitle: String? = null,
    ) {
        if (currentScreen != screen) {
            screenStack.add(currentScreen)
        }
        currentScreen = screen

        updateNavForScreen(screen, title, subtitle)
    }

    fun navigateBack() {
        if (screenStack.isNotEmpty()) {
            val previous = screenStack.removeLast()
            currentScreen = previous
            updateNavForScreen(previous, null, null)
            onRestoreScreen?.invoke(previous)
        }
    }

    fun replaceContent(newContent: View) {
        removeViewAt(1)
        contentView = newContent
        contentView.layoutParams =
            LayoutParams(
                LayoutParams.MATCH_PARENT,
                0,
                1f,
            )
        addView(contentView, 1)
    }

    private fun updateNavForScreen(
        screen: ScreenType,
        title: String?,
        subtitle: String?,
    ) {
        val (navDestination, topTitle, topSubtitle, showBack) =
            when (screen) {
                ScreenType.WORLD_MAP ->
                    Quad(
                        BottomNavBar.Destination.WORLD_MAP,
                        title ?: "World Map",
                        null,
                        false,
                    )
                ScreenType.JOURNEY ->
                    Quad(
                        BottomNavBar.Destination.WORLD_MAP,
                        title ?: "Journey",
                        null,
                        true,
                    )
                ScreenType.TOWN_ARRIVAL ->
                    Quad(
                        BottomNavBar.Destination.TOWN,
                        title ?: "Arrival",
                        null,
                        false,
                    )
                ScreenType.TOWN ->
                    Quad(
                        BottomNavBar.Destination.TOWN,
                        title ?: "Town",
                        subtitle,
                        false,
                    )
                ScreenType.MARKET ->
                    Quad(
                        BottomNavBar.Destination.TOWN,
                        title ?: "Market",
                        subtitle,
                        true,
                    )
                ScreenType.INVENTORY ->
                    Quad(
                        BottomNavBar.Destination.TOWN,
                        title ?: "Inventory",
                        subtitle,
                        true,
                    )
                ScreenType.LEDGER ->
                    Quad(
                        BottomNavBar.Destination.LEDGER,
                        title ?: "Ledger",
                        subtitle,
                        true,
                    )
                ScreenType.CHRONICLE ->
                    Quad(
                        BottomNavBar.Destination.LEDGER,
                        title ?: "Chronicle",
                        subtitle,
                        true,
                    )
                ScreenType.COMPANIONS ->
                    Quad(
                        BottomNavBar.Destination.COMPANIONS,
                        title ?: "Party",
                        subtitle,
                        true,
                    )
                ScreenType.SETTINGS ->
                    Quad(
                        BottomNavBar.Destination.WORLD_MAP,
                        title ?: "Settings",
                        null,
                        true,
                    )
            }

        bottomNav.setDestination(navDestination)
        currentDestination = navDestination
        topBar.render(topTitle, topSubtitle, showBack)
    }

    private data class Quad<A, B, C, D>(
        val first: A,
        val second: B,
        val third: C,
        val fourth: D,
    )
}
