package org.mbs3.android.ufpb.activity;

import org.mbs3.android.ufpb.R;

import de.danielweisser.android.ldapsync.Constants;
import de.danielweisser.android.ldapsync.client.Address;
import de.danielweisser.android.ldapsync.client.Contact;
import de.danielweisser.android.ldapsync.client.Organization;
import de.danielweisser.android.ldapsync.platform.ContactManager;
import de.danielweisser.android.ldapsync.syncadapter.Logger;
import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.util.Linkify;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class Profile extends Activity implements OnClickListener {

	final String TAG = "Profile";
	
	private Contact currentContact = null;
	
	private TextView mDisplayName;
	private TextView mPhone;
	private TextView mGeneral;
	private TextView mAddr;
	private TextView mStaffInfo;
	private Button mButton;
	
	@Override
	protected void onStart() {
		Log.i(TAG, "Started the profile activity");
		super.onStart();
		
		if(getIntent().getData() != null) {
			Cursor cursor = managedQuery(getIntent().getData(), null, null, null, null);
			if(cursor.moveToNext()) {
				Log.i(TAG, "DATA1: " + cursor.getString(cursor.getColumnIndex("DATA1")));
				Log.i(TAG, "DATA2: " + cursor.getString(cursor.getColumnIndex("DATA2")));
				Log.i(TAG, "DATA3: " + cursor.getString(cursor.getColumnIndex("DATA3")));
				Log.i(TAG, "DATA4: " + cursor.getString(cursor.getColumnIndex("DATA4")));
				
				ContactManager cm = new ContactManager(new Logger());
				String ufid = cursor.getString(cursor.getColumnIndex("DATA4"));
				
				String dn = cursor.getString(cursor.getColumnIndex("DATA1"));
				currentContact = cm.getContactByDn(getApplicationContext(), Constants.ACCOUNT_NAME, dn);
				if(currentContact != null) {
					Log.i(TAG, "Contact found for profile activity: " + currentContact);

					// these aren't in the regular phone structured data
					currentContact.setDn(dn);
					currentContact.setUfid(ufid);
					Organization organization = currentContact.getWorkOrganization();
					Address waddr = currentContact.getWorkAddress();
					
					//map everything from contact c into textviews
					mDisplayName.setText(currentContact.getDisplayName());
					
					String general = "";
					for(String email : currentContact.getEmails())
						general += "Email: " + email + "\n";
					
					if(organization.getPrimaryAffiliation() != null && !organization.getPrimaryAffiliation().equals("")) general += "Affiliation: " + organization.getPrimaryAffiliation() + "\n";
					mGeneral.setText(general);
					Linkify.addLinks(mGeneral, Linkify.EMAIL_ADDRESSES);
					
					
					String phone = "";
					if(currentContact.getCellWorkPhone() != null && !currentContact.getCellWorkPhone().equals("")) phone += "Work cell: " + currentContact.getCellWorkPhone() + "\n";
					if(currentContact.getHomePhone() != null && !currentContact.getHomePhone().equals("")) phone += "Home phone: " + currentContact.getHomePhone() + "\n";
					if(currentContact.getWorkPhone() != null && !currentContact.getWorkPhone().equals("")) phone += "Work phone: " + currentContact.getWorkPhone() + "\n";
					mPhone.setText(phone);
					Linkify.addLinks(mPhone, Linkify.PHONE_NUMBERS);

					String addr = "";
					if(waddr != null && !waddr.toString().equals(""))
						addr += "Preferred address: " + waddr.toString() + "\n";
					if(organization.getOfficeLocation() != null && !organization.getOfficeLocation().equals(""))
						addr += (!addr.equals("") ? "\n" : "") + "Office Location: " + organization.getOfficeLocation() + "\n";
					mAddr.setText(addr);
					Linkify.addLinks(mAddr, Linkify.MAP_ADDRESSES);
					
					String staffInfo = "";
					if(organization.getCompany() != null && !organization.getCompany().equals(""))
						staffInfo += "Unit: " + organization.getCompany() + "\n";
					if(organization.getTitle() != null && !organization.getTitle().equals(""))
						staffInfo += "Title: " + organization.getTitle() + "\n";
					mStaffInfo.setText(staffInfo);
				}
				else
					Log.w(TAG, "Contact NOT found for profile activity from DN: " + dn);
			}
			cursor.close();
		}
	}

	public void onCreate(Bundle savedInstanceState) {
		Log.i(TAG, "Created the profile activity");
		super.onCreate(savedInstanceState);
		
	    setContentView(R.layout.fullcontactview);
	    
	    mDisplayName = (TextView) findViewById(R.id.profile_text_name);
	    
	    mGeneral = (TextView) findViewById(R.id.profile_text_general);
	    mPhone = (TextView) findViewById(R.id.profile_text_phone);
	    mAddr = (TextView) findViewById(R.id.profile_text_addresses);
	    mStaffInfo = (TextView) findViewById(R.id.profile_text_staffinfo);
	    
	    mButton = (Button)findViewById(R.id.profile_button_webview);
	    mButton.setOnClickListener(this);
	}
	
	@Override
	protected void onResume() {
		Log.i(TAG, "Resumed the profile activity");
		super.onResume();
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

	@Override
	public void onClick(View v) {
		Log.i(TAG, "Button click received");
		if(currentContact == null || currentContact.getUfid() == null || currentContact.getUfid().equals(""))
			return;
		
		String ufid = currentContact.getUfid();
		String tag = convertUFIDToTag(ufid);
		if(tag != null && !tag.equals("")) {
			String url = "http://phonebook.ufl.edu/people/" + tag + "/";
			
			Intent myIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
			startActivity(myIntent);
		}

	}
	
}
