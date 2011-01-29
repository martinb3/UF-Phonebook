package org.mbs3.android.ufpb.activity;

import org.mbs3.android.ufpb.Constants;

import android.provider.Settings;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.util.Log;

public class ApplicationActivity extends Activity {
	private final String TAG = "ApplicationActivity";
	
	@Override
	protected void onStart() {
		Log.i(TAG, "Started the profile activity");
		super.onStart();
		
		showDialog(Constants.DIALOG_APP_MAIN);
	}
	
	@Override
	protected Dialog onCreateDialog(int id) {
		Dialog dialog;
	    switch(id) {
	    case Constants.DIALOG_APP_MAIN:
	    	AlertDialog.Builder builder = new AlertDialog.Builder(this);
	    	builder.setMessage("To use this application, you must add an account of type \"UF Phonebook Sync.\" Press OK to be taken to the accounts screen now.")
	    	       .setCancelable(true)
	    	       .setPositiveButton("OK", new DialogInterface.OnClickListener() {
	    	           public void onClick(DialogInterface dialog, int id) {
	    	                dialog.cancel();
	    	                finish();
	    	                
	    	                
	    	                Intent i = new Intent(Settings.ACTION_SYNC_SETTINGS);
	    	                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	    	                startActivity(i);

	    	           }
	    	       })
	    	       .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
	    	           public void onClick(DialogInterface dialog, int id) {
	    	                dialog.cancel();
	    	                finish();
	    	           }
	    	       });
	    	dialog = builder.create();
	        break;
	    default:
	        dialog = null;
	    }
	    return dialog;
	}
}
