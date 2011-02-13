package org.mbs3.android.ufpb;

/**
 * Constants for the LDAP sync adapter.
 * 
 * @author <a href="mailto:daniel.weisser@gmx.de">Daniel Weisser</a>
 */
public class Constants {

	
	
	public static final String ACCOUNT_NAME = "UF Phonebook Sync";
	
	/**
	 * Account type string.
	 */
	public static final String ACCOUNT_TYPE = "org.mbs3.android.ufpb";

	/**
	 * Authtoken type string.
	 */
	public static final String AUTHTOKEN_TYPE = "org.mbs3.android.ufpb";

	/**
	 * SD card LDAPSync folder.
	 */
	public static final String SDCARD_FOLDER = "/" + ACCOUNT_NAME.replace(" ", "");

	public static final String PROFILE_MIME_TYPE = "vnd.android.cursor.item/vnd.org.mbs3.android.ufpb.profile";

	public static final int DIALOG_RESYNC = 1;
	public static final int DIALOG_APP_MAIN = 2;
	public static final int DIALOG_ADD_CONTACT = 3;
	
	public static final String CUSTOM_CONTACT_DATA="org.mbs3.android.ufpb.CUSTOM_CONTACT_DATA";
}
