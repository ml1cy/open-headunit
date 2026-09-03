package com.andrerinas.openheadunit.newui.vehicle

import kotlinx.coroutines.flow.StateFlow

data class TyrePressures(
    val frontLeftBar: Float = 0f,
    val frontRightBar: Float = 0f,
    val rearLeftBar: Float = 0f,
    val rearRightBar: Float = 0f,
    val warnBelowBar: Float = 2.0f,
)

data class VehicleSnapshot(
    val connected: Boolean = false,
    val speedKmh: Int = 0,
    val speedPercent: Float = 0f,
    val speedNote: String = "Not connected",
    val coolantC: Int = 0,
    val coolantPercent: Float = 0f,
    val batteryV: Float = 0f,
    val batteryPercent: Float = 0f,
    val batteryNote: String = "",
    val fuelPercent: Int = 0,
    val rangeKm: Int = 0,
    val tyres: TyrePressures = TyrePressures(),
    val tripBKm: Float = 0f,
    val tripBLPer100km: Float = 0f,
    val tripBDuration: String = "0:00 moving",
    val sourceNote: String = "OBD-II adapter not connected",
)

/**
 * Real ELM327 AT-command client over Bluetooth SPP (see [Elm327VehicleRepository]), polling
 * standard PIDs (010C RPM, 010D speed, 0105 coolant, 012F fuel level, 0142 module voltage) at
 * 2-5Hz per ANDROID_IMPLEMENTATION.md. Tyre pressure has no standard OBD PID (vehicle-specific or
 * a separate TPMS receiver, per the same doc) and Trip B is accumulated/persisted by the app
 * itself from odometer deltas, not read from the vehicle — both are clearly marked as such in the
 * snapshot rather than presented as live sensor reads.
 */
interface VehicleRepository {
    val state: StateFlow<VehicleSnapshot>
    fun connect()
    fun disconnect()
    fun resetTripB()
}
