package de.danielweisser.android.ldapsync.client;

import java.io.ByteArrayOutputStream;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;

import com.unboundid.ldap.sdk.ReadOnlyEntry;

/**
 * Represents a LDAPSyncAdapter contact.
 * 
 * @author <a href="mailto:daniel.weisser@gmx.de">Daniel Weisser</a>
 *
 * TODO: attrs
 * o: IT-CNS SA OPEN SYSTEMS GROUP
 * title: IT Expert
 * 
 */
public class Contact {
	public static String DISPLAYNAME = "DISPLAYNAME";
	public static String FIRSTNAME = "FIRSTNAME";
	public static String LASTNAME = "LASTNAME";
	public static String TELEPHONE = "TELEPHONE";
	public static String MOBILE = "MOBILE";
	public static String HOMEPHONE = "HOMEPHONE";
	public static String MAIL = "MAIL";
	public static String PHOTO = "PHOTO";
	public static String STREET = "STREET";
	public static String CITY = "CITY";
	public static String STATE = "STATE";
	public static String ZIP = "ZIP";
	public static String COUNTRY = "COUNTRY";
	public static String UFID = "uflEduUniversityId";
	
	public static String OFFICELOCATION = "UFLEDUOFFICELOCATION";
	public static String COMPANY = "O";
	public static String TITLE = "TITLE";
	
	private String dn = "";
	private String displayName = "";
	private String firstName = "";
	private String lastName = "";
	private String cellWorkPhone = "";
	private String workPhone = "";
	private String homePhone = "";
	private String[] emails = null;
	
	private String ufid = "";
	
	public String getUfid() {
		return ufid;
	}

	public void setUfid(String ufid) {
		this.ufid = ufid;
	}

	private byte[] image = null;
	private Address waddress = null;
	private Organization worg = null;
	

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public String getDn() {
		return dn;
	}

