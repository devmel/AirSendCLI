package tools;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;

/**
 * A utility class for convenient parsing of command line
 * 
 * ProgramName Argument0 Argument1 -Option0 Option0Value -Option1 -Option2 Option2Value
 * 
 * <p>
 * public static void main(String[] args) {
 * 		CommandLineParser clp = new CommandLineParser(args);
 * 		...
 * }
 * </p>
 * 
 * @author      Jourdain Alexandre
 */


public class CommandLineParser {
	private ArrayList<String> argsList = new ArrayList<String>();
	private HashMap<String, Vector<String>> optsList = new HashMap<String, Vector<String>>();

	/**
	 * Constructs a CommandLineParser object
	 * @param  args arguments from main method
	 */
	public CommandLineParser(String[] args) {
		if(args!=null){
			for (int i = 0; i < args.length; i++) {
				switch (args[i].charAt(0)) {
				case '-':
					if(args[i].length()>1){
						String option = args[i].substring(1);
						String optionValue = null;
						//Check next
						int j = i + 1;
						if(j<args.length && args[j].length()>1 && args[j].charAt(0)!='-'){
							optionValue = args[j];
							i = j;
						}
						Vector<String> values = optsList.get(option);
						if(values == null){
							values = new Vector<String>();
						}
						values.add(optionValue);
						optsList.put(option, values);
	//					System.out.println("Found option: " + option + " value: "+optionValue);
					}
					break;
				default:
	//				System.out.println("Add arg to argument list: " + args[i]);
					argsList.add(args[i]);
					break;
				}
			}
		}
	}

	
	/**
	 * Returns the argument
	 * 
	 * @throws IndexOutOfBoundsException  if the index is out of range
	 * @param  index argument index number
	 * @return  the argument
	 */
	public String getArgument(int index) throws IndexOutOfBoundsException{
		return argsList.get(index);
	}
	
	/**
	 * Returns the number of arguments
	 * 
	 * @return  the number of arguments
	 */
	public int getArgumentCount(){
		return argsList.size();
	}
	
	/**
	 * Returns an array containing all arguments
	 * 
	 * @return  an array containing all arguments
	 */
	public String[] getArguments(){
		String[] ret = new String[argsList.size()];
		ret = argsList.toArray(ret);
		return ret;
	}
	
	/**
	 * Tests if the option name is present
	 * 
	 * @param  name an option name
	 * @return  true if the option names is present, false otherwise
	 */
	public boolean hasOption(String name){
		return optsList.containsKey(name);
	}
	
	/**
	 * Returns an array containing all option names
	 * 
	 * @return  an array containing all option names
	 */
	public String[] getOptionNames(){
		ArrayList<String> retKeys = new ArrayList<String>(optsList.keySet());
		String[] ret = new String[retKeys.size()];
		ret = retKeys.toArray(ret);
		return ret;
	}
	
	
	/**
	 * Returns an option value from a name
	 * 
	 * @param  name an option name
	 * @return  the option value
	 */
	public String getOptionValue(String name){
		String ret = null;
		Vector<String> values = optsList.get(name);
		if(values != null && values.size() > 0){
			ret = values.get(0);
		}
		return ret;
	}
	
	/**
	 * Returns an option value from a name and an index
	 * 
	 * @param  name an option name
	 * @param  index an index
	 * @return  the option value
	 */
	public String getOptionValue(String name, int index){
		String ret = null;
		Vector<String> values = optsList.get(name);
		if(values != null && index >= 0 && index < values.size()){
			ret = values.get(index);
		}
		return ret;
	}

	/**
	 * Returns the number of option value from a name
	 * 
	 * @param  name an option name
	 * @return  the number of option value
	 */
	public int getOptionValueCount(String name){
		Vector<String> values = optsList.get(name);
		if(values != null && values.size() > 0){
			return values.size();
		}
		return 0;
	}
	
}
