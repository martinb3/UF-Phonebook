/*

 * Copyright 2010 Daniel Weisser
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

package org.mbs3.android.ufpb2.syncadapter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.mbs3.android.ufpb2.Constants;
import org.mbs3.android.ufpb2.R;
import org.mbs3.android.ufpb2.Util;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.util.Log;

/**
 * A simple file logger, that logs the details of the synchronization process to SD card.
 * 
 * @author <a href="mailto:daniel.weisser@gmx.de">Daniel Weisser</a>
 */
public class Logger {

	private BufferedWriter f;
	private Context ctx;
	public static final String DATE_FORMAT_NOW = "yyyy-MM-dd HH:mm:ss";
	public static final String DATE_FORMAT_NOW_FILE = "yyyy-MM-dd-HH-mm-ss";
	public static final String TAG = "org.mbs3.android.ufpb2.syncadapter.Logger";

	private static String now() {
		Calendar cal = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_NOW);
		return sdf.format(cal.getTime());
	}

	private static String nowFile() {
		Calendar cal = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_NOW_FILE);
		return sdf.format(cal.getTime());
	}

	public void d(String tag, String message) {
		if(!shouldLog(ctx))
			return;

		try {
			Log.d(tag, message);
			if (f != null) {
				f.write(now() + ": " + message + "\n");
				f.flush();
			}
		} catch (IOException e) {
			Log.e("TAG", e.getMessage(), e);
		}
	}

	public void startLogging(Context ctx) {
		this.ctx=ctx;
		
		// if we were told not to log, this should be a no-op after we save the ctx
		if(!shouldLog(ctx.getApplicationContext()))
			return;
		
		if(!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
			Log.d(TAG, "Cannot write to debug log as external media is not mounted or is read only");
			return;
		}
		
		File sdCard = Environment.getExternalStorageDirectory();
		File dir = new File(sdCard.getAbsolutePath() + Constants.SDCARD_FOLDER);
		dir.mkdirs();
		File file = new File(dir, nowFile() + "_sync.log");

		try {
			f = new BufferedWriter(new FileWriter(file));
		} catch (FileNotFoundException e) {
			Log.e("TAG", e.getMessage(), e);
		} catch (IOException e) {
			Log.e("TAG", e.getMessage(), e);
		}
	}

	public void stopLogging() {
		try {
			if (f != null) {
				f.close();
			}
		} catch (IOException e) {
			Log.e("TAG", e.getMessage(), e);
		}
	}
	
	private static boolean shouldLog(Context ctx) {
		if(ctx == null) {
			Log.d(TAG,"Logger received null context, no debug log will be written");
			return false;
		}
		
		SharedPreferences p = Util.getPrefs(ctx);
		boolean shouldDebugLog = p.getBoolean(ctx.getString(R.string.pref_log_to_sd), false);
		Log.d(TAG,"Logger invoked, preferences dictate I " + (shouldDebugLog ? "will" : "won't") + " write to the debug log");
		return shouldDebugLog;
	}

}
