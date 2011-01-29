package org.mbs3.android.ufpb.platform;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import org.mbs3.android.ufpb.Constants;
import org.mbs3.android.ufpb.client.Address;
import org.mbs3.android.ufpb.client.Contact;
import org.mbs3.android.ufpb.syncadapter.Logger;

import android.content.ContentProviderClient;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.SyncResult;
import android.content.ContentProviderOperation.Builder;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.provider.ContactsContract.AggregationExceptions;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.Groups;
import android.provider.ContactsContract.RawContacts;
import android.provider.ContactsContract.Settings;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Organization;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.Photo;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal;
import android.util.Log;

/**
 * Class for managing contacts sync related operations
 * 
 * @author <a href="mailto:daniel.weisser@gmx.de">Daniel Weisser</a>
 */
public class ContactManager {
	private static final String TAG = "ContactManager";
	private Logger l;

	public ContactManager(Logger l) {
		this.l = l;
	}

	/**
	 * Synchronize raw contacts
	 * 
	 * @param context
	 *            The context of Authenticator Activity
	 * @param accountName
	 *            The account name
	 * @param contacts
	 *            The list of retrieved LDAP contacts
	 * @param syncResult
	 *            SyncResults for tracking the sync
	 */
	public synchronized void syncContacts(Context context, String accountName, HashMap<Contact, Long> contacts, SyncResult syncResult) {
		final ContentResolver resolver = context.getContentResolver();

		// Get all phone contacts for the LDAP account
		HashMap<String, Long> contactsOnPhone = getAllContactsOnPhone(resolver, accountName);
		
		// Update and create new contacts
		for (final Entry<Contact,Long> entry : contacts.entrySet()) {
			final Contact contact = entry.getKey();
			
			if (contactsOnPhone.containsKey(contact.getDn())) {
				Long contactId = contactsOnPhone.get(contact.getDn());
				String msg = "syncContacts: Update contact under account "+accountName+": " + contact + " (" + contactId + ")";
				Log.d(TAG, msg);
				l.d(msg);
				updateContact(resolver, contactId, contact);
				
				// update aggregation 
				// bind our contactId and entry.getValue() (contact id from origin)
				bindAggregation(resolver, entry.getValue(), contactId);
				
				syncResult.stats.numUpdates++;
				contactsOnPhone.remove(contact.getDn());
			} else {
				String msg = "syncContacts: Add contact under account "+accountName+": " + contact;
				Log.d(TAG, msg);
				l.d(msg);
				Long contactId = addContact(resolver, contact);
				
				// update aggregation
				// bind our contactId and entry.getValue() (contact id from origin)
				bindAggregation(resolver, entry.getValue(), contactId);
				syncResult.stats.numInserts++;
			}
		}

		// Delete contacts
		for (Entry<String, Long> contact : contactsOnPhone.entrySet()) {
			Log.d(TAG, "Delete contact: " + contact.getKey());
			deleteContact(resolver, contact.getValue());
			l.d("Delete contact: " + contact.getKey() + "(" + contact.getValue() + ")");
			syncResult.stats.numDeletes++;
		}
	}

