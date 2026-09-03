package com.andrerinas.openheadunit.newui.profiles

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ActiveProfileInfo(val id: String, val name: String, val initial: String)

data class DriverProfile(
    val id: String,
    val name: String,
    val initial: String,
    val seat: String,
    val volume: Int,
    val themeIsNight: Boolean,
    val chips: List<String>,
    val lastDriveText: String,
    val isActive: Boolean,
)

/** Read-only view used by the shell (profile chip, drawer) so it doesn't depend on the full profiles feature. */
interface ActiveProfileRepository {
    val active: StateFlow<ActiveProfileInfo>
}

/** Real, Room-backed driver profiles (see ProfileDatabase). Switching applies volume/theme for real. */
interface ProfileRepository : ActiveProfileRepository {
    val profiles: StateFlow<List<DriverProfile>>
    fun switchTo(id: String)
}

/** Single built-in profile until [ProfileRepository] (Room-backed) is wired in — keeps the app usable standalone. */
class SingleProfileRepository(name: String = "Driver") : ProfileRepository {
    private val info = ActiveProfileInfo(id = "default", name = name, initial = name.take(1).uppercase())
    override val active: StateFlow<ActiveProfileInfo> = MutableStateFlow(info).asStateFlow()
    override val profiles: StateFlow<List<DriverProfile>> = MutableStateFlow(
        listOf(
            DriverProfile(
                id = info.id, name = info.name, initial = info.initial, seat = "Seat 1",
                volume = 62, themeIsNight = true, chips = listOf("Default seat"),
                lastDriveText = "No saved history", isActive = true,
            ),
        ),
    ).asStateFlow()

    override fun switchTo(id: String) = Unit
}
