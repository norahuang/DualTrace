package ca.uvic.chisel.bfv;


import org.eclipse.core.runtime.*;
import org.eclipse.jface.resource.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.*;

import ca.uvic.chisel.bfv.ColourConstants;
import ca.uvic.chisel.bfv.ImageConstants;

/**
 * The activator class controls the plug-in life cycle
 * @author Laura Chan (some code auto-generated by Eclipse)
 */
public class BigFileActivator extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "ca.uvic.chisel.bfv"; //$NON-NLS-1$
	
	// The shared instance
	private static BigFileActivator plugin;
	
//	private SecurePreferenceStore securePreferenceStore;
	
	private ColorRegistry colourRegistry = null;

	/**
	 * The constructor
	 */
	public BigFileActivator() {
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	@Override
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		colourRegistry = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static BigFileActivator getDefault() {
		return plugin;
	}
	
	/**
	 * Returns an image descriptor for the image file at the given
	 * plug-in relative path
	 *
	 * @param path the path
	 * @return the image descriptor
	 */
	public static ImageDescriptor getImageDescriptor(String path) {
		return imageDescriptorFromPlugin(PLUGIN_ID, path);
	}
	
	/**
	 * Initializes an image registry containing the icons needed for this application
	 * @param registry ImageRegistry to initialize for this application
	 */
	@Override
	protected void initializeImageRegistry(ImageRegistry registry) {
		super.initializeImageRegistry(registry);
		Bundle bundle = Platform.getBundle(PLUGIN_ID);
		
		// Temporary icons for collapsed/expanded regions that have sub-regions. TODO replace with better icons
		ImageDescriptor parentRegionCollapsed = ImageDescriptor.createFromURL(FileLocator.find(bundle, 
				new Path("icons/parent_region_collapsed.gif"), null));
		ImageDescriptor parentRegionExpanded = ImageDescriptor.createFromURL(FileLocator.find(bundle, 
				new Path("icons/parent_region_expanded.gif"), null));
		registry.put(ImageConstants.ICON_PARENT_REGION_COLLAPSED, parentRegionCollapsed);
		registry.put(ImageConstants.ICON_PARENT_REGION_EXPANDED, parentRegionExpanded);
		
		// Temporary icons for file compare. TODO these are pretty ugly, look into replacing with better icons
		ImageDescriptor showAll = ImageDescriptor.createFromURL(FileLocator.find(bundle, new Path("icons/show_all.gif"), null));
		ImageDescriptor showDifferences = ImageDescriptor.createFromURL(FileLocator.find(bundle, new Path("icons/show_differences.gif"), null));
		ImageDescriptor showMatches = ImageDescriptor.createFromURL(FileLocator.find(bundle, new Path("icons/show_matches.gif"), null));
		registry.put(ImageConstants.ICON_SHOW_ALL, showAll);
		registry.put(ImageConstants.ICON_SHOW_DIFFERENCES, showDifferences);
		registry.put(ImageConstants.ICON_SHOW_MATCHES, showMatches);
		
		// Icons for tags and tag occurrences
		ImageDescriptor tagOccurrence = ImageDescriptor.createFromURL(FileLocator.find(bundle, new Path("icons/tag.gif"), null));
		ImageDescriptor tag = ImageDescriptor.createFromURL(FileLocator.find(bundle, new Path("icons/taggroup.gif"), null));
		registry.put(ImageConstants.ICON_TAG_OCCURRENCE, tagOccurrence);
		registry.put(ImageConstants.ICON_TAG, tag);
	}
	
	/**
	 * Gets this plugin's colour registry, initializing a new one if it is currently null. Code for this method adapted from 
	 * org.eclipse.ui.plugin.AbstractUIPlugin.getImageRegistry(), with uses of ImageRegistry converted to ColorRegistry.
	 * @return this plugin's colour registry
	 */
	public ColorRegistry getColorRegistry() {
		if (colourRegistry == null) {
			colourRegistry = createColorRegistry();
			initializeColorRegistry(colourRegistry);
		}
		return colourRegistry;
	}
	
	/**
	 * Sets up a ColorRegistry for managing colours used by this application. 
	 * @param registry ColorRegistry to initialize
	 */
	public void initializeColorRegistry(ColorRegistry registry) {
		// Add tooltip colours
		registry.put(ColourConstants.TOOLTIP_GREY, new RGB(221, 221, 221));
		registry.put(ColourConstants.TOOLTIP_RED, new RGB(255, 170, 170));
		registry.put(ColourConstants.TOOLTIP_ORANGE, new RGB(255, 204, 153));
		registry.put(ColourConstants.TOOLTIP_YELLOW, new RGB(255, 255, 153));
		registry.put(ColourConstants.TOOLTIP_GREEN, new RGB(153, 238, 187));
		registry.put(ColourConstants.TOOLTIP_CYAN, new RGB(170, 255, 255));
		registry.put(ColourConstants.TOOLTIP_BLUE, new RGB(136, 204, 255));
		registry.put(ColourConstants.TOOLTIP_PURPLE, new RGB(221, 170, 204));
		registry.put(ColourConstants.TOOLTIP_CREAM, new RGB(255, 255, 238));
	}
	
	/**
	 * Creates a new colour registry for this plugin. All of the code for this method was copied from 
	 * org.eclipse.ui.plugin.AbstractUIPlugin.createImageRegistry(), with uses of ImageRegistry converted to ColorRegistry.
	 * @return new colour registry for this plugin
	 */
	protected ColorRegistry createColorRegistry() {
    	//If we are in the UI Thread use that
    	if(Display.getCurrent() != null) {
			return new ColorRegistry(Display.getCurrent());
		}
    	
    	if(PlatformUI.isWorkbenchRunning()) {
			return new ColorRegistry(PlatformUI.getWorkbench().getDisplay());
		}
    	
    	//Invalid thread access if it is not the UI Thread 
    	//and the workbench is not created.
    	throw new SWTError(SWT.ERROR_THREAD_INVALID_ACCESS);
    }
}
