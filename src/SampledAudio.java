package src;

public class SampledAudio {
	public int sampleRate;
	public int numChannels;
	public int numFrames;
	public int bits;
	public double[] samples = {};
	
	public SampledAudio(){
		sampleRate = 44100;
		numChannels = 1;
		numFrames = 0;
		bits = 16;
	}
	
}
