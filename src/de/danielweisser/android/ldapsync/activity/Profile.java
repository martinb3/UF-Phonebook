package de.danielweisser.android.ldapsync.activity;

import android.app.Activity;
import android.util.Log;

public class Profile extends Activity {

	final String TAG = "Profile";
	
	@Override
	protected void onStart() {
		super.onStart();
		
		Log.i(TAG, "Started the activity");
		
		/*if(getIntent().getData() != null) {
			Cursor cursor = managedQuery(getIntent().getData(), null, null, null, null);
			if(cursor.moveToNext()) {
				Log.i(TAG, cursor.getString(cursor.getColumnIndex("DATA1")));
			}
		}*/

	}

}
