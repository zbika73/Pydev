/*
 * Created on May 21, 2006
 */
package com.python.pydev.refactoring.actions;

import java.util.ListResourceBundle;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.editor.IPyEditListener;
import org.python.pydev.editor.PyEdit;

import com.python.pydev.refactoring.ui.findreplace.PySearchInOpenDocumentsAction;


public class ActionCreatorPyEditListener implements IPyEditListener{

    public void onSave(PyEdit edit, IProgressMonitor monitor) {
    }

    public void onCreateActions(ListResourceBundle resources, PyEdit edit, IProgressMonitor monitor) {
        edit.addOfflineActionListener("r", new PyRenameInFileAction(edit), "Rename occurrences in file", false);
        
        edit.addOfflineActionListener("s", new PySearchInOpenDocumentsAction(edit), "Search in open documents", true);
        
        
//	Experimental code for creating a class derived of a class that's not public! -- depends on javassist.
//        try {
//			ClassLoader classLoader = IEditorStatusLine.class.getClassLoader();
//			ProtectionDomain protectionDomain = IEditorStatusLine.class.getProtectionDomain();
//
//        	
//        	ClassPool pool = ClassPool.getDefault();
//        	pool.insertClassPath(new ClassClassPath(this.getClass()));
//        	final CtClass ctClassNew = pool.makeClass("org.eclipse.ui.texteditor.PydevFindReplaceDialog");
//        	
//			CtClass ctClassOriginal = pool.get("org.eclipse.ui.texteditor.FindReplaceDialog");
//			ctClassNew.setModifiers(Modifier.PUBLIC);
//			ctClassNew.setSuperclass(ctClassOriginal);
//			
//			CtMethod afterCreateContents = CtNewMethod.make(
//				"public void createContents(org.eclipse.swt.widgets.Composite parent){" +
//					"super.createContents(parent);" +
//					"System.out.println(\"test\");" +
//				"}", ctClassNew);
//			ctClassNew.addMethod(afterCreateContents);
//			final Class class1 = ctClassNew.toClass(classLoader, protectionDomain);
//			RunInUiThread.async(new Runnable() {
//			
//			public void run() {
//				Object newInstance;
//				try {
//					newInstance = class1.getConstructor(Shell.class).newInstance(PyAction.getShell());
//					System.out.println(newInstance);
//				} catch (Throwable e) {
//					e.printStackTrace();
//				}
//			}
//		});
//		} catch (Exception e) {
//			e.printStackTrace();//ignored
//		}

        
        
        
//	Removed because the way the action was done is not really maintainable.
//
//		// -------------------------------------------------------------------------------------
//		// Find/Replace 
//		FindReplaceAction action = new FindReplaceAction(resources, "Editor.FindReplace.", edit);
//		action.setActionDefinitionId(IWorkbenchCommandConstants.EDIT_FIND_AND_REPLACE);
//		action.setId("org.python.pydev.editor.actions.findAndReplace");
//		edit.setAction(ITextEditorActionConstants.FIND, action);
    }

    public void onDispose(PyEdit edit, IProgressMonitor monitor) {
    }

    public void onSetDocument(IDocument document, PyEdit edit, IProgressMonitor monitor) {
    }

}
