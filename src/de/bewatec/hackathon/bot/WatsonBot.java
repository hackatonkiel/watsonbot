package de.bewatec.hackathon.bot;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import android.content.Context;
import android.database.CursorJoiner.Result;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaRecorder.AudioSource;
import android.util.Log;

import com.ibm.watson.developer_cloud.conversation.v1.ConversationService;
import com.ibm.watson.developer_cloud.http.HttpMediaType;
import com.ibm.watson.developer_cloud.speech_to_text.v1.SpeechToText;
import com.ibm.watson.developer_cloud.speech_to_text.v1.model.RecognizeOptions;
import com.ibm.watson.developer_cloud.speech_to_text.v1.model.SpeakerLabel;
import com.ibm.watson.developer_cloud.speech_to_text.v1.model.SpeechAlternative;
import com.ibm.watson.developer_cloud.speech_to_text.v1.model.SpeechResults;
import com.ibm.watson.developer_cloud.speech_to_text.v1.model.SpeechTimestamp;
import com.ibm.watson.developer_cloud.speech_to_text.v1.model.Transcript;
import com.ibm.watson.developer_cloud.speech_to_text.v1.websocket.RecognizeCallback;
import com.ibm.watson.developer_cloud.text_to_speech.v1.TextToSpeech;
import com.ibm.watson.developer_cloud.tone_analyzer.v3.ToneAnalyzer;
import com.ibm.watson.developer_cloud.tone_analyzer.v3.model.ToneAnalysis;
import com.ibm.watson.developer_cloud.tone_analyzer.v3.model.ToneOptions;

import de.bewatec.hackathon.actions.Action;
import de.bewatec.hackathon.actions.EmergencyAction;
import de.bewatec.hackathon.actions.StartTVAppAction;
import de.bewatec.hackathon.config.WatsonConfigData;
import de.bewatec.hackathon.listeners.ConversationListener;
import de.bewatec.hackathon.listeners.TextToSpeechListener;
import de.bewatec.hackathon.listeners.WatsonConversation;
import de.bewatec.hackathon.listeners.WatsonSpeechToText;
import de.bewatec.hackathon.listeners.WatsonTextToSpeech;
import de.bewatec.hackathon.utils.AudioStreamer;
import de.bewatec.hackathon.utils.WatsonBotUtils;

public class WatsonBot implements TextToSpeechListener, ConversationListener, RecognizeCallback {

	private JamesStates currentState;

	private AudioRecord audioRecorder;
	private AudioStreamer micStreamer;

	private WatsonSpeechToText speechToText;
	private WatsonConversation conversation;
	private WatsonTextToSpeech textToSpeech;
	private SpeechToText sttService;

	private ToneAnalyzer toneAnalyzerService;

	private MediaPlayer mPlayer;

	private int bufferSize;
	private static final String LOG_TAG = "WatsonBot";

	private boolean hasCurrenSpeaker = false;
	private SpeakerLabel currentSpeaker = null;
	private List<SpeakerLabel> speakerLabels = null;
	private Map<Integer, StringBuilder> speakerToWord;
	private List<SpeechTimestamp> allTimestamps = null;
	private Map<String, Action> actionList;
	private int[] labelValues = null;

	private AudioManager audioManager;
	
	private Context context;

	private Timer idleTimer;
	
	public WatsonBot(Context app) {
		this.context = app;
		this.bufferSize = AudioRecord.getMinBufferSize(WatsonConfigData.sampleRate, WatsonConfigData.channelConfig,
				WatsonConfigData.audioFormat) * 2;
		Log.d(LOG_TAG, "BufferSize " + bufferSize);
	}

