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

package org.mbs3.android.ufpb2.client;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;

import org.mbs3.android.ufpb2.R;
import org.mbs3.android.ufpb2.authenticator.LDAPAuthenticatorActivity;
import org.mbs3.android.ufpb2.syncadapter.SyncService;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.RootDSE;
import com.unboundid.ldap.sdk.SearchResult;
import com.unboundid.ldap.sdk.SearchResultEntry;
import com.unboundid.ldap.sdk.SearchScope;


/**
 * Provides utility methods for communicating with the LDAP server.
 * 
 * @author <a href="mailto:daniel.weisser@gmx.de">Daniel Weisser</a>
 */
public class LDAPUtilities {

	private static final String TAG = "LDAPUtilities";

	/**
	 * Executes the network requests on a separate thread.
	 * 
	 * @param runnable
	 *            The runnable instance containing network operations to be executed.
	 */
	public static Thread performOnBackgroundThread(final Runnable runnable) {
		final Thread t = new Thread() {
			@Override
			public void run() {
				try {
					runnable.run();
				} finally {
				}
			}
		};
		t.start();
		return t;
	}

	/**
	 * Sends the authentication response from server back to the caller main UI thread through its handler.
	 * 
	 * @param baseDNs
	 *            An array containing the baseDNs of the LDAP server
	 * @param result
	 *            The boolean holding authentication result
	 * @param handler
	 *            The main UI thread's handler instance.
	 * @param context
	 *            The caller Activity's context.
	 * @param message
	 *            A message if applicable
	 */
	private static void sendResult(final String[] baseDNs, final Boolean result, final Handler handler, final Context context, final String message) {
		if (handler == null || context == null) {
			return;
		}
		handler.post(new Runnable() {
			public void run() {
				((LDAPAuthenticatorActivity) context).onAuthenticationResult(baseDNs, result, message);
			}
		});
	}

	/**
	 * Obtains a list of all contacts from the LDAP Server.
	 * 
	 * @param ldapServer
	 *            The LDAP server data
	 * @param baseDN
	 *            The baseDN that will be used for the search
	 * @param searchFilter
	 *            The search filter
	 * @param mappingBundle
	 *            A bundle of all LDAP attributes that are queried
	 * @param mLastUpdated
	 *            Date of the last update
	 * @param context
	 *            The caller Activity's context
	 * @return List of all LDAP contacts
	 */
	public static HashMap<Contact,Long> fetchContacts(final LDAPServerInstance ldapServer, final HashMap<Long, ArrayList<String>> emails, final String baseDN, final String searchFilter, final Bundle mappingBundle,
			final Date mLastUpdated, final Context context) {

		final HashMap<String,Long> DNs = new HashMap<String,Long>();
		final HashMap<Contact,Long> friendList = new HashMap<Contact,Long>();
		
		Log.d(TAG, "fetchContacts: " + emails.size() +  " accounts that have email lists");
		
		LDAPConnection connection = null;
		try {
			connection = ldapServer.getConnection();

			// we do the raw contact ID so we can force an aggregation later
			for(final Entry<Long,ArrayList<String>> entry : emails.entrySet()) {
				Long origin = entry.getKey();
				
				ArrayList<String> allAddr = entry.getValue();
				Log.d(TAG, "fetchContacts: " + allAddr.size() +  " emails for account " + origin);
				
				
				for(String addr : allAddr ) {
					String emailFilter = "(&(mail="+addr+")"+searchFilter+")";
					Log.d(TAG, "fetchContacts: Attempting search using filter "+emailFilter);
					
					SearchResult searchResult = connection.search(baseDN, SearchScope.SUB, emailFilter, getUsedAttributes(mappingBundle));
					List<SearchResultEntry> results = searchResult.getSearchEntries();
					if(results.size() == 1) {
						SearchResultEntry e = results.get(0);
						DNs.put(e.getDN(), origin);
						Log.i(TAG, "fetchContacts: Found in directory: (from raw contact id "+origin+") " + addr + ", dn="+e.getDN()+")");
					}
					else {
						Log.i(TAG, "fetchContacts: Not found in directory: " + addr + ", or more than one result found (n="+results.size()+")");
					}
				}
			}


			Log.i(TAG, "fetchContacts: Searching for " + DNs.size() + " DNs in the directory");
			
			for(final Entry<String,Long> entry : DNs.entrySet()) {
				String dn = entry.getKey();

				Log.i(TAG, "fetchContacts: DN search base string: " + dn);
				SearchResult searchResult = connection.search(dn, SearchScope.BASE, searchFilter, getUsedAttributes(mappingBundle));
				//Log.i(TAG, searchResult.getEntryCount() + " entries returned for this DN.");

				for (SearchResultEntry e : searchResult.getSearchEntries()) {
					Contact u = Contact.valueOf(e, mappingBundle);
					if (u != null) {
						friendList.put(u, entry.getValue());
					}
				}
			}
		} catch (LDAPException e) {
			Log.v(TAG, "LDAPException on fetching contacts", e);
			NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
			int icon = R.drawable.icon;
			CharSequence tickerText = "Error on " + org.mbs3.android.ufpb2.Constants.ACCOUNT_NAME;
			Notification notification = new Notification(icon, tickerText, System.currentTimeMillis());
			Intent notificationIntent = new Intent(context, SyncService.class);
			PendingIntent contentIntent = PendingIntent.getService(context, 0, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);
			notification.setLatestEventInfo(context, tickerText, e.getMessage().replace("\\n", " "), contentIntent);
			notification.flags = Notification.FLAG_AUTO_CANCEL;
			mNotificationManager.notify(0, notification);
			return null;
		} finally {
			if (connection != null) {
				connection.close();
			}
		}

		return friendList;
	}

