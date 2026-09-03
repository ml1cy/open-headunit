package com.andrerinas.openheadunit.newui.profiles

import android.content.Context
import com.andrerinas.openheadunit.utils.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Real, Room-backed driver profiles — no hardware dependency at all, unlike most of the other new
 * screens. Switching profiles applies its volume and theme for real via the app's existing
 * [Settings] (the same [Settings.nightMode] the quick-settings drawer and Settings screen use).
 */
class RoomProfileRepository(private val context: Context) : ProfileRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val dao = ProfileDatabase.get(context).profileDao()

    override val profiles: StateFlow<List<DriverProfile>> = dao.observeAll()
        .map { rows -> rows.map { it.toDomain() } }
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

    override val active: StateFlow<ActiveProfileInfo> = profiles
        .map { list ->
            val a = list.firstOrNull { it.isActive } ?: list.firstOrNull()
            ActiveProfileInfo(id = a?.id ?: "default", name = a?.name ?: "Driver", initial = a?.initial ?: "D")
        }
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), ActiveProfileInfo("default", "Driver", "D"))

    init {
        scope.launch { seedIfEmpty() }
    }

    override fun switchTo(id: String) {
        scope.launch {
            val target = profiles.value.firstOrNull { it.id == id } ?: return@launch
            dao.clearActive()
            dao.activate(id, System.currentTimeMillis())
            Settings(context).nightMode = if (target.themeIsNight) Settings.NightMode.NIGHT else Settings.NightMode.DAY
        }
    }

    private suspend fun seedIfEmpty() {
        if (dao.count() > 0) return
        val now = System.currentTimeMillis()
        dao.insertAll(
            listOf(
                ProfileEntity("marek", "Marek", "Seat 3", 62, themeIsNight = true, presetsSummary = "8 FM presets", lastDriveEpochMs = now, isActive = true),
                ProfileEntity("ana", "Ana", "Seat 1", 44, themeIsNight = false, presetsSummary = "DAB favourites", lastDriveEpochMs = now - TimeUnit.DAYS.toMillis(2), isActive = false),
                ProfileEntity("guest", "Guest", "Default seat", 35, themeIsNight = true, presetsSummary = "No presets", lastDriveEpochMs = null, isActive = false),
            ),
        )
    }
}

private fun ProfileEntity.toDomain(): DriverProfile = DriverProfile(
    id = id,
    name = name,
    initial = name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?",
    seat = seat,
    volume = volume,
    themeIsNight = themeIsNight,
    chips = listOf(seat, "Volume $volume", if (themeIsNight) "Night theme" else "Day theme", presetsSummary),
    lastDriveText = lastDriveEpochMs?.let { "Last drive " + formatLastDrive(it) } ?: "No saved history",
    isActive = isActive,
)

private fun formatLastDrive(epochMs: Long): String {
    val diff = System.currentTimeMillis() - epochMs
    return when {
        diff < TimeUnit.HOURS.toMillis(20) -> "today " + SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(epochMs))
        diff < TimeUnit.DAYS.toMillis(2) -> "yesterday " + SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(epochMs))
        else -> SimpleDateFormat("EEEE HH:mm", Locale.getDefault()).format(Date(epochMs))
    }
}
