/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package ca.uvic.chisel.bfv.dialogs;

import java.util.ResourceBundle;

import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.window.Window;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;

import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.internal.texteditor.NLSUtility;
import org.eclipse.ui.internal.texteditor.TextEditorPlugin;

import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.TextEditorAction;

import ca.uvic.chisel.bfv.editor.BigFileViewer;


/**
 * Action for jumping to a particular line in the editor's text viewer. The user is requested to
 * enter the line number into an input dialog. The action is initially associated with a text editor
 * via the constructor, but that can be subsequently changed using <code>setEditor</code>.
 * <p>
 * The following keys, prepended by the given option prefix, are used for retrieving resources from
 * the given bundle:
 * <ul>
 * <li><code>"dialog.invalid_range"</code> - to indicate an invalid line number</li>
 * <li><code>"dialog.invalid_input"</code> - to indicate an invalid line number format</li>
 * <li><code>"dialog.title"</code> - the input dialog's title</li>
 * <li><code>"dialog.message"</code> - the input dialog's message</li>
 * </ul>
 * </p>
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 * 
 * @noextend This class is not intended to be subclassed by clients.
 */
public class GotoLineAction extends TextEditorAction {
	// org.eclipse.ui.texteditor.GotoLineAction is a short class,
	// and not intended for extension, soI copied the whole thing and modified.

	/**
	 * Validates whether the text found in the input field of the dialog forms a valid line number.
	 * A number is valid if it is one to which can be jumped.
	 */
	class NumberValidator implements IInputValidator {

		/*
		 * @see IInputValidator#isValid(String)
		 */
		@Override
		public String isValid(String input) {

			if (input == null || input.length() == 0)
				return " "; //$NON-NLS-1$

			try {
				int i= Integer.parseInt(input);
				if (i <= 0 || fLastLine < i)
					return fBundle.getString(fPrefix + "dialog.invalid_range"); //$NON-NLS-1$

			} catch (NumberFormatException x) {
				return fBundle.getString(fPrefix + "dialog.invalid_input"); //$NON-NLS-1$
			}

			return null;
		}
	}

	/**
	 * Standard input dialog with custom dialog bounds strategy and settings.
	 * 
	 * @since 2.0
	 */
	static class GotoLineDialog extends InputDialog {

		/*
		 * @see InputDialog#InputDialog(org.eclipse.swt.widgets.Shell, java.lang.String, java.lang.String, java.lang.String, org.eclipse.jface.dialogs.IInputValidator)
		 */
		public GotoLineDialog(Shell parent, String title, String message, String initialValue, IInputValidator validator) {
			super(parent, title, message, initialValue, validator);
		}

		/*
		 * @see org.eclipse.jface.dialogs.Dialog#getDialogBoundsSettings()
		 * @since 3.2
		 */
		@Override
		protected IDialogSettings getDialogBoundsSettings() {
			String sectionName= getClass().getName() + "_dialogBounds"; //$NON-NLS-1$
			IDialogSettings settings= TextEditorPlugin.getDefault().getDialogSettings();
			IDialogSettings section= settings.getSection(sectionName);
			if (section == null)
				section= settings.addNewSection(sectionName);
			return section;
		}

		/*
		 * @see org.eclipse.jface.dialogs.Dialog#getDialogBoundsStrategy()
		 * @since 3.2
		 */
		@Override
		protected int getDialogBoundsStrategy() {
			return DIALOG_PERSISTLOCATION;
		}
	}

	/** The biggest valid line number of the presented document */
	private long fLastLine;

	/** This action's resource bundle */
	private ResourceBundle fBundle;

	/** This action's prefix used for accessing the resource bundle */
	private String fPrefix;

	// These used to be in Eclipse's EditorMessages, but that's inaccessible, so I copied the important bits here.
	private static final String BUNDLE_FOR_CONSTRUCTED_KEYS= "org.eclipse.ui.texteditor.ConstructedEditorMessages";//$NON-NLS-1$
	private static ResourceBundle fgBundleForConstructedKeys= ResourceBundle.getBundle(BUNDLE_FOR_CONSTRUCTED_KEYS);

