package com.realsil.apps.bluetooth5.phone.longrange

import android.bluetooth.BluetoothDevice
import android.bluetooth.le.AdvertisingSetParameters
import android.content.Context
import android.text.TextUtils
import com.realsil.sdk.core.logger.ZLogger
import com.realsil.sdk.core.preference.BaseSharedPrefes


/**
 * @author bingshanguxue
 * @date 13/08/2017
 */

class LongRangeSettings private constructor(context: Context) : BaseSharedPrefes(context) {

    val nameFilter: String
        get() {
            return getString(KEY_LONGRANGE_SCAN_FILTER_NAME, "")
        }

    val isLongRangeClientLoopbackEnabled: Boolean
        get() {
            if (!contains(KEY_BT_LONGRANGE_CLIENT_LOOPBACK)) {
                set(KEY_BT_LONGRANGE_CLIENT_LOOPBACK, false)
                return false
            }

            return getBoolean(KEY_BT_LONGRANGE_CLIENT_LOOPBACK, false)
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
        ZLogger.v("isLongRangeClientLoopbackEnabled:$isLongRangeClientLoopbackEnabled")
        ZLogger.v("advSetPrimaryPhy:$advSetPrimaryPhy, advSetSecondaryPhy:$advSetSecondaryPhy")
        ZLogger.v("advSetInterval:$advSetInterval, advSetTxPowerLevel:$advSetTxPowerLevel")
        ZLogger.v("nameFilter:$nameFilter")
    }

    companion object {
        private val KEY_BT_LONGRANGE_CLIENT_LOOPBACK = "switch_bt_longrange_client_loopback"
        private val KEY_UPLOAD_FILE_PROMPT = "switch_dfu_upload_file_prompt"
        private val KEY_BANK_LINK = "switch_dfu_backlink"
        private val KEY_DFU_SUCCESS_HINT = "switch_dfu_success_hint"
        private val KEY_DFU_FIXED_IMAGE_FILE = "switch_dfu_fixed_image_file"
        val KEY_LONGRANGE_SCAN_FILTER_NAME = "rtk_edittext_longrange_scan_filter_name"
        private val KEY_ADV_SET_PRIMARY_PHY = "rtk_adv_set_primary_phy"
        private val KEY_ADV_SET_SECONDARY_PHY = "rtk_adv_set_secondary_phy"
        private val KEY_ADV_SET_INTERVAL = "rtk_adv_set_interval"
        private val KEY_ADV_SET_TX_POWER_LEVEL = "rtk_adv_set_tx_power_level"

        @Volatile
        private var instance: LongRangeSettings? = null

        fun initialize(context: Context) {
            if (instance == null) {
                synchronized(LongRangeSettings::class.java) {
                    if (instance == null) {
                        instance =
                            LongRangeSettings(context.applicationContext)
                    }
                }
            }
        }

        fun getInstance(): LongRangeSettings? {
            if (instance == null) {
                ZLogger.w("not initialized, please call initialize(Context context) first")
            }
            return instance
        }
    }

}
