package de.danielweisser.android.ldapsync.activity;

import de.danielweisser.android.ldapsync.R;
import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebView;

public class Profile extends Activity {

	final String TAG = "Profile";
	
	WebView mWebView;
	
	@Override
	protected void onStart() {
		super.onStart();
		
		Log.i(TAG, "Started the profile activity");
		
		if(getIntent().getData() != null) {
			Cursor cursor = managedQuery(getIntent().getData(), null, null, null, null);
			if(cursor.moveToNext()) {
				Log.i(TAG, "DATA1: " + cursor.getString(cursor.getColumnIndex("DATA1")));
				Log.i(TAG, "DATA2: " + cursor.getString(cursor.getColumnIndex("DATA2")));
				Log.i(TAG, "DATA3: " + cursor.getString(cursor.getColumnIndex("DATA3")));
				Log.i(TAG, "DATA4: " + cursor.getString(cursor.getColumnIndex("DATA4")));
				
				String ufid = cursor.getString(cursor.getColumnIndex("DATA4"));
				String tag = convertUFIDToTag(ufid);
				
				if(tag != null && !tag.equals("")) {
					mWebView.clearHistory();
					mWebView.loadUrl("http://phonebook.ufl.edu/people/" + tag + "/");
				}
			}
		}
	}

	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.fullcontactview);

	    Log.i(TAG, "Created the profile activity");
	    
	    mWebView = (WebView) findViewById(R.id.webview);
	    mWebView.getSettings().setJavaScriptEnabled(true);
	    mWebView.loadUrl("http://phonebook.ufl.edu"); // helps cache!
	}

	final private long UFID_MASK = 56347812;

	
	public String convertUFIDToTag(String ufid) {
		
			long lUfid = Long.valueOf(ufid);
		
			String filter = "TSJWHEVN";
			
			String encoded = String.format("%09o", lUfid ^ UFID_MASK);

			String result = "";
			for(int i = 0; i < encoded.length(); i++) {
				char c = encoded.charAt(i);
				int v = Integer.valueOf(""+c);
				result += filter.charAt(v);
			}
			
			return result;
	}
	
}
