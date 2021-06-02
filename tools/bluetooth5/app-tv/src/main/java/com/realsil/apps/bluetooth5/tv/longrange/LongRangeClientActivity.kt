package com.realsil.apps.bluetooth5.tv.longrange

import android.bluetooth.*
import android.os.Bundle
import android.os.ParcelUuid
import android.view.View
import com.realsil.apps.bluetooth5.tv.R
import com.realsil.apps.bluetooth5.support.core.GattClientActivity
import com.realsil.apps.bluetooth5.support.longrange.LongRangeProfile
import com.realsil.sdk.core.bluetooth.GlobalGatt
import com.realsil.sdk.core.bluetooth.scanner.ExtendedBluetoothDevice
import com.realsil.sdk.core.bluetooth.scanner.LeScannerPresenter
import com.realsil.sdk.core.bluetooth.scanner.ScannerCallback
import com.realsil.sdk.core.bluetooth.scanner.ScannerParams
import com.realsil.sdk.core.bluetooth.scanner.compat.CompatScanFilter
import com.realsil.sdk.core.logger.ZLogger
import com.realsil.sdk.core.utility.DataConverter
import com.realsil.sdk.support.logger.LogView
import kotlinx.android.synthetic.main.tv_activity_longrange_receiver.*
import java.util.*

class LongRangeClientActivity : GattClientActivity() {

    private var mTxCharacteristic: BluetoothGattCharacteristic? = null
    private var mRxCharacteristic: BluetoothGattCharacteristic? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.tv_activity_longrange_receiver)

        toolbar.setTitle(R.string.title_longrange)
        setSupportActionBar(toolbar)
        if (supportActionBar != null) {
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        }
        toolbar.setNavigationOnClickListener(
            View.OnClickListener { v: View? -> onBackPressed() })


        btnConnect.setOnClickListener {
            updateScanParameters()
            mScannerPresenter?.startScan()
        }
        btnDisconnect.setOnClickListener {
            disconnect()
        }

        logView.configure(
            LogView.LogConfigure.Builder()
                .autoClearEnabled(true)
                .autoClearLineNumber(30)
                .build())

        initialize()

        val scannerParams = ScannerParams(ScannerParams.SCAN_MODE_GATT)
        scannerParams.scanPeriod = 60 * 1000.toLong()
        scannerParams.isConnectable = false
        scannerParams.autoScanDelay = 1000
        scannerParams.isAutoDiscovery = true

//        var nameFilter = LongRangeSettings.getInstance()!!.nameFilter
//        if (!TextUtils.isEmpty(nameFilter)) {
//            val scanFilters: MutableList<CompatScanFilter> = ArrayList()
//            scanFilters.add(CompatScanFilter.Builder().setDeviceName(nameFilter).build())
//            scannerParams.scanFilters = scanFilters
//        }

        mScannerPresenter = LeScannerPresenter(this, scannerParams, mScannerCallback)
    }

    override fun updateLog(message: String) {
        super.updateLog(message)
        runOnUiThread {
            logView.d("\r\n" + message)
        }
    }

    private fun updateScanParameters() {
        val scannerParams = ScannerParams(ScannerParams.SCAN_MODE_GATT)
        scannerParams.scanPeriod = 60 * 1000.toLong()
        scannerParams.isConnectable = false
        scannerParams.autoScanDelay = 1000
        scannerParams.isAutoDiscovery = false

        val scanFilters: MutableList<CompatScanFilter> = ArrayList()

//        var nameFilter = LongRangeSettings.getInstance()!!.nameFilter
//        if (!TextUtils.isEmpty(nameFilter)) {
//            scanFilters.add(CompatScanFilter.Builder().setDeviceName(nameFilter).build())
//        }
        scanFilters.add(CompatScanFilter.Builder().setServiceUuid(ParcelUuid(LongRangeProfile.DATA_TRANSMIT_SERVICE)).build())
        scannerParams.scanFilters = scanFilters


        mScannerPresenter?.setScannerParams(scannerParams)
    }


    private val mScannerCallback: ScannerCallback = object : ScannerCallback() {
        override fun onNewDevice(device: ExtendedBluetoothDevice) {
            super.onNewDevice(device)
            runOnUiThread {
//                logView.logIn("\r\n>> ${device.getName()}:${device.device}\r\nscanRecord:${device.getSpecScanRecord()}")
            }

            mScannerPresenter?.stopScan()
            connect(device.device.address)
        }

        override fun onScanStateChanged(state: Int) {
            super.onScanStateChanged(state)
            if (!mScannerPresenter!!.isScanning) {
            }
        }
    }


    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     * @return Return true if the connection is initiated successfully. The connection result
     * is reported asynchronously through the
     * `BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)`
     * callback.
     */
    fun connect(address: String?): Boolean {
        if (mBluetoothAdapter == null) {
            ZLogger.w("BluetoothAdapter not initialized")
            return false
        }
        if (address == null) {
            ZLogger.w("unspecified address.")
            return false
        }

//        // Previously connected device.  Try to reconnect.
//        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress)
//                && mBluetoothGatt != null) {
//            ZLogger.d("Trying to use an existing mBluetoothGatt for connection.");
//            if (mBluetoothGatt.connect()) {
//                mConnectionState = STATE_CONNECTING;
//                return true;
//            } else {
//                return false;
//            }
//        }
        val device: BluetoothDevice = mBluetoothAdapter!!.getRemoteDevice(address)
        if (device == null) {
            ZLogger.w("Device not found.  Unable to connect.")
            return false
        }
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
//        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
        GlobalGatt.getInstance().connect(address, mGattCallback)
        ZLogger.d("Trying to create a new connection.")
        mBluetoothDeviceAddress = address
//        mConnectionState = BluetoothLeService.STATE_CONNECTING
        return true
    }


    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled        If true, enable notification.  False otherwise.
     */
    fun setCharacteristicNotification(
        characteristic: BluetoothGattCharacteristic,
        enabled: Boolean
    ) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            ZLogger.w("BluetoothAdapter not initialized")
            return
        }
        updateLog("setCharacteristicNotification")
        mBluetoothGatt!!.setCharacteristicNotification(characteristic, enabled)

        // This is specific to Heart Rate Measurement.
        val descriptor = characteristic.getDescriptor(
            LongRangeProfile.CLIENT_CONFIG
        )
        descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        mBluetoothGatt!!.writeDescriptor(descriptor)
    }

    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    private val mGattCallback: BluetoothGattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            val intentAction: String
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                mBluetoothGatt = GlobalGatt.getInstance().getBluetoothGatt(mBluetoothDeviceAddress)
//                intentAction = BluetoothLeService.ACTION_GATT_CONNECTED
//                mConnectionState = BluetoothLeService.STATE_CONNECTED
//                broadcastUpdate(intentAction)
                ZLogger.d("Connected to GATT server.")

                //                // Attempts to discover services after successful connection.
                if (mBluetoothGatt != null) {
                    ZLogger.d(
                        "Attempting to start service discovery:" +
                                mBluetoothGatt!!.discoverServices()
                    )

                    mBluetoothGatt!!.setPreferredPhy(
                        BluetoothDevice.PHY_LE_CODED_MASK,
                        BluetoothDevice.PHY_LE_CODED_MASK, BluetoothDevice.PHY_OPTION_S8
                    )
                }


                runOnUiThread {
                    btnConnect.visibility = View.GONE
                    btnDisconnect.visibility = View.VISIBLE
                }
                //                    mBluetoothGatt.setPreferredPhy(BluetoothDevice.PHY_LE_2M_MASK,
