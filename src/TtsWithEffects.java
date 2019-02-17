package src;

public class TtsWithEffects {
	public static SampledAudio ttsWithEffects(String text) throws Exception{
		String ttsText = EffectsAdder.preprocessText(text);
		MarkedAudio tts;
		try {
			tts = TextToSpeechInterface.textToSpeechMarkedAudio(ttsText);
		} catch (Exception e) {
			throw e;
		}
		SampledAudio result = EffectsAdder.addBasicEffects(tts);
		return result;
	}
}
