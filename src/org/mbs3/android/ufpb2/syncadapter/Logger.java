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

	
	public static final String DATE_FORMAT_NOW = "yyyy-MM-dd HH:mm:ss";
	public static final String DATE_FORMAT_NOW_FILE = "yyyy-MM-dd-HH-mm-ss";
	public static final String TAG = "org.mbs3.android.ufpb2.syncadapter.Logger";
	
	private static Logger _logger;

	private BufferedWriter _writer;
	
	private Context _context;
	private SimpleDateFormat sdfNow = new SimpleDateFormat(DATE_FORMAT_NOW);
	private SimpleDateFormat sdfNowFile = new SimpleDateFormat(DATE_FORMAT_NOW_FILE);

	
	public static synchronized Logger getLogger(Context context) {
		if(_logger == null) {
			_logger = new Logger(context);
		}
		return _logger;
	}
	
	private Logger(Context context) {
		if (context != null) {
			_context = context.getApplicationContext();
		}
		// context = null
	}

	private synchronized void writeLog(String tag, String msg) {
		try {
			// if we were told not to log, this should be a no-op after we save the ctx
			boolean shouldDebugLog = shouldLog();
			Log.v(TAG, "Logger invoked, preferences dictate I " + (shouldDebugLog ? "will" : "won't") + " write to the debug log");
			if (!shouldDebugLog)
				return;

			if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
				Log.d(TAG, "Cannot write to debug log as external media is not mounted or is read only");
				return;
			}

			if(_writer == null) {
				File sdCard = Environment.getExternalStorageDirectory();
				File dir = new File(sdCard.getAbsolutePath() + Constants.SDCARD_FOLDER);
				dir.mkdirs();
				
				Calendar cal = Calendar.getInstance();
				

				File _logfile = new File(dir, sdfNowFile.format(cal.getTime()) + "_sync.log");
				_writer = new BufferedWriter(new FileWriter(_logfile));
			}
			
			Calendar cal = Calendar.getInstance();
			_writer.write(tag + ": " + sdfNow.format(cal.getTime()) + ": " + msg + "\n");
			_writer.flush();
				
			if(_writer != null) {
				_writer.close();
				_writer = null;
			}
			
			
		} catch (FileNotFoundException e) {
			Log.e(TAG, e.getMessage(), e);
		} catch (IOException e) {
			Log.e(TAG, e.getMessage(), e);
		} catch (Throwable throwable) {
			Log.e(TAG, "Start logging failed", throwable);
		}
	}

	private boolean shouldLog() {
		if (_context == null) {
			Log.d(TAG, "Logger received null context, no debug log will be written");
			return false;
		}
		
		try {
			SharedPreferences p = Util.getPrefs(_context);
			boolean shouldDebugLog = p.getBoolean(_context.getString(R.string.pref_log_to_sd), false);
			return shouldDebugLog;
		} catch (Throwable throwable) {
			Log.e(TAG, "Start logging failed", throwable);
		}

		return false;
	}

	public void d(String tag, String message) {
		d(tag, message, null);
	}

	public void d(String tag, String message, Throwable throwable) {
		if (throwable == null)
			Log.d(tag, message);
		else
			Log.d(tag, message, throwable);

		writeLog(tag,message);
	}

	public void e(String tag, String message) {
		e(tag, message, null);
	}

	public void e(String tag, String message, Throwable throwable) {
		if (throwable == null)
			Log.e(tag, message);
		else
			Log.e(tag, message, throwable);

		writeLog(tag, message);
	}

	public void v(String tag, String message) {
		v(tag, message, null);
	}

	public void v(String tag, String message, Throwable throwable) {
		if (throwable == null)
			Log.v(tag, message);
		else
			Log.v(tag, message, throwable);

		writeLog(tag,message);
	}

	public void i(String tag, String message) {
		i(tag, message, null);
	}

	public void i(String tag, String message, Throwable throwable) {
		if (throwable == null)
			Log.i(tag, message);
		else
			Log.i(tag, message, throwable);

		writeLog(tag, message);
	}

}
