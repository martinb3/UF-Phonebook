package org.mbs3.android.ufpb2.activity;

import org.mbs3.android.ufpb2.R;
import org.mbs3.android.ufpb2.Util;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class SyncErrorActivity extends Activity implements OnClickListener {

	public static final String TAG = "SyncErrorActivity";
	private TextView mErrorMessage;
	private Throwable throwable;
	private Button buttonDismiss;
	private Button buttonSend;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		String savedStr = (savedInstanceState != null ? savedInstanceState.toString() : "null");
		Log.i(TAG, "onCreate " + savedStr);
		super.onCreate(savedInstanceState);

		throwable = (Throwable) getIntent().getSerializableExtra("throwable");

		super.setContentView(R.layout.sync_error_layout);
		mErrorMessage = (TextView) findViewById(R.id.error_text);
		buttonSend = (Button) findViewById(R.id.buttonSendErrorReport);
		buttonSend.setOnClickListener(this);
		buttonDismiss = (Button) findViewById(R.id.buttonDismissErrorReport);
		buttonDismiss.setOnClickListener(this);

		mErrorMessage.setText(Util.getStackTrace(throwable));
	}

	public void onClick(View v) {
		if (v == buttonDismiss) {
			Log.i(TAG, "Dismiss");
			finish();
		} else if (v == buttonSend) {
			Log.i(TAG, "Send");

			final Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
			emailIntent.setType("message/rfc822");
			emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[] { "martin@mbs3.org" });
			emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "UF Phonebook Sync Error Report");
			emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, Util.getStackTrace(throwable));

			// start the email activity - note you need to start it with a chooser
			startActivity(Intent.createChooser(emailIntent, "Send error report..."));

			finish();
		} else {
			Log.i(TAG, "Unknown click");
		}
	}
}
