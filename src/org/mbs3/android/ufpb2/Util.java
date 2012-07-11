package org.mbs3.android.ufpb2;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class Util {
	public static SharedPreferences getPrefs(Context ctx) {
		return PreferenceManager.getDefaultSharedPreferences(ctx.getApplicationContext());
	}
}
