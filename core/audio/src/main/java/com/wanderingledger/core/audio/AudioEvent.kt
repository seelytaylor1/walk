package com.wanderingledger.core.audio

sealed class AudioEvent {
    object StepBankTick : AudioEvent() // soft tick every 500 steps added

    object TravelBegin : AudioEvent() // footstep swell

    object TownArrival : AudioEvent() // warm bell chord

    object MarketBuy : AudioEvent() // coin clink

    object MarketSell : AudioEvent() // richer coin clink

    object LedgerOpen : AudioEvent() // page rustle

    object RumorReceived : AudioEvent() // parchment scratch

    object EncounterStart : AudioEvent() // tension sting

    object EncounterSuccess : AudioEvent() // resolve swell

    object EncounterFailure : AudioEvent() // low thud

    object BondIncrease : AudioEvent() // gentle warm tone
}