	public void init() {
		audioRecorder = new AudioRecord(AudioSource.MIC, WatsonConfigData.sampleRate, WatsonConfigData.channelConfig,
				WatsonConfigData.audioFormat, this.bufferSize);
		micStreamer = new AudioStreamer(audioRecorder);
		micStreamer.startRecording();

		sttService = new SpeechToText(WatsonConfigData.API_USER, WatsonConfigData.API_PW);
		sttService.setEndPoint(WatsonConfigData.API_URL);

		RecognizeOptions options = new RecognizeOptions.Builder()
				.timestamps(true)
				.speakerLabels(true)
				// .continuous(true)
				.interimResults(true)
				.contentType(
						HttpMediaType.AUDIO_PCM + ";rate=" + WatsonConfigData.sampleRate + ";channels="
								+ WatsonConfigData.channelCount).build();

		speechToText = new WatsonSpeechToText(this.sttService, options);

		ConversationService conversationService = new ConversationService(ConversationService.VERSION_DATE_2017_04_21,
				WatsonConfigData.CONVERSATION_USER, WatsonConfigData.CONVERSATION_PW);
		conversationService.setEndPoint(WatsonConfigData.CONVERSATION_API_URL);

		conversation = new WatsonConversation(conversationService);

		conversation.setListener(this);

		TextToSpeech textToSpeechService = new TextToSpeech();

		textToSpeechService = new TextToSpeech(WatsonConfigData.SPEECH_TO_TEXT_USER, WatsonConfigData.SPEECH_TO_TEXT_PW);
		textToSpeechService.setEndPoint(WatsonConfigData.SPEECH_TO_TEXT_API_URL);

		textToSpeech = new WatsonTextToSpeech(textToSpeechService, this);

		toneAnalyzerService = new ToneAnalyzer(ToneAnalyzer.VERSION_DATE_2016_05_19);
		toneAnalyzerService.setUsernameAndPassword("b2581d44-9650-4d62-8e62-2ac17e3a377b", "YgEZXR404fsH");
		toneAnalyzerService.setEndPoint("https://gateway-fra.watsonplatform.net/tone-analyzer/api");

		audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
		audioManager.setMicrophoneMute(true);
		
		
		speakerLabels = new ArrayList<SpeakerLabel>();
		speakerToWord = new HashMap<Integer, StringBuilder>();
		allTimestamps = new ArrayList<SpeechTimestamp>();
		actionList = new HashMap<String, Action>();
		actionList.put("emergency call", new EmergencyAction(context));
		actionList.put("tv on", new StartTVAppAction(context));
		
		labelValues = new int[10]; // max 10 Labels
	}

	public void startConversation() {
		this.currentState = JamesStates.IDLE;
		// conversation.execute("");
		Log.d(LOG_TAG,"now Listening");
		this.audioManager.setMicrophoneMute(false);
		speechToText.startSpeechToText(WatsonBot.this, micStreamer);
	}

	public void stopConversation() {
		this.currentState = null;
		stopPlaying();
	}

	@Override
	public void onConnected() {
		// TODO Auto-generated method stub
	}

	@Override
	public void onDisconnected() {
		// TODO Auto-generated method stub
	}

	@Override
	public void onError(Exception arg0) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onInactivityTimeout(RuntimeException arg0) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onListening() {
		// TODO Auto-generated method stub
	}

	private String extractItems(SpeechResults speech) {
		Transcript finalResult = null;
		SpeechAlternative alternativeWithMaxConf;
		List<SpeechTimestamp> timeStamps;

		if (speech != null) {
			if (speech.getResults() != null) {
				for (Transcript result : speech.getResults()) {
					if (result.isFinal()) {
						finalResult = result;
						break;
					}
				}

				if (finalResult != null) {

					alternativeWithMaxConf = WatsonBotUtils.getAlternativesWithMaxConfidence(finalResult);

					return alternativeWithMaxConf.getTranscript();

					// Log.d(LOG_TAG, "Final transcript: " + alternativeWithMaxConf.getTranscript());

					/*
					 * printAllSpeakers(); if (alternativeWithMaxConf != null) { timeStamps =
					 * alternativeWithMaxConf.getTimestamps(); saveTimestamps(timeStamps); sortWordToLabelByTimeStamp(); }
					 */
				}
			}

			// if(speech.getSpeakerLabels() != null){
			// Log.d(LOG_TAG,"Labels?: "+speech.toString());
			// }
			// addSpeakerLabels(speech.getSpeakerLabels());// If labels available they will be saved

		}
		return null;
	}
	