//                            BluetoothDevice.PHY_LE_2M_MASK, BluetoothDevice.PHY_OPTION_NO_PREFERRED);

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
//                intentAction = BluetoothLeService.ACTION_GATT_DISCONNECTED
//                mConnectionState = BluetoothLeService.STATE_DISCONNECTED
                ZLogger.i("Disconnected from GATT server.")
//                broadcastUpdate(intentAction)
                runOnUiThread {
                    btnConnect.visibility = View.VISIBLE
                    btnDisconnect.visibility = View.GONE
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                var service = gatt.getService(LongRangeProfile.DATA_TRANSMIT_SERVICE)
                service?.let {
                    mTxCharacteristic =
                        service.getCharacteristic(LongRangeProfile.TX_CHARACTERISTIC)
                    mRxCharacteristic =
                        service.getCharacteristic(LongRangeProfile.RX_CHARACTERISTIC)
                    if (mRxCharacteristic != null) {
                        setCharacteristicNotification(mTxCharacteristic!!, true)
                    }
                }
//                broadcastUpdate(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED)
            } else {
                updateLog("discoverServices failed, status=$status")
//                notifyError(String.format(Locale.US, "discoverServices failed, status=%d", status))
            }
        }

        override fun onPhyUpdate(gatt: BluetoothGatt?, txPhy: Int, rxPhy: Int, status: Int) {
            super.onPhyUpdate(gatt, txPhy, rxPhy, status)
            updateLog("onPhyUpdate: txPhy=$txPhy, rxPhy=$rxPhy,status=$status")

//            innerStep1();
        }

        override fun onPhyRead(gatt: BluetoothGatt?, txPhy: Int, rxPhy: Int, status: Int) {
            super.onPhyRead(gatt, txPhy, rxPhy, status)

            updateLog("onPhyRead: txPhy=$txPhy, rxPhy=$rxPhy,status=$status")
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
//                broadcastUpdate(BluetoothLeService.ACTION_DATA_AVAILABLE, characteristic)
            } else {
                updateLog(String.format("readCharacteristic failed, status=%d", status))
            }
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            super.onCharacteristicWrite(gatt, characteristic, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                runOnUiThread {
                    logView.i("<< ${characteristic.uuid} ${DataConverter.bytes2Hex(characteristic.value)}")
                }
//                broadcastUpdate(BluetoothLeService.ACTION_DATA_AVAILABLE, characteristic)
            } else {
                updateLog(String.format("writeCharacteristic failed, status=%d", status))
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {

            updateLog(">> ${characteristic.uuid}\n ${DataConverter.bytes2Hex(characteristic.value)}\n")
//            broadcastUpdate(BluetoothLeService.ACTION_DATA_AVAILABLE, characteristic)
            if (cbxLoopback.isChecked && mRxCharacteristic != null) {
                mRxCharacteristic!!.value = characteristic.value
                GlobalGatt.getInstance().writeCharacteristic(mBluetoothDeviceAddress, mRxCharacteristic)
            }
        }

        fun onConnectionUpdated(
            gatt: BluetoothGatt?,
            interval: Int,
            latency: Int,
            timeout: Int,
            status: Int
        ) {
            ZLogger.v(
                String.format(
                    "onConnectionUpdated:interval=%d, latency=%d, timeout=%d, status=%d",
                    interval, latency, timeout, status
                )
            )
        }
    }
}