package org.python.pydev.debug.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListResourceBundle;
import java.util.Map;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IBreakpointListener;
import org.eclipse.debug.core.IBreakpointManager;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.ui.actions.IToggleBreakpointsTarget;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.IAnnotationModelExtension;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.MarkerAnnotation;
import org.python.pydev.core.callbacks.ICallbackListener;
import org.python.pydev.debug.ui.actions.AbstractBreakpointRulerAction;
import org.python.pydev.editor.IPyEditListener;
import org.python.pydev.editor.IPyEditListener4;
import org.python.pydev.editor.PyEdit;

/**
 * This class is used to keep the annotations related to the debugger in sync with external editors
 * (if we're not dealing with an external editor, this class won't actually do anything)
 * 
 * @author Fabio
 */
public class PyEditBreakpointSync implements IBreakpointListener, IPyEditListener, IPyEditListener4 {

    private PyEdit edit;

    // breakpoints listening -------------------------------------------------------------------------------------------
    // breakpoints listening -------------------------------------------------------------------------------------------
    // breakpoints listening -------------------------------------------------------------------------------------------
    
    public void breakpointAdded(IBreakpoint breakpoint) {
        updateAnnotations();
    }

    public void breakpointChanged(IBreakpoint breakpoint, IMarkerDelta delta) {
    }

    public void breakpointRemoved(IBreakpoint breakpoint, IMarkerDelta delta) {
        updateAnnotations();
    }
    
    

    // pyedit listening ------------------------------------------------------------------------------------------------
    // pyedit listening ------------------------------------------------------------------------------------------------
    // pyedit listening ------------------------------------------------------------------------------------------------
    
    public void onCreateActions(ListResourceBundle resources, PyEdit edit, IProgressMonitor monitor) {
    }

    public void onDispose(PyEdit edit, IProgressMonitor monitor) {
        if(this.edit != null){
            this.edit = null;
            IBreakpointManager breakpointManager= DebugPlugin.getDefault().getBreakpointManager();
            breakpointManager.removeBreakpointListener(this);
        }
    }

    public void onSave(PyEdit edit, IProgressMonitor monitor) {
        updateAnnotations();
    }

    /**
     * When the document is set, this class will start listening for the breakpoint manager, so that any changes in it
     * will update the debug annotations.
     */
    public void onSetDocument(IDocument document, PyEdit edit, IProgressMonitor monitor) {
        if(this.edit != null){
            this.edit = null;
            IBreakpointManager breakpointManager= DebugPlugin.getDefault().getBreakpointManager();
            breakpointManager.removeBreakpointListener(this);
        }
        
        if(AbstractBreakpointRulerAction.isExternalFileEditor(edit)){
            this.edit = edit;
            IBreakpointManager breakpointManager= DebugPlugin.getDefault().getBreakpointManager();
            breakpointManager.addBreakpointListener(this);
        }
        
        //initial update (the others will be on changes)
        updateAnnotations();
    }
    
    

    // update annotations ----------------------------------------------------------------------------------------------
    // update annotations ----------------------------------------------------------------------------------------------
    // update annotations ----------------------------------------------------------------------------------------------
    
    @SuppressWarnings("unchecked")
    private void updateAnnotations() {
        if(edit != null){
            IDocumentProvider provider= edit.getDocumentProvider();
            IAnnotationModel model = provider.getAnnotationModel(edit.getEditorInput());
            IAnnotationModelExtension modelExtension = (IAnnotationModelExtension) model;
            
            if(model == null){
                return;
            }
            List<Annotation> existing = new ArrayList<Annotation>();
            Iterator<Annotation> it = model.getAnnotationIterator();
            if(it == null){
                return;
            }
            while(it.hasNext()){
                existing.add(it.next());
            }
            
            
            IDocument doc = edit.getDocument();
            IResource resource = AbstractBreakpointRulerAction.getResourceForDebugMarkers(edit);
            IEditorInput externalFileEditorInput = AbstractBreakpointRulerAction.getExternalFileEditorInput(edit);
            List<IMarker> markers = AbstractBreakpointRulerAction.getMarkersFromEditorResource(
            		resource, doc, externalFileEditorInput, 0, false);
            
            
            Map<Annotation, Position> annotationsToAdd = new HashMap<Annotation, Position>();
            for(IMarker m:markers){
                Position pos = AbstractBreakpointRulerAction.getMarkerPosition(doc, m);
                MarkerAnnotation newAnnotation = new MarkerAnnotation(m);
                annotationsToAdd.put(newAnnotation, pos);
            }
            
            //update all in a single step
            modelExtension.replaceAnnotations(existing.toArray(new Annotation[0]), annotationsToAdd);
        }
    }

    
	public void onEditorCreated(final PyEdit edit) {
		edit.onGetAdapter.registerListener(new ICallbackListener() {
			
			public Object call(Object obj) {
		    	if(IToggleBreakpointsTarget.class == obj){
		    		Map<String, Object> cache = edit.getCache();
		    		String key = "PyEditBreakpointSync.ToggleBreakpointsTarget";
					Object object = cache.get(key);
		    		if(object == null){
		    			object = new PyToggleBreakpointsTarget();
		    			cache.put(key, object);
		    		}
		    		
		    		return object;
		    	}
				return null;
			}
		});
	}

}
