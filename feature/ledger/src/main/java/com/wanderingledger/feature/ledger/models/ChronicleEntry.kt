package com.wanderingledger.feature.ledger.model

import androidx.compose.ui.graphics.vector.ImageVector

enum class EntryType {
    TRAVEL,
    ENCOUNTER,
    MARKET,
    COMPANION,
    RUMOR
}

enum class ChronicleIcon(val vectorName: String) {
    COMPASS("tablercompass"),
    MAP("tablermap"),
    DIALOG("tablerchatcircle"),
    FIST("tablerhandshake"),
    CIGAR("tablercigar");
}
