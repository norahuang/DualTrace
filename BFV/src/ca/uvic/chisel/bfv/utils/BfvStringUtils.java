package ca.uvic.chisel.bfv.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class BfvStringUtils {

	public static String[] betterSplit(String toSplit, String delim) {
		return betterSplit(toSplit, delim, false);
	}
	
	public static String[] betterSplit(String toSplit, String delim, boolean maintainSplitChars) {
		List<String> splits = new ArrayList<>();
		
		Matcher m = Pattern.compile(delim).matcher(toSplit);
		int curPos = 0;
		while (m.find()) {
			
			int startOffset = m.start();
			int endOffset = m.end();
			
			if(!maintainSplitChars) {
				splits.add(toSplit.substring(curPos, startOffset));
			} else {
				splits.add(toSplit.substring(curPos, endOffset));
			}
			
			curPos = endOffset;
		}
		
		splits.add(toSplit.substring(curPos));
		return splits.toArray(new String[splits.size()]);
	}
	
}
