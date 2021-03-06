/*
 * Created on 01/10/2005
 */
package com.python.pydev.analysis;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.text.Document;
import org.python.pydev.core.ICodeCompletionASTManager;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.TestDependent;
import org.python.pydev.core.Tuple;
import org.python.pydev.core.callbacks.ICallback;
import org.python.pydev.core.docutils.StringUtils;
import org.python.pydev.editor.autoedit.TestIndentPrefs;
import org.python.pydev.editor.codecompletion.revisited.CodeCompletionTestsBase;
import org.python.pydev.editor.codecompletion.revisited.ProjectModulesManager;
import org.python.pydev.editor.codecompletion.revisited.modules.AbstractModule;
import org.python.pydev.editor.codecompletion.revisited.modules.CompiledModule;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceModule;
import org.python.pydev.ui.pythonpathconf.InterpreterInfo;

import com.python.pydev.analysis.additionalinfo.AbstractAdditionalDependencyInfo;
import com.python.pydev.analysis.additionalinfo.AdditionalProjectInterpreterInfo;
import com.python.pydev.analysis.additionalinfo.AdditionalSystemInterpreterInfo;
import com.python.pydev.analysis.additionalinfo.builders.InterpreterObserver;
import com.python.pydev.analysis.messages.IMessage;

public class AnalysisTestsBase extends CodeCompletionTestsBase {

    protected String sDoc;
    protected Document doc;
    protected OccurrencesAnalyzer analyzer;
    protected IMessage[] msgs;
    protected AnalysisPreferencesStub prefs;
    protected boolean forceAdditionalInfoRecreation = false;

    //additional info
    protected InterpreterObserver observer;

    /**
     * @return Returns the manager.
     */
    protected ICodeCompletionASTManager getManager() {
        return (ICodeCompletionASTManager) nature.getAstManager();
    }

    /*
     * @see TestCase#setUp()
     */
    public void setUp() throws Exception {
        super.setUp();
        AbstractAdditionalDependencyInfo.TESTING = true;
        ProjectModulesManager.IN_TESTS = true;
        //additional info
        observer = new InterpreterObserver();

        CompiledModule.COMPILED_MODULES_ENABLED = true;
        
        final String paths = getSystemPythonpathPaths();
        String lower = paths.toLowerCase();
        lower = StringUtils.replaceAllSlashes(lower);
        final Set<String> s = new HashSet<String>(Arrays.asList(lower.split("\\|")));
        InterpreterInfo.configurePathsCallback = new ICallback<Boolean, Tuple<List<String>, List<String>>>(){
            
            public Boolean call(Tuple<List<String>, List<String>> arg) {
                List<String> toAsk = arg.o1;
                List<String> l = arg.o2;
                
                for(String t:toAsk){
                    if(s.contains(StringUtils.replaceAllSlashes(t.toLowerCase()))){
                        l.add(t);
                        //System.out.println("Added:"+t);
                    }
                }
                return Boolean.TRUE;
            }
            
        };
        
        restorePythonPath(paths, false);
        prefs = new AnalysisPreferencesStub();
        analyzer = new OccurrencesAnalyzer();
        
    }

    protected String getSystemPythonpathPaths() {
        final String paths;
        if(TestDependent.PYTHON_WXPYTHON_PACKAGES != null){
            paths = TestDependent.PYTHON_LIB+"|"+TestDependent.PYTHON_SITE_PACKAGES+"|"+TestDependent.PYTHON_WXPYTHON_PACKAGES;
        }else{
            paths = TestDependent.PYTHON_LIB+"|"+TestDependent.PYTHON_SITE_PACKAGES;
        }
        return paths;
    }

    /*
     * @see TestCase#tearDown()
     */
    public void tearDown() throws Exception {
        super.tearDown();
        AbstractAdditionalDependencyInfo.TESTING = false;
        CompiledModule.COMPILED_MODULES_ENABLED = false;
    }

    /**
     * Uses the doc attribute as the module and makes the analysis, checking if no error is found.
     */
    protected void checkNoError() {
        analyzer = new OccurrencesAnalyzer();
        msgs = analyze();
        
        printMessages(msgs,0);
    }

