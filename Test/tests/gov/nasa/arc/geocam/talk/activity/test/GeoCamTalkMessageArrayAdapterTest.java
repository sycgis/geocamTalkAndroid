// __BEGIN_LICENSE__
// Copyright (C) 2008-2010 United States Government as represented by
// the Administrator of the National Aeronautics and Space Administration.
// All Rights Reserved.
// __END_LICENSE__

package gov.nasa.arc.geocam.talk.activity.test;

import gov.nasa.arc.geocam.talk.R;
import gov.nasa.arc.geocam.talk.activity.GeoCamTalkMessageAdapter;
import gov.nasa.arc.geocam.talk.bean.GeoCamTalkMessage;
import gov.nasa.arc.geocam.talk.injected.FakeGeoCamTalkMessageFactory;
import gov.nasa.arc.geocam.talk.test.GeoCamTestCase;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.xtremelabs.robolectric.Robolectric;

public class GeoCamTalkMessageArrayAdapterTest extends GeoCamTestCase{
	
	@Test
	public void shouldProperlyDisplayGeolocaionStatus() throws Exception {
		//arrange
		List<GeoCamTalkMessage> msgs = new ArrayList<GeoCamTalkMessage>();
		
		msgs.add(FakeGeoCamTalkMessageFactory.getMessage("testing", "Patrick", true));
		msgs.add(FakeGeoCamTalkMessageFactory.getMessage("testing2", "Not Patrick", false));

		GeoCamTalkMessageAdapter adapter = new GeoCamTalkMessageAdapter(
				Robolectric.application.getApplicationContext()); 
		adapter.setTalkMessages(msgs);
		
		View temp1 = View.inflate(Robolectric.application.getApplicationContext(), R.layout.list_item, null);		
		View temp2 = View.inflate(Robolectric.application.getApplicationContext(), R.layout.list_item, null);		
		
        //act
		LinearLayout geoLocatedMsgView = (LinearLayout) adapter.getView(0, temp1, null);
		LinearLayout nonGeoLocatedMsgView = (LinearLayout) adapter.getView(1, temp2, null);
        
		//assert
		assertTrue(((ImageView) geoLocatedMsgView.findViewById(R.id.hasGeoLocation)).getVisibility() == View.VISIBLE);
		assertTrue(((ImageView) nonGeoLocatedMsgView.findViewById(R.id.hasGeoLocation)).getVisibility() == View.INVISIBLE);
	}

}
