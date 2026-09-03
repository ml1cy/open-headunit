package com.andrerinas.openheadunit.newui

import android.content.Context
import com.andrerinas.openheadunit.App
import com.andrerinas.openheadunit.connection.CommManager
import com.andrerinas.openheadunit.newui.media.DefaultNowPlayingRepository
import com.andrerinas.openheadunit.newui.media.NowPlayingRepository
import com.andrerinas.openheadunit.newui.navigation.NavRepository
import com.andrerinas.openheadunit.newui.profiles.ProfileRepository
import com.andrerinas.openheadunit.newui.radio.RadioRepository
import com.andrerinas.openheadunit.newui.telephony.PhoneRepository
import com.andrerinas.openheadunit.newui.vehicle.VehicleRepository
import com.andrerinas.openheadunit.utils.SUExecutor
import com.andrerinas.openheadunit.utils.Settings

/**
 * Manual DI holder for the new UI, one per process (mirrors the existing [App]/`AppComponent`
 * pattern — this project doesn't use Hilt/Dagger). Each repository is real, not mocked; see the
 * individual implementation files for what hardware/protocol each one actually speaks, and their
 * interface KDoc for the platform limitations that shaped them (esp. telephony and radio).
 */
class HuContainer private constructor(private val appContext: Context) {

    val settings: Settings get() = App.provide(appContext).settings
    val suExecutor: SUExecutor get() = App.provide(appContext).suExecutor
    val commManager: CommManager get() = App.provide(appContext).commManager

    val nowPlayingRepository: NowPlayingRepository by lazy { DefaultNowPlayingRepository() }

    val profileRepository: ProfileRepository by lazy {
        com.andrerinas.openheadunit.newui.profiles.RoomProfileRepository(appContext)
    }

    val radioRepository: RadioRepository by lazy {
        com.andrerinas.openheadunit.newui.radio.SerialRadioRepository(appContext, nowPlayingRepository)
    }

    val vehicleRepository: VehicleRepository by lazy {
        com.andrerinas.openheadunit.newui.vehicle.Elm327VehicleRepository(appContext).also { it.connect() }
    }

    val phoneRepository: PhoneRepository by lazy {
        com.andrerinas.openheadunit.newui.telephony.CallLogPhoneRepository(appContext)
    }

    val navRepository: NavRepository by lazy {
        com.andrerinas.openheadunit.newui.navigation.IntentNavRepository(appContext)
    }

    companion object {
        @Volatile private var instance: HuContainer? = null

        fun get(context: Context): HuContainer =
            instance ?: synchronized(this) {
                instance ?: HuContainer(context.applicationContext).also { instance = it }
            }
    }
}
