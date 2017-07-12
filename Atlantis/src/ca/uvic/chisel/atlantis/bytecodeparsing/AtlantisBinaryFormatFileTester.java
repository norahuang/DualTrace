package ca.uvic.chisel.atlantis.bytecodeparsing;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;

public class AtlantisBinaryFormatFileTester extends PropertyTester {
	
	@Override
	public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
		IFile file = (IFile) receiver;
		IContainer folder = file.getParent(); 
		return AtlantisBinaryFormat.isCompleteBinaryFileSystem(folder.getRawLocation().makeAbsolute().toFile());
	}

}