	private void mapCursorToContact(final Cursor c, Contact existingContact) {
		if (c != null) {
			while (c.moveToNext()) {
				String mimetype = c.getString(c.getColumnIndex(Data.MIMETYPE));
				if (mimetype.equals(StructuredName.CONTENT_ITEM_TYPE)) {
					existingContact.setDisplayName(c.getString(c.getColumnIndex(Data.DATA1)));
					existingContact.setFirstName(c.getString(c.getColumnIndex(Data.DATA2)));
					existingContact.setLastName(c.getString(c.getColumnIndex(Data.DATA3)));
				} else if (mimetype.equals(Email.CONTENT_ITEM_TYPE)) {
					int type = c.getInt(c.getColumnIndex(Data.DATA2));
					if (type == Email.TYPE_WORK) {
						String[] mails = new String[] { c.getString(c.getColumnIndex(Data.DATA1)) };
						existingContact.setEmails(mails);
					}
				} else if (mimetype.equals(Phone.CONTENT_ITEM_TYPE)) {
					int type = c.getInt(c.getColumnIndex(Data.DATA2));
					if (type == Phone.TYPE_WORK_MOBILE) {
						existingContact.setCellWorkPhone(c.getString(c.getColumnIndex(Data.DATA1)));
					} else if (type == Phone.TYPE_WORK) {
						existingContact.setWorkPhone(c.getString(c.getColumnIndex(Data.DATA1)));
					} else if (type == Phone.TYPE_HOME) {
						existingContact.setHomePhone(c.getString(c.getColumnIndex(Data.DATA1)));
					}
				} else if (mimetype.equals(Photo.CONTENT_ITEM_TYPE)) {
					existingContact.setImage(c.getBlob(c.getColumnIndex(Photo.PHOTO)));
				} else if (mimetype.equals(StructuredPostal.CONTENT_ITEM_TYPE)) {
					int type = c.getInt(c.getColumnIndex(Data.DATA2));
					Address address = new Address();
					address.setStreet(c.getString(c.getColumnIndex(Data.DATA4)));
					address.setCity(c.getString(c.getColumnIndex(Data.DATA7)));
					address.setCountry(c.getString(c.getColumnIndex(Data.DATA10)));
					address.setZip(c.getString(c.getColumnIndex(Data.DATA9)));
					address.setState(c.getString(c.getColumnIndex(Data.DATA8)));
					if (type == StructuredPostal.TYPE_WORK) {
						existingContact.setWorkAddress(address);
					}
				} else if (mimetype.equals(Organization.CONTENT_ITEM_TYPE)) { //organization
					int type = c.getInt(c.getColumnIndex(Data.DATA2));
					org.mbs3.android.ufpb.client.Organization org = new org.mbs3.android.ufpb.client.Organization();
					org.setTitle(c.getString(c.getColumnIndex(Data.DATA4)));
					org.setCompany(c.getString(c.getColumnIndex(Data.DATA1)));
					org.setOfficeLocation(c.getString(c.getColumnIndex(Data.DATA9)));
					org.setPrimaryAffiliation(c.getString(c.getColumnIndex(Organization.JOB_DESCRIPTION)));
					if (type == Organization.TYPE_WORK) {
						existingContact.setWorkOrganization(org);
					}
				}
			}
		}
	}
	
	public Contact getContactByDn(Context context, String accountName, String dn) {
		
		final String selection = Data.RAW_CONTACT_ID + "=?" + " AND " + RawContacts.ACCOUNT_NAME + "=?";
		final String[] projection = new String[] { Data.MIMETYPE, Data.DATA1, Data.DATA2, Data.DATA3, Data.DATA4, Data.DATA5, Data.DATA6, Data.DATA7, Data.DATA8, Data.DATA9, Data.DATA10, Data.DATA15 };
		
		try {
			final ContentResolver resolver = context.getContentResolver();

			// Get all phone contacts for the LDAP account
			HashMap<String, Long> contactsOnPhone = getAllContactsOnPhone(resolver, accountName);
			
			Long contactId = contactsOnPhone.get(dn);
			final Cursor c = resolver.query(Data.CONTENT_URI, projection, selection, new String[] { contactId + "", accountName }, null);
			
			Log.i(TAG, "getContactByDn: Fetching full contact for profile using DN " + dn + ", account "+accountName+", and raw ID " + contactId + " (resulted in "+c.getCount()+" raw contacts)");
			
			Contact existingContact = null;
			if(c.getCount() > 0) {
				existingContact = new Contact();
				mapCursorToContact(c, existingContact);
				existingContact.setDn(dn);
			}
			c.close();
			return existingContact;
		} catch (SQLiteException e) {
			Log.e(TAG, e.getMessage(), e);
		} catch (IllegalStateException e) {
			Log.e(TAG, e.getMessage(), e);
		}
		
		return null;
	}
	
