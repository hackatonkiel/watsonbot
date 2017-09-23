package de.bewatec.hackathon.actions;


import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import android.content.Context;
import android.util.Log;


import de.bewatec.hackathon.config.WatsonConfigData;

public class EmergencyAction implements Action{
	private static String LOG_TAG ="SMSAction";
	private static String message = "Schwesternruf Zimmer 14: Herr Ramon Schubert.";
	private static String speechResult = "Please keep calm, the ward team will get informed";
	private Context context;
	
	public EmergencyAction(Context context) {
		this.context = context;
	}

	@Override
	public void execute() {
		
		HttpClient cl = new DefaultHttpClient();
		HttpPost post = new HttpPost("https://api.twilio.com/2010-04-01/Accounts/" + WatsonConfigData.TWILIO_USER + "/Messages");
		
		
		post.addHeader("Authorization", "Basic QUNhOTExYTEyY2E1ZmViODA4YWQzZDM3OTM1YzdhNGU4YjoxZDQwODdkMjQzZjgwODlmNDI5YWZiNmNmODk4ODhjOQ==");
	
		List<NameValuePair> params = new ArrayList<NameValuePair>(5);
		params.add(new BasicNameValuePair("To", "004917634356394"));
		params.add(new BasicNameValuePair("From", "+41798074682"));
		params.add(new BasicNameValuePair("Body", message));
		
		try {
			post.setEntity(new UrlEncodedFormEntity(params));
			
			HttpResponse response = cl.execute(post);
			
			Log.d(LOG_TAG, response.getStatusLine().getStatusCode() + "  " + response.getStatusLine().getReasonPhrase()) ;
				
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		new CallAction(context).execute();
	}

	@Override
	public String getSpeechResult() {
		// TODO Auto-generated method stub
		return speechResult;
	}

}
