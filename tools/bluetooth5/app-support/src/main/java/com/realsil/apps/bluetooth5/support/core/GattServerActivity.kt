package com.realsil.apps.bluetooth5.support.core

import android.app.Activity
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresApi
import com.realsil.sdk.core.logger.ZLogger
import com.realsil.sdk.core.utility.DataConverter
import com.realsil.sdk.support.base.BaseActivity
import java.util.*

open class GattServerActivity : BaseActivity() {

    /* Bluetooth API */
    protected var mBluetoothManager: BluetoothManager? = null
    protected var mBluetoothAdapter: BluetoothAdapter? = null
    private var mBluetoothGattServer: BluetoothGattServer? = null
    protected var mBluetoothLeAdvertiser: BluetoothLeAdvertiser? = null

    protected var scanResponseId = 0x00

    /* Collection of notification subscribers */
    private val mConnectedDevices: MutableSet<BluetoothDevice> =
        HashSet()
    /* Collection of notification subscribers */
    private val mRegisteredDevices: MutableSet<BluetoothDevice> =
        HashSet()

    override fun onDestroy() {
        super.onDestroy()
        val bluetoothAdapter = mBluetoothManager!!.adapter
        if (bluetoothAdapter.isEnabled) {
            stopServer()
            stopAdvertising()
        }
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

        mBluetoothLeAdvertiser = mBluetoothAdapter!!.bluetoothLeAdvertiser
        if (mBluetoothLeAdvertiser == null) {
            ZLogger.w("Bluetooth LE advertising not supported")
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

            refresh()

            startAdvertising()
            startServer()
        }
    }

    open fun refresh() {

    }

    open fun startAdvertising() {

    }

    fun startAdvertising(settings: AdvertiseSettings, data: AdvertiseData): Boolean {
        if (!mBluetoothAdapter?.isEnabled!!) {
            ZLogger.w("bt is off")
            return false
        }

        mBluetoothLeAdvertiser!!
            .startAdvertising(settings, data, mAdvertiseCallback)
        return true
    }

    fun startAdvertising(
        settings: AdvertiseSettings,
        data: AdvertiseData,
        scanResponse: AdvertiseData
    ): Boolean {
        mBluetoothLeAdvertiser!!
            .startAdvertising(settings, data, scanResponse, mAdvertiseCallback)
        return true
    }

    /**
     * Stop Bluetooth advertisements.
     */
    fun stopAdvertising():Boolean {
        mBluetoothLeAdvertiser?.stopAdvertising(mAdvertiseCallback)
        return true
    }

    open fun processAdvertiseStartSuccess() {

    }

    open fun processAdvertiseStartFailure(errorCode: Int) {

    }

    /**
     * Callback to receive information about the advertisement process.
     */
    private val mAdvertiseCallback: AdvertiseCallback =
        object : AdvertiseCallback() {
            override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
                if (settingsInEffect != null) {
                    ZLogger.d("LE Advertise Started.\n$settingsInEffect")
                } else {
                    ZLogger.d("LE Advertise Started.")
                }
                processAdvertiseStartSuccess()
            }

