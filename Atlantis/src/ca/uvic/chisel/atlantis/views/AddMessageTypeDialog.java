package ca.uvic.chisel.atlantis.views;

import javax.xml.bind.JAXBException;

import org.apache.commons.io.FilenameUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;

import ca.uvic.chisel.atlantis.functionparsing.Instruction;
import ca.uvic.chisel.bfv.dualtrace.MessageFunction;
import ca.uvic.chisel.bfv.dualtrace.MessageType;
import ca.uvic.chisel.bfv.editor.BigFileEditor;
import ca.uvic.chisel.bfv.utils.BfvFileUtils;

/**
 * Dialog for adding a tag to the file.
 * @author Laura Chan
 */
public class AddMessageTypeDialog extends TitleAreaDialog {

	private ComboViewer comboViewerMessageType;
	private ComboViewer comboViewerSide;
	private Text MessageAddressTextField;
	private Text MessageLengthAddressTextField;
	private Text ChannelIDAddressTextField;
	private Text ChannelNameAddressTexField;
	
	private String messageTypeName;
	private String messageAddress;
	private String messageLengthAddress;
	private String channelIDReg;
	private String channelNameAddress;
	private String functionType;
	private String functionName;
	private Instruction firstIns;


	/**
	 * Constructs a new dialog for adding or editing a comment.
	 * @param parentShell parent shell widget for this dialog
	 * @param commentToEdit comment to edit if this should be an edit dialog, or null if a new comment is to be added
	 */
	public AddMessageTypeDialog(Shell parentShell, Instruction ins, String functionName) {
		super(parentShell);
		this.functionName = functionName;
		this.firstIns = ins;
	}
	
	
	@Override
	public void create() {
		super.create();
        setTitle("Add this function to a Communication type");
	}

