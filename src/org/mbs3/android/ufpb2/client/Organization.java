package org.mbs3.android.ufpb2.client;

import java.io.Serializable;

import android.text.TextUtils;

public class Organization implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -7027496788077299135L;
	private String officelocation = "";
	private String company = "";
	private String title = "";
	private String primaryAffiliation = "";
	
	public String getPrimaryAffiliation() {
		return primaryAffiliation;
	}

	public void setPrimaryAffiliation(String primaryAffiliation) {
		this.primaryAffiliation = primaryAffiliation;
	}
	
	public void setOfficeLocation(String officelocation) {
		this.officelocation = officelocation;
	}

	public String getOfficeLocation() {
		return officelocation;
	}
	
	public void setCompany(String company) {
		this.company = company;
	}

	public String getCompany() {
		return company;
	}
	
	public void setTitle(String title) {
		this.title = title;
	}

	public String getTitle() {
		return title;
	}
	
	/**
	 * Quick check for an empty address.
	 * 
	 * @return <code>true</code> if all fields are empty.
	 */
	public boolean isEmpty() {
		if (TextUtils.isEmpty(company) && TextUtils.isEmpty(title) && TextUtils.isEmpty(officelocation)) {
			return true;
		}
		return false;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((company == null) ? 0 : company.hashCode());
		result = prime * result + ((officelocation == null) ? 0 : officelocation.hashCode());
		result = prime * result + ((title == null) ? 0 : title.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof Organization))
			return false;
		Organization other = (Organization) obj;
		if (company == null) {
			if (other.company != null)
				return false;
		} else if (!company.equals(other.company))
			return false;
		if (officelocation == null) {
			if (other.officelocation != null)
				return false;
		} else if (!officelocation.equals(other.officelocation))
			return false;
		if (title == null) {
			if (other.title != null)
				return false;
		} else if (!title.equals(other.title))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "Organization [primaryAffiliation=" + primaryAffiliation + ", company=" + company + ", officelocation=" + officelocation + ", title=" + title
				+ "]";
	}
}
