package tools;
import java.util.Vector;

import com.devmel.tools.Binary;

public class Pulses {

	public static int[] toPulses(int sampleRate, byte[] binary){
		int[] ret = null;
		Vector<Integer> vector = new Vector<Integer>();
		if(binary != null && sampleRate > 0){
			int pulseUs = 1000000/sampleRate;
			String bins = Binary.fromBytes(binary);
			char[] bin = bins.toCharArray();
			byte[] data = new byte[bin.length];
			int next = 0;
			int i = 0;
			char current = bin[i];
			for(;i<data.length;i++){
				if(bin[i] == current){
					next += pulseUs;
				}else{
					vector.add(next);
					current = bin[i];
					next = pulseUs;
				}
			}
			vector.add(next);
			int count = 0;
			ret = new int[vector.size()];
			for(int j: vector) ret[count++] = j;
		}
		return ret;
	}
	
	public static byte[] fromPulses(int sampleRate, int[] pulses, boolean isGapStart){
		byte[] ret = null;
		if(sampleRate > 0){
			int pulseUs = 1000000/sampleRate;
			char current = '0';
			if(!isGapStart)
				current = '1';
			StringBuffer str = new StringBuffer();
			for(int p:pulses){
				int size = p/pulseUs;
				for(int i=0;i<size;i++){
					str.append(current);
				}
				if(current=='1'){
					current = '0';
				}else{
					current = '1';
				}
			}
			ret = Binary.toBytes(str.toString());
		}
		return ret;
	}

}