	/**
	 * Creates the dialog area, which contains a combobox for specifying the comment's group and a text field for entering/editing the comment's text
	 */
	@Override
	public Control createDialogArea(Composite parent) {
		parent.setLayout(new GridLayout(1, true));
		
		Label comboLabel1 = new Label(parent, SWT.NONE);
		comboLabel1.setText("Enter or select a Communication Type:");
		
		GridData comboGridData1 = new GridData();
		comboGridData1.horizontalAlignment = SWT.FILL;
		comboGridData1.grabExcessHorizontalSpace = true;
		comboViewerMessageType = new ComboViewer(parent, SWT.BORDER);
		comboViewerMessageType.setContentProvider(new ArrayContentProvider());
		comboViewerMessageType.setLabelProvider(new LabelProvider());
		comboViewerMessageType.getCombo().setLayoutData(comboGridData1);
		
		BigFileEditor fileEditor = (BigFileEditor) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
		comboViewerMessageType.setInput(fileEditor.getProjectionViewer().getFileModel().getMessageTypes(true));
		
		Label comboLabel2 = new Label(parent, SWT.NONE);
		comboLabel2.setText("Function Type:");
		GridData comboGridData2 = new GridData();
		comboGridData2.horizontalAlignment = SWT.FILL;
		comboGridData2.grabExcessHorizontalSpace = true;
		comboViewerSide = new ComboViewer(parent, SWT.BORDER);
		comboViewerSide.setContentProvider(new ArrayContentProvider());
		comboViewerSide.setLabelProvider(new LabelProvider());
		comboViewerSide.getCombo().setLayoutData(comboGridData2);	
		comboViewerSide.insert("Send", 0);
		comboViewerSide.insert("Receive",1);
		comboViewerSide.insert("SendChannelCreate", 2);
		comboViewerSide.insert("ReceiveChannelCreate",3);
		
		
		
		Label textLabel1 = new Label(parent, SWT.NONE);
		textLabel1.setText("Enter Register Name for the Message Address:(For Send or Receive function only)");
		MessageAddressTextField = new Text(parent, SWT.BORDER);
		MessageAddressTextField.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Label textLabel2 = new Label(parent, SWT.NONE);
		textLabel2.setText("Enter Register Name Or Address for the Message Lenght:(For Send or Receive function only)");
		MessageLengthAddressTextField = new Text(parent, SWT.BORDER);
		MessageLengthAddressTextField.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Label textLabel3 = new Label(parent, SWT.NONE);
		textLabel3.setText("Enter Register Name Or Address for the Channel Handler ID:");
		ChannelIDAddressTextField = new Text(parent,SWT.BORDER);
		ChannelIDAddressTextField.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));	
		
		Label textLabel4 = new Label(parent, SWT.NONE);
		textLabel4.setText("Enter Register Name Or Address for the Channel Name(For Channel create function only)");
		ChannelNameAddressTexField = new Text(parent,SWT.BORDER);
		ChannelNameAddressTexField.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		return parent;
	}
	
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		GridLayout layout = new GridLayout(2, true);
		parent.setLayout(layout);
		
		GridData okGridData = new GridData();
		okGridData.horizontalAlignment = SWT.FILL;
		okGridData.grabExcessHorizontalSpace = true;
		
		final Button ok = new Button(parent, SWT.PUSH); // done manually so we can implement a custom selection listener without clashing with the default one
		ok.setLayoutData(okGridData);
		ok.setText(IDialogConstants.OK_LABEL);
		ok.setData(new Integer(IDialogConstants.OK_ID)); // sets the button's ID; needed since we're not using createButton()
		ok.setEnabled(false);
		MessageAddressTextField.addListener(SWT.CHANGED, new Listener(){
			@Override
			public void handleEvent(Event event) {
				if(MessageAddressTextField.getText().trim().length() > 0){ // If it has no visible characters, we won't let them set it
					ok.setEnabled(true);
				} else {
					ok.setEnabled(false);
				}
			}
		});
		ok.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) {
				messageTypeName = comboViewerMessageType.getCombo().getText();
				messageAddress = MessageAddressTextField.getText();
				messageLengthAddress = MessageLengthAddressTextField.getText();
				channelIDReg = ChannelIDAddressTextField.getText();
				channelNameAddress = ChannelNameAddressTexField.getText();
				functionType = comboViewerSide.getCombo().getText();	
				
				okPressed();
			}
		});
		// Set the OK button to be the default button
		Shell shell = parent.getShell();
		if (shell != null) {
			shell.setDefaultButton(ok);
		}
		
		Button cancel = createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
		GridData cancelGridData = new GridData();
		cancelGridData.horizontalAlignment = SWT.FILL;
		cancelGridData.grabExcessHorizontalSpace = true;
		cancel.setLayoutData(cancelGridData);
	}
	
    @Override
    protected void okPressed() {
        try {
			saveInput();
		} catch (JAXBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        super.okPressed();
    }
    
    private void saveInput() throws JAXBException, CoreException{	
		BigFileEditor fileEditor = (BigFileEditor) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
		MessageType messagetype = fileEditor.getProjectionViewer().getFileModel().getMessageType(messageTypeName);
		IFile input = BfvFileUtils.convertFileIFile(fileEditor.getEmptyFile());
		String associatedFileName = FilenameUtils.getBaseName(input.getName());
		MessageFunction func = new MessageFunction();
		func.setName(functionName);
		func.setMessageAddress(messageAddress);
		func.setMessageLengthAddress(messageLengthAddress);
		func.setChannelIdReg(channelIDReg);
		func.setChannelNameAddress(channelNameAddress);
		func.setAssociatedFileName(associatedFileName);

		func.setFirst(firstIns.getIdStringGlobalUnique(), firstIns.getFirstLine(), firstIns.getInstructionNameIndex(), firstIns.getFullText(), firstIns.getModule(), firstIns.getModuleId(), firstIns.getModuleOffset(), firstIns.getParentFunction().toString());
		
		if (messagetype == null){
			messagetype = new MessageType(messageTypeName);
		}
		
		if (functionType.equals("Send"))
		{
			func.setFunctionType("Send");
			messagetype.setSend(func);
			
		}else if(functionType.equals("Receive"))
		{
			func.setFunctionType("Receive");
			messagetype.setReceive(func);
		}else if(functionType.equals("SendChannelCreate"))
		{
			func.setFunctionType("SendChannelCreate");
			messagetype.setSendChannelCreate(func);
		}else if(functionType.equals("ReceiveChannelCreate"))
		{
			func.setFunctionType("ReceiveChannelCreate");
			messagetype.setReceiveChannelCreate(func);
		}
		
		fileEditor.getProjectionViewer().getFileModel().addMessageType(messagetype);
    }
	
	public String getMessageTypeName() {
		return messageTypeName;
	}
	public void setMessageTypeName(String messagePair) {
		this.messageTypeName = messagePair;
	}

	public String getMessageAddress() {
		return messageAddress;
	}

	public void setMessageAddress(String messageAddress) {
		this.messageAddress = messageAddress;
	}

	public String getMessageLengthAddress() {
		return messageLengthAddress;
	}

	public void setMessageLengthAddress(String messageLengthAddress) {
		this.messageLengthAddress = messageLengthAddress;
	}

	public String getMessageIDAddress() {
		return channelIDReg;
	}

	public void setMessageIDAddress(String messageIDAddress) {
		this.channelIDReg = messageIDAddress;
	}

	public ComboViewer getComboViewerSide() {
		return comboViewerSide;
	}

	public void setComboViewerSide(ComboViewer comboViewerSide) {
		this.comboViewerSide = comboViewerSide;
	}
	
	public String getSendOrReceive() {
		return functionType;
	}

	public void setSendOrReceive(String sendOrReceive) {
		this.functionType = sendOrReceive;
	}

	public String getFunctionName() {
		return functionName;
	}

	public void setFunction(String functionName) {
		this.functionName = functionName;
	}

	public Instruction getFistIns() {
		return firstIns;
	}


	public void setFistIns(Instruction fistIns) {
		this.firstIns = fistIns;
	}


}
