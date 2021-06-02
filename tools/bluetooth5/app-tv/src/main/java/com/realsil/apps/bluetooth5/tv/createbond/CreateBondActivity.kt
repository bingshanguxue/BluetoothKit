package com.realsil.apps.bluetooth5.tv.createbond

import android.annotation.TargetApi
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.View
import com.realsil.apps.bluetooth5.tv.R
import com.realsil.sdk.core.bluetooth.connection.le.BluetoothGattManager
import com.realsil.sdk.core.bluetooth.impl.BluetoothDeviceImpl
import com.realsil.sdk.core.bluetooth.scanner.ScannerParams
import com.realsil.sdk.core.bluetooth.scanner.SpecScanRecord
import com.realsil.sdk.core.logger.ZLogger
import com.realsil.sdk.support.base.BaseActivity
import com.realsil.sdk.support.logger.LogView
import com.realsil.sdk.support.scanner.ScannerActivity
import kotlinx.android.synthetic.main.tv_activity_create_bond.*

class CreateBondActivity : BaseActivity() {
    private var mBtAdapter: BluetoothAdapter? = null
    private var mDevice: BluetoothDevice? = null
    private var bluetoothGattManager: BluetoothGattManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.tv_activity_create_bond)

        toolbar.setTitle(R.string.title_create_bond)
        setSupportActionBar(toolbar)
        if (supportActionBar != null) {
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        }
        toolbar.setNavigationOnClickListener(
            View.OnClickListener { v: View? -> onBackPressed() })


        btnScan.setOnClickListener {
            selectTargetDevice()
        }
        btnCreateBond.setOnClickListener {
            createBond()
        }
        btnRemoveBond.setOnClickListener {
            removeBond()
        }
        btnConnect.setOnClickListener {
            connect()
        }
        btnDisconnect.setOnClickListener {
            disconnect()
        }

        logView.configure(
            LogView.LogConfigure.Builder()
                .autoClearEnabled(true)
                .autoClearLineNumber(30)
                .build())

        mBtAdapter = BluetoothAdapter.getDefaultAdapter()
        if (!mBtAdapter!!.isEnabled) {
            mBtAdapter!!.enable()
        }
        bluetoothGattManager = BluetoothGattManager.getInstance()
        if (bluetoothGattManager == null) {
            BluetoothGattManager.initial(this)
            bluetoothGattManager = BluetoothGattManager.getInstance()
        }

        registerBroadcast()

        refresh()
    }


    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(mBroadcastReceiver)
        } catch (e: Exception) {
            ZLogger.e(e.toString())
        }
    }

    private fun createBond() {
        var ret: Boolean
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ret = BluetoothDeviceImpl.createBond(
                mDevice,
                BluetoothDevice.TRANSPORT_LE
            )
        } else {
            ret = BluetoothDeviceImpl.createBond(
                mDevice,
                0x02
            )
        }

        if (!ret) {
            logView.w("createBond:$ret")
        }
    }

    private fun removeBond() {
        var ret = BluetoothDeviceImpl.removeBond(
            mDevice
        )
        if (!ret) {
            logView.w("removeBond:$ret")
        }
    }
    private fun connect() {
        if (mDevice == null) {
            return
        }
        var ret = bluetoothGattManager!!.connect(mDevice!!.address, mGattCallback)
        if (!ret) {
            logView.w("connect:$ret")
        }
    }

    private fun disconnect() {
        if (mDevice == null) {
            return
        }
        var ret = bluetoothGattManager!!.disconnectGatt(mDevice!!.address)
        if (!ret) {
            logView.w("disconnect:$ret")
        }
    }


    private fun selectTargetDevice() {
        val scannerParams =
            ScannerParams(ScannerParams.SCAN_MODE_GATT)
        scannerParams.isNameNullable = true
        val intent = Intent(
            this,
            ScannerActivity::class.java
        )
        intent.putExtra(
            ScannerActivity.EXTRA_KEY_SCAN_PARAMS,
            scannerParams
        )
        startActivityForResult(
            intent,
            REQUEST_CODE_BT_SCANNER
        )
    }


    override fun onBtScannerCallback(device: BluetoothDevice?, specScanRecord: SpecScanRecord?) {
        super.onBtScannerCallback(device, specScanRecord)
        mDevice = device
        if (device != null) {
            logView.i("select device: ${device.address}")
        }
        refresh()
    }

    private val mGattCallback: BluetoothGattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothGatt.STATE_CONNECTED) {
                    runOnUiThread {
                        logView.i("${gatt.device.address} Connected")
                        refresh()
                    }
                } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                    runOnUiThread {
                        logView.i("${gatt.device.address} Disconnected")
                        refresh()
                    }
                }
            } else {
                runOnUiThread {
                    logView.i("${gatt.device.address} connect error, status=${status}")
                    refresh()
                }
            }
        }

    }

    private fun refresh() {
        if (mDevice == null) {
            btnScan.visibility = View.VISIBLE
            btnCreateBond.visibility = View.GONE
            btnRemoveBond.visibility = View.GONE
            btnConnect.visibility = View.GONE
            btnDisconnect.visibility = View.GONE
        } else {
            btnScan.visibility = View.VISIBLE
            if (mDevice!!.bondState == BluetoothDevice.BOND_BONDED) {
                btnCreateBond.visibility = View.GONE
                btnRemoveBond.visibility = View.VISIBLE
            } else {
                btnCreateBond.visibility = View.VISIBLE
                btnRemoveBond.visibility = View.GONE
            }

            if (bluetoothGattManager!!.isConnected(mDevice!!.address)) {
                btnConnect.visibility = View.GONE
                btnDisconnect.visibility = View.VISIBLE
            } else {
                btnConnect.visibility = View.VISIBLE
                btnDisconnect.visibility = View.GONE
            }
        }
    }

    private var mBroadcastReceiver: BtBroadcastReceiver? =
        null

    private fun registerBroadcast() {
        mBroadcastReceiver =
            BtBroadcastReceiver()
        val filter = IntentFilter()
        //BT
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        filter.addAction(BluetoothDevice.ACTION_FOUND)
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        //ACL
//        filter.addAction(BluetoothAdapter.ACTION_BLE_ACL_CONNECTED);
//        filter.addAction(BluetoothAdapter.ACTION_BLE_ACL_DISCONNECTED);
        //BOND
        filter.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST)
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        registerReceiver(mBroadcastReceiver, filter)
    }


    inner class BtBroadcastReceiver : BroadcastReceiver() {
        @TargetApi(Build.VERSION_CODES.KITKAT)
        override fun onReceive(
            context: Context,
            intent: Intent
        ) {
            val action = intent.action
            val device =
                intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
            ZLogger.d(
                D,
                action
            )
            when (action) {
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {

                }
                BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                    val bondState = intent.getIntExtra(
                        BluetoothDevice.EXTRA_BOND_STATE,
                        BluetoothDevice.ERROR
                    )
                    runOnUiThread {
                        when (bondState) {
                            BluetoothDevice.BOND_NONE -> {
                                logView.d("BOND_NONE: ")
                                refresh()
                            }
                            BluetoothDevice.BOND_BONDING -> {
                                logView.d("BOND_BONDING: ")
                            }
                            BluetoothDevice.BOND_BONDED -> {
                                logView.i("BOND_BONDED: ")
                                refresh()
                            }
                            else -> {
                            }
                        }
                    }
                }
                else -> {
                }
            }
        }
    }

}