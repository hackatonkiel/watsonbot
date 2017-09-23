package de.bewatec.hackathon.config;

import android.media.AudioFormat;

public abstract class WatsonConfigData {

	public static final String API_USER = "b66e0a0e-07e2-4234-b9df-ce06ff9fbe03";
	public static final String API_PW = "rdicgMeKaIZ6";
	
	public static final String WATSON_HOTWORD = "watson";
	public static final String WATSON_HELP = "help";
	
	public static final String CONVERSATION_USER = "6b60b2cd-adc8-4c0e-88fa-20796033cc0c";
	public static final String CONVERSATION_PW = "NJDVNoAQ2IqY";
	public static final String CONVERSATION_API_URL = "https://gateway-fra.watsonplatform.net/conversation/api";
	public static final String CONVERSATION_WORKSPACE_ID = "a2b9bb63-cf2e-4b21-bb68-0c5dbd94775c";
	
	public static final String SPEECH_TO_TEXT_USER ="7be1ff7e-7d93-4635-b233-8a904b3ea72f";
	public static final String SPEECH_TO_TEXT_PW ="YLxLnsTj5VoX";
	
	
	public static final String TWILIO_USER = "ACa911a12ca5feb808ad3d37935c7a4e8b";
	public static final String TWILIO_AUTH_TOKEN = "1d4087d243f8089f429afb6cf89888c9";
	
	public static final String SPEECH_TO_TEXT_API_URL ="https://stream-fra.watsonplatform.net/text-to-speech/api";
	
	
	public static final String API_URL = "https://stream-fra.watsonplatform.net/speech-to-text/api";
	public static final String WEBSOCKET_API_URL = "wss://stream-fra.watsonplatform.net/speech-to-text/api";
	public static String username = "ede15103-70bb-4ed9-a83f-458952162841";
	public static String password = "qOcrKIscUkKq";
	
	public static int sampleRate = 16000;
	public static int channelCount = 1;

	public static int channelConfig = AudioFormat.CHANNEL_IN_MONO;
	public static int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
	public static int bufferSize = 1024*2;//AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat);
	

}