            override fun onStartFailure(errorCode: Int) {
                //setting advertise data failed, status: 18
                ZLogger.w("LE Advertise Failed: $errorCode")
                when (errorCode) {
                    ADVERTISE_FAILED_DATA_TOO_LARGE -> showAlertMessage(
                        "ADVERTISE_FAILED_DATA_TOO_LARGE"
                    )
                    ADVERTISE_FAILED_TOO_MANY_ADVERTISERS -> showAlertMessage(
                        "ADVERTISE_FAILED_TOO_MANY_ADVERTISERS"
                    )
                    ADVERTISE_FAILED_ALREADY_STARTED -> showAlertMessage(
                        "ADVERTISE_FAILED_ALREADY_STARTED"
                    )
                    ADVERTISE_FAILED_INTERNAL_ERROR -> showAlertMessage(
                        "ADVERTISE_FAILED_INTERNAL_ERROR"
                    )
                    ADVERTISE_FAILED_FEATURE_UNSUPPORTED -> showAlertMessage(
                        "ADVERTISE_FAILED_FEATURE_UNSUPPORTED"
                    )
                    else -> showAlertMessage("startAdvertising failed with unknown error $errorCode")
                }
                processAdvertiseStartFailure(errorCode)
            }
        }


    protected var mAdvertisingSetParameters: AdvertisingSetParameters.Builder? = null
    protected var currentAdvertisingSet: AdvertisingSet? = null

    /**
     * Wait for onAdvertisingEnabled callback...
     * */
    fun setAdvertisingEnabled(enabled: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            currentAdvertisingSet?.enableAdvertising(enabled, 0, 0)
        }
    }

    /**
     * Wait for onAdvertisingParametersUpdated callback...
     * */
    fun setAdvertisingParameters() {
        currentAdvertisingSet?.setAdvertisingParameters(
            mAdvertisingSetParameters?.setTxPowerLevel(
                AdvertisingSetParameters.TX_POWER_LOW
            )?.build()
        )
    }

    /**
     * AdvertisingSetParameters parameters
     * W/System.err: java.lang.IllegalStateException: Advertising can't be both connectable and scannable
     */
    fun startAdvertisingSet(
        parameters: AdvertisingSetParameters?,
        advertiseData: AdvertiseData?,
        scanResponse: AdvertiseData?,
        periodicParameters: PeriodicAdvertisingParameters?,
        periodicData: AdvertiseData?
    ) {
        try {
            mBluetoothLeAdvertiser?.startAdvertisingSet(
                parameters,
                advertiseData,
                scanResponse,
                periodicParameters,
                periodicData,
                mAdvertisingSetCallback
            )
        } catch (e: Exception) {
            ZLogger.e(e.toString())
            showShortToast(e.message)
        }
    }

    /**
     * When done with the advertising:
     * */
    fun stopAdvertisingSet() {
        mBluetoothLeAdvertiser?.stopAdvertisingSet(mAdvertisingSetCallback)
    }

    open fun updateLog(message: String) {
    }
    open fun logw(message: String) {
    }


    open fun processAdvertisingSetStartFailed() {
    }

    open fun processAdvertisingSetStarted() {
        scanResponseId = 0x00
    }

    open fun processAdvertisingSetStopped() {
    }

    private val mAdvertisingSetCallback: AdvertisingSetCallback =
        @RequiresApi(Build.VERSION_CODES.O)
        object : AdvertisingSetCallback() {
            override fun onAdvertisingSetStarted(
                advertisingSet: AdvertisingSet?,
                txPower: Int,
                status: Int
            ) {
                super.onAdvertisingSetStarted(advertisingSet, txPower, status)
                updateLog("onAdvertisingSetStarted(): txPower:$txPower , status: $status")

                if (status != ADVERTISE_SUCCESS) {
                    // 1加6
//                    E/bt_stack: [ERROR:btm_ble_multi_adv.cc(522)] setting advertise data failed, status: 18
                    // Huawei Mate 20
//                    E/bt_stack: [ERROR:btm_ble_multi_adv.cc(560)] setting periodic parameters failed, status: 18
                    processAdvertisingSetStartFailed()
                    return
                }
                currentAdvertisingSet = advertisingSet
                processAdvertisingSetStarted()
                // After onAdvertisingSetStarted callback is called, you can modify the
                // advertising data and scan response data:
            }

            override fun onAdvertisingSetStopped(advertisingSet: AdvertisingSet?) {
                super.onAdvertisingSetStopped(advertisingSet)
                updateLog("onAdvertisingSetStopped()")
                processAdvertisingSetStopped()
            }

            override fun onAdvertisingEnabled(
                advertisingSet: AdvertisingSet?,
                enable: Boolean,
                status: Int
            ) {
                super.onAdvertisingEnabled(advertisingSet, enable, status)
                updateLog("onAdvertisingEnabled(): status:$status")
            }

            override fun onAdvertisingDataSet(
                advertisingSet: AdvertisingSet?,
                status: Int
            ) {
                super.onAdvertisingDataSet(advertisingSet, status)
                updateLog("onAdvertisingDataSet(): status:$status")
            }

            override fun onScanResponseDataSet(
                advertisingSet: AdvertisingSet?,
                status: Int
            ) {
                super.onScanResponseDataSet(advertisingSet, status)
                if (status == AdvertisingSetCallback.ADVERTISE_SUCCESS) {
                    updateLog("onScanResponseDataSet(): seqId=$scanResponseId, status:$status")
                    scanResponseId++
                } else {
                    // 12: data exceed
                    logw("onScanResponseDataSet(): seqId=$scanResponseId, status:$status")
                }

//                val periodicParameters = (PeriodicAdvertisingParameters.Builder())
//                    .setInterval(AdvertisingSetParameters.INTERVAL_MEDIUM)
//                    .setIncludeTxPower(true)
//                currentAdvertisingSet?.setPeriodicAdvertisingParameters(
//                    periodicParameters.build())
            }

            override fun onAdvertisingParametersUpdated(
                advertisingSet: AdvertisingSet?,
                txPower: Int,
                status: Int
            ) {
                super.onAdvertisingParametersUpdated(advertisingSet, txPower, status)
                updateLog(
                    String.format(
                        Locale.US,
                        "onAdvertisingParametersUpdated(): txPower: %d, status:%d",
                        txPower,
                        status
                    )
                )
            }

            override fun onPeriodicAdvertisingParametersUpdated(
                advertisingSet: AdvertisingSet?,
                status: Int
            ) {
                super.onPeriodicAdvertisingParametersUpdated(advertisingSet, status)
                updateLog("onPeriodicAdvertisingParametersUpdated(): status:$status")
//                currentAdvertisingSet?.enableAdvertising(true, 0, 0)
                // Wait for onAdvertisingEnabled callback...
            }

            override fun onPeriodicAdvertisingDataSet(
                advertisingSet: AdvertisingSet?,
                status: Int
            ) {
                super.onPeriodicAdvertisingDataSet(advertisingSet, status)
                updateLog("onPeriodicAdvertisingDataSet(): status:$status")
            }

            override fun onPeriodicAdvertisingEnabled(
                advertisingSet: AdvertisingSet?,
                enable: Boolean,
                status: Int
            ) {
                super.onPeriodicAdvertisingEnabled(advertisingSet, enable, status)
                updateLog("onPeriodicAdvertisingEnabled(): enable: $enable, status:$status")
            }
        }

    /**
     * Initialize the GATT server instance with the services/characteristics
     * from the Time Profile.
     */
    open fun startServer(): Boolean {
        mBluetoothGattServer = mBluetoothManager!!.openGattServer(this, mGattServerCallback)
        if (mBluetoothGattServer == null) {
            ZLogger.w("Unable to create GATT server")
            return false
        }
        return true
    }

    /**
     * Shut down the GATT server.
     */
    open fun stopServer() {
        if (mBluetoothGattServer == null) {
            return
        }
        mBluetoothGattServer?.close()
    }

    open fun processConnectionStateChange() {

    }

    open fun processCharacteristicReadRequest(
        device: BluetoothDevice, requestId: Int, offset: Int,
        characteristic: BluetoothGattCharacteristic
    ): Boolean {
        return false
    }

    open fun processCharacteristicWriteRequest(
        device: BluetoothDevice?,
        requestId: Int,
        characteristic: BluetoothGattCharacteristic?,
        preparedWrite: Boolean,
        responseNeeded: Boolean,
        offset: Int,
        value: ByteArray?
    ): Boolean {
        return false
    }

    open fun processDescriptorWriteRequest(
        device: BluetoothDevice, requestId: Int,
        descriptor: BluetoothGattDescriptor,
        preparedWrite: Boolean, responseNeeded: Boolean,
        offset: Int, value: ByteArray
    ): Boolean {
        return false
    }

    open fun processDescriptorReadRequest(
        device: BluetoothDevice, requestId: Int, offset: Int,
        descriptor: BluetoothGattDescriptor
    ): Boolean {
        return false
    }


    /**
     * Callback to handle incoming requests to the GATT server.
     * All read/write requests for characteristics and descriptors are handled here.
     */
    private val mGattServerCallback: BluetoothGattServerCallback =
        object : BluetoothGattServerCallback() {
            override fun onConnectionStateChange(
                device: BluetoothDevice,
                status: Int,
                newState: Int
            ) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        ZLogger.i("BluetoothDevice CONNECTED: $device")
                        mConnectedDevices.add(device)
                        processConnectionStateChange()
                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        ZLogger.i("BluetoothDevice DISCONNECTED: $device")
                        //Remove device from any active subscriptions
                        mRegisteredDevices.remove(device)
                        mConnectedDevices.remove(device)
                        processConnectionStateChange()
                    }
                } else {
                    ZLogger.w("Error when connecting: $status")
                    //Remove device from any active subscriptions
                    mRegisteredDevices.remove(device)
                    mConnectedDevices.remove(device)
                    processConnectionStateChange()
                    // There are too many gatt errors (some of them not even in the documentation) so we just
                    // show the error to the user.
//                    val errorMessage =
//                        getString(R.string.status_errorWhenConnecting) + ": " + status
//                    runOnUiThread { showLongToast(errorMessage) }
                }

            }

            override fun onCharacteristicReadRequest(
                device: BluetoothDevice, requestId: Int, offset: Int,
                characteristic: BluetoothGattCharacteristic
            ) {
                val ret =
                    processCharacteristicReadRequest(device, requestId, offset, characteristic)
                if (!ret) {
                    // Invalid characteristic
                    updateLog("Invalid Characteristic Read: " + characteristic.uuid)
                    sendResponse(
                        device,
                        requestId,
                        BluetoothGatt.GATT_FAILURE,
                        0,
                        null
                    )
                }

            }


            override fun onDescriptorReadRequest(
                device: BluetoothDevice, requestId: Int, offset: Int,
                descriptor: BluetoothGattDescriptor
            ) {
                val ret = processDescriptorReadRequest(device, requestId, offset, descriptor)
                if (!ret) {
                    updateLog("Unknown descriptor read request")
                    sendResponse(
                        device,
                        requestId,
                        BluetoothGatt.GATT_FAILURE,
                        0,
                        null
                    )
                }
            }

            override fun onDescriptorWriteRequest(
                device: BluetoothDevice, requestId: Int,
                descriptor: BluetoothGattDescriptor,
                preparedWrite: Boolean, responseNeeded: Boolean,
                offset: Int, value: ByteArray
            ) {
                val ret = processDescriptorWriteRequest(
                    device,
                    requestId,
                    descriptor,
                    preparedWrite,
                    responseNeeded,
                    offset,
                    value
                )
                if (!ret) {
                    updateLog("Unknown descriptor write request")
                    if (responseNeeded) {
                        sendResponse(
                            device,
                            requestId,
                            BluetoothGatt.GATT_FAILURE,
                            0,
                            null
                        )
                    }
                }
            }

            override fun onNotificationSent(device: BluetoothDevice?, status: Int) {
                super.onNotificationSent(device, status)
                ZLogger.d("Notification sent. Status: $status, device:${device?.address}")
                synchronized(mNotificationLock) {
                    mNotificationWriteCompleted = true
                    mNotificationLock.notifyAll()
                }
            }

            override fun onMtuChanged(device: BluetoothDevice?, mtu: Int) {
                super.onMtuChanged(device, mtu)
                updateLog("onMtuChanged: mtu=$mtu")
            }

            override fun onPhyUpdate(
                device: BluetoothDevice?,
                txPhy: Int,
                rxPhy: Int,
                status: Int
            ) {
                super.onPhyUpdate(device, txPhy, rxPhy, status)
                updateLog("onPhyUpdate: txPhy=$txPhy, rxPhy=$rxPhy, status=$status")
            }

            override fun onExecuteWrite(
                device: BluetoothDevice?,
                requestId: Int,
                execute: Boolean
            ) {
                super.onExecuteWrite(device, requestId, execute)
                updateLog("onMtuChanged: requestId=$requestId, execute=#execute")
            }

            override fun onCharacteristicWriteRequest(
                device: BluetoothDevice?,
                requestId: Int,
                characteristic: BluetoothGattCharacteristic?,
                preparedWrite: Boolean,
                responseNeeded: Boolean,
                offset: Int,
                value: ByteArray?
            ) {
                super.onCharacteristicWriteRequest(
                    device,
                    requestId,
                    characteristic,
                    preparedWrite,
                    responseNeeded,
                    offset,
                    value
                )
                val ret = processCharacteristicWriteRequest(
                    device,
                    requestId,
                    characteristic,
                    preparedWrite,
                    responseNeeded,
                    offset,
                    value
                )
            }

            override fun onPhyRead(device: BluetoothDevice?, txPhy: Int, rxPhy: Int, status: Int) {
                super.onPhyRead(device, txPhy, rxPhy, status)
                updateLog("onPhyRead: txPhy=$txPhy, rxPhy=$rxPhy, status=$status")
            }

            override fun onServiceAdded(status: Int, service: BluetoothGattService?) {
                super.onServiceAdded(status, service)
                updateLog("status=$status, service=${service?.uuid}")
            }
        }

    fun registerClient(device: BluetoothDevice) {
        mRegisteredDevices.add(device)
    }

    fun unregisterClient(device: BluetoothDevice) {
        mRegisteredDevices.remove(device)
    }

    fun isClientRegistered(device: BluetoothDevice) : Boolean {
        return mRegisteredDevices.contains(device)
    }

    fun isExistConnectedDevice():Boolean {
        if (mConnectedDevices.size <= 0) {
            return false;
        }
        return mConnectedDevices.size > 0
    }

    fun sendResponse(
        device: BluetoothDevice,
        requestId: Int,
        status: Int,
        offset: Int,
        value: ByteArray?
    ) {
        mBluetoothGattServer!!.sendResponse(
            device,
            requestId,
            status,
            offset,
            value
        )
    }

    fun addService(service: BluetoothGattService?): Boolean? {
        return mBluetoothGattServer?.addService(service)
    }

    fun getService(uuid: UUID?): BluetoothGattService? {
        return mBluetoothGattServer?.getService(uuid)
    }

    private val mNotificationLock = java.lang.Object()
    private var mNotificationWriteCompleted = false

    /**
     * @param confirm true for indication (acknowledge) and false for notification (unacknowledge).
     */
    fun notifyCharacteristicChanged(
        characteristic: BluetoothGattCharacteristic,
        confirm: Boolean
    ): Boolean {
        if (mRegisteredDevices.isEmpty()) {
            ZLogger.d("No subscribers registered")
            return false
        }
        ZLogger.v("Sending update to ${mRegisteredDevices.size} subscribers")
        for (device in mRegisteredDevices) {
            synchronized(mNotificationLock) {
                mNotificationWriteCompleted = false
                updateLog("<< ${device.address} : ${DataConverter.bytes2Hex(characteristic.value)}")
                mBluetoothGattServer!!.notifyCharacteristicChanged(device, characteristic, confirm)
                if (!mNotificationWriteCompleted) {
                    mNotificationLock.wait(1600)
                }
            }
        }
        return true
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
                        startAdvertising()
                        startServer()
                    }
                    BluetoothAdapter.STATE_OFF -> {
                        stopServer()
                        stopAdvertising()
                    }
                    else -> {
                    }
                }
            }
        }
}