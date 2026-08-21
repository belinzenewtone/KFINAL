package com.lifeos.sms

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Process-wide event bus for SMS parser → ViewModel communication.
 *
 * Replaces the React Native event emitter bridge (SmsReceiverModule.emitNewTransaction).
 * SmsProcessWorker emits here via SmsReceiverModule (Compose stub); Compose ViewModels
 * collect here to trigger UI refreshes without polling.
 *
 * Using a Kotlin object means there is exactly ONE instance per process — the same
 * shared memory that a singleton would give, but with zero injection boilerplate for
 * the caller (SmsReceiverModule, which is not in the DI graph).
 */
object SmsEventBus {
    /** Lightweight snapshot of an inserted transaction for heads-up alerts. */
    data class NewTransactionEvent(
        val mpesaCode: String,
        val amount: Double,
        val merchant: String?,
        val transactionType: String,
        val isFuliza: Boolean,
    )

    // ── New transaction (any insert from the SMS parser) ──────────────────────

    private val _newTransaction = MutableSharedFlow<NewTransactionEvent>(extraBufferCapacity = 32)

    /**
     * Collected by [TransactionViewModel] and [InsightsViewModel] to trigger a
     * lightweight reload when the SMS parser inserts a new transaction row, and
     * by the notification layer to post a heads-up alert.
     */
    val newTransaction: SharedFlow<NewTransactionEvent> = _newTransaction.asSharedFlow()

    /** Called by [SmsReceiverModule] stub after every successful DB insert. */
    fun notifyNewTransaction(tx: SmsParser.ParsedTransaction) {
        _newTransaction.tryEmit(
            NewTransactionEvent(
                mpesaCode        = tx.mpesaCode,
                amount           = tx.amount,
                merchant         = tx.counterparty,
                transactionType  = tx.transactionType,
                isFuliza         = tx.category == SmsParserConfig.SmsCategory.FULIZA_CHARGE,
            )
        )
    }

    // ── Fuliza limit prompt ──────────────────────────────────────────────────

    // replay=1 ensures a late collector (e.g. MainScaffold composing after the
    // worker emits during background import) still receives the prompt.
    private val _fulizaLimitNeeded = MutableSharedFlow<Double>(replay = 1, extraBufferCapacity = 4)

    /**
     * Emitted when a Fuliza charge SMS arrives but the user has not yet configured
     * their Fuliza limit. The Double is the detected outstanding balance.
     *
     * Collect in a ViewModel or composable that can prompt the user to enter the limit.
     */
    val fulizaLimitNeeded: SharedFlow<Double> = _fulizaLimitNeeded.asSharedFlow()

    /** Called by [SmsReceiverModule] stub when Fuliza is detected without a configured limit. */
    fun notifyFulizaLimitNeeded(outstanding: Double) {
        _fulizaLimitNeeded.tryEmit(outstanding)
    }
}
