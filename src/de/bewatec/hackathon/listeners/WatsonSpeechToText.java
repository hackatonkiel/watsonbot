package de.bewatec.hackathon.listeners;

import java.io.InputStream;

import okhttp3.WebSocket;



import android.util.Log;

import com.ibm.watson.developer_cloud.speech_to_text.v1.SpeechToText;
import com.ibm.watson.developer_cloud.speech_to_text.v1.model.RecognizeOptions;
import com.ibm.watson.developer_cloud.speech_to_text.v1.websocket.RecognizeCallback;


public class WatsonSpeechToText  {

	private static String LOG_TAG = "WatsonSpeechToText";
	
	private SpeechToText service = null;
	private RecognizeOptions options;
	
	private WebSocket speechSocket;
	
	public WatsonSpeechToText(SpeechToText service, RecognizeOptions options){
		this.service = service;
		
		this.options = options;
		
	}
	

	
	public void startSpeechToText(final RecognizeCallback listener,final InputStream stream){
		
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				speechSocket = service.recognizeUsingWebSocket(stream, options, listener);				
				if(speechSocket == null){
					Log.d(LOG_TAG, "socket is null");
				}
			}
		}).start();
		
		
	}
	
	public void endSpeechToText(){
		if(speechSocket != null){
			speechSocket.close(1000, null);
			speechSocket = null;
		}
	}
	
	

	
	
	

}
