/*
 * Created on May 5, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.refactoring;

import org.python.pydev.core.ExtensionHelper;

/**
 * @author Fabio Zadrozny
 */
public abstract class AbstractPyRefactoring implements IPyRefactoring{

    /**
     * Instead of making all static, let's use a singleton... it may be useful...
     */
    private volatile static IPyRefactoring pyRefactoring;

    
    /**
     * 
     * @return the pyrefactoring instance that is available (can be some plugin contribution). 
     */
    public synchronized static IPyRefactoring getPyRefactoring(){
        if (AbstractPyRefactoring.pyRefactoring == null){
            IPyRefactoring r = (IPyRefactoring) ExtensionHelper.getParticipant(ExtensionHelper.PYDEV_REFACTORING);
            if(r != null){
                AbstractPyRefactoring.pyRefactoring = r;
            }else{
                throw new RuntimeException("Refactoring engine not in place! com.python.pydev.refactoring plugin not in place?");
            }
        }
        return AbstractPyRefactoring.pyRefactoring;
    }


    public synchronized static void setPyRefactoring(IPyRefactoring refactorer) {
        pyRefactoring = refactorer;
    }



}
