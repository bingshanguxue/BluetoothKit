package com.realsil.apps.bluetooth5.tv.extendedadv

import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertisingSetParameters
import android.bluetooth.le.PeriodicAdvertisingParameters
import android.os.Bundle
import android.os.ParcelUuid
import android.view.View
import com.google.android.material.snackbar.Snackbar
import com.realsil.apps.bluetooth5.support.adv.Utils
import com.realsil.apps.bluetooth5.support.core.GattServerActivity
import com.realsil.apps.bluetooth5.support.longrange.DataProvider
import com.realsil.apps.bluetooth5.support.longrange.LongRangeProfile
import com.realsil.apps.bluetooth5.tv.R
import com.realsil.sdk.core.logger.ZLogger
import kotlinx.android.synthetic.main.tv_activity_adv_advertiser.*
import kotlin.math.roundToLong

class AdvertiserActivity : GattServerActivity() {

    var initialized = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.tv_activity_adv_advertiser)

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
//            if (!buttonView.isPressed) {
//                return@setOnCheckedChangeListener
//            }
            if (!initialized) {
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

        sliderAdvDataLength.addOnChangeListener { slider, value, fromUser ->
            tvAdvDataLength.text = String.format("%.0f", value)
        }
        tvAdvDataLength.text = sliderAdvDataLength.value.toString()
        btnSetAdvertisingData.setOnClickListener {
            val advertiseData = generateAdvertiseData()
            currentAdvertisingSet?.setAdvertisingData(advertiseData)
            // Wait for onAdvertisingDataSet callback...
        }
        btnSetScanResponseData.setOnClickListener {
            if (scanResponseId >= 0xFFFF) {
                scanResponseId = 0
            }

            val advertiseData = generateAdvertiseData()
            var totalBytes = Utils.totalBytes(mBluetoothAdapter, advertiseData, false)
            ZLogger.v("totalBytes of scanResponse Data is $totalBytes")
            currentAdvertisingSet?.setScanResponseData(advertiseData)
            // Wait for onScanResponseDataSet callback...
        }

        initialize()
        initialized = true
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
//            sliderAdvDataLength.valueTo = 255F;//maxDataLength.toFloat()
            sliderAdvDataLength.valueTo = (255 - Utils.getMaxServiceDataLength(
                mBluetoothAdapter,
                false,
                ParcelUuid.fromString("00008fdb-4444-1000-8000-00805f9b34fb"),
                cbxAdvTxPowerLevel.isChecked,
                cbxAdvDeviceName.isChecked
            )).toFloat();
        }
    }

    fun generateLegacyAdvertiseData(): AdvertiseData? {
        return (AdvertiseData.Builder())
            .setIncludeDeviceName(cbxAdvDeviceName.isChecked)
            .setIncludeTxPowerLevel(cbxAdvTxPowerLevel.isChecked)
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
    }

    fun generateAdvertiseData(): AdvertiseData? {
        return (AdvertiseData.Builder())
            .setIncludeDeviceName(cbxAdvDeviceName.isChecked)
            .setIncludeTxPowerLevel(cbxAdvTxPowerLevel.isChecked)
            .addServiceUuid(ParcelUuid(LongRangeProfile.DATA_TRANSMIT_SERVICE))
            .addServiceData(
                ParcelUuid(LongRangeProfile.DATA_TRANSMIT_SERVICE),
                DataProvider.generateStreamWithSeq(
                    0xFFFF,
                    0,
                    sliderAdvDataLength.value.roundToLong()
                )
            )
            .build()
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
            .setIncludeDeviceName(cbxAdvDeviceName.isChecked)
            .setIncludeTxPowerLevel(cbxAdvTxPowerLevel.isChecked)
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
                    .setIncludeDeviceName(cbxAdvDeviceName.isChecked)
                    .setIncludeTxPowerLevel(cbxAdvTxPowerLevel.isChecked)
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
        var advertiseData = generateLegacyAdvertiseData()

        var scanResponseData: AdvertiseData? = null
        if (cbxScannable.isChecked) {
            scanResponseData = generateAdvertiseData()
        }
        val periodicParameters = (PeriodicAdvertisingParameters.Builder())
            .setInterval(AdvertisingSetParameters.INTERVAL_MEDIUM)
            .setIncludeTxPower(true)

        val periodicData = generateAdvertiseData()

        startAdvertisingSet(parameters.build(), advertiseData, scanResponseData, null, null)
    }

    override fun processAdvertisingSetStartFailed() {
        super.processAdvertisingSetStartFailed()
//        showShortToast("start failed")
        Snackbar.make(switchAdv, "start failed", Snackbar.LENGTH_SHORT)
//            .setAction("Action", null)
            .show()
        if (switchAdv.isChecked) {
            switchAdv.isChecked = false
        }

        cbxConnectable.isEnabled = true
        cbxScannable.isEnabled = true
        sliderAdvDataLength.isEnabled = true
        cbxAdvDeviceName.isEnabled = true
        cbxAdvTxPowerLevel.isEnabled = true


        btnEnableAdv.isEnabled = false
        btnDisableAdv.isEnabled = false
        btnSetAdvertisingData.isEnabled = false
        btnSetScanResponseData.isEnabled = false
        btnSetPeriodicAdvertisingParameters.isEnabled = false
        btnEnablePeriodicAdv.isEnabled = false
    }

    override fun processAdvertisingSetStarted() {
        super.processAdvertisingSetStarted()
        if (!switchAdv.isChecked) {
            switchAdv.isChecked = true
        }
        switchAdv.requestFocus()
        cbxConnectable.isEnabled = false
        cbxScannable.isEnabled = false
        sliderAdvDataLength.isEnabled = false
        cbxAdvDeviceName.isEnabled = false
        cbxAdvTxPowerLevel.isEnabled = false

        btnEnableAdv.isEnabled = true
        btnDisableAdv.isEnabled = true
        btnSetAdvertisingData.isEnabled = true
        btnSetScanResponseData.isEnabled = true
        btnSetPeriodicAdvertisingParameters.isEnabled = true
        btnEnablePeriodicAdv.isEnabled = true
    }

    override fun processAdvertisingSetStopped() {
        super.processAdvertisingSetStopped()
        if (switchAdv.isChecked) {
            switchAdv.isChecked = false
        }
        cbxConnectable.isEnabled = true
        cbxScannable.isEnabled = true
        sliderAdvDataLength.isEnabled = true
        cbxAdvDeviceName.isEnabled = true
        cbxAdvTxPowerLevel.isEnabled = true

        btnEnableAdv.isEnabled = false
        btnDisableAdv.isEnabled = false
        btnSetAdvertisingData.isEnabled = false
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