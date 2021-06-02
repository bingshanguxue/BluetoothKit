package com.realsil.apps.bluetooth5.tv

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.realsil.apps.bluetooth5.tv.createbond.CreateBondActivity
import com.realsil.apps.bluetooth5.tv.extendedadv.AdvReceiverActivity
import com.realsil.apps.bluetooth5.tv.extendedadv.AdvertiserActivity
import com.realsil.apps.bluetooth5.tv.longrange.LongRangeClientActivity
import com.realsil.apps.bluetooth5.tv.longrange.LongRangeServerActivity
import kotlinx.android.synthetic.main.tv_activity_main.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.tv_activity_main)

        btnLongRangeReceiver.setOnClickListener {
            startActivity(Intent(this, LongRangeClientActivity::class.java))
        }
        btnLongRangeTransmitter.setOnClickListener {
            startActivity(Intent(this, LongRangeServerActivity::class.java))
        }
        btnExAdvAdvertiser.setOnClickListener {
            startActivity(Intent(this, AdvertiserActivity::class.java))
        }
        btnExAdvReceiver.setOnClickListener {
            startActivity(Intent(this, AdvReceiverActivity::class.java))
        }
        btnCreateBond.setOnClickListener {
            startActivity(Intent(this, CreateBondActivity::class.java))
        }
    }
}