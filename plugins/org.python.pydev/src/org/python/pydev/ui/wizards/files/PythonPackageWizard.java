/*
 * Created on Jan 17, 2006
 */
package org.python.pydev.ui.wizards.files;

import java.io.ByteArrayInputStream;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.python.pydev.core.docutils.StringUtils;
import org.python.pydev.ui.filetypes.FileTypesPreferencesPage;


public class PythonPackageWizard  extends AbstractPythonWizard {

    public PythonPackageWizard() {
        super("Create a new Python package");
    }

    public static final String WIZARD_ID = "org.python.pydev.ui.wizards.files.PythonPackageWizard";

    @Override
    protected PythonAbstractPathPage createPathPage() {
        return new PythonAbstractPathPage(this.description, selection){

            @Override
            protected boolean shouldCreatePackageSelect() {
                return false;
            }
            
        };
    }

    /**
     * We will create the complete package path given by the user (all filled with __init__) 
     * and we should return the last __init__ module created.
     */
    @Override
    protected IFile doCreateNew(IProgressMonitor monitor) throws CoreException {
        IContainer validatedSourceFolder = filePage.getValidatedSourceFolder();
        IFile lastFile = null;
        if(validatedSourceFolder == null){
            return null;
        }
        IContainer parent = validatedSourceFolder;
        String validatedName = filePage.getValidatedName();
        for (String packagePart : StringUtils.dotSplit(validatedName)) {
            IFolder folder = parent.getFolder(new Path(packagePart));
            if(!folder.exists()){
                folder.create(true, true, monitor);
            }
            parent = folder;
            IFile file = parent.getFile(new Path("__init__"+FileTypesPreferencesPage.getDefaultDottedPythonExtension()));
            if(!file.exists()){
                file.create(new ByteArrayInputStream(new byte[0]), true, monitor);
            }
            lastFile = file;
        }
        
        
        return lastFile;
    }


}
