package hu.lifeos.app.satori

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import hu.lifeos.app.AppContainer
import hu.lifeos.app.data.MoodEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Egyszer megjelenő esemény (szintlépés, újrakezdés vigasztalása) a UI felé. */
sealed interface SatoriEvent {
    data class LevelUp(val stageIndex: Int) : SatoriEvent
    data class Comfort(val title: String, val text: String) : SatoriEvent
    data object ResetPulse : SatoriEvent
}

class SatoriViewModel(private val c: AppContainer) : ViewModel() {
    val content: SatoriContent = c.satori.content
    val state: StateFlow<SatoriState> = c.satori.state
    val loaded: StateFlow<Boolean> = c.satori.loaded

    val todayMood: StateFlow<MoodEntity?> = c.db.moods().observe(SatoriLogic.today())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _event = MutableStateFlow<SatoriEvent?>(null)
    val event: StateFlow<SatoriEvent?> = _event.asStateFlow()
    fun consumeEvent() { _event.value = null }

    fun setType(id: String) = c.satori.update { it.copy(type = id) }
    fun setTheme(id: String) = c.satori.update { it.copy(theme = id) }
    fun setMode(id: String) = c.satori.update { SatoriLogic.setMode(it, id) }
    fun setWeeklyAllowance(n: Int) = c.satori.update { s -> s.withCur { it.copy(weeklyAllowance = n.coerceIn(0, 7)) } }

    fun finishOnboarding(type: String, theme: String, mode: String) = c.satori.update {
        SatoriLogic.setMode(it.copy(type = type, theme = theme, onboardingDone = true), mode)
    }

    fun togglePledge() = c.satori.update { s ->
        val t = SatoriLogic.today()
        s.withCur { it.copy(pledgeDate = if (it.pledgeDate == t) null else t) }
    }

    fun closeDay(outcome: String, note: String) {
        val before = state.value
        val prevStreak = before.cur().streak
        val (next, effective) = SatoriLogic.applyDayOutcome(before, outcome, note.trim())
        c.satori.update { next }
        val newStreak = next.cur().streak
        val prevIdx = SatoriLogic.stageIndex(content, prevStreak)
        val newIdx = SatoriLogic.stageIndex(content, newStreak)
        when {
            newIdx > prevIdx -> _event.value = SatoriEvent.LevelUp(newIdx)
            effective == "sikertelen" && prevStreak > 0 -> _event.value = SatoriEvent.ResetPulse
        }
    }

    fun restart() {
        c.satori.update { SatoriLogic.restart(it) }
        val m = content.comfortMessages.random()
        _event.value = SatoriEvent.Comfort(m.getOrElse(0) { "" }, m.getOrElse(1) { "" })
    }

    fun addJournal(stateId: String, note: String) = c.satori.update { s ->
        s.withCur { it.copy(journal = listOf(JournalEntry(SatoriLogic.today(), SatoriLogic.nowTime(), stateId, note.trim(), "journal")) + it.journal) }
    }

    fun addReason(r: String) { if (r.isNotBlank()) c.satori.update { s -> s.withCur { it.copy(reasons = it.reasons + r.trim()) } } }
    fun removeReason(i: Int) = c.satori.update { s -> s.withCur { it.copy(reasons = it.reasons.filterIndexed { idx, _ -> idx != i }) } }
    fun setIfThen(p: String) = c.satori.update { s -> s.withCur { it.copy(ifThenPlan = p.trim()) } }
    fun setAlly(name: String, phone: String) = c.satori.update { it.copy(ally = Ally(name.trim(), phone.trim())) }

    fun logCraving(trigger: String) = c.satori.update {
        it.copy(cravingLog = it.cravingLog + CravingRec(SatoriLogic.today(), it.type, trigger, System.currentTimeMillis()))
    }

    fun toggleMerleg(toolId: String) = c.satori.update { s ->
        val k = SatoriLogic.today()
        val arr = s.merleg[k].orEmpty()
        s.copy(merleg = s.merleg + (k to (if (toolId in arr) arr - toolId else arr + toolId)))
    }

    /** A Satori hangulat-gombjai a közös Hangulat táblába írnak. */
    fun setMood(mood: Int?, energy: Int?) = viewModelScope.launch {
        val d = SatoriLogic.today()
        val cur = c.db.moods().get(d)
        c.db.moods().upsert(MoodEntity(d, mood ?: cur?.mood, energy ?: cur?.energy))
    }

    fun importLegacy(text: String, onResult: (Boolean) -> Unit) = viewModelScope.launch {
        onResult(runCatching { c.satori.importLegacy(text) }.isSuccess)
    }
}
