package src;

import java.util.ArrayList;
import java.util.Properties;

public class EffectsAdder {

	private static class EffectAccumulator{
		/* used to track how added effects have shifted the marked times in a MarkedAudio */
		private class PointChange{
			public double location;
			public double changeInTime;
			public PointChange(double location, double changeInTime){
				this.location = location;
				this.changeInTime = changeInTime;
			}
		}
		
		ArrayList<PointChange> pointChanges = new ArrayList<PointChange>();
		
		public EffectAccumulator(){
			
		}
		
		public double originalTimeToNewTime(double original){
			/* if an effect was added before original time, it affects the new time */
			double changeInTime = 0;
			for (PointChange pc : pointChanges){
				if (pc.location < original){
					changeInTime += pc.changeInTime;
				}
			}
			return original + changeInTime;
		}
		
		public void addPointChange(double location, double changeInTime){
			PointChange pc = new PointChange(location, changeInTime);
			pointChanges.add(pc);
		}
	
	}
	
	private static ArrayList<SampleEffect> findPointMarks(Properties marks, String markString, SampleEffect.EffectName effectName){
		ArrayList<SampleEffect> effects = new ArrayList<SampleEffect>();
		int index = 0;
		String value = "";
		while ((value = marks.getProperty(markString + index)) != null){
			SampleEffect se = new SampleEffect();
			se.effect = effectName;
			se.startTime = Double.parseDouble(value);
			se.type = SampleEffect.EffectType.POINT_CHANGE;
			effects.add(se);
			index++;
		}
		return effects;
	}
	
	public static ArrayList<SampleEffect> findPairMarks(Properties marks, String markString, SampleEffect.EffectName effectName){
		ArrayList<SampleEffect> effects = new ArrayList<SampleEffect>();
		int index = 0;
		double startTime = 0;
		String value = "";
		while ((value = marks.getProperty(markString + index)) != null){
			if (index % 2 == 0){
				startTime = Double.parseDouble(value);
			}else{
				SampleEffect se = new SampleEffect();
				se.effect = effectName;
				se.startTime = startTime;
				se.endTime = Double.parseDouble(value);
				se.type = SampleEffect.EffectType.OTHER; //TOOD this shouldn't be determined here
				effects.add(se);
			}
			index++;
		}
		return effects;
	}
	
	private static ArrayList<SampleEffect> marksToSampleEffects(Properties marks){
		ArrayList<SampleEffect> se1 = findPointMarks(marks, "at", SampleEffect.EffectName.INSERT_THE_N_WORD);
		ArrayList<SampleEffect> se2 = findPairMarks(marks, "asterisk", SampleEffect.EffectName.LOUD);
		se1.addAll(se2);
		return se1;
	}
	
	public static SampledAudio addBasicEffects(MarkedAudio tts) {
		
		SampledAudio sa = tts.sa.clone();
		
		/* map marks to SampleEffects */
		
		ArrayList<SampleEffect> seList = marksToSampleEffects(tts.marks);
		
		EffectAccumulator effectAccumulator = new EffectAccumulator();
		
		for (SampleEffect se : seList){
			double resultingChangeInTime = 0;
			
			switch(se.effect){
			case LOUD:
				int sampleLength = sa.samples.length;
				
				double samplesPerTime = sampleLength / sa.getDuration();
				for(int i = (int) (se.startTime * samplesPerTime); i < (int) (se.endTime * samplesPerTime); i++){
					if (i >= sa.samples.length){
						break;
					}
					sa.samples[i] = clamp(sa.samples[i] * 50, -1, 1);
					sa.samples[i] = sa.samples[i] / 2;
				}
				break;
			case INSERT_THE_N_WORD:
				SampledAudio thenword = TextToSpeechInterface.wavToSampledAudio("../ttsdata/content/thenword.wav");
				sa = sa.insertOtherSampledAudio(effectAccumulator.originalTimeToNewTime(se.startTime), thenword).clone();
				resultingChangeInTime = thenword.getDuration();
				
				break;
			default:
				break;
			}
			
			
			switch(se.type){
			case OTHER:
				
				break;
			case POINT_CHANGE:
				
				effectAccumulator.addPointChange(se.startTime, resultingChangeInTime);
				break;
			case STRETCH_CHANGE:
				break;
			}
		}
	
		return sa;
	}
	public static double clamp(double val, double min, double max) {
	    return Math.max(min, Math.min(max, val));
	}
	
	
	
	public static String preprocessText(String text){
		//remove < and >
		String temp1 = text.replace("<", "");
		String temp2 = temp1.replace(">", "");
		
		//replace asterix with <mark name="asterisk#"/>
		
		String temp3 = asteriskToMark(temp2, 0);
		
		//replace @ with <mark name="at#"/>
		
		String temp4 = atToMark(temp3, 0);
		
		//append "end" mark to end of synthesis
		String result = temp4 + "<mark name=\"end\"/>";
		
		return result;
	}
	
	private static String asteriskToMark(String input, int startIndex){
		for (int i = 0; i < input.length(); i++){
			if (input.charAt(i) == "*".charAt(0)){
				String before = input.substring(0, i);
				String after = input.substring(i + 1, input.length());
				String result = before + "<mark name=\"" + "asterisk" + startIndex + "\"/>" + asteriskToMark(after, startIndex + 1);
				return result;
			}
		}
		return input;
	}
	
	private static String atToMark(String input, int startIndex){
		for (int i = 0; i < input.length(); i++){
			if (input.charAt(i) == "@".charAt(0)){
				String before = input.substring(0, i);
				String after = input.substring(i + 1, input.length());
				String result = before + "<mark name=\"" + "at" + startIndex + "\"/>" + atToMark(after, startIndex + 1);
				return result;
			}
		}
		return input;
	}
}
