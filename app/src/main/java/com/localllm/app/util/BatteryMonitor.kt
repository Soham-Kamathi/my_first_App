package com.localllm.app.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Monitor for tracking battery status and power state.
 * Used to optimize inference behavior based on battery conditions.
 */
@Singleton
class BatteryMonitor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val batteryManager: BatteryManager by lazy {
        context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
    }

    /**
     * Get current battery level as percentage (0-100).
     */
    fun getBatteryLevel(): Int {
        val batteryIntent = context.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        
        return if (level >= 0 && scale > 0) {
            (level * 100 / scale)
        } else {
            -1
        }
    }

    /**
     * Check if the device is currently charging.
     */
    fun isCharging(): Boolean {
        val batteryIntent = context.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )
        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        return status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL
    }

    /**
     * Get the current charging type.
     */
    fun getChargingType(): ChargingType {
        val batteryIntent = context.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )
        val plugged = batteryIntent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1
        
        return when (plugged) {
            BatteryManager.BATTERY_PLUGGED_AC -> ChargingType.AC
            BatteryManager.BATTERY_PLUGGED_USB -> ChargingType.USB
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> ChargingType.WIRELESS
            else -> ChargingType.NONE
        }
    }

    /**
     * Get the battery temperature in degrees Celsius.
     */
    fun getBatteryTemperature(): Float {
        val batteryIntent = context.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )
        val temperature = batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) ?: -1
        return if (temperature >= 0) temperature / 10.0f else -1f
    }

    /**
     * Get the battery health status.
     */
    fun getBatteryHealth(): BatteryHealth {
        val batteryIntent = context.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )
        val health = batteryIntent?.getIntExtra(BatteryManager.EXTRA_HEALTH, -1) ?: -1
        
        return when (health) {
            BatteryManager.BATTERY_HEALTH_GOOD -> BatteryHealth.GOOD
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> BatteryHealth.OVERHEAT
            BatteryManager.BATTERY_HEALTH_DEAD -> BatteryHealth.DEAD
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> BatteryHealth.OVER_VOLTAGE
            BatteryManager.BATTERY_HEALTH_COLD -> BatteryHealth.COLD
            else -> BatteryHealth.UNKNOWN
        }
    }

    /**
     * Check if inference should be throttled based on battery conditions.
     * 
     * @param lowBatteryThreshold Battery level below which to throttle (default 20%)
     * @return true if inference should be throttled or restricted
     */
    fun shouldThrottleInference(lowBatteryThreshold: Int = 20): Boolean {
        val level = getBatteryLevel()
        val charging = isCharging()
        val health = getBatteryHealth()
        val temperature = getBatteryTemperature()
        
        // Throttle if:
        // 1. Battery is low and not charging
        // 2. Battery is overheating
        // 3. Battery temperature is too high (> 45°C)
        return (level in 0..lowBatteryThreshold && !charging) ||
                health == BatteryHealth.OVERHEAT ||
                (temperature > 45f && temperature > 0)
    }

    /**
     * Get recommended thread count based on battery state.
     * Returns a reduced count when on battery to save power.
     */
    fun getRecommendedThreadCount(maxThreads: Int): Int {
        val isCharging = isCharging()
        val batteryLevel = getBatteryLevel()
        
        return when {
            isCharging -> maxThreads
            batteryLevel > 50 -> maxOf(1, maxThreads - 1)
            batteryLevel > 20 -> maxOf(1, maxThreads / 2)
            else -> 1 // Critical battery - minimal threads
        }
    }

    /**
     * Observe battery changes as a Flow.
     */
    fun observeBatteryChanges(): Flow<BatteryState> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                trySend(getCurrentBatteryState())
            }
        }
        
        context.registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        
        // Emit initial state
        trySend(getCurrentBatteryState())
        
        awaitClose {
            context.unregisterReceiver(receiver)
        }
    }

    /**
     * Get the current battery state.
     */
    fun getCurrentBatteryState(): BatteryState {
        return BatteryState(
            level = getBatteryLevel(),
            isCharging = isCharging(),
            chargingType = getChargingType(),
            temperature = getBatteryTemperature(),
            health = getBatteryHealth()
        )
    }
}

/**
 * Charging type enumeration.
 */
enum class ChargingType {
    NONE,
    AC,
    USB,
    WIRELESS
}

/**
 * Battery health enumeration.
 */
enum class BatteryHealth {
    GOOD,
    OVERHEAT,
    DEAD,
    OVER_VOLTAGE,
    COLD,
    UNKNOWN
}

/**
 * Complete battery state data class.
 */
data class BatteryState(
    val level: Int,
    val isCharging: Boolean,
    val chargingType: ChargingType,
    val temperature: Float,
    val health: BatteryHealth
) {
    val isLow: Boolean get() = level in 0..20
    val isCritical: Boolean get() = level in 0..10
    val isOverheating: Boolean get() = temperature > 45f || health == BatteryHealth.OVERHEAT
}
