package ca.uvic.chisel.atlantis.handlers;

import java.io.File;
import java.net.URISyntaxException;
import java.util.Scanner;

import javax.swing.JFileChooser;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.part.FileEditorInput;

import ca.uvic.chisel.atlantis.DualTracePerspective;
import ca.uvic.chisel.atlantis.bytecodeparsing.AtlantisBinaryFormat;
import ca.uvic.chisel.atlantis.utils.AtlantisFileUtils;
import ca.uvic.chisel.bfv.editor.RegistryUtils;
import ca.uvic.chisel.bfv.utils.BfvFileUtils;
import ca.uvic.chisel.bfv.utils.IFileUtils;
import ca.uvic.chisel.gibraltar.GibraltarMain;

public class OpenDualTraceHandler2 extends AbstractHandler {
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		// Show file selector based on the project view, and open the selected trace
		// into a second window, with separate views.

		IFile file = getPathOfSelectedFile(event);
		
		if(null == file){
			try(
				Scanner sc = new Scanner(System.in);
				){
				
				JFileChooser f = new JFileChooser("Select a binary trace, to open as a dual trace alongside current active trace");
				f.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				
				File runningDir;
				try {
					runningDir = new File(GibraltarMain.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
					f.setCurrentDirectory(runningDir);
				} catch (URISyntaxException e) {
					e.printStackTrace();
				}
				
		        f.showOpenDialog(null);
		        
	
		        System.out.println(f.getCurrentDirectory());
		        System.out.println(f.getSelectedFile());
		        if(null == f.getSelectedFile()){
		        	return null;
		        }
		        file = AtlantisFileUtils.convertFileIFile(f.getSelectedFile());
			}
		}
		
		IWorkbenchPage secondWindowPage = openSecondWindow();
		
		IEditorDescriptor desc = PlatformUI.getWorkbench().getEditorRegistry().getDefaultEditor(file.getName());
		try {
			
			// The related OpenBigFileAction checks to see if it should convert to empty file or not.
			// We are not following suit here, because it didn't seem important. Maybe it is?
			// if (shouldConvertToTempFile(desc, file)) {
			// Need to receive IFile and produce IFile, because the editor
			// likes those.
			IFileUtils fileUtil = RegistryUtils.getFileUtils();
			File f = BfvFileUtils.convertFileIFile(file);
			f = fileUtil.convertFileToBlankFile(f);
			IFile convertedFile = BfvFileUtils.convertFileIFile(f);
			convertedFile.getProject().refreshLocal(IResource.DEPTH_INFINITE, null);
			if (!convertedFile.exists()) {
				fileUtil.createEmptyFile(convertedFile);
			}
			
			// NB file is converted above
			secondWindowPage.openEditor(new FileEditorInput(convertedFile), desc.getId());
			
		} catch (CoreException e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	 IWorkbenchPage openSecondWindow(){
		IWorkbenchWindow dualTraceWindow = null;
		try {
			IWorkbenchWindow workbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
			// First implemented in AtlantisBifFileOpenAction
			String dualTracePerspectiveId = DualTracePerspective.ID;
	        IAdaptable theInput = null;
	        IWorkbenchWindow mainWindow = workbenchWindow;
	        Point size = mainWindow.getShell().getSize();
	        dualTraceWindow = workbenchWindow.getWorkbench().openWorkbenchWindow(dualTracePerspectiveId, theInput);
	        // Get the dimensions of the main window, and copy those to the dual window.
	        dualTraceWindow.getShell().setSize(size);
	        dualTraceWindow.getShell().setText("Atlantis Dual Trace Window");
			workbenchWindow.getWorkbench().showPerspective(dualTracePerspectiveId, dualTraceWindow, theInput);
		} catch (WorkbenchException e) {
			e.printStackTrace();
		}
         return dualTraceWindow.getActivePage();
	}
	
	@Override
	protected void setBaseEnabled(boolean b){
		return;
	}
	
	private IFile getPathOfSelectedFile(ExecutionEvent event) {
        // The plugin.xml should only be allowing commands to be issued on IFolder and IFile entities.
		IFile f = null;
		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		if (window != null){
			window = HandlerUtil.getActiveWorkbenchWindow(event);
	        IStructuredSelection selection = (IStructuredSelection) window.getSelectionService().getSelection();
	        Object firstElement = selection.getFirstElement();
	        if(firstElement instanceof IFile) {
	        	return (IFile) firstElement;
	        }
	        if(firstElement instanceof IFolder) {
	        	IFolder folder = (IFolder) firstElement;
	        	AtlantisBinaryFormat binaryFormat = new AtlantisBinaryFormat(folder.getRawLocation().makeAbsolute().toFile());
	        	// arbitrary, just any file in the binary set is needed
	        	return AtlantisFileUtils.convertFileIFile(binaryFormat.getExecVtableFile());
	        }
		}
        return null;
    }
}
