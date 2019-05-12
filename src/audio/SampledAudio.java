package src.audio;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;

import src.audio.util.WavFile;
import src.webserver.VirtualFileSystem;

public class SampledAudio implements Serializable{
	@Override
	public String toString() {
		return "SampledAudio [sampleRate=" + sampleRate + ", numChannels="
				+ numChannels + ", numFrames=" + numFrames + ", bits=" + bits + ", samples.length=" + samples.length + "]";
	}

	public int sampleRate;
	public int numChannels;
	public int numFrames;
	public int bits;
	
	public double[] samples = {};
	
	public SampledAudio insertOtherSampledAudio(int frameIndex, SampledAudio other){
		
		int totalFrames = (int) (sampleRate * (getDuration() + other.getDuration()));
		double[] output = new double[totalFrames];
		//samples from start of this SampledAudio
		for (int i = 0; i < frameIndex; i++){
			if (i >= output.length || i >= samples.length){
				break;
			}
			output[i] = getFrame(i);
		}
		//samples from other SampledAudio
		for (int j = 0; j < sampleRate * other.getDuration(); j++){
			int otherFrameIndex = (int)(j * (((double)other.sampleRate) / ((double)sampleRate))); //conversion
			if (j + frameIndex >= output.length){
				break;
			}
			output[j + frameIndex] = other.getFrame(otherFrameIndex);
		}
		//samples from end of this SampledAudio
		for (int k = 0; k < output.length; k++){
			if(k + (int)(frameIndex + sampleRate * other.getDuration()) >= output.length || k + frameIndex >= samples.length){
				break;
			}
			output[k + (int)(frameIndex + sampleRate * other.getDuration())] = getFrame(k + frameIndex);
		}
		
		SampledAudio result = new SampledAudio();
		result.sampleRate = sampleRate;
		result.numChannels = 1;
		result.numFrames = totalFrames;
		result.bits = bits;
		result.samples = output;
		return result;
	}
	
	public SampledAudio insertOtherSampledAudio(double time, SampledAudio other){
		System.out.println("insertOtherSampledAudio: time= " + time + ", this: " + this.toString() + ", other: " + other.toString());
		double location = time/this.getDuration();
		int frameIndex = (int) (location*numFrames);
		return insertOtherSampledAudio(frameIndex, other);
	}
	
	public double getDuration(){
		return ((double)numFrames) / ((double)sampleRate);
	}
	
	public double getFrame(int frameIndex){
		int sampleIndex = frameIndex * numChannels;
		if (sampleIndex >= samples.length){
			sampleIndex = samples.length - 1;
		}
		if (sampleIndex < 0){
			sampleIndex = 0;
		}
		return samples[sampleIndex];
	}

	public void setFrame(int frameIndex, double value){
		for (int i = 0; i < numChannels;  i++){
			int sampleIndex = frameIndex * numChannels + i;
			if (sampleIndex >= samples.length){
				break;
			}
			samples[sampleIndex] = value;
		}
	}
	
	public SampledAudio(){
		sampleRate = 44100;
		numChannels = 1;
		numFrames = 0;
		bits = 16;
	}
	
	public static double clamp(double val, double min, double max) {
	    return Math.max(min, Math.min(max, val));
	}
	
	public SampledAudio clone(){
		SampledAudio sa = new SampledAudio();
		sa.bits = bits;
		sa.numChannels = numChannels;
		sa.numFrames = numFrames;
		sa.sampleRate = sampleRate;
		sa.samples = samples.clone();
		return sa;
	}

	
	public void writeToWav(String path) {
		VirtualFileSystem.createNecessaryDirectories(path);
		File file = new File(path);
		String canPath = null;
		try {
			canPath = file.getCanonicalPath();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		try {
			WavFile wavFile = WavFile.newWavFile(new File(canPath), numChannels, numFrames, bits, sampleRate);
			wavFile.writeFrames(samples, numFrames);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
