package ca.uvic.chisel.bfv.editor;

import java.io.File;
import java.util.Collection;
import java.util.Iterator;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.util.OpenStrategy;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.OpenSystemEditorAction;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.internal.ide.DialogUtil;
import org.eclipse.ui.internal.ide.IDEWorkbenchMessages;
import org.eclipse.ui.internal.ide.IIDEHelpContextIds;
import org.eclipse.ui.part.FileEditorInput;

import ca.uvic.chisel.bfv.utils.BfvFileUtils;
import ca.uvic.chisel.bfv.utils.IFileUtils;

/**
 * Built from org.eclipse.ui.actions.OpenFileAction, but to produce
 * a FileEditorInput that supports large files.
 *
 */
public class OpenBigFileAction extends OpenSystemEditorAction {
	
	IFileUtils fileUtil = RegistryUtils.getFileUtils();
	
    /**
     * The id of this action.
     */
	public static final String ID = PlatformUI.PLUGIN_ID + "OpenBigFileAction";
	
    /**
     * The editor to open.
     */
    protected IEditorDescriptor editorDescriptor;

    /**
     * Overshadowing, but same content, as parent.
     * Need at this level, but package private in parent.
     */
	private IWorkbenchPage workbenchPage;

	private Collection<String> editorIdsToConvert;


    /**
     * Creates a new action that will open editors on the then-selected file 
     * resources. Equivalent to <code>OpenFileAction(page,null)</code>.
     *
     * @param page the workbench page in which to open the editor
     */
    public OpenBigFileAction(IWorkbenchPage page, Collection<String> editorIdsToConvert) {
        this(page, null, editorIdsToConvert);
        this.workbenchPage = page;

    }

    /**
     * Creates a new action that will open instances of the specified editor on 
     * the then-selected file resources.
     *
     * @param page the workbench page in which to open the editor
     * @param descriptor the editor descriptor, or <code>null</code> if unspecified
     * @param editorIdsToConvert 
     */
    public OpenBigFileAction(IWorkbenchPage page, IEditorDescriptor descriptor, Collection<String> editorIdsToConvert) {
        super(page);
        this.workbenchPage = page;
        setText(descriptor == null ? IDEWorkbenchMessages.OpenFileAction_text : descriptor.getLabel());
        PlatformUI.getWorkbench().getHelpSystem().setHelp(this,
				IIDEHelpContextIds.OPEN_FILE_ACTION);
        setToolTipText(IDEWorkbenchMessages.OpenFileAction_toolTip);
        setId(ID);
        this.editorDescriptor = descriptor;
        this.editorIdsToConvert = editorIdsToConvert;
    }

    /**
     * Ensures that the contents of the given file resource are local.
     *
     * @param file the file resource
     * @return <code>true</code> if the file is local, and <code>false</code> if
     *   it could not be made local for some reason
     */
    protected boolean ensureFileLocal(final IFile file) {
        //Currently fails due to Core PR.  Don't do it for now
        //1G5I6PV: ITPCORE:WINNT - IResource.setLocal() attempts to modify immutable tree
        //file.setLocal(true, IResource.DEPTH_ZERO);
        return true;
    }

    @Override
    public void run() {
    	 Iterator itr = getSelectedResources().iterator();
         while (itr.hasNext()) {
             IResource resource = (IResource) itr.next();
             if (resource instanceof IFile) {
 				openFile((IFile) resource);
 			}
         }
    }
    
    /**
     * Opens an editor on the given file resource.
     *
     * @param file the file resource
     */
	protected void openFile(IFile file) {
    	
    	 try {
    		 if(shouldConvertToTempFile(editorDescriptor, file)) {
    			 // Need to receive IFile and produce IFile, because the editor likes those.
    			 File f = BfvFileUtils.convertFileIFile(file);
    			 f = fileUtil.convertFileToBlankFile(f);
    			 file = BfvFileUtils.convertFileIFile(f);
    			 file.getProject().refreshLocal(IResource.DEPTH_INFINITE, null);
    			 if(!file.exists()){
    				 fileUtil.createEmptyFile(file);
    			  }
    		 }
    		 
             boolean activate = OpenStrategy.activateOnOpen();
             if (editorDescriptor == null) {
                 IDE.openEditor(getWorkbenchPage(), file, activate);
             } else {
                 if (ensureFileLocal(file)) {
                     getWorkbenchPage().openEditor(new FileEditorInput(file),
                    		 editorDescriptor.getId(), activate);
                 }
             }
         } catch (PartInitException e) {
             DialogUtil.openError(getWorkbenchPage().getWorkbenchWindow()
                     .getShell(), IDEWorkbenchMessages.OpenFileAction_openFileShellTitle,
                     e.getMessage(), e);
         } catch (CoreException e){
        		PartInitException partException = new PartInitException(e.getMessage(), e);
        		 DialogUtil.openError(getWorkbenchPage().getWorkbenchWindow()
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
    	
    	return editorIdsToConvert.contains(descriptor.getId());
    }

    /**
     * Not overriding technically.
     * 
     * @return
     */
	protected IWorkbenchPage getWorkbenchPage() {
		return workbenchPage;
	}

}
