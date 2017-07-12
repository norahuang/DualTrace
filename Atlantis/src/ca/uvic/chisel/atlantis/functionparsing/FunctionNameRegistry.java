package ca.uvic.chisel.atlantis.functionparsing;

import java.util.HashMap;
import java.util.Locale;

public class FunctionNameRegistry {
	private static HashMap<String, HashMap<Long, String>> registry;
	
	static {
		registry = new HashMap<String, HashMap<Long, String>>();
	}
	
	public static void registerFunction(String module, long offset, String name) {
		module = module.toUpperCase(Locale.US);
		HashMap<Long, String> moduleList = registry.get(module);
		
		if(moduleList == null) {
			moduleList = new HashMap<Long, String>();
			registry.put(module, moduleList);
		}
		
		moduleList.put(offset, name);
	}
	
	public static String getFunction(String module, long offset) {
		module = module.toUpperCase(Locale.US);
		HashMap<Long, String> moduleList = registry.get(module);
		
		if(moduleList != null) {
			return moduleList.get(offset);
		}
		
		return null;
	}
}