	private static String[] getUsedAttributes(Bundle mappingBundle) {
		ArrayList<String> ldapAttributes = new ArrayList<String>();
		String[] ldapArray = new String[mappingBundle.size()];
		for (String key : mappingBundle.keySet()) {
			ldapAttributes.add(mappingBundle.getString(key));
		}
		ldapArray = ldapAttributes.toArray(ldapArray);
		return ldapArray;
	}

	/**
	 * Attempts to authenticate the user credentials on the server.
	 * 
	 * @param ldapServer
	 *            The LDAP server data
	 * @param handler
	 *            The main UI thread's handler instance.
	 * @param context
	 *            The caller Activity's context
	 * @return Thread The thread on which the network mOperations are executed.
	 */
	public static Thread attemptAuth(final LDAPServerInstance ldapServer, final Handler handler, final Context context) {
		final Runnable runnable = new Runnable() {
			public void run() {
				authenticate(ldapServer, handler, context);
			}
		};
		// run on background thread.
		return LDAPUtilities.performOnBackgroundThread(runnable);
	}

	/**
	 * Tries to authenticate against the LDAP server and
	 * 
	 * @param ldapServer
	 *            The LDAP server data
	 * @param handler
	 *            The handler instance from the calling UI thread.
	 * @param context
	 *            The context of the calling Activity.
	 * @return {code false} if the authentication fails, {code true} otherwise
	 */
	public static boolean authenticate(LDAPServerInstance ldapServer, Handler handler, final Context context) {
		LDAPConnection connection = null;
		try {
			connection = ldapServer.getConnection();
			if (connection != null) {
				RootDSE s = connection.getRootDSE();
				String[] baseDNs = null;
				if (s != null) {
					baseDNs = s.getNamingContextDNs();
				}

				sendResult(baseDNs, true, handler, context, null);
				return true;
			}
		} catch (LDAPException e) {
			Log.e(TAG, "Error authenticating", e);
			sendResult(null, false, handler, context, e.getMessage());
			return false;
		} finally {
			if (connection != null) {
				connection.close();
			}
		}
		return false;
	}

