package ca.uvic.chisel.atlantis.functionparsing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import ca.uvic.chisel.atlantis.datacache.AtlantisFileModelDataLayer;

public class FunctionTreeContentProvider implements ITreeContentProvider {

	private AtlantisFileModelDataLayer fileModel;
	
	public FunctionTreeContentProvider(AtlantisFileModelDataLayer fileModel) {
		this.fileModel = fileModel;
	}
	
	@Override
	public void dispose() { }

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) { }

	@Override
	public Object[] getChildren(Object parentObject) {
		ArrayList<FunctionOrModule> children = new ArrayList<FunctionOrModule>();
		
		if(fileModel != null && fileModel.getFunctionDb().isConnected()) {
			for(Function function : fileModel.getFunctionDb().getFunctionsFromModule(
					this.asFOM(parentObject).getModule(), fileModel.getInstructionDb())) {
				children.add(this.genFOM(function));
			}
		}
		
		Collections.sort(children, new Comparator<FunctionOrModule>() {
			@Override
			public int compare(FunctionOrModule lhs, FunctionOrModule rhs) {
				String leftFoundName = FunctionNameRegistry.getFunction(
						lhs.getFunction().getFirst().getModule(), 
						lhs.getFunction().getFirst().getModuleOffset());
				String rightFoundName = FunctionNameRegistry.getFunction(
						rhs.getFunction().getFirst().getModule(), 
						rhs.getFunction().getFirst().getModuleOffset());
				
				if(leftFoundName == null && rightFoundName != null) {
					return 1;
				} else if(leftFoundName != null && rightFoundName == null) {
					return -1;
				} else if(leftFoundName == null && rightFoundName == null) {
					return lhs.getFunction().compareTo(rhs.getFunction());
				} else {
					return leftFoundName.compareToIgnoreCase(rightFoundName);
				}
			}
		});
		
		return children.toArray();
	}

	@Override
	public Object[] getElements(Object inputElement) {
		ArrayList<FunctionOrModule> elements = new ArrayList<FunctionOrModule>();
		
		if(fileModel != null && fileModel.getInstructionDb().isConnected()) {		
			for(String module : fileModel.getInstructionDb().getModules()) {
				elements.add(this.genFOM(module));
			}
		}
		
		return elements.toArray();
	}

	@Override
	public Object getParent(Object childObject) {
		FunctionOrModule child = (FunctionOrModule)childObject;
		
		// Was occurring when the decomposition view was in use and
		// the active trace was switched.
		if(null == child.getFunction()){
			return null;
		}
		
		return this.genFOM(child.getFunction().getFirst().getModule());
	}

	@Override
	public boolean hasChildren(Object parentObject) {
		
		return !this.asFOM(parentObject).isFunction();
	}
	
	public FunctionOrModule asFOM(Object obj) {
		return (FunctionOrModule)obj;
	}
	
	public FunctionOrModule genFOM(Function func) {
		return new FunctionOrModule(func);
	}
	
	public FunctionOrModule genFOM(String mod) {
		return new FunctionOrModule(mod);
	}
	
	public class FunctionOrModule
	{
		private Function function;
		private String module;
		private boolean isFunction;
		
		public FunctionOrModule(Function function) {
			this.setFunction(function);
		}
		
		public FunctionOrModule(String module) {
			this.setModule(module);
		}
		
		public Function getFunction() {
			return function;
		}
		
		public void setFunction(Function function) {
			this.function = function;
			this.setIsFunction(true);
		}
		
		public String getModule() {
			return module;
		}
		
		public void setModule(String module) {
			this.module = module;
			this.setIsFunction(false);
		}

		public boolean isFunction() {
			return isFunction;
		}

		public void setIsFunction(boolean isFunction) {
			this.isFunction = isFunction;
		}
		
		@Override
		public boolean equals(Object other) {
			FunctionOrModule fom = (FunctionOrModule)other;
			
			if(this.isFunction != fom.isFunction()) {
				return false;
			}
			
			if(this.isFunction) {
				return this.getFunction().equals(fom.getFunction());
			}
			
			return this.getModule().equals(fom.getModule());
		}
		
		@Override
		public String toString() {
			return this.isFunction() ? this.function.toString() : this.module;
		}
	}
}
