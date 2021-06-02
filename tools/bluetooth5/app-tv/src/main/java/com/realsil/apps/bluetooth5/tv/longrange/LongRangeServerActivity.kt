package com.realsil.apps.bluetooth5.tv.longrange

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.os.Bundle
import android.os.ParcelUuid
import android.view.View
import com.realsil.apps.bluetooth5.tv.R
import com.realsil.apps.bluetooth5.support.core.GattServerActivity
import com.realsil.apps.bluetooth5.support.longrange.DataProvider
import com.realsil.apps.bluetooth5.support.longrange.LongRangeProfile
import com.realsil.sdk.core.logger.ZLogger
import com.realsil.sdk.core.utility.DataConverter
import com.realsil.sdk.support.logger.LogView
import kotlinx.android.synthetic.main.tv_activity_longrange_transmitter.*
import java.util.*

class LongRangeServerActivity : GattServerActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.tv_activity_longrange_transmitter)

        toolbar.setTitle(R.string.title_longrange)
        setSupportActionBar(toolbar)
        if (supportActionBar != null) {
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        }
        toolbar.setNavigationOnClickListener(
            View.OnClickListener { v: View? -> onBackPressed() })

        btnEnableAdv.setOnClickListener {
            startAdvertising()
        }
        btnStopAdv.setOnClickListener {
            stopAdvertising()
        }
        btnStartServer.setOnClickListener {
            startServer()
        }

        btnStartTx.setOnClickListener {
            startTx()
        }

        btnStopTx.setOnClickListener {
            stopTx()
        }

        logView.configure(
            LogView.LogConfigure.Builder()
                .autoClearEnabled(true)
                .autoClearLineNumber(30)
                .build())

        btnEnableAdv.visibility = View.GONE
        btnStopAdv.visibility = View.GONE
        btnStartServer.visibility = View.GONE

        initialize()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopTx()
