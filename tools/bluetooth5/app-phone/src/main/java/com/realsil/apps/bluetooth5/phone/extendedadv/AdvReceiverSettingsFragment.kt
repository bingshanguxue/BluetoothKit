package com.realsil.apps.bluetooth5.phone.extendedadv

import android.content.SharedPreferences
import android.os.Bundle
import com.realsil.apps.bluetooth5.phone.R
import com.realsil.sdk.support.settings.BasePreferenceFragment


/**
 * @author bingshanguxue
 * @date 13/08/2017
 */

class AdvReceiverSettingsFragment : BasePreferenceFragment() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.pref_adv_receiver, rootKey)

        SummaryListener(AdvSettings.KEY_ADV_SCAN_FILTER_NAME)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
    }

    companion object {
        val TAG = "AdvertiserSettingsFragment"

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        fun newInstance(): AdvReceiverSettingsFragment {
            val fragment = AdvReceiverSettingsFragment()
            val args = Bundle()
            fragment.arguments = args
            return fragment
        }
    }

}
