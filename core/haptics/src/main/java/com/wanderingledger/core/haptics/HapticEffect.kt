package com.wanderingledger.core.haptics

enum class HapticEffect {
    /** Light tap — navigation, minor interactions */
    SOFT_TAP,

    /** Crisp click — confirmations, purchases */
    CONFIRM,

    /** Rich double-pulse — rewards, bond increases, arrivals */
    REWARD,

    /** Heavy single pulse — errors, insufficient resources */
    ERROR,
}
