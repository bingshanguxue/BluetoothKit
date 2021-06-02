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

package com.realsil.apps.bluetooth5

import com.realsil.sdk.core.RtkConfigure
import com.realsil.sdk.core.RtkCore
import com.realsil.sdk.support.RealtekApplication
import com.realsil.sdk.core.logger.WriteLog
import com.realsil.sdk.support.settings.RtkSettings


/**
 * @author bingshanguxue
 */
class MyApplication : RealtekApplication() {

    override fun onCreate() {
        super.onCreate()
        // Optional, log related
        WriteLog.install(this, "BT5", 7)
        val isDebug = RtkSettings.getInstance()!!.isDebugEnabled

        // Mandatory, initialize rtk-core library
        // this: context
        // isDebug: true, switch on debug log; false, switch off debug log
        val configure = RtkConfigure.Builder()
            .debugEnabled(isDebug)
            .printLog(true)
            .logTag("BT5")
            .globalLogLevel(RtkSettings.getInstance()!!.debugLevel)
            .build()
        RtkCore.initialize(this, configure)
    }

    companion object {
        private val TAG = "MyApplication"
    }

}
