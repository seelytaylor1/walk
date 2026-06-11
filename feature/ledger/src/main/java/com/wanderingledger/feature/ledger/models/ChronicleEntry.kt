package com.wanderingledger.feature.ledger.model

enum class EntryType {
    TRAVEL,
    ENCOUNTER,
    MARKET,
    COMPANION,
    RUMOR,
}

enum class ChronicleIcon(
    val vectorName: String,
) {
    COMPASS("tablercompass"),
    MAP("tablermap"),
    DIALOG("tablerchatcircle"),
    FIST("tablerhandshake"),
    CIGAR("tablercigar"),
}

data class ChronicleEntry(
    val id: Long,
    val type: EntryType,
    val title: String,
    val summary: String,
    val townName: String,
    val timestampMs: Long,
    val routeLabel: String? = null,
    val companionName: String? = null,
    val companionNote: String? = null,
    val goldDelta: Long? = null,
    val icon: ChronicleIcon = ChronicleIcon.COMPASS,
) {
    val timestampFormatted: String
        get() = "Day ${timestampMs / 86_400_000L + 1}"
}
