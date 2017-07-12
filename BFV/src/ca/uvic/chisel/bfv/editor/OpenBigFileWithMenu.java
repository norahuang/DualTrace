package ca.uvic.chisel.bfv.editor;

import java.io.File;
import java.util.Collection;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorRegistry;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.actions.OpenWithMenu;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.internal.WorkbenchPage;
import org.eclipse.ui.internal.ide.DialogUtil;
import org.eclipse.ui.internal.ide.IDEWorkbenchMessages;
import org.eclipse.ui.part.FileEditorInput;

import ca.uvic.chisel.bfv.utils.BfvFileUtils;
import ca.uvic.chisel.bfv.utils.IFileUtils;

public class OpenBigFileWithMenu extends OpenWithMenu {

	private IAdaptable file;
	private IWorkbenchPage page;
	private Collection<String> editorIdsToConvert;


	public OpenBigFileWithMenu(IWorkbenchPage page, IAdaptable file, Collection<String> editorIdsToConvert) {
		super(page, file);
		this.page = page;
		this.file = file;
		this.editorIdsToConvert = editorIdsToConvert;
	}
	
	/**
     * Converts the IAdaptable file to IFile or null.
     */
    private IFile getFileResource() {
        if (this.file instanceof IFile) {
            return (IFile) this.file;
        }
        IResource resource = (IResource) this.file
                .getAdapter(IResource.class);
        if (resource instanceof IFile) {
            return (IFile) resource;
        }
       
        return null;
    }
	
	
	@Override
	protected void openEditor(IEditorDescriptor editorDescriptor, boolean openUsingDescriptor) {
		IFileUtils fileUtils = RegistryUtils.getFileUtils();
		IFile file = getFileResource();
	 try {
	  if(shouldConvertToTempFile(editorDescriptor, file)) {
		  File f = BfvFileUtils.convertFileIFile(file);
		  f = fileUtils.convertFileToBlankFile(f);
		  file = BfvFileUtils.convertFileIFile(f);
		  file.getProject().refreshLocal(IResource.DEPTH_INFINITE, null);
		  if(!file.exists()){
			  fileUtils.createEmptyFile(file);
		  }
	  }
	  
      if (file == null) {
          return;
      }
      
      	if (openUsingDescriptor) {
      		((WorkbenchPage) page).openEditorFromDescriptor(new FileEditorInput(file), editorDescriptor, true, null);
      	} else {
	            String editorId = editorDescriptor == null ? IEditorRegistry.SYSTEM_EXTERNAL_EDITOR_ID
	                    : editorDescriptor.getId();
	            
	            page.openEditor(new FileEditorInput(file), editorId, true, 1);
	            // only remember the default editor if the open succeeds
	            IDE.setDefaultEditor(file, editorId);
      	}
      } catch (PartInitException e) {
          DialogUtil.openError(page.getWorkbenchWindow().getShell(),
                  IDEWorkbenchMessages.OpenWithMenu_dialogTitle,
                  e.getMessage(), e);
      } catch (CoreException e){
  		PartInitException partException = new PartInitException(e.getMessage(), e);
		 DialogUtil.openError(page.getWorkbenchWindow()
                .getShell(), IDEWorkbenchMessages.OpenFileAction_openFileShellTitle,
                partException.getMessage(), partException);
}
	}
	
   protected boolean shouldConvertToTempFile(IEditorDescriptor descriptor, IFile file) {
    	if(descriptor == null) {
    		descriptor = IDE.getDefaultEditor(file);
    	}
    	
    	if(descriptor == null) {
    		return false;
    	}
    	
    	return editorIdsToConvert.contains((descriptor.getId()));
    }
}
