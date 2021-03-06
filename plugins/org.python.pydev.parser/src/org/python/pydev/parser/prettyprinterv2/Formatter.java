/*
 * Created on Feb 17, 2006
 */
package org.python.pydev.parser.prettyprinterv2;

import org.eclipse.jface.text.IDocument;
import org.python.pydev.core.IPyEdit;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.Tuple;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.docutils.SyntaxErrorException;
import org.python.pydev.core.log.Log;
import org.python.pydev.parser.PyParser;
import org.python.pydev.parser.jython.SimpleNode;

public class Formatter implements IFormatter{

    public void formatAll(IDocument doc, IPyEdit edit, boolean isOpenedFile, boolean throwSyntaxError) throws SyntaxErrorException {
            Tuple<SimpleNode, Throwable> objects;
            try{
                objects = PyParser.reparseDocument(new PyParser.ParserInfo(doc, true, edit.getPythonNature()));
            }catch(MisconfigurationException e1){
                Log.log(e1);
                return;
            }
            
            if(objects.o2!= null){
                if(throwSyntaxError){
                    throw new SyntaxErrorException();
                }
                
            } else if (objects.o1 != null) {
                try{
                    SimpleNode n = objects.o1;
                    PrettyPrinterV2 prettyPrinterV2 = new PrettyPrinterV2(new PrettyPrinterPrefsV2("\n", edit.getIndentPrefs().getIndentationString(), edit.getGrammarVersionProvider()));
                    doc.set(prettyPrinterV2.print(n));
                }catch(Exception e){
                    Log.log(e);
                }
            }
    }

    public void formatSelection(IDocument doc, int startLine, int endLineIndex, IPyEdit edit, PySelection ps) {
    }

}