	private void updateContact(ContentResolver resolver, long rawContactId, Contact contact) {
		ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();

		Contact existingContact = new Contact();

		final String selection = Data.RAW_CONTACT_ID + "=?";
		final String[] projection = new String[] { Data.MIMETYPE, Data.DATA1, Data.DATA2, Data.DATA3, Data.DATA4, Data.DATA5, Data.DATA6, Data.DATA7, Data.DATA8, Data.DATA9, Data.DATA10, Data.DATA15 };
		
		try {
			final Cursor c = resolver.query(Data.CONTENT_URI, projection, selection, new String[] { rawContactId + "" }, null);

			mapCursorToContact(c, existingContact);
			prepareFields(rawContactId, contact, existingContact, ops, false);

			if (ops.size() > 0) {
				resolver.applyBatch(ContactsContract.AUTHORITY, ops);
			}
		} catch (RemoteException e) {
			Log.e(TAG, e.getMessage(), e);
		} catch (OperationApplicationException e) {
			Log.e(TAG, e.getMessage(), e);
		} catch (SQLiteException e) {
			Log.e(TAG, e.getMessage(), e);
		} catch (IllegalStateException e) {
			Log.e(TAG, e.getMessage(), e);
		}
	}

	private void deleteContact(ContentResolver resolver, Long rawContactId) {
		try {
			resolver.delete(RawContacts.CONTENT_URI, RawContacts._ID + "=?", new String[] { "" + rawContactId });
		} catch (SQLiteException e) {
			Log.e(TAG, e.getMessage(), e);
		} catch (IllegalStateException e) {
			Log.e(TAG, e.getMessage(), e);
		}
	}

	/**
	 * Retrieves all contacts that are on the phone for this account.
	 * 
	 * @return
	 */
	private static HashMap<String, Long> getAllContactsOnPhone(ContentResolver resolver, String accountName) {
		final String[] projection = new String[] { RawContacts._ID, RawContacts.SYNC1, RawContacts.SOURCE_ID };
		final String selection = RawContacts.ACCOUNT_NAME + "=?";

		final Cursor c = resolver.query(RawContacts.CONTENT_URI, projection, selection, new String[] { accountName }, null);
		HashMap<String, Long> contactsOnPhone = new HashMap<String, Long>();
		if (c != null) {
			while (c.moveToNext()) {
				String srcId = c.getString(c.getColumnIndex(RawContacts.SOURCE_ID));
				long id = c.getLong(c.getColumnIndex(RawContacts._ID));
				contactsOnPhone.put(srcId, id);
			}
			c.close();
		}
		
		Log.d(TAG, "Found all contacts of account name " + accountName + ": #="+contactsOnPhone.size());
		return contactsOnPhone;
	}

	private Uri addCallerIsSyncAdapterFlag(Uri uri) {
		Uri.Builder b = uri.buildUpon();
		b.appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true");
		return b.build();
	}

	/**
	 * Add a new contact to the RawContacts table.
	 * 
	 * @param resolver
	 * @param accountName
	 * @param contact
	 */
	private long addContact(ContentResolver resolver, Contact contact) {
		ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();

		Uri uri = addCallerIsSyncAdapterFlag(RawContacts.CONTENT_URI);

		ContentValues cv = new ContentValues();
		cv.put(RawContacts.ACCOUNT_TYPE, Constants.ACCOUNT_TYPE);
		cv.put(RawContacts.ACCOUNT_NAME, Constants.ACCOUNT_NAME);
		cv.put(RawContacts.SOURCE_ID, contact.getDn());
		cv.put(RawContacts.SYNC1, contact.getUfid());

		// This is the first insert into the raw contacts table
		Builder builder = ContentProviderOperation.newInsert(uri).withValues(cv);
		ContentProviderOperation i1 = builder.build();
		
		ops.add(i1);
		prepareFields(-1, contact, new Contact(), ops, true);

		// Now create the contact with a single batch operation
		try {
			
			ContentProviderResult[] res = resolver.applyBatch(ContactsContract.AUTHORITY, ops);
			// The first insert is the one generating the ID for this contact
			long id = ContentUris.parseId(res[0].uri);
			
			String msg = "The new contact has id: " + id;
			l.d(msg);
			Log.d(TAG, msg);
			
			
			return id;
		} catch (Exception e) {
			Log.e(TAG, "Cannot create contact ", e);
		}
		
		return -1;
	}

