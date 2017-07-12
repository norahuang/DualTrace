package ca.uvic.chisel.bfv.editor;

import java.io.File;

import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.ui.ISources;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;

import ca.uvic.chisel.bfv.datacache.IFileModelDataLayerFactory;
import ca.uvic.chisel.bfv.utils.BfvFileUtils;
import ca.uvic.chisel.bfv.utils.IFileUtils;

public class DeIndexBigFileDelegate
	extends org.eclipse.core.commands.AbstractHandler
{

	@Override
	public Object execute(ExecutionEvent arg0) throws ExecutionException {
		TreeSelection treeSelection = (TreeSelection) HandlerUtil.getVariable(arg0, ISources.ACTIVE_CURRENT_SELECTION_NAME);
		Object firstElement = treeSelection.getFirstElement();
		IResource resource = (IResource)Platform.getAdapterManager().getAdapter(firstElement, IResource.class);
	
			IFile rawFile = null;
		if (resource.getType() == IResource.FILE) {
			rawFile = (IFile)resource;
		}else if(resource.getType() == IResource.FOLDER){
			IFolder folder = (IFolder)resource;
			rawFile = folder.getFile("exec.vtable");
			if(!rawFile.exists()){
				MessageBox box = new MessageBox(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell());
				box.setMessage("This file is not valid for de-indexing.");
				box.open();
				return null;
			}
		}
		
		IFolder folder = (IFolder)rawFile.getParent();
		
		File rF = BfvFileUtils.convertFileIFile(rawFile);
		IFileUtils fileUtil = RegistryUtils.getFileUtils();
		File blankFile = fileUtil.convertFileToBlankFile(rF);
		File file = RegistryUtils.getFileUtils().convertBlankFileToActualFile(blankFile);
		
		IFileModelDataLayerFactory fileModelFactory = RegistryUtils.getFileModelDataLayerFactory();
		boolean indexExists = fileModelFactory.checkFileModelForIndex(file, BfvFileUtils.convertIFolderToFile(folder));
		
		if(indexExists){
			MessageBox box = new MessageBox(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
					SWT.ICON_QUESTION | SWT.YES | SWT.NO);
			box.setMessage("Would you like to de-index this trace file? Are you really sure?\n\n"+file.toString());
			int response = box.open();
			if(response == SWT.YES){
				fileModelFactory.removeIndexForFile(blankFile, file);
				MessageBox successBox = new MessageBox(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell());
				successBox.setMessage("Trace de-indexed: "+file.toString());
				try {
//					ResourcesPlugin.getWorkspace().getRoot().refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
					resource.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
				} catch (CoreException e) {
					// Not a problem.
				}
				successBox.open();
			}
		} else {
			MessageBox box = new MessageBox(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell());
			box.setMessage("This trace has not been indexed, so it cannot be de-indexed.");
			box.open();
		}
		return null;
	}

}

