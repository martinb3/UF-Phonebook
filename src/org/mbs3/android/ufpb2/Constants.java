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

package org.mbs3.android.ufpb2;

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
	public static final String ACCOUNT_TYPE = "org.mbs3.android.ufpb2";

	/**
	 * Authtoken type string.
	 */
	public static final String AUTHTOKEN_TYPE = "org.mbs3.android.ufpb2";

	/**
	 * SD card LDAPSync folder.
	 */
	public static final String SDCARD_FOLDER = "/" + ACCOUNT_NAME.replace(" ", "");

	public static final String PROFILE_MIME_TYPE = "vnd.android.cursor.item/vnd.org.mbs3.android.ufpb.profile";

	public static final int DIALOG_RESYNC = 1;
	public static final int DIALOG_APP_MAIN = 2;
	public static final int DIALOG_ADD_CONTACT = 3;
	
	public static final String CUSTOM_CONTACT_DATA="org.mbs3.android.ufpb2.CUSTOM_CONTACT_DATA";
}
