package com.realsil.apps.bluetooth5.phone.longrange

import android.content.SharedPreferences
import android.os.Bundle
import com.realsil.apps.bluetooth5.phone.R
import com.realsil.sdk.support.settings.BasePreferenceFragment


/**
 * @author bingshanguxue
 * @date 13/08/2017
 */

class LongRangeClientSettingsFragment : BasePreferenceFragment() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.pref_longrange_client, rootKey);
        SummaryListener(LongRangeSettings.KEY_LONGRANGE_SCAN_FILTER_NAME)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
    }

    companion object {
        val TAG = "LongRangeClientSettingsFragment"

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        fun newInstance(): LongRangeClientSettingsFragment {
            val fragment = LongRangeClientSettingsFragment()
            val args = Bundle()
            fragment.arguments = args
            return fragment
        }
    }

}