	// get speech to text results
	@Override
	public void onTranscription(SpeechResults speech) {
		 String resultString = extractItems(speech);
		 if(resultString != null)
			Log.d(LOG_TAG, "Speech: "+resultString + " in state (" + currentState.name() + ")");
		
		if (this.currentState == JamesStates.IDLE || this.currentState == JamesStates.LISTENING) {
			final SpeechResults finalSpeech = speech;
			new Thread(new Runnable() {

				@Override
				public void run() {
					String transcript = extractItems(finalSpeech);
					
					// Just listen on:
					//		- on or after buzzword
					//		- on a help call
					if(transcript != null){
						if(transcript.toLowerCase(Locale.ENGLISH).contains(WatsonConfigData.WATSON_HOTWORD)){
							 Log.d(LOG_TAG, "Speech: "+transcript + " contains watson");
							currentState = JamesStates.LISTENING;
							idleTimer = new Timer();
							idleTimer.schedule(new TimerTask() {
								
							@Override
							public void run() {
									currentState = JamesStates.IDLE;						
								}
							}, 10000);
							
						}
						
						if(WatsonBot.this.currentState == JamesStates.LISTENING 
								|| transcript.toLowerCase(Locale.ENGLISH).contains(WatsonConfigData.WATSON_HELP)){
							
		
								
								Log.d(LOG_TAG, "SpeechResult: " + transcript );
								conversation.textToSpeech(transcript);
								
							/*
							 * if (speakerToWord != null) { if (currentSpeaker != null) { Log.d(LOG_TAG, "CurrentSpeaker:" + " (" +
							 * currentSpeaker.getSpeaker() + ") " + speakerToWord.get(currentSpeaker).toString()); } }
							 */
						
						}
					}
				}

			}).start();
		}

	}

	@Override
	public void onTranscriptionComplete() {
		// TODO Auto-generated method stub

	}

	// Conversation results from watson
	@Override
	public void getConversationResult(String answer) {
		this.audioManager.setMicrophoneMute(true);
		this.currentState = JamesStates.TALKING;
		Log.d(LOG_TAG,"now talking");
		Log.d(LOG_TAG, "Conversation answ. :" + answer);
		String answer_lower = answer.toLowerCase(Locale.ENGLISH);
		if(actionList.containsKey(answer_lower)){
			Log.d(LOG_TAG, "Execute action for : " + answer_lower.toLowerCase(Locale.ENGLISH));
			Action currentAction = actionList.get(answer_lower);
			currentAction.execute();
			if(currentAction.getSpeechResult() == null)
				textToSpeech.textToSpeech(answer);
			else
				textToSpeech.textToSpeech(currentAction.getSpeechResult());
		}
		else
			textToSpeech.textToSpeech(answer);
	}

	// File where audio from texttospeech is saved
	@Override
	public void synthesizedToFile(String fileName) {
		startPlaying(fileName);
	}

	private void startPlaying(String file) {
		mPlayer = new MediaPlayer();
		mPlayer.setOnCompletionListener(new OnCompletionListener() {

			@Override
			public void onCompletion(MediaPlayer mp) {
				
				
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				audioManager.setMicrophoneMute(false);
				Log.d(LOG_TAG,"now Listening");
				currentState = JamesStates.LISTENING;
			}
		});
		try {

			mPlayer.setDataSource(file);
			mPlayer.prepare();
			mPlayer.start();
		} catch (IOException e) {
			Log.e(LOG_TAG, "prepare() failed");
		}
	}

	private void stopPlaying() {
		if (mPlayer != null) {
			mPlayer.release();
			mPlayer = null;
		}
	}

	public enum JamesStates {

		// Do not listen, just say hello to the user
		GREETING(1),

		// Stop talking and just listen
		LISTENING(2),

		// Stop listening and just talk
		TALKING(3),

		IDLE(4);
		
		int number;

		JamesStates(int number) {
			this.number = number;
		}
		
	
	}
	
	public void endSpeechToText(){
		speechToText.endSpeechToText();
	}

}
