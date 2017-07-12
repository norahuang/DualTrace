package ca.uvic.chisel.atlantis.wizards;

import ca.uvic.chisel.atlantis.project.*;

import java.net.URI;

import org.eclipse.core.runtime.*;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.*;
import org.eclipse.ui.dialogs.*;
import org.eclipse.ui.wizards.newresource.BasicNewProjectResourceWizard;

/**
 * Wizard for creating a new trace analysis project.
 * @author Laura Chan
 */
public class TraceAnalysisProjectWizard extends Wizard implements INewWizard, IExecutableExtension {
	
	private WizardNewProjectCreationPage page;
	private IConfigurationElement config;
	
	/**
	 * Creates a new wizard.
	 */
	public TraceAnalysisProjectWizard() {
		this.setWindowTitle("Trace Analysis Project Wizard");
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		// TODO Auto-generated method stub
	}

	@Override
	public boolean performFinish() {
		URI location = null;
		if (!page.useDefaults()) {
			location = page.getLocationURI();
		}
		TraceAnalysisProjectUtil.createProject(page.getProjectName(), location);
		BasicNewProjectResourceWizard.updatePerspective(config);
		return true;
	}

	@Override
	public void addPages() {
		super.addPages();
		page = new WizardNewProjectCreationPage("New Trace Analysis Project Wizard");
		page.setTitle("New Trace Analysis Project");
		page.setDescription("Create a new trace analysis project.");
		this.addPage(page);
	}

	@Override
	public void setInitializationData(IConfigurationElement config,	String propertyName, Object data) throws CoreException {
		this.config = config;
	}
}
