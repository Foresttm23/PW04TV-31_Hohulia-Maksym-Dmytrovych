package com.example.mse.ui.screens.task13

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mse.data.source.HydroSensorSimulator
import com.example.mse.logic.SensorFailureException
import com.example.mse.logic.SensorPipeline
import com.example.mse.logic.SensorPipeline.retryWithBackoff
import com.example.mse.model.CascadeSnapshot
import com.example.mse.model.LevelReading
import com.example.mse.model.LevelStatus
import com.example.mse.model.SensorState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

data class BasicState(val collecting: Boolean = false, val samples: List<Double> = emptyList(), val count: Int = 0)

data class MediumState(
    val running: Boolean = false,
    val reading: LevelReading? = null,
    val stableStatus: LevelStatus? = null,
    val received: Int = 0,
    val dropped: Int = 0,
    val error: String? = null
)

data class AdvancedState(
    val sensor: SensorState = SensorState.Stopped,
    val snapshot: CascadeSnapshot? = null,
    val pair: Pair<Double, Double>? = null,
    val retries: Int = 0,
    val recoveries: Int = 0,
    val subscribers: Int = 0
)

data class SensorUiState(
    val basic: BasicState = BasicState(),
    val medium: MediumState = MediumState(),
    val advanced: AdvancedState = AdvancedState(),
    val faultInjected: Boolean = false,
    val outflowM3s: Double = 1200.0,
    val log: List<String> = emptyList()
)

private const val MAX_RETRIES = 4
private const val RETRY_BASE_MS = 1000L
private const val STATUS_DEBOUNCE_MS = 1500L

