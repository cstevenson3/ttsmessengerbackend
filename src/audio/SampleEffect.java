package src.audio;

public class SampleEffect{
	public double startTime;
	public double endTime;
	public EffectName effect;
	public EffectType type;
	public SampleEffect(){
		startTime = 0;
		endTime = 0;
		effect = EffectName.NONE;
		type = EffectType.OTHER;
	}
	
	public enum EffectType{
		OTHER, POINT_CHANGE, STRETCH_CHANGE
	}
	
	public enum EffectName {
	    NONE, LOUD, INSERT_EXAMPLE_AUDIO
	}
}