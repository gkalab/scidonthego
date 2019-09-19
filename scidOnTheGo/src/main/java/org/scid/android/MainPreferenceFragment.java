package org.scid.android;

import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceFragment;

import static org.scid.android.Preferences.DATA_ENGINE_NAME;
import static org.scid.android.Preferences.DATA_ENGINE_NAMES;

public class MainPreferenceFragment extends PreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        String[] engineNames = getActivity().getIntent().getStringArrayExtra(DATA_ENGINE_NAMES);
        String engineName = getActivity().getIntent().getStringExtra(DATA_ENGINE_NAME);
        ListPreference enginePreference = (ListPreference) findPreference("analysisEngine");
        enginePreference.setEntryValues(engineNames);
        enginePreference.setEntries(engineNames);
        enginePreference.setDefaultValue(engineName);
    }
}
