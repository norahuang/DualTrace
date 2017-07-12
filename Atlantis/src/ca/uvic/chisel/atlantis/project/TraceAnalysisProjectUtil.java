package ca.uvic.chisel.atlantis.project;

import java.net.URI;
import java.util.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;

/**
 * Contains static utility methods relating to trace analysis projects. 
 * @author Laura Chan
 */
public class TraceAnalysisProjectUtil {
	
	/**
	 * Creates a new trace analysis project.
	 * @param name name of project to create
	 * @param location location in which to create the project
	 * @return the project that was created
	 */
	public static IProject createProject(String name, URI location) {
		Assert.isNotNull(name);
		Assert.isTrue(name.trim().length() > 0);
		
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(name);
		if (!project.exists()) {
			IProjectDescription description = project.getWorkspace().newProjectDescription(project.getName());
			
			// Determine project location
			URI projectLocation = location;
			if (location != null && ResourcesPlugin.getWorkspace().getRoot().getLocationURI().equals(location)) {
				projectLocation = null; // will use default location
			} 
			description.setLocationURI(projectLocation);
			
			// Add Trace Project nature
			String[] natures = description.getNatureIds();
			natures = Arrays.copyOf(natures, natures.length + 1);
			natures[natures.length - 1] = TraceAnalysisProjectNature.ID;
			description.setNatureIds(natures);
			
			// Create and open the project
			try {
				project.create(description, null);
				if (!project.isOpen()) {
					project.open(null);
				}
			} catch (CoreException e) {
				e.printStackTrace();
				project = null;
			}
		} else {
			System.out.println("INFO: project " + name + " already exists");
		}
		return project;
	}
}
