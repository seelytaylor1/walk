# Design System Module

The `core:designsystem` module provides the shared design system for the Wandering Ledger app, including theme, colors, typography, shapes, and reusable UI components.

## Overview

This module implements Material Design 3 with custom theming tailored for a walking and trading game experience. It uses warm, earthy tones inspired by medieval trading routes and parchment.

## Theme

### Colors

The color palette uses:
- **Primary**: Warm amber/gold for trading theme
- **Secondary**: Earthy brown for roads and travel
- **Tertiary**: Sage green for nature/walking
- **Semantic colors**: Success (green), Warning (orange), Info (blue)
- **Game-specific colors**: StepBankColor, GoldColor, ContrabandColor, ReputationColor

Both light and dark themes are supported.

### Typography

Uses system fonts with Material Design 3 type scale:
- Display styles for large headings
- Headline styles for section headers
- Title styles for card titles and important labels
- Body styles for main content
- Label styles for buttons and small labels

### Shapes

Rounded corners for a friendly, approachable feel:
- Extra Small: 4dp
- Small: 8dp
- Medium: 12dp
- Large: 16dp
- Extra Large: 24dp

### Spacing

Consistent spacing system:
- Extra Small: 4dp
- Small: 8dp
- Medium: 16dp
- Large: 24dp
- Extra Large: 32dp
- Huge: 48dp

## Components

### Buttons

- `WLButton`: Primary button for main actions
- `WLOutlinedButton`: Outlined button for secondary actions
- `WLTextButton`: Text button for tertiary actions

```kotlin
WLButton(
    text = "Travel",
    onClick = { /* action */ },
    enabled = true
)
```

### Cards

- `WLCard`: Standard card for content grouping
- `WLOutlinedCard`: Outlined card for secondary content
- `WLClickableCard`: Clickable card for interactive elements

```kotlin
WLCard {
    Text("Town Name")
    Text("Reputation: 50")
}
```

### Badges

- `WLBadge`: Generic badge for status display
- `SupplyBadge`: Supply level badge (Scarce/Normal/Abundant)
- `ContrabandBadge`: Badge for illegal goods
- `StepBadge`: Step count badge
- `GoldBadge`: Gold amount badge

```kotlin
SupplyBadge(supplyLevel = 2) // Shows "Abundant"
```

### List Items

- `WLListItem`: Standard list item
- `MarketListItem`: Market item with buy/sell prices
- `InventoryListItem`: Inventory item with quantity
- `CompanionListItem`: Companion with role and bond level

```kotlin
MarketListItem(
    goodName = "Spices",
    buyPrice = 100,
    sellPrice = 150,
    supplyLevel = 1,
    isContraband = false,
    onClick = { /* action */ }
)
```

### Dialogs

- `WLAlertDialog`: Standard alert dialog
- `WLDialog`: Custom dialog for complex content
- `WLConfirmationDialog`: Confirmation dialog for important actions
- `WLResultDialog`: Result dialog for outcomes

```kotlin
WLConfirmationDialog(
    title = "Confirm Travel",
    message = "This will cost 100 steps.",
    onConfirm = { /* action */ },
    onDismiss = { /* dismiss */ }
)
```

### Other Components

- `WLTopAppBar`: Standard top app bar
- `WLEmptyState`: Empty state for no content
- `WLLoadingIndicator`: Loading indicator for async operations
- `WLInlineLoadingIndicator`: Inline loading indicator

## Usage

### Apply Theme

Wrap your app content with `WanderingLedgerTheme`:

```kotlin
@Composable
fun MyApp() {
    WanderingLedgerTheme {
        // Your app content
    }
}
```

### Use Components

Import components from the package:

```kotlin
import com.wanderingledger.core.designsystem.component.*
import com.wanderingledger.core.designsystem.theme.*

@Composable
fun MyScreen() {
    WanderingLedgerTheme {
        Column {
            WLTopAppBar(title = "Town Market")
            
            WLCard {
                Text("Welcome to the market!")
            }
            
            WLButton(
                text = "Buy Goods",
                onClick = { /* action */ }
            )
        }
    }
}
```

## Preview Annotations

Use preview annotations for development:

```kotlin
import com.wanderingledger.core.designsystem.preview.*

@ThemePreviews // Light and dark theme previews
@Composable
fun MyComponentPreview() {
    WanderingLedgerTheme {
        MyComponent()
    }
}
```

Available annotations:
- `@ThemePreviews`: Light and dark themes
- `@DevicePreviews`: Phone and tablet sizes
- `@CompletePreviews`: All combinations

## Dependencies

This module exports Compose dependencies, so feature modules only need to depend on `core:designsystem` to get Compose support.

```kotlin
dependencies {
    implementation(projects.core.designsystem)
}
```

## Design Principles

1. **Consistency**: Use design system components throughout the app
2. **Accessibility**: Minimum touch target of 48dp, sufficient color contrast
3. **Readability**: Clear typography hierarchy for text-heavy content
4. **Game Feel**: Warm, earthy colors that evoke medieval trading routes
5. **Material Design 3**: Follow Material Design guidelines while maintaining game aesthetic