    /**
     * Uses the doc attribute as the module and makes the analysis, checking if no error is found.
     * @return the messages that were reported as errors
     */
    protected IMessage[] checkError(int numberOfErrors) {
        analyzer = new OccurrencesAnalyzer();
        msgs = analyze();
        
        printMessages(msgs,numberOfErrors);
        return msgs;
    }

    private IMessage[] analyze() {
        try{
            return analyzer.analyzeDocument(nature, (SourceModule) AbstractModule.createModuleFromDoc(null, null, doc, nature, 0), 
                    prefs, doc, new NullProgressMonitor(), new TestIndentPrefs(true, 4));
        }catch(MisconfigurationException e){
            throw new RuntimeException(e);
        }
    }
    

    // ---------------------------------------------------------------------- additional info
    @Override
    protected boolean restoreSystemPythonPath(boolean force, String path) {
        boolean restored = super.restoreSystemPythonPath(force, path);
        if(restored){
            IProgressMonitor monitor = new NullProgressMonitor();
            
            //try to load it from previous session
            IInterpreterManager interpreterManager = getInterpreterManager();
            try{
                if(forceAdditionalInfoRecreation || !AdditionalSystemInterpreterInfo.loadAdditionalSystemInfo(interpreterManager, interpreterManager.getDefaultInterpreter())){
                    observer.notifyDefaultPythonpathRestored(interpreterManager, interpreterManager.getDefaultInterpreter(), monitor);
                }
            }catch(MisconfigurationException e){
                throw new RuntimeException(e);
            }
        }
        return restored;
    }
    
    @Override
    protected boolean restoreProjectPythonPath(boolean force, String path) {
        boolean ret = super.restoreProjectPythonPath(force, path);
        if(ret){
            //try to load it from previous session
            boolean loaded;
			try {
				loaded = AdditionalProjectInterpreterInfo.loadAdditionalInfoForProject(nature);
			} catch (MisconfigurationException e) {
				throw new RuntimeException(e);
			}
			if(forceAdditionalInfoRecreation || !loaded){
                observer.notifyProjectPythonpathRestored(nature, new NullProgressMonitor());
            }
        }
        return ret;
    }

    //------------------------------------------------------------------------- analysis
    /**
     * @param msgs
     */
    protected void printMessages(IMessage ... msgs) {
        for (int i = 0; i < msgs.length; i++) {
            System.out.println(msgs[i]);
        }
    }
    protected void assertNotContainsMsg(String msg, IMessage[] msgs2) {
        if(containsMsg(msg, msgs2) != null){
            fail("The message "+msg+" was found within the messages (it should not have been found).");
        }
    }
    protected IMessage assertContainsMsg(String msg, IMessage[] msgs2) {
        return assertContainsMsg(msg, msgs2, -1);
    }

    protected IMessage assertContainsMsg(String msg, IMessage[] msgs2, int line) {
        IMessage found = containsMsg(msg, msgs2, line);
        
        if(found != null){
            return found;
        }
        
        StringBuffer msgsAvailable = new StringBuffer();
        for (IMessage message : msgs2) {
            msgsAvailable.append(message.getMessage());
            msgsAvailable.append("\n");
        }
        fail(StringUtils.format("No message named %s could be found. Available: %s", msg, msgsAvailable));
        return null;
    }

    /**
     * Checks if a specific message is contained within the messages passed
     */
    protected IMessage containsMsg(String msg, IMessage[] msgs2) {
        return containsMsg(msg, msgs2, -1);
    }
    
    /**
     * Checks if a specific message is contained within the messages passed
     */
    protected IMessage containsMsg(String msg, IMessage[] msgs2, int line) {
        IMessage ret = null;
        for (IMessage message : msgs2) {
            if(message.getMessage().equals(msg)){
                if(line != -1){
                    ret = message;
                    if(line == message.getStartLine(doc)){
                        return message;
                    }
                }else{
                    return message;
                }
            }
        }
        
        if(line != -1){
            fail("The message :"+msg+" was not found in the specified line ("+line+")");
        }
        return ret;
    }

    protected void printMessages(IMessage[] msgs, int i) {
        if(msgs.length != i){
            printMessages(msgs);
        }
        assertEquals(i, msgs.length);
    }

}
