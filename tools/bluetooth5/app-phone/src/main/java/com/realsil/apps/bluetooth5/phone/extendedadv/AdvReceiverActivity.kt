package com.realsil.apps.bluetooth5.phone.extendedadv

import android.os.Bundle
import android.os.ParcelUuid
import android.text.TextUtils
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.realsil.apps.bluetooth5.phone.R
import com.realsil.apps.bluetooth5.support.adv.Utils
import com.realsil.apps.bluetooth5.support.longrange.LongRangeProfile
import com.realsil.sdk.core.bluetooth.scanner.ExtendedBluetoothDevice
import com.realsil.sdk.core.bluetooth.scanner.LeScannerPresenter
import com.realsil.sdk.core.bluetooth.scanner.ScannerCallback
import com.realsil.sdk.core.bluetooth.scanner.ScannerParams
import com.realsil.sdk.core.bluetooth.scanner.compat.CompatScanFilter
import kotlinx.android.synthetic.main.phone_activity_adv_receiver.*
import java.util.*

class AdvReceiverActivity : AppCompatActivity() {
    protected var mScannerPresenter: LeScannerPresenter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.phone_activity_adv_receiver)

        val scannerParams = ScannerParams(ScannerParams.SCAN_MODE_GATT)
        scannerParams.scanPeriod = 60 * 1000.toLong()
        scannerParams.isConnectable = false
        scannerParams.autoScanDelay = 1000
        scannerParams.isAutoDiscovery = true

//        val scanFilters: MutableList<CompatScanFilter> = ArrayList()
//        scanFilters.add(CompatScanFilter.Builder().setDeviceName("冰珊孤雪").build())
//        scannerParams.scanFilters = scanFilters

        mScannerPresenter = LeScannerPresenter(this, scannerParams, mScannerCallback)

        toolbar.setTitle(R.string.title_extended_advertising)
        setSupportActionBar(toolbar)
        if (supportActionBar != null) {
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        }
        toolbar.setNavigationOnClickListener(
            View.OnClickListener { v: View? -> onBackPressed() })

        btnStartScan.setOnClickListener {
            logView.clear()
            updateScanParameters()
            mScannerPresenter?.startScan()
        }
        btnStopScan.setOnClickListener {
            mScannerPresenter?.stopScan()
        }
    }

    private fun updateScanParameters() {
        val scannerParams = ScannerParams(ScannerParams.SCAN_MODE_GATT)
        scannerParams.scanPeriod = 60 * 1000.toLong()
        scannerParams.isConnectable = false
        scannerParams.autoScanDelay = 1000
        scannerParams.isAutoDiscovery = true

        val scanFilters: MutableList<CompatScanFilter> = ArrayList()

        var nameFilter = AdvSettings.getInstance()!!.nameFilter
        if (!TextUtils.isEmpty(nameFilter)) {
            scanFilters.add(CompatScanFilter.Builder().setDeviceName(nameFilter).build())
        }
        scanFilters.add(CompatScanFilter.Builder().setServiceUuid(ParcelUuid(LongRangeProfile.DATA_TRANSMIT_SERVICE)).build())
        scannerParams.scanFilters = scanFilters

        mScannerPresenter?.setScannerParams(scannerParams)
    }

    override fun onDestroy() {
        super.onDestroy()
        mScannerPresenter?.onDestroy()
    }

    private val mScannerCallback: ScannerCallback = object : ScannerCallback() {
        override fun onNewDevice(device: ExtendedBluetoothDevice) {
            super.onNewDevice(device)
            var scanRecord = device.getSpecScanRecord()
            runOnUiThread {
                logView.d(
                    "${scanRecord.deviceName}:${device.device}<br>" +
                            "advertiseFlags:${scanRecord.advertiseFlags}<br>" +
                            "ServiceUuids:${scanRecord.serviceUuids}<br>" +
                            "ManufacturerSpecificData:${Utils.toString(scanRecord.manufacturerSpecificData)}<br>" +
                            "ServiceData:${Utils.toString(scanRecord.serviceData)}<br>" +
                            "TxPowerLevel:${scanRecord.txPowerLevel}<br>"
                )
            }
        }

        override fun onScanStateChanged(state: Int) {
            super.onScanStateChanged(state)
            if (!mScannerPresenter!!.isScanning) {
            }
        }

        override fun onAutoScanTrigger() {
            super.onAutoScanTrigger()

            runOnUiThread {
                logView.clear()
            }
        }
    }
}