package ca.uvic.chisel.atlantis.views;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.part.ViewPart;

public class ThreadsView extends ViewPart {

	public ThreadsView() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void createPartControl(Composite parent) {
		RowLayout layout = new RowLayout(3);
		parent.setLayout(layout);
		
		Button thread1Check = new Button(parent, SWT.CHECK);
		thread1Check.setText("Dummy thread 1");
		
		Button thread2Check = new Button(parent, SWT.CHECK);
		thread2Check.setText("Dummy thread 2");
		
		Button thread3Check = new Button(parent, SWT.CHECK);
		thread3Check.setText("Dummy thread 3");
	}

	@Override
	public void setFocus() {
		// TODO Auto-generated method stub
	}
}
