package org.mbs3.android.ufpb;

import java.util.List;

import org.mbs3.android.ufpb.client.Contact;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class ContactListAdapter extends ArrayAdapter<Contact> {

	public static final String TAG = "ContactListAdapter";
	
	public ContactListAdapter(Context context, int textViewResourceId, Contact[] objects) {
		super(context, textViewResourceId, objects);
	}

	public ContactListAdapter(Context context, int resource, int textViewResourceId, Contact[] objects) {
		super(context, resource, textViewResourceId, objects);
	}

	public ContactListAdapter(Context context, int resource, int textViewResourceId, List<Contact> objects) {
		super(context, resource, textViewResourceId, objects);
	}

	public ContactListAdapter(Context context, int resource, int textViewResourceId) {
		super(context, resource, textViewResourceId);
	}

	public ContactListAdapter(Context context, int textViewResourceId, List<Contact> objects) {
		super(context, textViewResourceId, objects);
	}

	public ContactListAdapter(Context context, int textViewResourceId) {
		super(context, textViewResourceId);
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {

		View v = convertView;
		
		if (v == null) {
			LayoutInflater vi = (LayoutInflater)super.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			v = vi.inflate(R.layout.two_line_list_item, null);
		}
		
		Contact c = getItem(position);
		if (c != null) {
			TextView tt = (TextView) v.findViewById(R.id.text1);
			TextView bt = (TextView) v.findViewById(R.id.text2);
			
			if (tt != null) {
				tt.setText(c.getDisplayName()); 
			}
			
			if(bt != null){
				String line2 = "";
				if(c.getWorkOrganization() != null && c.getWorkOrganization().getPrimaryAffiliation() != null)
					line2=c.getWorkOrganization().getPrimaryAffiliation();
				bt.setText(line2);
			}
		}
		
		return v;
	}

}
