package ca.uvic.chisel.atlantis.project;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.runtime.CoreException;

/**
 * Nature for trace analysis projects
 * @author Laura Chan
 */
public class TraceAnalysisProjectNature implements IProjectNature {
	public static final String ID = "ca.uvic.chisel.atlantis.project.TraceAnalysisProjectNature";

	@Override
	public void configure() throws CoreException {
		// TODO Auto-generated method stub

	}

	@Override
	public void deconfigure() throws CoreException {
		// TODO Auto-generated method stub

	}

	@Override
	public IProject getProject() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setProject(IProject project) {
		// TODO Auto-generated method stub

	}
}
