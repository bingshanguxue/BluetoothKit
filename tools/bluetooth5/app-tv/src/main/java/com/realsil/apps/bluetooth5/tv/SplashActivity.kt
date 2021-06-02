/*
 * Copyright (C) 2019 Realsil Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.realsil.apps.bluetooth5.tv

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import com.realsil.apps.bluetooth5.tv.extendedadv.AdvSettings
import com.realsil.sdk.support.base.BaseActivity
import java.util.*


/**
 * @author bingshanguxue
 */
class SplashActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        AdvSettings.initialize(this)

        requestPermissions()
    }

    override fun getRequestPermissions(): ArrayList<String> {
        val permissionsNeeded = ArrayList<String>()
        //Camera
//        permissionsNeeded.add(Manifest.permission.CAMERA);
        //Contact
//        permissionsNeeded.add(Manifest.permission.READ_CONTACTS);
        //Location:位置服务
        permissionsNeeded.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        permissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION)
        //MicroPhone:录音
//        permissionsNeeded.add(Manifest.permission.RECORD_AUDIO);
        //Phone:拨打电话
//        permissionsNeeded.add(Manifest.permission.CALL_PHONE);
//        permissionsNeeded.add(Manifest.permission.READ_PHONE_STATE);
        //SMS:短信
//        permissionsNeeded.add(Manifest.permission.RECEIVE_SMS);
//        expectPermissions.add(Manifest.permission.READ_SMS);
        //Storage:文件存储
        permissionsNeeded.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        permissionsNeeded.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
//        permissionsNeeded.add(Manifest.permission.MODIFY_AUDIO_SETTINGS);
//        permissionsNeeded.add(Manifest.permission.WRITE_SETTINGS);
//        permissionsNeeded.add(Manifest.permission.ACCESS_WIFI_STATE)
//        permissionsNeeded.add(Manifest.permission.CHANGE_WIFI_STATE)
//        permissionsNeeded.add(Manifest.permission.INTERNET)
//        permissionsNeeded.add(Manifest.permission.ACCESS_NETWORK_STATE)

        return permissionsNeeded
    }

    override fun onPermissionsGranted() {
        super.onPermissionsGranted()
        // Jump to MainActivity after DELAY milliseconds
        Handler().postDelayed({
            val intent = Intent(this@SplashActivity, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
            startActivity(intent)

            finish()
        }, SPLASH_DELAY.toLong())
    }

    override fun onBackPressed() {
        // Do nothing. Protect from exiting the application when splash screen is showing.
    }

    companion object {
        /**
         * Splash screen duration time in milliseconds
         */
        private val SPLASH_DELAY = 1000
    }
}