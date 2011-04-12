package gov.nasa.arc.geocam.talk.activity;

import gov.nasa.arc.geocam.talk.R;
import gov.nasa.arc.geocam.talk.UIUtils;
import gov.nasa.arc.geocam.talk.bean.GeoCamTalkMessage;
import gov.nasa.arc.geocam.talk.bean.TalkServerIntent;
import gov.nasa.arc.geocam.talk.service.IAudioPlayer;
import gov.nasa.arc.geocam.talk.service.IIntentHelper;
import gov.nasa.arc.geocam.talk.service.IMessageStore;
import gov.nasa.arc.geocam.talk.service.ISiteAuth;
import gov.nasa.arc.geocam.talk.service.ITalkServer;

import java.util.List;

import roboguice.activity.RoboActivity;
import roboguice.inject.InjectResource;
import roboguice.inject.InjectView;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.google.inject.Inject;

public class GeoCamTalkActivity extends RoboActivity {

	@Inject
	ITalkServer djangoTalk;
	@InjectView(R.id.TalkListView)
	ListView talkListView;
	@InjectResource(R.string.url_server_root)
	String serverRootUrl;
	@Inject
	GeoCamTalkMessageArrayAdapter adapter;
	@Inject
	IMessageStore messageStore;
	@Inject
	ISiteAuth siteAuth;
	@Inject
	IAudioPlayer player;

	@Inject
	IIntentHelper intentHelper;
	
	List<GeoCamTalkMessage> talkMessages;

	private BroadcastReceiver receiver = new BroadcastReceiver() {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			if(intent.getAction().contentEquals(TalkServerIntent.INTENT_NEW_MESSAGES.toString()))
			{
				GeoCamTalkActivity.this.newMessages();
			}
		}
	};

	
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.default_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		case R.id.settings_menu_button:
			Log.i("Talk", "Settings Button");
			Intent intent = new Intent(this, GeoCamTalkSettings.class);
			// intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			this.startActivity(intent);
			return true;
		case R.id.create_message_menu_button:
			Log.i("Talk", "Create Button");
			return false;
		case R.id.message_list_menu_button:
			Log.i("Talk", "Message List Button");
			return false;
		case R.id.logout_menu_button:
			try {
				UIUtils.logout(siteAuth);
				//Intent intent = new Intent(this, GeoCamTalkSettings.class);
				// intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				this.startActivity(new Intent(this, GeoCamTalkSettings.class));
				unregisterReceiver(receiver);
			}
			catch (Exception e)
			{
				UIUtils.displayException(getApplicationContext(), e, "You're screwed");	
			}
			return false;
		default:
			Log.i("Talk", "NO BUTTON!!!");
			return super.onOptionsItemSelected(item);
		}
	}

	public void onGoHomeClick(View v) {
		List<GeoCamTalkMessage> talkMessages = null;

		try {
			talkMessages = messageStore.getAllMessages();
		} catch (Exception e) {
			Log.i("Talk", "Error:" + e.getMessage());
		}

		if (talkMessages != null) {
			adapter.setTalkMessages(talkMessages);
			talkListView.setAdapter(adapter);
		}
	}

	public void onCreateTalkClick(View v) {
		UIUtils.createTalkMessage(this);
	}

	public void newMessages() {
		try {
			talkMessages = messageStore.getAllMessages();
		} catch (Exception e) {
			Log.i("Talk", "Error:" + e.getMessage());
		}

		if (talkMessages != null) {
			adapter.setTalkMessages(talkMessages);
			talkListView.setAdapter(adapter);
		}		
	}
	
	@Override
    protected void onResume() {
        super.onResume();        

        IntentFilter filter = new IntentFilter();
        filter.addAction(TalkServerIntent.INTENT_NEW_MESSAGES.toString());
        registerReceiver(receiver, filter);

		setContentView(R.layout.main);

		talkListView.setOnItemClickListener(new AdapterView.OnItemClickListener(){
		    @Override
		    public void onItemClick (AdapterView<?> parentView, View childView, int position, long id) {
		    	GeoCamTalkMessage msg = adapter.getTalkMessage(position);
		    	if(msg.hasAudio())
		    	{
		    		try
		    		{
			    		UIUtils.playAudio(getApplicationContext(), msg, player, siteAuth);
			    	} catch (Exception e)
			    	{
			    		UIUtils.displayException(getApplicationContext(), e, "Cannot retrieve audio");
			    	}
		    	}
		    }
			});
		

		try {
			talkMessages = messageStore.getAllMessages();
		} catch (Exception e) {
			Log.i("Talk", "Error:" + e.getMessage());
		}

		if (talkMessages != null) {
			adapter.setTalkMessages(talkMessages);
			talkListView.setAdapter(adapter);
		} else {
			Toast.makeText(this.getApplicationContext(), "Communication Error with Server",
					Toast.LENGTH_SHORT).show();
		} 
    }
	
    @Override
    protected void onPause() {
        unregisterReceiver(receiver);
        super.onPause();
    }
}