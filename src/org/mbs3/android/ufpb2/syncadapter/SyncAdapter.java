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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.mbs3.android.ufpb2.Constants;
import org.mbs3.android.ufpb2.authenticator.LDAPAuthenticatorActivity;
import org.mbs3.android.ufpb2.client.Contact;
import org.mbs3.android.ufpb2.client.LDAPServerInstance;
import org.mbs3.android.ufpb2.client.LDAPUtilities;
import org.mbs3.android.ufpb2.platform.ContactManager;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.SyncResult;
import android.os.Bundle;
import android.text.TextUtils;

/**
 * SyncAdapter implementation for synchronizing LDAP contacts to the platform ContactOperations provider.
 * 
 * @author <a href="mailto:daniel.weisser@gmx.de">Daniel Weisser</a>
 */
public class SyncAdapter extends AbstractThreadedSyncAdapter {
	private static final String TAG = "SyncAdapter";

	private final AccountManager mAccountManager;
	private final Context mContext;

	//private Date mLastUpdated;

	public SyncAdapter(Context context, boolean autoInitialize) {
		super(context, autoInitialize);
		mContext = context;
		mAccountManager = AccountManager.get(context);
	}

	@Override
	public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {
		Logger l = Logger.getLogger(getContext());
		
		l.d(TAG,"Start the sync");
		HashMap<Contact, Long> users = new HashMap<Contact, Long>();
		String authtoken = null;
		try {
			// use the account manager to request the credentials
			authtoken = mAccountManager.blockingGetAuthToken(account, Constants.AUTHTOKEN_TYPE, true /* notifyAuthFailure */);
			final String host = mAccountManager.getUserData(account, LDAPAuthenticatorActivity.PARAM_HOST);
			final String username = mAccountManager.getUserData(account, LDAPAuthenticatorActivity.PARAM_USERNAME);
			final int port = Integer.parseInt(mAccountManager.getUserData(account, LDAPAuthenticatorActivity.PARAM_PORT));
			final String sEnc = mAccountManager.getUserData(account, LDAPAuthenticatorActivity.PARAM_ENCRYPTION);
			int encryption = 0;
			if (!TextUtils.isEmpty(sEnc)) {
				encryption = Integer.parseInt(sEnc);
			}
			LDAPServerInstance ldapServer = new LDAPServerInstance(host, port, encryption, username, authtoken);

			final String searchFilter = mAccountManager.getUserData(account, LDAPAuthenticatorActivity.PARAM_SEARCHFILTER);
			final String baseDN = mAccountManager.getUserData(account, LDAPAuthenticatorActivity.PARAM_BASEDN);

			// LDAP name mappings
			final Bundle mappingBundle = new Bundle();
			mappingBundle.putString(Contact.DISPLAYNAME, mAccountManager.getUserData(account, LDAPAuthenticatorActivity.PARAM_MAPPING + Contact.DISPLAYNAME));
			mappingBundle.putString(Contact.FIRSTNAME, mAccountManager.getUserData(account, LDAPAuthenticatorActivity.PARAM_MAPPING + Contact.FIRSTNAME));
			mappingBundle.putString(Contact.LASTNAME, mAccountManager.getUserData(account, LDAPAuthenticatorActivity.PARAM_MAPPING + Contact.LASTNAME));
			mappingBundle.putString(Contact.TELEPHONE, mAccountManager.getUserData(account, LDAPAuthenticatorActivity.PARAM_MAPPING + Contact.TELEPHONE));
			mappingBundle.putString(Contact.MOBILE, mAccountManager.getUserData(account, LDAPAuthenticatorActivity.PARAM_MAPPING + Contact.MOBILE));
			mappingBundle.putString(Contact.HOMEPHONE, mAccountManager.getUserData(account, LDAPAuthenticatorActivity.PARAM_MAPPING + Contact.HOMEPHONE));
			mappingBundle.putString(Contact.MAIL, mAccountManager.getUserData(account, LDAPAuthenticatorActivity.PARAM_MAPPING + Contact.MAIL));
			mappingBundle.putString(Contact.PHOTO, mAccountManager.getUserData(account, LDAPAuthenticatorActivity.PARAM_MAPPING + Contact.PHOTO));
			mappingBundle.putString(Contact.STREET, mAccountManager.getUserData(account, LDAPAuthenticatorActivity.PARAM_MAPPING + Contact.STREET));
			mappingBundle.putString(Contact.CITY, mAccountManager.getUserData(account, LDAPAuthenticatorActivity.PARAM_MAPPING + Contact.CITY));
			mappingBundle.putString(Contact.ZIP, mAccountManager.getUserData(account, LDAPAuthenticatorActivity.PARAM_MAPPING + Contact.ZIP));
			mappingBundle.putString(Contact.STATE, mAccountManager.getUserData(account, LDAPAuthenticatorActivity.PARAM_MAPPING + Contact.STATE));
			mappingBundle.putString(Contact.COUNTRY, mAccountManager.getUserData(account, LDAPAuthenticatorActivity.PARAM_MAPPING + Contact.COUNTRY));
			mappingBundle.putString(Contact.OFFICELOCATION, mAccountManager.getUserData(account, LDAPAuthenticatorActivity.PARAM_MAPPING + Contact.OFFICELOCATION));
			mappingBundle.putString(Contact.COMPANY, mAccountManager.getUserData(account, LDAPAuthenticatorActivity.PARAM_MAPPING + Contact.COMPANY));
			mappingBundle.putString(Contact.TITLE, mAccountManager.getUserData(account, LDAPAuthenticatorActivity.PARAM_MAPPING + Contact.TITLE));
			mappingBundle.putString(Contact.UFID, mAccountManager.getUserData(account, LDAPAuthenticatorActivity.PARAM_MAPPING + Contact.UFID));
			mappingBundle.putString(Contact.PRIMARYAFFILIATION, mAccountManager.getUserData(account, LDAPAuthenticatorActivity.PARAM_MAPPING + Contact.PRIMARYAFFILIATION));
			
			ContactManager cm = new ContactManager();
			HashMap<Long, ArrayList<String>> emails = cm.getAllAccountEmailAddresses(mContext.getContentResolver(), account.name);
			l.d(TAG, "Now that I've found all emails, calling fetch for LDAP data");
			HashMap<Contact, Long> resUsers = LDAPUtilities.fetchContacts(ldapServer, emails, baseDN, searchFilter, mappingBundle, this.getContext());
			if(resUsers != null && resUsers.size() > 0)
				users.putAll(resUsers);
			else if(resUsers.size() != 0)
				syncResult.stats.numIoExceptions++;
			
			// update platform contacts.
			String msg = "Calling contactManager's sync contacts for " + account.name + " with " + users.size() + " users";
			l.d(TAG,msg);
			
			cm.syncContacts(mContext, account.name, users, syncResult);
		} catch (final AuthenticatorException e) {
			syncResult.stats.numParseExceptions++;
			l.e(TAG, "AuthenticatorException", e);
		} catch (final OperationCanceledException e) {
			l.e(TAG, "OperationCanceledExcetpion", e);
		} catch (final IOException e) {
			l.e(TAG, "IOException", e);
			syncResult.stats.numIoExceptions++;
		}
	}
}
