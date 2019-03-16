package src;

import java.io.Serializable;
import java.util.Properties;

public class MarkedAudio implements Serializable{
	public SampledAudio sa;
	public Properties marks;

	@Override
	public String toString(){
		String result = "";
		result += sa.toString() + "\n";
		for(String key:marks.stringPropertyNames()){
			result += "Key: " + key + ", value: " + marks.getProperty(key) + "\n";
		}
		return result;
	}
}
