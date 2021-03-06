package org.python.pydev.editor.actions;

import java.io.File;
import java.util.List;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.structure.FastStringBuffer;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.parser.fastparser.FastParser;
import org.python.pydev.parser.jython.ast.stmtType;
import org.python.pydev.parser.visitors.NodeUtils;
import org.python.pydev.plugin.PydevPlugin;

public class PyCopyQualifiedName extends PyAction{

    public void run(IAction action) {
    	FastStringBuffer buf = new FastStringBuffer();
        try {
        	PyEdit pyEdit = getPyEdit();
        	
        	PySelection pySelection = new PySelection(pyEdit);
        	
        	IPythonNature nature = pyEdit.getPythonNature();
        	File editorFile = pyEdit.getEditorFile();
			buf.append(nature.resolveModule(editorFile));
			
			List<stmtType> path = FastParser.parseToKnowGloballyAccessiblePath(
					pySelection.getDoc(), pySelection.getStartLineIndex());
			for (stmtType stmtType : path) {
				if(buf.length() > 0){
					buf.append('.');
				}
				buf.append(NodeUtils.getRepresentationString(stmtType));
			}
			
		} catch (MisconfigurationException e1) {
			PydevPlugin.log(e1);
			return;
		}
        
        Transfer[] dataTypes = new Transfer[] {TextTransfer.getInstance()};
		Object[] data = new Object[] {buf.toString()};
        
		Clipboard clipboard= new Clipboard(getShell().getDisplay());
		try {
			clipboard.setContents(data, dataTypes);
		} catch (SWTError e) {
			if (e.code != DND.ERROR_CANNOT_SET_CLIPBOARD) {
				throw e;
			}
			MessageDialog.openError(getShell(), "Error copying to clipboard.", e.getMessage());
		} finally {
			clipboard.dispose();
		}
    }

}