	/**
	 * Obtains a list of all contacts from the LDAP Server.
	 * 
	 * @param ldapServer
	 *            The LDAP server data
	 * @param baseDN
	 *            The baseDN that will be used for the search
	 * @param searchFilter
	 *            The search filter
	 * @param mappingBundle
	 *            A bundle of all LDAP attributes that are queried
	 * @param mLastUpdated
	 *            Date of the last update
	 * @param context
	 *            The caller Activity's context
	 * @return List of all LDAP contacts
	 */
	public static HashSet<Contact> searchContacts(final LDAPServerInstance ldapServer, final String searchTerms, final String baseDN, final String searchFilter, final Bundle mappingBundle, final Context context) {

		//Feb 12 14:30:04 dir8 slapd[11475]: conn=1099868 op=1 SRCH base="ou=People,dc=ufl,dc=edu" scope=2 deref=2 filter="(&(|(cn=foo*)(sn=foo*)(uid=foo)(mail=foo@*))(&(!(eduPersonPrimaryAffiliation=affiliate))(!(eduPersonPrimaryAffiliation=-*-))))"

		
		final HashSet<String> DNs = new HashSet<String>();
		final HashSet<Contact> friendList = new HashSet<Contact>();
		
		Log.d(TAG, "searchContacts: " + searchTerms +  " terms");
		
		LDAPConnection connection = null;
		try {
			connection = ldapServer.getConnection();
			String emailFilter = "(&(&(|(cn="+searchTerms+"*)(sn="+searchTerms+"*)(uid="+searchTerms+")(mail="+searchTerms+"@*))(&(!(eduPersonPrimaryAffiliation=affiliate))(!(eduPersonPrimaryAffiliation=-*-))))"+searchFilter+")";
			SearchResult searchResult1 = connection.search(baseDN, SearchScope.SUB, emailFilter, "dn");
			List<SearchResultEntry> results = searchResult1.getSearchEntries();
			Log.i(TAG, "fetchContacts: Found " + results.size() + " results for this contact (filter was "+emailFilter+")");

			// don't get more than 50 DNs
			for(int i = 0; i < results.size() && i < 50; i++) {
				SearchResultEntry result = results.get(i);
				DNs.add(result.getDN());
				Log.i(TAG, "fetchContacts: Found in directory: dn="+result.getDN()+")");
			}
	
			Log.i(TAG, "fetchContacts: Searching for " + DNs.size() + " DNs in the directory");
			for(final String dn : DNs) {
	
				Log.i(TAG, "fetchContacts: DN search base string: " + dn);
				SearchResult searchResult2 = connection.search(dn, SearchScope.BASE, searchFilter, getUsedAttributes(mappingBundle));
				//Log.i(TAG, searchResult.getEntryCount() + " entries returned for this DN.");
	
				for (SearchResultEntry e : searchResult2.getSearchEntries()) {
					Contact u = Contact.valueOf(e, mappingBundle);
					if (u != null) {
						friendList.add(u);
					}
				}
			}
		} catch (LDAPException e) {
			Log.v(TAG, "LDAPException on fetching contacts", e);
			NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
			int icon = R.drawable.icon;
			CharSequence tickerText = "Error on " + org.mbs3.android.ufpb2.Constants.ACCOUNT_NAME;
			Notification notification = new Notification(icon, tickerText, System.currentTimeMillis());
			Intent notificationIntent = new Intent(context, SyncService.class);
			PendingIntent contentIntent = PendingIntent.getService(context, 0, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);
			notification.setLatestEventInfo(context, tickerText, e.getMessage().replace("\\n", " "), contentIntent);
			notification.flags = Notification.FLAG_AUTO_CANCEL;
			mNotificationManager.notify(0, notification);
			return null;
		} finally {
			if (connection != null) {
				connection.close();
			}
		}
	
		return friendList;
	}
}