	private void prepareFields(long rawContactId, Contact newC, Contact existingC, ArrayList<ContentProviderOperation> ops, boolean isNew) {
		ContactMerger contactMerger = new ContactMerger(rawContactId, newC, existingC, ops, l);
		contactMerger.updateName();
		contactMerger.updateMail(Email.TYPE_WORK);

		contactMerger.updatePhone(Phone.TYPE_WORK_MOBILE);
		contactMerger.updatePhone(Phone.TYPE_WORK);
		contactMerger.updatePhone(Phone.TYPE_HOME);

		contactMerger.updateAddress(StructuredPostal.TYPE_WORK);
		
		contactMerger.updateOrganization(Organization.TYPE_WORK);

		contactMerger.updatePicture();
		contactMerger.updateCustomProfile();
	}

	public static void makeGroupVisible(String accountName, ContentResolver resolver) {
		try {
			ContentProviderClient client = resolver.acquireContentProviderClient(ContactsContract.AUTHORITY_URI);
			ContentValues cv = new ContentValues();
			cv.put(Groups.ACCOUNT_NAME, accountName);
			cv.put(Groups.ACCOUNT_TYPE, Constants.ACCOUNT_TYPE);
			cv.put(Settings.UNGROUPED_VISIBLE, true);
			client.insert(Settings.CONTENT_URI.buildUpon().appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true").build(), cv);
		} catch (RemoteException e) {
			Log.d(TAG, "Cannot make the Group Visible");
		}
	}

	public HashMap<Long,ArrayList<String>> getAllAccountEmailAddresses(ContentResolver resolver, String exceptAccountName) {
		Log.d(TAG, "getAllAccountEmailAddresses: Searching for email addrs to sync except from account name " + exceptAccountName);
		
		HashMap<Long,ArrayList<String>> addrs = new HashMap<Long,ArrayList<String>>();
		
		Cursor accountCursor = resolver.query(RawContacts.CONTENT_URI, 
				new String[]{RawContacts._ID},
				RawContacts.ACCOUNT_NAME+" !=? OR " + RawContacts.ACCOUNT_NAME+" IS NULL",
				new String[]{exceptAccountName}, null);
		
		Log.d(TAG, "getAllAccountEmailAddresses: Found " + accountCursor.getCount() + " accounts when looking for email addresses that aren't part of account " + exceptAccountName);
		while(accountCursor.moveToNext()) {
			long id = accountCursor.getLong(accountCursor.getColumnIndex(RawContacts._ID));
			
			ArrayList<String> emails = new ArrayList<String>();
			Cursor emailCursor = resolver.query(ContactsContract.CommonDataKinds.Email.CONTENT_URI, 
					new String[] {
					Email.DATA1,
					Email.DATA2,
				}, 
				Email.RAW_CONTACT_ID +"=?",
				new String[]{id+""},
				null);
			
			while(emailCursor.moveToNext()) {
				String DATA2 = emailCursor.getString(emailCursor.getColumnIndex(Email.DATA1));
				// email type, we don't care
				//int DATA3 = c1.getInt(2);
				
				emails.add(DATA2);
				Log.d(TAG, "getAllAccountEmailAddresses: Found email " + DATA2 + " from raw contact ID " + id);
			}
			
			if(emails.size() > 0)
				addrs.put(id, emails);
			emailCursor.close();
		}
		accountCursor.close();
		return addrs;
	}
	
	private void bindAggregation(ContentResolver resolver, long raw1, long raw2) {
		
		Log.i(TAG, "Binding raw contact IDs " + raw1 + " and " + raw2);
		
		ContentValues values = new ContentValues();
        values.put(AggregationExceptions.RAW_CONTACT_ID1, raw1);
        values.put(AggregationExceptions.RAW_CONTACT_ID2, raw2);
        values.put(AggregationExceptions.TYPE, AggregationExceptions.TYPE_KEEP_TOGETHER);
		try {
			resolver.update(AggregationExceptions.CONTENT_URI, values, null, null);
		}
		catch (Exception ex) {
			Log.i(TAG, "Failed aggregation for raw1=" + raw1 + ", raw2=" + raw2, ex);
		}
	}
}
