package src;

public class TtsWithEffects {
	public static SampledAudio ttsWithEffects(String text) throws Exception{
		MarkedAudio tts = null;
		if (TextToSpeechCache.contains(text)){
			tts = TextToSpeechCache.getFromCache(text);
		}else{
			String ttsText = EffectsAdder.preprocessText(text);
			
			try {
				tts = TextToSpeechInterface.textToSpeechMarkedAudio(ttsText);
				TextToSpeechCache.addToCache(text, tts);
			} catch (Exception e) {
				throw e;
			}
		}
		SampledAudio result = EffectsAdder.addBasicEffects(tts);
		return result;
	}
}
