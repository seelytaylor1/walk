package com.wanderingledger.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wanderingledger.core.data.BuyResult
import com.wanderingledger.core.data.MarketRepository
import com.wanderingledger.core.data.MarketState
import com.wanderingledger.core.data.SellResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface MarketEffect {
    data object BuySuccess : MarketEffect
    data object SellSuccess : MarketEffect
    data object TransactionError : MarketEffect
}

class MarketViewModel(
    private val marketRepository: MarketRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {
    private val _state = MutableStateFlow<MarketState?>(null)
    val state: StateFlow<MarketState?> = _state.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _effects = MutableSharedFlow<MarketEffect>(extraBufferCapacity = 8)
    val effects: SharedFlow<MarketEffect> = _effects.asSharedFlow()

    private var observeJob: Job? = null

    /** Start observing market state for [townId]. Call when the market screen becomes active. */
    fun activate(townId: Long) {
        observeJob?.cancel()
        _message.value = null
        observeJob = viewModelScope.launch {
            marketRepository.observeMarket(townId).collect { _state.value = it }
        }
    }

    /** Stop observing. Call when leaving the market screen. */
    fun deactivate() {
        observeJob?.cancel()
        observeJob = null
    }

    fun buy(townId: Long, goodId: Long): Job = viewModelScope.launch {
        val result = withContext(ioDispatcher) {
            marketRepository.buyGood(townId, goodId, quantity = 1)
        }
        _message.value = result.toBuyMessage()
        when {
            result is BuyResult.Success -> _effects.emit(MarketEffect.BuySuccess)
            result is BuyResult.NotEnoughGold || result == BuyResult.InventoryFull ->
                _effects.emit(MarketEffect.TransactionError)
        }
    }

    fun sell(townId: Long, goodId: Long): Job = viewModelScope.launch {
        val result = withContext(ioDispatcher) {
            marketRepository.sellGood(townId, goodId, quantity = 1)
        }
        _message.value = result.toSellMessage()
        when {
            result is SellResult.Success -> _effects.emit(MarketEffect.SellSuccess)
            result is SellResult.NotEnoughInventory ->
                _effects.emit(MarketEffect.TransactionError)
        }
    }

    private fun BuyResult.toBuyMessage(): String? = when (this) {
        is BuyResult.Success -> "Bought ${quantity}x for ${goldSpent}g. Gold remaining: ${remainingGold}g."
        is BuyResult.NotEnoughGold -> "Not enough gold. Need ${required}g, have ${available}g."
        BuyResult.InventoryFull -> "Inventory is full."
        BuyResult.GoodNotAvailable -> "That good is not available here."
        BuyResult.InvalidQuantity -> null
    }

    private fun SellResult.toSellMessage(): String? = when (this) {
        is SellResult.Success -> "Sold ${quantity}x for ${goldEarned}g. Gold: ${remainingGold}g."
        is SellResult.NotEnoughInventory -> "You only have $available of that good."
        SellResult.GoodNotAvailable -> "That good is not available here."
        SellResult.InvalidQuantity -> null
    }
}
