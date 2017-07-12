package ca.uvic.chisel.atlantis.bytecodeparsing;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.resources.IFolder;

public class AtlantisBinaryFormatFolderTester extends PropertyTester {
	
	@Override
	public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
		IFolder folder = (IFolder) receiver; 
		return AtlantisBinaryFormat.isCompleteBinaryFileSystem(folder.getRawLocation().makeAbsolute().toFile());
	}

}
