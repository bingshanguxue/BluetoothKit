package com.realsil.apps.bluetooth5.phone

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.realsil.apps.bluetooth5.phone.createbond.CreatebondActivity
import com.realsil.apps.bluetooth5.phone.extendedadv.AdvReceiverActivity
import com.realsil.apps.bluetooth5.phone.extendedadv.AdvertiserActivity
import com.realsil.apps.bluetooth5.phone.longrange.LongRangeClientActivity
import com.realsil.apps.bluetooth5.phone.longrange.LongRangeServerActivity
import kotlinx.android.synthetic.main.phone_activity_main.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.phone_activity_main)

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
            startActivity(Intent(this, CreatebondActivity::class.java))
        }
    }
}