	public void setDn(String dn) {
		this.dn = dn;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public void setCellWorkPhone(String cellWorkPhone) {
		this.cellWorkPhone = cellWorkPhone;
	}

	public String getCellWorkPhone() {
		return cellWorkPhone;
	}

	public String getWorkPhone() {
		return workPhone;
	}

	public void setWorkPhone(String workPhone) {
		this.workPhone = workPhone;
	}

	public void setHomePhone(String homePhone) {
		this.homePhone = homePhone;
	}

	public String getHomePhone() {
		return homePhone;
	}

	public String[] getEmails() {
		return emails;
	}

	public void setEmails(String[] emails) {
		this.emails = emails;
	}

	public byte[] getImage() {
		return image;
	}

	public void setImage(byte[] image) {
		this.image = image;
	}
	
	public void setWorkOrganization(Organization worg) {
		this.worg = worg;
	}

	public Organization getWorkOrganization() {
		return worg;
	}

	public void setWorkAddress(Address waddress) {
		this.waddress = waddress;
	}

	public Address getWorkAddress() {
		return waddress;
	}
	
	/**
	 * Creates and returns an instance of the user from the provided LDAP data.
	 * 
	 * @param user
	 *            The LDAPObject containing user data
	 * @param mB
	 *            Mapping bundle for the LDAP attribute names.
	 * @return user The new instance of LDAP user created from the LDAP data.
	 */
	public static Contact valueOf(ReadOnlyEntry user, Bundle mB) {
		Contact c = new Contact();
		try {
			c.setDn(user.getDN());
			c.setDisplayName(user.hasAttribute(mB.getString(DISPLAYNAME)) ? user.getAttributeValue(mB.getString(DISPLAYNAME)) : null);
			c.setFirstName(user.hasAttribute(mB.getString(FIRSTNAME)) ? user.getAttributeValue(mB.getString(FIRSTNAME)) : null);
			c.setLastName(user.hasAttribute(mB.getString(LASTNAME)) ? user.getAttributeValue(mB.getString(LASTNAME)) : null);
			if ((user.hasAttribute(mB.getString(FIRSTNAME)) ? user.getAttributeValue(mB.getString(FIRSTNAME)) : null) == null
					|| (user.hasAttribute(mB.getString(LASTNAME)) ? user.getAttributeValue(mB.getString(LASTNAME)) : null) == null) {
				return null;
			}
			c.setWorkPhone(user.hasAttribute(mB.getString(TELEPHONE)) ? user.getAttributeValue(mB.getString(TELEPHONE)) : null);
			c.setCellWorkPhone(user.hasAttribute(mB.getString(MOBILE)) ? user.getAttributeValue(mB.getString(MOBILE)) : null);
			c.setHomePhone(user.hasAttribute(mB.getString(HOMEPHONE)) ? user.getAttributeValue(mB.getString(HOMEPHONE)) : null);
			c.setEmails(user.hasAttribute(mB.getString(MAIL)) ? user.getAttributeValues(mB.getString(MAIL)) : null);
			
			c.setUfid(user.hasAttribute(mB.getString(UFID)) ? user.getAttributeValue(mB.getString(UFID)) : null);
			
			byte[] image = null;
			if (user.hasAttribute(mB.getString(PHOTO))) {
				byte[] array = user.getAttributeValueBytes(mB.getString(PHOTO));

				try {
					Bitmap myBitmap = BitmapFactory.decodeByteArray(array, 0, array.length);
					if (myBitmap != null) {
						ByteArrayOutputStream baos = new ByteArrayOutputStream();
						myBitmap.compress(Bitmap.CompressFormat.JPEG, 90, baos);
						image = baos.toByteArray();
					}
				} catch (OutOfMemoryError e) {
					// Do not set an image, when an OutOfMemoryError occurs
					image = null;
					array = null;
				}
			}
			c.setImage(image);

			// Get address
			if(user.hasAttribute(mB.getString(STREET)) && user.getAttributeValue(mB.getString(STREET)).contains("$")) {
				Address a = new Address();
				//LINE1$LINE2$LINE3$CITY, STATE, COUNTRY$ZIP
				String input = user.getAttributeValue(mB.getString(STREET))
					.replace("$", ", ")
					.replace(" FL, US", " FL");
					;
				a.setStreet(input);
				c.setWorkAddress(a);
			}
			else if (user.hasAttribute(mB.getString(STREET)) || user.hasAttribute(mB.getString(CITY)) || user.hasAttribute(mB.getString(STATE))
					|| user.hasAttribute(mB.getString(ZIP)) || user.hasAttribute(mB.getString(COUNTRY))) {
				Address a = new Address();
				a.setStreet(user.hasAttribute(mB.getString(STREET)) ? user.getAttributeValue(mB.getString(STREET)) : null);
				a.setCity(user.hasAttribute(mB.getString(CITY)) ? user.getAttributeValue(mB.getString(CITY)) : null);
				a.setState(user.hasAttribute(mB.getString(STATE)) ? user.getAttributeValue(mB.getString(STATE)) : null);
				a.setZip(user.hasAttribute(mB.getString(ZIP)) ? user.getAttributeValue(mB.getString(ZIP)) : null);
				a.setCountry(user.hasAttribute(mB.getString(COUNTRY)) ? user.getAttributeValue(mB.getString(COUNTRY)) : null);
				c.setWorkAddress(a);
			}
			
			// Get organization
			if(user.hasAttribute(mB.getString(OFFICELOCATION)) || user.hasAttribute(mB.getString(COMPANY)) || user.hasAttribute(mB.getString(TITLE))) {

				Organization o = new Organization();
				o.setCompany(user.hasAttribute(mB.getString(COMPANY)) ? user.getAttributeValue(mB.getString(COMPANY)) : null);
				o.setTitle(user.hasAttribute(mB.getString(TITLE)) ? user.getAttributeValue(mB.getString(TITLE)) : null);

				//LINE1$LINE2$LINE3$CITY, STATE, COUNTRY$ZIP
				String input = user.getAttributeValue(mB.getString(OFFICELOCATION))
					.replace("$", ", ")
					.replace(" FL, US", " FL");
					;
					
				o.setOfficeLocation(input);
				c.setWorkOrganization(o);
			}
		} catch (final Exception ex) {
			Log.i("User", "Error parsing LDAP user object: " + ex.toString());
			for(StackTraceElement ste : ex.getStackTrace()) {
				Log.i("User", "Element: " + ste.toString());
			}
		}
		return c;
	}

	@Override
	public String toString() {
		return "Contact [displayName=" + displayName + ", dn=" + dn + "]";
	}
}
