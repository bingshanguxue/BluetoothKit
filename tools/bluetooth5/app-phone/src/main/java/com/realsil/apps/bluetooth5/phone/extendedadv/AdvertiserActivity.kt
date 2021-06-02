package com.realsil.apps.bluetooth5.phone.extendedadv

import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.AdvertisingSetParameters
import android.bluetooth.le.PeriodicAdvertisingParameters
import android.os.Bundle
import android.os.ParcelUuid
import android.view.View
import androidx.slidingpanelayout.widget.SlidingPaneLayout.PanelSlideListener
import com.google.android.material.snackbar.Snackbar
import com.realsil.apps.bluetooth5.phone.R
import com.realsil.apps.bluetooth5.support.adv.Utils
import com.realsil.apps.bluetooth5.support.core.GattServerActivity
import com.realsil.apps.bluetooth5.support.longrange.DataProvider
import com.realsil.apps.bluetooth5.support.longrange.LongRangeProfile
import com.realsil.sdk.core.logger.ZLogger
import kotlinx.android.synthetic.main.phone_activity_adv_advertiser.*
import kotlin.math.roundToLong

class AdvertiserActivity : GattServerActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.phone_activity_adv_advertiser)

        toolbar.setTitle(R.string.title_extended_advertising)
        setSupportActionBar(toolbar)
        if (supportActionBar != null) {
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        }
        toolbar.setNavigationOnClickListener(
            View.OnClickListener { v: View? -> onBackPressed() })

        cbxConnectable.setOnCheckedChangeListener { buttonView, isChecked ->
            if (!buttonView.isPressed) {
                return@setOnCheckedChangeListener
            }
            if (cbxAdvExtension.isChecked) {
                if (isChecked) {
                    cbxScannable.isChecked = false
                    cbxScannable.isEnabled = false
                } else {
                    cbxScannable.isEnabled = true
                }
            } else {
                if (isChecked) {
                    cbxScannable.isChecked = true
                    cbxScannable.isEnabled = false
                } else {
                    cbxScannable.isChecked = false
                    cbxScannable.isEnabled = true
                }
            }

            refresh()
        }
        cbxScannable.setOnCheckedChangeListener { buttonView, isChecked ->
            if (!buttonView.isPressed) {
                return@setOnCheckedChangeListener
            }
            if (cbxAdvExtension.isChecked) {
                if (isChecked) {
                    cbxConnectable.isChecked = false
                    cbxConnectable.isEnabled = false
                } else {
                    cbxConnectable.isEnabled = true
                }
            } else {
                cbxConnectable.isEnabled = true
            }
            refresh()
        }
        cbxAdvExtension.setOnCheckedChangeListener { buttonView, isChecked ->
            if (!buttonView.isPressed) {
                return@setOnCheckedChangeListener
            }
            if (isChecked) {
                if (cbxConnectable.isChecked) {
                    cbxScannable.isChecked = false
                }
            } else {
//                cbxConnectable.isEnabled = true
            }
            refresh()
        }
        switchAdv.setOnCheckedChangeListener { buttonView, isChecked ->
            if (!buttonView.isPressed) {
                return@setOnCheckedChangeListener
            }
            if (isChecked) {
                startAdvertising()
            } else {
                stopAdvertisingSet()
            }
        }

        btnEnableAdv.setOnClickListener {
            currentAdvertisingSet?.enableAdvertising(true, 0, 0)
        }
        btnDisableAdv.setOnClickListener {
            currentAdvertisingSet?.enableAdvertising(false, 0, 0)
        }
        btnEnablePeriodicAdv.setOnClickListener {
            currentAdvertisingSet?.setPeriodicAdvertisingEnabled(true)
        }
        btnSetPeriodicAdvertisingParameters.setOnClickListener {
            val periodicParameters = (PeriodicAdvertisingParameters.Builder())
                .setInterval(AdvertisingSetParameters.INTERVAL_MEDIUM)
                .setIncludeTxPower(true)
            currentAdvertisingSet?.setPeriodicAdvertisingParameters(
                periodicParameters.build()
            )
        }
        btnSetAdvertisingData.setOnClickListener {
            val parameters = AdvertiseData.Builder()
                .setIncludeDeviceName(AdvSettings.getInstance()!!.isAdvertisingDeviceNameEnabled)
                .setIncludeTxPowerLevel(AdvSettings.getInstance()!!.isAdvertisingTxPowerLevelEnabled)
                .addServiceUuid(ParcelUuid.fromString("00008fdb-5555-1000-8000-00805f9b34fb"))
                .addServiceData(
                    ParcelUuid.fromString("00008fdb-5555-1000-8000-00805f9b34fb"),
                    DataProvider.generateStreamWithSeq(
                        scanResponseId,
                        0,
                        sliderAdvDataLength.value.roundToLong()
                    )
                )
                .build()

            currentAdvertisingSet?.setAdvertisingData(parameters)
            // Wait for onAdvertisingDataSet callback...
        }

        sliderAdvDataLength.addOnChangeListener { slider, value, fromUser ->
            tvAdvDataLength.text = String.format("%.0f", value)
        }
        tvAdvDataLength.text = sliderAdvDataLength.value.toString()


        sliderScanResponeDataLength.addOnChangeListener { slider, value, fromUser ->
            tvScanResponeDataLength.text = String.format("%.0f", value)
        }
        tvScanResponeDataLength.text = sliderScanResponeDataLength.value.toString()

        btnSetScanResponseData.setOnClickListener {
            if (scanResponseId >= 0xFFFF) {
                scanResponseId = 0
            }


            val advertiseData = AdvertiseData.Builder()
                .setIncludeDeviceName(AdvSettings.getInstance()!!.isScanResponseDeviceNameEnabled)
                .setIncludeTxPowerLevel(AdvSettings.getInstance()!!.isScanResponseTxPowerLevelEnabled)
                .addServiceUuid(ParcelUuid.fromString("00008fdb-4444-1000-8000-00805f9b34fb"))
                .addServiceData(
                    ParcelUuid.fromString("00008fdb-4444-1000-8000-00805f9b34fb"),
                    DataProvider.generateStreamWithSeq(
                        scanResponseId,
                        0,
                        sliderScanResponeDataLength.value.roundToLong()
                    )
                )
//                .addServiceUuid(ParcelUuid.fromString("00008fdb-3333-1000-8000-00805f9b34fb"))
//                .addServiceData(
//                    ParcelUuid.fromString("00008fdb-3333-1000-8000-00805f9b34fb"),
//                    DataProvider.generateStreamWithSeq(
//                        scanResponseId,
//                        0,
//                        sliderScanResponeDataLength.value.roundToLong()
//                    )
//                )
                .build()

            var totalBytes = Utils.totalBytes(mBluetoothAdapter, advertiseData, false)
            ZLogger.v("totalBytes of scanResponse Data is $totalBytes")
            currentAdvertisingSet?.setScanResponseData(advertiseData)
            // Wait for onScanResponseDataSet callback...
        }

        slidePanelLayout.setPanelSlideListener(object : PanelSlideListener {
            override fun onPanelSlide(panel: View, slideOffset: Float) {
//                ZLogger.v("onPanelSlide");
            }

            override fun onPanelOpened(panel: View) {
//                ZLogger.v("onPanelSlide");
            }

            override fun onPanelClosed(panel: View) {
//                ZLogger.v("onPanelClosed");
                refresh()
            }
        })
        initialize()
    }


    override fun refresh() {
        super.refresh()
        mBluetoothAdapter?.let {
            val maxDataLength = mBluetoothAdapter!!.leMaximumAdvertisingDataLength
            val supportCodedPhy = mBluetoothAdapter!!.isLeCodedPhySupported
            val support2MPhy = mBluetoothAdapter!!.isLe2MPhySupported
            val isLeExtendedAdvertisingSupported =
                mBluetoothAdapter!!.isLeExtendedAdvertisingSupported
            val isLePeriodicAdvertisingSupported =
                mBluetoothAdapter!!.isLePeriodicAdvertisingSupported

            ZLogger.v(
                "leMaximumAdvertisingDataLength=$maxDataLength, supportCodedPhy=$supportCodedPhy, support2MPhy=$support2MPhy, " +
                        "isLeExtendedAdvertisingSupported=$isLeExtendedAdvertisingSupported, " +
                        "isLePeriodicAdvertisingSupported=$isLePeriodicAdvertisingSupported"
            )

//        sliderAdvDataLength.valueFrom = 0.0f
            sliderAdvDataLength.valueTo = 255F;//maxDataLength.toFloat()

            sliderScanResponeDataLength.valueTo = (255 - Utils.getMaxServiceDataLength(
                mBluetoothAdapter,
                false,
                ParcelUuid.fromString("00008fdb-4444-1000-8000-00805f9b34fb"),
                AdvSettings.getInstance()!!.isScanResponseTxPowerLevelEnabled,
                AdvSettings.getInstance()!!.isScanResponseDeviceNameEnabled
            )).toFloat();
            tvLocalName.text = mBluetoothAdapter!!.name
        }

        if (cbxScannable.isChecked) {
            advertiseDataPanel.visibility = View.GONE
            scanResponsePanel.visibility = View.VISIBLE
        } else {
            advertiseDataPanel.visibility = View.VISIBLE
            scanResponsePanel.visibility = View.GONE
        }
    }

    override fun startAdvertising() {
        super.startAdvertising()
        if (cbxAdvExtension.isChecked) {
            startExtendAdv()
        } else {
            startLegacyAdv()
        }
    }

    fun startLegacyAdv() {
        val parameters = (AdvertisingSetParameters.Builder())
            .setLegacyMode(true)
            .setScannable(cbxScannable.isChecked)
            .setConnectable(cbxConnectable.isChecked)
            .setInterval(AdvSettings.getInstance()!!.advSetInterval)
            .setTxPowerLevel(AdvSettings.getInstance()!!.advSetTxPowerLevel)
            .setPrimaryPhy(AdvSettings.getInstance()!!.advSetPrimaryPhy)
            .setSecondaryPhy(AdvSettings.getInstance()!!.advSetSecondaryPhy)
//        if (switchPhy.isChecked) {
//            parameters.setPrimaryPhy(BluetoothDevice.PHY_LE_CODED)
//                .setSecondaryPhy(BluetoothDevice.PHY_LE_2M)
//        }

        // You should be able to fit large amounts of data up to maxDataLength. This goes up to 1650 bytes. For legacy advertising this would not work
        // 1123456789ABCDEF2123456789ABCDEF3123456789ABCDEF4123456789ABCDEF5123456789ABCDEF6123456789ABCDEF
        var advertiseData: AdvertiseData? = null

//        if (cbxConnectable.isChecked) {
        advertiseData = (AdvertiseData.Builder())
            .setIncludeDeviceName(AdvSettings.getInstance()!!.isAdvertisingDeviceNameEnabled)
            .setIncludeTxPowerLevel(AdvSettings.getInstance()!!.isAdvertisingTxPowerLevelEnabled)
            .addServiceUuid(ParcelUuid(LongRangeProfile.DATA_TRANSMIT_SERVICE))
//            .addServiceData(
//                ParcelUuid(LongRangeProfile.DATA_TRANSMIT_SERVICE),
//                DataProvider.generateStreamWithSeq(
//                    0xFFFF,
//                    0,
//                    sliderAdvDataLength.value.roundToLong()
//                )
//            )
            .build()
//        }

        var advertiseDataTotalBytes = Utils.totalBytes(mBluetoothAdapter, advertiseData, false)
        ZLogger.v("totalBytes of advertiseData is $advertiseDataTotalBytes")

//        "222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222"
//            .toByteArray()
        var scanResponseData: AdvertiseData? = null
        if (cbxScannable.isChecked) {
            scanResponseData =
                AdvertiseData.Builder()
                    .setIncludeDeviceName(AdvSettings.getInstance()!!.isScanResponseDeviceNameEnabled)
                    .setIncludeTxPowerLevel(AdvSettings.getInstance()!!.isScanResponseTxPowerLevelEnabled)
                    .addServiceUuid(ParcelUuid.fromString("00008fdb-2222-1000-8000-00805f9b34fb"))
//                    .addServiceData(
//                        ParcelUuid.fromString("00008fdb-2222-1000-8000-00805f9b34fb"),
//                        DataProvider.generateStreamWithSeq(
//                            0xFFFF,
//                            0,
//                            sliderAdvDataLength.value.roundToLong()
//                        )
//                    )
                    .build()
        }
        var scanResponseDataTotalBytes = Utils.totalBytes(mBluetoothAdapter, scanResponseData, false)
        ZLogger.v("totalBytes of scanResponseData is $scanResponseDataTotalBytes")

        startAdvertisingSet(parameters.build(), advertiseData, scanResponseData, null, null)
    }

    fun startExtendAdv() {
        // Check if all features are supported
        if (!mBluetoothAdapter!!.isLe2MPhySupported) {
            logView.w("2M PHY not supported!")
            return
        }
        if (!mBluetoothAdapter!!.isLeExtendedAdvertisingSupported) {
            logView.w("LE Extended Advertising not supported!")
            return
        }

        val parameters = (AdvertisingSetParameters.Builder())
            .setLegacyMode(false)
            .setScannable(cbxScannable.isChecked)
            .setConnectable(cbxConnectable.isChecked)
            .setInterval(AdvSettings.getInstance()!!.advSetInterval)
            .setTxPowerLevel(AdvSettings.getInstance()!!.advSetTxPowerLevel)
            .setPrimaryPhy(AdvSettings.getInstance()!!.advSetPrimaryPhy)
            .setSecondaryPhy(AdvSettings.getInstance()!!.advSetSecondaryPhy)
//        if (switchPhy.isChecked) {
//            parameters.setPrimaryPhy(BluetoothDevice.PHY_LE_CODED)
//                .setSecondaryPhy(BluetoothDevice.PHY_LE_2M)
//        }

        // You should be able to fit large amounts of data up to maxDataLength. This goes up to 1650 bytes. For legacy advertising this would not work
        // 1123456789ABCDEF2123456789ABCDEF3123456789ABCDEF4123456789ABCDEF5123456789ABCDEF6123456789ABCDEF
        var advertiseData: AdvertiseData? = null

//        if (cbxConnectable.isChecked) {
        advertiseData = (AdvertiseData.Builder())
            .setIncludeDeviceName(AdvSettings.getInstance()!!.isAdvertisingDeviceNameEnabled)
            .setIncludeTxPowerLevel(AdvSettings.getInstance()!!.isAdvertisingTxPowerLevelEnabled)
            .addServiceUuid(ParcelUuid(LongRangeProfile.DATA_TRANSMIT_SERVICE))
            .addServiceData(
                ParcelUuid(LongRangeProfile.DATA_TRANSMIT_SERVICE),
                DataProvider.generateStreamWithSeq(
                    0xFFFF,
                    0,
                    sliderAdvDataLength.value.roundToLong()
                )
            )
//            .addServiceUuid(ParcelUuid.fromString("00008fdb-1111-1000-8000-00805f9b34fb"))
//            .addServiceData(
//                ParcelUuid.fromString("00008fdb-1111-1000-8000-00805f9b34fb"),
//                "11110000"
//                    .toByteArray()
//            )
//            .addServiceUuid(ParcelUuid.fromString("00001805-0000-1000-8000-00805f9b34fb"))
//            .addServiceData(
//                ParcelUuid.fromString("00001805-0000-1000-8000-00805f9b34fb"),
//                "11110000"
//                    .toByteArray()
//            )
//            .addServiceUuid(ParcelUuid.fromString("00008fdb-0000-1000-8000-00805f9b34fb"))
//            .addServiceData(
//                ParcelUuid.fromString("00008fdb-0000-1000-8000-00805f9b34fb"),
//                "00001111"
//                    .toByteArray()
//            )
            .build()
//        }

//        "222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222"
//            .toByteArray()
        var scanResponseData: AdvertiseData? = null
        if (cbxScannable.isChecked) {
            scanResponseData =
                AdvertiseData.Builder()
                    .setIncludeDeviceName(AdvSettings.getInstance()!!.isScanResponseDeviceNameEnabled)
                    .setIncludeTxPowerLevel(AdvSettings.getInstance()!!.isScanResponseTxPowerLevelEnabled)
                    .addServiceUuid(ParcelUuid.fromString("00008fdb-2222-1000-8000-00805f9b34fb"))
                    .addServiceData(
                        ParcelUuid.fromString("00008fdb-2222-1000-8000-00805f9b34fb"),
                        DataProvider.generateStreamWithSeq(
                            0xFFFF,
                            0,
                            sliderAdvDataLength.value.roundToLong()
                        )
                    )
                    .build()
        }
        val periodicParameters = (PeriodicAdvertisingParameters.Builder())
            .setInterval(AdvertisingSetParameters.INTERVAL_MEDIUM)
            .setIncludeTxPower(true)

        val periodicData =
            AdvertiseData.Builder()
//                .addServiceUuid(ParcelUuid.fromString("00001805-0000-1000-8000-00805f9b34fb"))
//                .addServiceUuid(ParcelUuid.fromString("00008fdb-0000-1000-8000-00805f9b34fb"))
                .addServiceUuid(ParcelUuid.fromString("00008fdb-3333-1000-8000-00805f9b34fb"))
                .addServiceData(
                    ParcelUuid.fromString("00008fdb-3333-1000-8000-00805f9b34fb"),
                    "2222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222"
                        .toByteArray()
                )
                .setIncludeTxPowerLevel(true)
                .setIncludeDeviceName(true)
                .build()

        startAdvertisingSet(parameters.build(), advertiseData, scanResponseData, null, null)
    }
    override fun processAdvertisingSetStartFailed() {
        super.processAdvertisingSetStartFailed()
//        showShortToast("start failed")
        Snackbar.make(switchAdv, "start failed", Snackbar.LENGTH_SHORT)
//            .setAction("Action", null)
            .show()
        switchAdv.isChecked = false
        cbxConnectable.isEnabled = true
        cbxScannable.isEnabled = true


        btnEnableAdv.isEnabled = false
        btnDisableAdv.isEnabled = false
        btnSetAdvertisingData.isEnabled = false
        sliderAdvDataLength.isEnabled = true
        sliderScanResponeDataLength.isEnabled = false
        btnSetScanResponseData.isEnabled = false
        btnSetPeriodicAdvertisingParameters.isEnabled = false
        btnEnablePeriodicAdv.isEnabled = false
    }

    override fun processAdvertisingSetStarted() {
        super.processAdvertisingSetStarted()
        switchAdv.isChecked = true
        cbxConnectable.isEnabled = false
        cbxScannable.isEnabled = false

        btnEnableAdv.isEnabled = true
        btnDisableAdv.isEnabled = true
        btnSetAdvertisingData.isEnabled = true
        sliderAdvDataLength.isEnabled = false
        sliderScanResponeDataLength.isEnabled = true
        btnSetScanResponseData.isEnabled = true
        btnSetPeriodicAdvertisingParameters.isEnabled = true
        btnEnablePeriodicAdv.isEnabled = true
    }

    override fun processAdvertisingSetStopped() {
        super.processAdvertisingSetStopped()
        switchAdv.isChecked = false
        cbxConnectable.isEnabled = true
        cbxScannable.isEnabled = true

        btnEnableAdv.isEnabled = false
        btnDisableAdv.isEnabled = false
        btnSetAdvertisingData.isEnabled = false
        sliderAdvDataLength.isEnabled = true
        sliderScanResponeDataLength.isEnabled = false
        btnSetScanResponseData.isEnabled = false
        btnSetPeriodicAdvertisingParameters.isEnabled = false
        btnEnablePeriodicAdv.isEnabled = false
    }

    override fun updateLog(message: String) {
        super.updateLog(message)
        runOnUiThread {
            logView.d("\r\n" + message)
        }
    }

    override fun logw(message: String) {
        super.updateLog(message)
        runOnUiThread {
            logView.w("\r\n" + message)
        }
    }
}