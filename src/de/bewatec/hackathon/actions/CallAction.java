package de.bewatec.hackathon.actions;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

public class CallAction implements Action{

	private static final String LOG_TAG = "WatsonAction";
	private Context context;
	private String dummyNumber = "110";
	private String dummyName = "Schwesternzimmer";
	
	
	public CallAction(Context context) {
		this.context = context;
	}

	@Override
	public void execute() {
		Log.d(LOG_TAG,"Now would execute emergency call!");
		
		Intent intent = new Intent(Intent.ACTION_CALL);
		intent.setData(Uri.parse("sip://normal.call.bewatec.de/" + Uri.encode(dummyNumber) + "/" + 
						Uri.encode(String.format("%s(%s)", dummyNumber, dummyName))));
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		this.context.startActivity(intent);
		
	}

	@Override
	public String getSpeechResult() {
		// TODO Auto-generated method stub
		return null;
	}

	
	
	
}
