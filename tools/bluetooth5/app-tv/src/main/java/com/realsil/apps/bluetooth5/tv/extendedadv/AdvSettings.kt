package com.realsil.apps.bluetooth5.tv.extendedadv

import android.bluetooth.BluetoothDevice
import android.bluetooth.le.AdvertisingSetParameters
import android.content.Context
import android.text.TextUtils
import com.realsil.sdk.core.preference.BaseSharedPrefes
import com.realsil.sdk.core.logger.ZLogger


/**
 * @author bingshanguxue
 * @date 13/08/2017
 */

class AdvSettings private constructor(context: Context) : BaseSharedPrefes(context) {

    val isAdvertisingDeviceNameEnabled: Boolean
        get() {
            if (!contains(KEY_ADV_ADVERTISING_DEVICE_NAME)) {
                set(KEY_ADV_ADVERTISING_DEVICE_NAME, false)
                return false
            }

            return getBoolean(KEY_ADV_ADVERTISING_DEVICE_NAME, false)
        }

    val isAdvertisingTxPowerLevelEnabled: Boolean
        get() {
            if (!contains(KEY_ADV_ADVERTISING_TX_POWER_LEVEL)) {
                set(KEY_ADV_ADVERTISING_TX_POWER_LEVEL, false)
                return false
            }

            return getBoolean(KEY_ADV_ADVERTISING_TX_POWER_LEVEL, false)
        }

    val isScanResponseDeviceNameEnabled: Boolean
        get() {
            if (!contains(KEY_SCAN_RESPONSE_DEVICE_NAME)) {
                set(KEY_SCAN_RESPONSE_DEVICE_NAME, false)
                return false
            }

            return getBoolean(KEY_SCAN_RESPONSE_DEVICE_NAME, false)
        }
    val isScanResponseTxPowerLevelEnabled: Boolean
        get() {
            if (!contains(KEY_SCAN_RESPONSE_TX_POWER_LEVEL)) {
                set(KEY_SCAN_RESPONSE_TX_POWER_LEVEL, false)
                return false
            }

            return getBoolean(KEY_SCAN_RESPONSE_TX_POWER_LEVEL, false)
        }

    val nameFilter: String
        get() {
            return getString(KEY_ADV_SCAN_FILTER_NAME, "")
        }

    val advSetPrimaryPhy: Int
        get() {
            val value = getString(KEY_ADV_SET_PRIMARY_PHY, null)
            if (TextUtils.isEmpty(value)) {
                set(KEY_ADV_SET_PRIMARY_PHY, BluetoothDevice.PHY_LE_1M.toString())
                return BluetoothDevice.PHY_LE_1M
            } else {
                return Integer.parseInt(value)
            }
        }

    val advSetSecondaryPhy: Int
        get() {
            val value = getString(KEY_ADV_SET_SECONDARY_PHY, null)
            if (TextUtils.isEmpty(value)) {
                set(KEY_ADV_SET_SECONDARY_PHY, BluetoothDevice.PHY_LE_1M.toString())
                return BluetoothDevice.PHY_LE_1M
            } else {
                return Integer.parseInt(value)
            }
        }

    val advSetInterval: Int
        get() {
            val value = getString(KEY_ADV_SET_INTERVAL, null)
            if (TextUtils.isEmpty(value)) {
                set(KEY_ADV_SET_INTERVAL, AdvertisingSetParameters.INTERVAL_LOW.toString())
                return AdvertisingSetParameters.INTERVAL_LOW
            } else {
                return Integer.parseInt(value)
            }
        }

    val advSetTxPowerLevel: Int
        get() {
            val value = getString(KEY_ADV_SET_TX_POWER_LEVEL, null)
            if (TextUtils.isEmpty(value)) {
                set(
                    KEY_ADV_SET_TX_POWER_LEVEL,
                    AdvertisingSetParameters.TX_POWER_MEDIUM.toString()
                )
                return AdvertisingSetParameters.TX_POWER_MEDIUM
            } else {
                return Integer.parseInt(value)
            }
        }


    init {
        ZLogger.v("isAdvertisingDeviceNameEnabled:$isAdvertisingDeviceNameEnabled, isAdvertisingTxPowerLevelEnabled:$isAdvertisingTxPowerLevelEnabled")
        ZLogger.v("isScanResponseDeviceNameEnabled:$isScanResponseDeviceNameEnabled, isScanResponseTxPowerLevelEnabled:$isScanResponseTxPowerLevelEnabled")
        ZLogger.v("advSetPrimaryPhy:$advSetPrimaryPhy, advSetSecondaryPhy:$advSetSecondaryPhy")
        ZLogger.v("advSetInterval:$advSetInterval, advSetTxPowerLevel:$advSetTxPowerLevel")
        ZLogger.v("nameFilter:$nameFilter")
    }

    companion object {
        val KEY_ADV_SCAN_FILTER_NAME = "rtk_edittext_adv_scan_filter_name"
        private val KEY_ADV_SET_PRIMARY_PHY = "rtk_adv_set_primary_phy"
        private val KEY_ADV_SET_SECONDARY_PHY = "rtk_adv_set_secondary_phy"
        private val KEY_ADV_SET_INTERVAL = "rtk_adv_set_interval"
        private val KEY_ADV_SET_TX_POWER_LEVEL = "rtk_adv_set_tx_power_level"
        private val KEY_ADV_ADVERTISING_DEVICE_NAME = "switch_adv_advertising_device_name"
        private val KEY_ADV_ADVERTISING_TX_POWER_LEVEL = "switch_adv_advertising_tx_power_level"
        private val KEY_SCAN_RESPONSE_DEVICE_NAME = "switch_adv_scan_response_device_name"
        private val KEY_SCAN_RESPONSE_TX_POWER_LEVEL = "switch_adv_scan_response_tx_power_level"

        @Volatile
        private var instance: AdvSettings? = null

        fun initialize(context: Context) {
            if (instance == null) {
                synchronized(AdvSettings::class.java) {
                    if (instance == null) {
                        instance =
                            AdvSettings(context.applicationContext)
                    }
                }
            }
        }

        fun getInstance(): AdvSettings? {
            if (instance == null) {
                ZLogger.w("not initialized, please call initialize(Context context) first")
            }
            return instance
        }
    }

}
