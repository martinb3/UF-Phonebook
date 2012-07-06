package org.mbs3.android.ufpb2.activity;

import org.mbs3.android.ufpb2.R;

import android.os.Bundle;

public class PreferenceActivity extends android.preference.PreferenceActivity {
	@Override
	public void onCreate(Bundle savedInstanceState) {     
	    super.onCreate(savedInstanceState);        
	    addPreferencesFromResource(R.xml.preferences);        
	}
}