	private final BigFileViewer fileViewer;
	
	/**
	 * Creates a new action for the given text editor. The action configures its visual
	 * representation with default values.
	 * 
	 * @param editor the text editor
	 * @param viewer 
	 * @see TextEditorAction#TextEditorAction(ResourceBundle, String, ITextEditor)
	 * @since 3.5
	 */
	public GotoLineAction(ITextEditor editor, BigFileViewer viewer) {
		super(fgBundleForConstructedKeys, "Editor.GotoLine.", editor); //$NON-NLS-1$
		// Need the file viewer so that we can *later* get the FileModelDAO from it.
		// Initialization order requires this class to be created at a certain time,
		// which occurs before the FileViewer is completed.
		this.fileViewer = viewer;
		fBundle= fgBundleForConstructedKeys;
		fPrefix= "Editor.GotoLine."; //$NON-NLS-1$
	}

	/**
	 * Jumps to the given line.
	 * 
	 * @param line the line to jump to
	 */
	private void gotoLine(long line) {
		// Extra stuff needs to be done by the fileViewer. Let it do work.
		try {
			fileViewer.gotoLineAtOffset((int)line, 0);
		} catch (BadLocationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Jumps to the given line.
	 *
	 * @param line the line to jump to
	 */
	private void gotoLineOriginal(long line) {

		ITextEditor editor= getTextEditor();

		IDocumentProvider provider= editor.getDocumentProvider();
		IDocument document= provider.getDocument(editor.getEditorInput());
		try {

			int start= document.getLineOffset((int)line);
			editor.selectAndReveal(start, 0);

			IWorkbenchPage page= editor.getSite().getPage();
			page.activate(editor);

		} catch (BadLocationException x) {
			// ignore
		}
	}

	/*
	 * @see Action#run()
	 */
	@Override
	public void run() {
		ITextEditor editor= getTextEditor();

		if (editor == null)
			return;

		IDocumentProvider docProvider= editor.getDocumentProvider();
		if (docProvider == null)
			return;

		IDocument document= docProvider.getDocument(editor.getEditorInput());
		if (document == null)
			return;
		
		fLastLine= this.fileViewer.getFileModel().getNumberOfLines();

		String title= fBundle.getString(fPrefix + "dialog.title"); //$NON-NLS-1$
		String message= NLSUtility.format(fBundle.getString(fPrefix + "dialog.message"), new Long(fLastLine)); //$NON-NLS-1$

		String currentLineStr= ""; //$NON-NLS-1$
		ISelection selection= editor.getSelectionProvider().getSelection();
		if (selection instanceof ITextSelection) {
			ITextSelection textSelection= (ITextSelection)selection;
			Control textWidget= (Control)editor.getAdapter(Control.class);
			boolean caretAtStartOfSelection= false;
			if (textWidget instanceof StyledText)
				caretAtStartOfSelection= ((StyledText)textWidget).getSelection().x == ((StyledText)textWidget).getCaretOffset();
			int currentLine;
			if (caretAtStartOfSelection)
				currentLine= textSelection.getStartLine();
			else {
				int endOffset= textSelection.getOffset() + textSelection.getLength();
				try {
					currentLine= document.getLineOfOffset(endOffset);
				} catch (BadLocationException ex) {
					currentLine= -1;
				}
			}
			if (currentLine > -1)
				currentLineStr= Integer.toString(currentLine + 1);
		}

		GotoLineDialog d= new GotoLineDialog(editor.getSite().getShell(), title, message, currentLineStr, new NumberValidator());
		if (d.open() == Window.OK) {
			try {
				long line= Long.parseLong(d.getValue());
				gotoLine(line - 1);
			} catch (NumberFormatException x) {
			}
		}
	}
}

