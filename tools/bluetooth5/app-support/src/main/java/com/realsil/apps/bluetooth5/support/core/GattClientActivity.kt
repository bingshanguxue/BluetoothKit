package com.realsil.apps.bluetooth5.support.core

import android.app.Activity
import android.bluetooth.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import com.realsil.sdk.core.bluetooth.GlobalGatt
import com.realsil.sdk.core.bluetooth.scanner.LeScannerPresenter
import com.realsil.sdk.core.logger.ZLogger
import com.realsil.sdk.support.base.BaseActivity

open class GattClientActivity : BaseActivity() {

    /* Bluetooth API */
    protected var mBluetoothManager: BluetoothManager? = null
    protected var mBluetoothAdapter: BluetoothAdapter? = null
    protected var mScannerPresenter: LeScannerPresenter? = null
    protected var mBluetoothGatt: BluetoothGatt? = null
    protected var mBluetoothDeviceAddress: String? = null


    override fun onDestroy() {
        super.onDestroy()

        GlobalGatt.getInstance().close(mBluetoothDeviceAddress)

        mScannerPresenter?.onDestroy()

        unregisterReceiver(mBluetoothReceiver)
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_CODE_ENABLE_BT ->                 // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    //do nothing
//                    showShortToast(R.string.rtkbt_ota_toast_bt_enabled);
                    initialize()
                } else {
                    // User did not enable Bluetooth or an error occured
//                    showShortToast(R.string.rtkbt_ota_toast_bt_not_enabled);
                    finish()
                }
            else -> {
            }
        }
    }

    open fun initialize() {
        mBluetoothManager =
            getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        mBluetoothAdapter = mBluetoothManager!!.adapter
        // We can't continue without proper Bluetooth support
        if (!checkBluetoothSupport(mBluetoothAdapter)) {
            finish()
            return
        }

        // Register for system Bluetooth events
        val filter =
            IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        registerReceiver(mBluetoothReceiver, filter)

        if (!mBluetoothAdapter!!.isEnabled) {
            ZLogger.d("Bluetooth is currently disabled...enabling")
            mBluetoothAdapter!!.enable()
        } else {
            ZLogger.d("Bluetooth enabled...starting services")
//            startAdvertising()
//            startServer()
        }
    }


    open fun updateLog(message:String) {
        ZLogger.d(message)
    }

    /**
     * Verify the level of Bluetooth support provided by the hardware.
     * @param bluetoothAdapter System [BluetoothAdapter].
     * @return true if Bluetooth is properly supported, false otherwise.
     */
    private fun checkBluetoothSupport(bluetoothAdapter: BluetoothAdapter?): Boolean {
        if (bluetoothAdapter == null) {
            ZLogger.w("Bluetooth is not supported")
            return false
        }
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            ZLogger.w("Bluetooth LE is not supported")
            return false
        }
        return true
    }

    /**
     * Listens for Bluetooth adapter events to enable/disable
     * advertising and server functionality.
     */
    private val mBluetoothReceiver: BroadcastReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(
                context: Context,
                intent: Intent
            ) {
                val state = intent.getIntExtra(
                    BluetoothAdapter.EXTRA_STATE,
                    BluetoothAdapter.STATE_OFF
                )
                when (state) {
                    BluetoothAdapter.STATE_ON -> {
//                        startAdvertising()
//                        startServer()
                    }
                    BluetoothAdapter.STATE_OFF -> {
//                        stopServer()
//                        stopAdvertising()
                    }
                    else -> {
                    }
                }
            }
        }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * `BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)`
     * callback.
     */
    fun disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            ZLogger.w("BluetoothAdapter not initialized")
            return
        }
        mBluetoothGatt?.disconnect()
    }

    /**
     * Request a read on a given `BluetoothGattCharacteristic`. The read result is reported
     * asynchronously through the `BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)`
     * callback.
     *
     * @param characteristic The characteristic to read from.
     */
    fun writeCharacteristic(characteristic: BluetoothGattCharacteristic?) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            ZLogger.w("BluetoothAdapter not initialized")
            return
        }
        mBluetoothGatt?.writeCharacteristic(characteristic)
    }


}