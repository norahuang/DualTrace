package ca.uvic.chisel.atlantis.handlers;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map.Entry;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import ca.uvic.chisel.atlantis.models.MemoryReference;
import ca.uvic.chisel.atlantis.views.MemoryVisualization;

// This class is currently not actually used
public class CopyMemoryViewHandler extends AbstractHandler {

	  @Override
	  public Object execute(ExecutionEvent event) throws ExecutionException {

	    IWorkbenchWindow window = PlatformUI.getWorkbench()
	        .getActiveWorkbenchWindow();
	    IWorkbenchPage page = window.getActivePage();
	    IViewPart view = page.findView(MemoryVisualization.ID);
	    Clipboard cb = new Clipboard(Display.getDefault());
	    ISelection selection = view.getSite().getSelectionProvider()
	        .getSelection();

	    ArrayList<Entry<String, MemoryReference>> memoryList = new ArrayList<Entry<String, MemoryReference>>();
	    if (selection != null && selection instanceof IStructuredSelection) {
		      IStructuredSelection sel = (IStructuredSelection) selection;
		      
		      for (Iterator<Entry<String, MemoryReference>> iterator = sel.iterator(); iterator.hasNext();) {
		    	  Entry<String, MemoryReference> person = iterator.next();
		          memoryList.add(person);
		        }
		    }
	    
	    StringBuilder sb = new StringBuilder();
	    for (Entry<String, MemoryReference> person : memoryList) {
	      sb.append(person.getValue());
	    }
	    TextTransfer textTransfer = TextTransfer.getInstance();
	    cb.setContents(new Object[] { sb.toString() },
	        new Transfer[] { textTransfer });
	    
/*	    if (selection != null && selection instanceof IStructuredSelection) {
	      IStructuredSelection sel = (IStructuredSelection) selection;
	      
		    TextTransfer textTransfer = TextTransfer.getInstance();
		    cb.setContents(new Object[] { selection.toString() },
		        new Transfer[] { textTransfer });
	    }*/

	    return null;
	  }

}