@OptIn(ExperimentalCoroutinesApi::class)
class SensorStreamsViewModel(
    periodMs: Long = 800
) : ViewModel() {
    private val fault = AtomicBoolean(false)
    private val simulator = HydroSensorSimulator(periodMs = periodMs, failureInjected = fault::get)

    private val _state = MutableStateFlow(SensorUiState())
    val uiState: StateFlow<SensorUiState> = _state.asStateFlow()

    // Номер сесії: кожен запуск дає нове значення, тому flatMapLatest гарантовано перепідписується (null = зупинено).
    private val session = MutableStateFlow<Int?>(null)
    private val outflow = MutableStateFlow(1200.0)

    private var coldJob: Job? = null
    private var mediumJob: Job? = null

    // "Гарячі" потоки: працюють у viewModelScope незалежно від кількості підписників (Eagerly),
    // нові підписники одразу отримують останнє значення (replay = 1).
    private val hotLevel: SharedFlow<Double> = session
        .flatMapLatest { if (it == null) emptyFlow() else simulator.waterLevel() }
        .shareIn(viewModelScope, SharingStarted.Eagerly, replay = 1)

    private val hotInflow: SharedFlow<Double> = session
        .flatMapLatest { if (it == null) emptyFlow() else inflowWithRecovery() }
        .shareIn(viewModelScope, SharingStarted.Eagerly, replay = 1)

    private val hotForecast: SharedFlow<Double> = session
        .flatMapLatest { if (it == null) emptyFlow() else simulator.forecastLevel() }
        .shareIn(viewModelScope, SharingStarted.Eagerly, replay = 1)

    init {
        // Лічильник справжніх підписників hot-потоку (SharedFlow від shareIn не віддає subscriptionCount).
        val subscribers = MutableStateFlow(0)
        fun <T> Flow<T>.counted() = onStart { subscribers.update { it + 1 } }.onCompletion { subscribers.update { it - 1 } }
        subscribers.onEach { n -> updateAdvanced { it.copy(subscribers = n) } }.launchIn(viewModelScope)

        val level = hotLevel.filter(SensorPipeline::isPlausible)

        SensorPipeline.snapshots(level, hotInflow, outflow, hotForecast)
            .counted()
            .onEach { s -> updateAdvanced { it.copy(snapshot = s) } }
            .launchIn(viewModelScope)

        // buffer: zip мовчки блокує джерело, поки друге не віддасть значення; без буфера простій
        // датчика припливу зупинив би і спільний потік рівня.
        SensorPipeline.pairedSamples(level.buffer(64), hotInflow.buffer(Channel.BUFFERED))
            .counted()
            .onEach { p -> updateAdvanced { it.copy(pair = p) } }
            .launchIn(viewModelScope)
    }

    private fun inflowWithRecovery() = simulator.inflow()
        .retryWithBackoff(MAX_RETRIES, RETRY_BASE_MS) { attempt, delayMs ->
            updateAdvanced {
                it.copy(sensor = SensorState.Retrying(attempt, MAX_RETRIES, delayMs), retries = it.retries + 1)
            }
            log("Приплив: повтор $attempt/$MAX_RETRIES через $delayMs мс")
        }
        .onEach {
            if (_state.value.advanced.sensor !is SensorState.Running) {
                if (_state.value.advanced.sensor is SensorState.Retrying) {
                    updateAdvanced { a -> a.copy(recoveries = a.recoveries + 1) }
                    log("Приплив: з'єднання відновлено")
                }
                updateAdvanced { it.copy(sensor = SensorState.Running) }
            }
        }
        // Після вичерпання спроб потік закінчується, але flatMapLatest і shareIn лишаються живими.
        .catch { e ->
            updateAdvanced { it.copy(sensor = SensorState.Failed(e.message ?: "Помилка датчика")) }
            log("Приплив: спроби вичерпано — ${e.message}")
        }

    fun startAll() {
        stopAll()
        startCold()
        startMedium()
        session.update { (it ?: 0) + 1 }
        updateAdvanced { it.copy(sensor = SensorState.Running) }
        log("Моніторинг запущено")
    }

    fun stopAll() {
        coldJob?.cancel()
        mediumJob?.cancel()
        session.value = null
        _state.update {
            it.copy(
                basic = it.basic.copy(collecting = false),
                medium = it.medium.copy(running = false),
                advanced = it.advanced.copy(sensor = SensorState.Stopped)
            )
        }
    }

    private fun startCold() {
        coldJob = viewModelScope.launch {
            _state.update { it.copy(basic = BasicState(collecting = true)) }
            simulator.waterLevel().collect { v ->
                _state.update {
                    it.copy(basic = it.basic.copy(samples = (listOf(v) + it.basic.samples).take(8), count = it.basic.count + 1))
                }
            }
        }
    }

    private fun startMedium() {
        mediumJob = viewModelScope.launch {
            _state.update { it.copy(medium = MediumState(running = true)) }
            val raw = simulator.waterLevel(faultable = true).onEach { v ->
                _state.update {
                    val dropped = if (SensorPipeline.isPlausible(v)) 0 else 1
                    it.copy(medium = it.medium.copy(received = it.medium.received + 1, dropped = it.medium.dropped + dropped))
                }
            }
            val readings = SensorPipeline.levelReadings(raw)
                .onEach { r -> _state.update { it.copy(medium = it.medium.copy(reading = r)) } }
            SensorPipeline.stableStatus(readings, STATUS_DEBOUNCE_MS)
                .catch { e ->
                    val text = if (e is SensorFailureException) e.message else "Несподівана помилка: ${e.message}"
                    _state.update { it.copy(medium = it.medium.copy(running = false, error = text)) }
                    log("Потік рівня перервано: $text")
                }
                .collect { s -> _state.update { it.copy(medium = it.medium.copy(stableStatus = s)) } }
        }
    }

    fun setFault(enabled: Boolean) {
        fault.set(enabled)
        _state.update { it.copy(faultInjected = enabled) }
        log(if (enabled) "Збій датчиків увімкнено" else "Збій датчиків вимкнено")
    }

    fun setOutflow(value: Double) {
        outflow.value = value
        _state.update { it.copy(outflowM3s = value) }
    }

    private fun updateAdvanced(transform: (AdvancedState) -> AdvancedState) =
        _state.update { it.copy(advanced = transform(it.advanced)) }

    private fun log(message: String) {
        val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        _state.update { it.copy(log = (listOf("$time  $message") + it.log).take(40)) }
    }
}