//        syncExecutor?.shutdown()
    }

    override fun updateLog(message: String) {
        super.updateLog(message)
        runOnUiThread {
            logView.d("\r\n" + message)
        }
    }

    override fun startAdvertising() {
        val settings =
            AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
                .setConnectable(true)
                .setTimeout(0)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
                .build()
        val data = AdvertiseData.Builder()
            .setIncludeDeviceName(true)
            .setIncludeTxPowerLevel(false)
            .addServiceUuid(ParcelUuid(LongRangeProfile.DATA_TRANSMIT_SERVICE))
//            .addServiceData(ParcelUuid(LongRangeProfile.DATA_TRANSMIT_SERVICE), "1234".toByteArray())
            .build()

        startAdvertising(settings, data)
    }

    /**
     * Initialize the GATT server instance with the services/characteristics
     * from the Time Profile.
     */
    override fun startServer(): Boolean {
        val ret = super.startServer()
        if (!ret) {
            return false
        }
        addService(LongRangeProfile.createTimeService())

        // Initialize the local UI
//        updateLocalUi(System.currentTimeMillis())
        return true
    }

    override fun processConnectionStateChange() {
        super.processConnectionStateChange()
        if (isExistConnectedDevice()) {
            runOnUiThread {
                if (stopped) {
                    btnStartTx.isEnabled = true
                    btnStopTx.isEnabled = false
                } else {
                    btnStartTx.isEnabled = false
                    btnStopTx.isEnabled = true
                }
            }
        } else {
            runOnUiThread {
                stopTx()
            }
        }
    }

    override fun processCharacteristicReadRequest(
        device: BluetoothDevice,
        requestId: Int,
        offset: Int,
        characteristic: BluetoothGattCharacteristic
    ): Boolean {
        super.processCharacteristicReadRequest(device, requestId, offset, characteristic)

//        val now = System.currentTimeMillis()
//        if (LongRangeProfile.CURRENT_TIME == characteristic.uuid) {
//            ZLogger.i("Read CurrentTime")
//            sendResponse(
//                device,
//                requestId,
//                BluetoothGatt.GATT_SUCCESS,
//                0,
//                LongRangeProfile.getExactTime(
//                    now,
//                    LongRangeProfile.ADJUST_NONE
//                )
//            )
//            return true
//        }
//        else if (LongRangeProfile.LOCAL_TIME_INFO == characteristic.uuid) {
//            ZLogger.i("Read LocalTimeInfo")
//            sendResponse(
//                device,
//                requestId,
//                BluetoothGatt.GATT_SUCCESS,
//                0,
//                TimeProfile.getLocalTimeInfo(now)
//            )
//            return true
//        } else {
        return false
//        }
    }

    override fun processCharacteristicWriteRequest(
        device: BluetoothDevice?,
        requestId: Int,
        characteristic: BluetoothGattCharacteristic?,
        preparedWrite: Boolean,
        responseNeeded: Boolean,
        offset: Int,
        value: ByteArray?
    ): Boolean {
        super.processCharacteristicWriteRequest(
            device,
            requestId,
            characteristic,
            preparedWrite,
            responseNeeded,
            offset,
            value
        )

        if (characteristic != null) {
            if (LongRangeProfile.RX_CHARACTERISTIC == characteristic.uuid) {
                device?.let {
                    sendResponse(
                        it,
                        requestId,
                        BluetoothGatt.GATT_SUCCESS,
                        0,
                        value
                    )
                    runOnUiThread {
                        logView.i(">> ${device.address} - ${characteristic.uuid} ${DataConverter.bytes2Hex(value)}")
                    }
//                    updateLog(">> ${device.address} - ${characteristic.uuid} ${DataConverter.bytes2Hex(value)}")
                }
                return true
            }
        }
        return false
    }

    override fun processDescriptorWriteRequest(
        device: BluetoothDevice,
        requestId: Int,
        descriptor: BluetoothGattDescriptor,
        preparedWrite: Boolean,
        responseNeeded: Boolean,
        offset: Int,
        value: ByteArray
    ): Boolean {
        super.processDescriptorWriteRequest(
            device,
            requestId,
            descriptor,
            preparedWrite,
            responseNeeded,
            offset,
            value
        )

        if (LongRangeProfile.CLIENT_CONFIG == descriptor.uuid) {
            if (Arrays.equals(
                    BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE,
                    value
                )
            ) {
                ZLogger.d("Subscribe device to notifications: $device")
                registerClient(device)
            } else if (Arrays.equals(
                    BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE,
                    value
                )
            ) {
                ZLogger.d("Unsubscribe device from notifications: $device")
                unregisterClient(device)
            }

            if (responseNeeded) {
                sendResponse(
                    device,
                    requestId,
                    BluetoothGatt.GATT_SUCCESS,
                    0,
                    null
                )
            }
            return true
        } else {
            return false
        }
    }

    override fun processDescriptorReadRequest(
        device: BluetoothDevice,
        requestId: Int,
        offset: Int,
        descriptor: BluetoothGattDescriptor
    ): Boolean {
        super.processDescriptorReadRequest(device, requestId, offset, descriptor)

        if (LongRangeProfile.CLIENT_CONFIG == descriptor.uuid) {
            ZLogger.d("Config descriptor read")
            val returnValue: ByteArray = if (isClientRegistered(device)) {
                BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            } else {
                BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
            }
            ZLogger.v("returnValue=${DataConverter.bytes2Hex(returnValue)}")
            sendResponse(
                device,
                requestId,
                BluetoothGatt.GATT_SUCCESS,
                0,
                returnValue
            )
            return true
        } else {
            return false
        }
    }


    private var stopped = true
    var seq = 1

    private fun startTx() {
        updateLog("TX start")
        stopped = false
        seq = 1;

        btnStartTx.isEnabled = false
        btnStopTx.isEnabled = true

        Thread(Runnable {
            while (!stopped) {
//                ZLogger.v("stopped=" + stopped)
                if (seq >= 0xFFFF) {
                    seq = 1
                }
                val data = DataProvider.generateStreamWithSeq(seq, 0, 100)
                sendData(data)
                seq++
            }
        }).start()
    }

    private fun stopTx() {
        stopped = true
        btnStartTx.isEnabled = true
        btnStopTx.isEnabled = false
        ZLogger.v("pending to stop")
    }

    private fun sendData(data: ByteArray): Boolean {
        val characteristic = getService(LongRangeProfile.DATA_TRANSMIT_SERVICE)
            ?.getCharacteristic(LongRangeProfile.TX_CHARACTERISTIC)
            ?: return false

        characteristic.value = data
        return notifyCharacteristicChanged(characteristic, false)
    }


}