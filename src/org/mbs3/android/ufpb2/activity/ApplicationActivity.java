package org.mbs3.android.ufpb2.activity;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashSet;

import org.mbs3.android.ufpb2.R;
import org.mbs3.android.ufpb2.Constants;
import org.mbs3.android.ufpb2.ContactListAdapter;
import org.mbs3.android.ufpb2.authenticator.LDAPAuthenticatorActivity;
import org.mbs3.android.ufpb2.client.Contact;
import org.mbs3.android.ufpb2.client.LDAPServerInstance;
import org.mbs3.android.ufpb2.client.LDAPUtilities;

import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.ListView;

public class ApplicationActivity extends ListActivity {
	private final String TAG = "ApplicationActivity";
	
	private ProgressDialog dialog;
	private String lastSearch = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		requestWindowFeature(Window.FEATURE_PROGRESS);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		
		dialog = new ProgressDialog(this);
		
		super.setContentView(R.layout.results_list);
		super.onCreate(savedInstanceState);
	}
	
	@Override
	protected void onStart() {
		Log.i(TAG, "Started "+TAG);
		super.onStart();

		Intent startedIntent = getIntent();
		Log.i(TAG, "Reading intent: " + startedIntent);

		
		if(startedIntent != null && startedIntent.getAction().equals(Intent.ACTION_MAIN)) {
			AccountManager acctMgr = AccountManager.get(getApplicationContext());
			Account [] accounts = acctMgr.getAccountsByType(Constants.ACCOUNT_TYPE);
			if(accounts == null || accounts.length <= 0)
				showDialog(Constants.DIALOG_APP_MAIN);
			else
				onSearchRequested();
		}
		
		else if(startedIntent != null && startedIntent.getAction().equals(Intent.ACTION_SEARCH)) {
			String query = startedIntent.getStringExtra(SearchManager.QUERY);
			
			// a repeat?
			if(lastSearch != null && lastSearch.equals(query) && getListAdapter().getCount() > 0) {
				return;
			}

			Log.i(TAG, "Search query: " + query);
			lastSearch = query;
			new ContactsBackgroundTask().execute(query);
		}
		
		else {
			finish();
		}
	}
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		Contact c = (Contact)l.getItemAtPosition(position);
		
		Log.i(TAG, "List item clicked: " + c);
		//super.onListItemClick(l, v, position, id);
		
		/*test for preferences
		 * startActivity(new Intent(getBaseContext(),PreferenceActivity.class));
		if(true)
		return;*/

		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.putExtra(Constants.CUSTOM_CONTACT_DATA, (Serializable)c);
		intent.setClass(getApplicationContext(), ProfileActivity.class);
		startActivity(intent);
	}
	
	private Contact[] doBigSearch(String searchTerms) {
		
		Context mContext = getApplicationContext();
		AccountManager mAccountManager = AccountManager.get(mContext);
		
		Account [] accounts = mAccountManager.getAccountsByType(Constants.ACCOUNT_TYPE);
		Account account = accounts[0];
		
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
			
			HashSet<Contact> users = LDAPUtilities.searchContacts(ldapServer, searchTerms, baseDN, searchFilter, mappingBundle, mContext);
			
			if(users != null) {
				// update platform contacts.
				String msg = "Found search results for " + account.name + " with " + users.size() + " users";
				Log.d(TAG, msg);
			
				return users.toArray(new Contact[]{});
			}
			else {
				
			}
		} catch (final AuthenticatorException e) {
			Log.e(TAG, "AuthenticatorException", e);
		} catch (final OperationCanceledException e) {
			Log.e(TAG, "OperationCanceledExcetpion", e);
		} catch (final IOException e) {
			Log.e(TAG, "IOException", e);
		}
		return new Contact[0];
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		Dialog dialog;
	    switch(id) {
	    case Constants.DIALOG_APP_MAIN:
	    	AlertDialog.Builder builder = new AlertDialog.Builder(this);
	    	builder.setMessage(R.string.search_activity_account)
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
	
	public class ContactsBackgroundTask extends AsyncTask <String, Void, Contact[]> {
		@Override
		protected void onPreExecute() {
			dialog.setMessage(" Searching the UF Phonebook");
			dialog.show();
		}
		
		@Override
		protected void onPostExecute(Contact[] results) {
            dialog.dismiss();
            setListAdapter(new ContactListAdapter(
					getApplicationContext(), 
					R.layout.two_line_list_item, 
					results
					));
		}
		
		@Override
		protected Contact[] doInBackground(String... params) {
			Contact[] results = doBigSearch(params[0]);
			return results;
		}

	}
	
}
