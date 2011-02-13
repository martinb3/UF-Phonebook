package org.mbs3.android.ufpb.syncadapter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import org.mbs3.android.ufpb.Constants;
import org.mbs3.android.ufpb.authenticator.LDAPAuthenticatorActivity;
import org.mbs3.android.ufpb.client.Contact;
import org.mbs3.android.ufpb.client.LDAPServerInstance;
import org.mbs3.android.ufpb.client.LDAPUtilities;
import org.mbs3.android.ufpb.platform.ContactManager;

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
import android.util.Log;

/**
 * SyncAdapter implementation for synchronizing LDAP contacts to the platform ContactOperations provider.
 * 
 * @author <a href="mailto:daniel.weisser@gmx.de">Daniel Weisser</a>
 */
public class SyncAdapter extends AbstractThreadedSyncAdapter {
	private static final String TAG = "SyncAdapter";

	private final AccountManager mAccountManager;
	private final Context mContext;

	private Date mLastUpdated;

	public SyncAdapter(Context context, boolean autoInitialize) {
		super(context, autoInitialize);
		mContext = context;
		mAccountManager = AccountManager.get(context);
	}

	@Override
	public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {
		Logger l = new Logger();
		l.startLogging();
		
		l.d("Start the sync");
		Log.d(TAG, "Start the sync.");
		
		
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
			
			ContactManager cm = new ContactManager(l);
			HashMap<Long, ArrayList<String>> emails = cm.getAllAccountEmailAddresses(mContext.getContentResolver(), account.name);
			Log.d(TAG, "Now that I've found all emails, calling fetch for LDAP data");
			users = LDAPUtilities.fetchContacts(ldapServer, emails, baseDN, searchFilter, mappingBundle, mLastUpdated, this.getContext());
			
			if (users == null) {
				syncResult.stats.numIoExceptions++;
				return;
			}
			// update the last synced date.
			mLastUpdated = new Date();
			// update platform contacts.
			String msg = "Calling contactManager's sync contacts for " + account.name + " with " + users.size() + " users";
			Log.d(TAG, msg);
			l.d(msg);
			
			cm.syncContacts(mContext, account.name, users, syncResult);
			l.stopLogging();
		} catch (final AuthenticatorException e) {
			syncResult.stats.numParseExceptions++;
			Log.e(TAG, "AuthenticatorException", e);
		} catch (final OperationCanceledException e) {
			Log.e(TAG, "OperationCanceledExcetpion", e);
		} catch (final IOException e) {
			Log.e(TAG, "IOException", e);
			syncResult.stats.numIoExceptions++;
		}
	}
}