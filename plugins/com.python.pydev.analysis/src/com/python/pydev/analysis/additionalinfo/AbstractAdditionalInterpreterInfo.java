/*
 * Created on 07/09/2005
 */
package com.python.pydev.analysis.additionalinfo;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.python.pydev.core.FullRepIterable;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.REF;
import org.python.pydev.core.Tuple;
import org.python.pydev.core.Tuple3;
import org.python.pydev.core.docutils.StringUtils;
import org.python.pydev.core.log.Log;
import org.python.pydev.core.structure.FastStack;
import org.python.pydev.core.structure.FastStringBuffer;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceModule;
import org.python.pydev.logging.DebugSettings;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.visitors.NodeUtils;
import org.python.pydev.parser.visitors.scope.ASTEntry;
import org.python.pydev.parser.visitors.scope.DefinitionsASTIteratorVisitor;
import org.python.pydev.plugin.PydevPlugin;


/**
 * This class contains additional information on an interpreter, so that we are able to make code-completion in
 * a context-insensitive way (and make additionally auto-import).
 * 
 * The information that is needed for that is the following:
 * 
 * - Classes that are available in the global context
 * - Methods that are available in the global context
 * 
 * We must access this information very fast, so the underlying structure has to take that into consideration.
 * 
 * It should not 'eat' too much memory because it should be all in memory at all times
 * 
 * It should also be easy to query it. 
 *      Some query situations include: 
 *          - which classes have the method xxx and yyy?
 *          - which methods and classes start with xxx?
 *          - is there any class or method with the name xxx?
 *      
 * The information must be persisted for reuse (and persisting and restoring it should be fast).
 * 
 * We need to store information for any interpreter, be it python, jython...
 * 
 * For creating and keeping this information up-to-date, we have to know when:
 * - the interpreter used changes (the InterpreterInfo should be passed after the change)
 * - some file changes (pydev_builder)
 * 
 * @author Fabio
 */
public abstract class AbstractAdditionalInterpreterInfo {

    /**
     * this is the number of initials that is used for indexing
     */
    public static final int NUMBER_OF_INITIALS_TO_INDEX = 3;

    /**
     * Do you want to debug this class?
     */
    private static final boolean DEBUG_ADDITIONAL_INFO = false;

    /**
     * Defines that some operation should be done on top level tokens
     */
    public final static int TOP_LEVEL = 1;
    
    /**
     * Defines that some operation should be done on inner level tokens
     */
    public final static int INNER = 2;
    
    

    /**
     * indexes used so that we can access the information faster - it is ordered through a tree map, and should be
     * very fast to access given its initials.
     * 
     * It contains only top/level information for a module
     * 
     * This map is persisted.
     */
    protected TreeMap<String, List<IInfo>> topLevelInitialsToInfo = new TreeMap<String, List<IInfo>>();
    
    /**
     * indexes so that we can get 'inner information' from classes, such as methods or inner classes from a class 
     */
    protected TreeMap<String, List<IInfo>> innerInitialsToInfo = new TreeMap<String, List<IInfo>>();
    

    /**
     * Should be used before re-creating the info, so that we have enough memory. 
     */
    public void clearAllInfo() {
        synchronized (lock) {
            if(topLevelInitialsToInfo != null){
                topLevelInitialsToInfo.clear();
            }
            if(innerInitialsToInfo != null){
                innerInitialsToInfo.clear();
            }
        }
    }
        
    
    protected Object lock = new Object();


    /**
     * The filter interface
     */
    public interface Filter{
        boolean doCompare(String lowerCaseQual, IInfo info);
        boolean doCompare(String lowerCaseQual, String infoName);
    }
    
    /**
     * A filter that checks if tokens are equal
     */
    private Filter equalsFilter  = new Filter(){
        public boolean doCompare(String qualifier, IInfo info) {
            return doCompare(qualifier, info.getName());
        }
        public boolean doCompare(String qualifier, String infoName) {
            return infoName.equals(qualifier);
        }
    };

    /**
     * A filter that checks if the tokens starts with a qualifier
     */
    private Filter startingWithFilter = new Filter(){

        public boolean doCompare(String lowerCaseQual, IInfo info) {
            return doCompare(lowerCaseQual, info.getName());
        }
        public boolean doCompare(String qualifier, String infoName) {
            return infoName.toLowerCase().startsWith(qualifier);
        }
        
    };

    /**
     * 2 because we've removed some info (the hash is no longer saved)
     */
    private static final int version = 2;

    public AbstractAdditionalInterpreterInfo(){
    }
    
    /**
     * That's the function actually used to add some info
     * 
     * @param info information to be added
     */
    protected void add(IInfo info, int doOn) {
        synchronized (lock) {
            String name = info.getName();
            String initials = getInitials(name);
            TreeMap<String, List<IInfo>> initialsToInfo;
            
            if(doOn == TOP_LEVEL){
                if(info.getPath() != null && info.getPath().length() > 0){
                    throw new RuntimeException("Error: the info being added is added as an 'top level' info, but has path. Info:"+info);
                }
                initialsToInfo = topLevelInitialsToInfo;
                
            }else if (doOn == INNER){
                if(info.getPath() == null || info.getPath().length() == 0){
                    throw new RuntimeException("Error: the info being added is added as an 'inner' info, but does not have a path. Info: "+info);
                }
                initialsToInfo = innerInitialsToInfo;
                
            }else{
                throw new RuntimeException("List to add is invalid: "+doOn);
            }
            List<IInfo> listForInitials = getAndCreateListForInitials(initials, initialsToInfo);
            listForInitials.add(info);
        }
    }

    /**
     * @param name the name from where we want to get the initials
     * @return the initials for the name
     */
    protected String getInitials(String name) {
        if(name.length() < NUMBER_OF_INITIALS_TO_INDEX){
            return name;
        }
        return name.substring(0, NUMBER_OF_INITIALS_TO_INDEX).toLowerCase();
    }
    
    /**
     * @param initials the initials we are looking for
     * @param initialsToInfo this is the list we should use (top level or inner)
     * @return the list of tokens with the specified initials (must be exact match)
     */
    protected List<IInfo> getAndCreateListForInitials(String initials, TreeMap<String, List<IInfo>> initialsToInfo) {
        List<IInfo> lInfo = initialsToInfo.get(initials);
        if(lInfo == null){
            lInfo = new ArrayList<IInfo>();
            initialsToInfo.put(initials, lInfo);
        }
        return lInfo;
    }

    /**
     * adds a method to the definition
     * @param doOn 
     * @return 
     */
    protected IInfo addMethod(FunctionDef def, String moduleDeclared, int doOn, String path) {
        FuncInfo info2 = FuncInfo.fromFunctionDef(def, moduleDeclared, path);
        add(info2, doOn);
        return info2;
    }
    
    /**
     * Adds a class to the definition
     * @param doOn 
     * @return 
     */
    protected IInfo addClass(ClassDef def, String moduleDeclared, int doOn, String path) {
        ClassInfo info = ClassInfo.fromClassDef(def, moduleDeclared, path);
        add(info, doOn);
        return info;
    }
    
    
    /**
     * Adds an attribute to the definition (this is either a global, a class attribute or an instance (self) attribute
     * @return 
     */
    protected IInfo addAttribute(String def, String moduleDeclared, int doOn, String path) {
        AttrInfo info = AttrInfo.fromAssign(def, moduleDeclared, path);
        add(info, doOn);
        return info;
    }

    /**
     * Adds a class or a function to the definition
     * 
     * @param classOrFunc the class or function we want to add
     * @param moduleDeclared the module where it is declared
     * @param doOn 
     * @return 
     */
    protected IInfo addClassOrFunc(SimpleNode classOrFunc, String moduleDeclared, int doOn, String path) {
        if(classOrFunc instanceof ClassDef){
            return addClass((ClassDef) classOrFunc, moduleDeclared, doOn, path);
        }else{
            return addMethod((FunctionDef) classOrFunc, moduleDeclared, doOn, path);
        }
    }

    private IInfo addAssignTargets(ASTEntry entry, String moduleName, int doOn, String path, boolean lastIsMethod ) {
        String rep = NodeUtils.getFullRepresentationString(entry.node);
        if(lastIsMethod){
            List<String> parts = StringUtils.dotSplit(rep);
            if(parts.size() >= 2){
                //at least 2 parts are required
                if(parts.get(0).equals("self")){
                    rep = parts.get(1);
                    return addAttribute(rep, moduleName, doOn, path);
                }
            }
        }else{
            return addAttribute(FullRepIterable.getFirstPart(rep), moduleName, doOn, path);
        }
        return null;
    }

    /**
     * Adds information for a source module
     * @param m the module we want to add to the info
     */
    public void addSourceModuleInfo(SourceModule m, IPythonNature nature, boolean generateDelta) {
        addAstInfo(m.getAst(), m.getName(), nature, generateDelta);
    }

    
    public List<IInfo> addAstInfo(SimpleNode node, String moduleName, IPythonNature nature, boolean generateDelta) {
    	List <IInfo> createdInfos = new ArrayList<IInfo>();
    	if(node == null || moduleName == null){
    		return createdInfos;
    	}
    	try {
			Tuple<DefinitionsASTIteratorVisitor, Iterator<ASTEntry>> tup = getInnerEntriesForAST(node);
			if(DebugSettings.DEBUG_ANALYSIS_REQUESTS){
			    Log.toLogFile(this, "Adding ast info to: "+moduleName);
			}
			
			try {
			    Iterator<ASTEntry> entries = tup.o2;
			
			    FastStack<SimpleNode> tempStack = new FastStack<SimpleNode>();
			    
			    synchronized (this.lock) {
			        while (entries.hasNext()) {
			            ASTEntry entry = entries.next();
			            IInfo infoCreated = null;
			            
			            if(entry.parent == null){ //we only want those that are in the global scope
			                if(entry.node instanceof ClassDef || entry.node instanceof FunctionDef){
			                	infoCreated = this.addClassOrFunc(entry.node, moduleName, TOP_LEVEL, null);
			                }else{
			                    //it is an assign
			                	infoCreated = this.addAssignTargets(entry, moduleName, TOP_LEVEL, null, false);
			                }
			            }else{
			                if(entry.node instanceof ClassDef || entry.node instanceof FunctionDef){
			                    //ok, it has a parent, so, let's check to see if the path we got only has class definitions
			                    //as the parent (and get that path)
			                    Tuple<String,Boolean> pathToRoot = this.getPathToRoot(entry, false, false, tempStack);
			                    if(pathToRoot != null && pathToRoot.o1 != null && pathToRoot.o1.length() > 0){
			                        //if the root is not valid, it is not only classes in the path (could be a method inside
			                        //a method, or something similar).
			                    	infoCreated = this.addClassOrFunc(entry.node, moduleName, INNER, pathToRoot.o1);
			                    }
			                }else{
			                    //it is an assign
			                    Tuple<String,Boolean> pathToRoot = this.getPathToRoot(entry, true, false, tempStack);
			                    if(pathToRoot != null && pathToRoot.o1 != null && pathToRoot.o1.length() > 0){
			                    	infoCreated = this.addAssignTargets(entry, moduleName, INNER, pathToRoot.o1, pathToRoot.o2);
			                    }
			                }
			            }
			            
			            if(infoCreated != null){
			            	createdInfos.add(infoCreated);
			            }
			        }
			    }        
			} catch (Exception e) {
			    PydevPlugin.log(e);
			}
		} catch (Exception e) {
			Log.log(e);
		}
		return createdInfos;
    }
    
    /**
     * @return an iterator that'll get the outline entries for the given ast.
     */
    public static Tuple<DefinitionsASTIteratorVisitor, Iterator<ASTEntry>> getInnerEntriesForAST(SimpleNode node) throws Exception {
        DefinitionsASTIteratorVisitor visitor = new DefinitionsASTIteratorVisitor();
        node.accept(visitor);
        Iterator<ASTEntry> entries = visitor.getOutline();
        return new Tuple<DefinitionsASTIteratorVisitor, Iterator<ASTEntry>>(visitor, entries);
    }
    
    
    
    /**
     * @return a set with the module names that have tokens.
     */
    public Set<String> getAllModulesWithTokens() {
        HashSet<String> ret = new HashSet<String>();
        synchronized (lock) {
            Set<Entry<String, List<IInfo>>> entrySet = this.topLevelInitialsToInfo.entrySet();
            for (Entry<String, List<IInfo>> entry : entrySet) {
                List<IInfo> value = entry.getValue();
                for (IInfo info : value) {
                    ret.add(info.getDeclaringModuleName());
                }
            }
            
            entrySet = this.innerInitialsToInfo.entrySet();
            for (Entry<String, List<IInfo>> entry : entrySet) {
                List<IInfo> value = entry.getValue();
                for (IInfo info : value) {
                    ret.add(info.getDeclaringModuleName());
                }
            }
        }
        return ret;

    }



    /**
     * @param lastMayBeMethod if true, it gets the path and accepts a method (if it is the last in the stack)
     * if false, null is returned if a method is found. 
     * 
     * @param tempStack is a temporary stack object (which may be cleared)
     * 
     * @return a tuple, where the first element is the path where the entry is located (may return null).
     * and the second element is a boolen that indicates if the last was actually a method or not.
     */
    private Tuple<String, Boolean> getPathToRoot(ASTEntry entry, boolean lastMayBeMethod, boolean acceptAny, 
            FastStack<SimpleNode> tempStack) {
        if(entry.parent == null){
            return null;
        }
        //just to be sure that it's empty
        tempStack.clear();
        
        boolean lastIsMethod = false; 
        //if the last 'may be a method', in this case, we have to remember that it will actually be the first one 
        //to be analyzed.
        
        //let's get the stack
        while(entry.parent != null){
            if(entry.parent.node instanceof ClassDef){
                tempStack.push(entry.parent.node);
                
            }else if(entry.parent.node instanceof FunctionDef){
                if(!acceptAny){
                    if(lastIsMethod){
                        //already found a method
                        return null;
                    }
                    
                    if(!lastMayBeMethod){
                        return null;
                    }
                    
                    //ok, the last one may be a method... (in this search, it MUST be the first one...)
                    if(tempStack.size() != 0){
                        return null; 
                    }
                }
                
                //ok, there was a class, so, let's go and set it
                tempStack.push(entry.parent.node);
                lastIsMethod = true;
                
            }else{
                return null;
                
            }
            entry = entry.parent;
        }

        //now that we have the stack, let's make it into a path...
        FastStringBuffer buf = new FastStringBuffer();
        while(tempStack.size() > 0){
            if(buf.length() > 0){
                buf.append(".");
            }
            buf.append(NodeUtils.getRepresentationString(tempStack.pop()));
        }
        return new Tuple<String, Boolean>(buf.toString(), lastIsMethod);
    }

    /**
     * Removes all the info associated with a given module
     * @param moduleName the name of the module we want to remove info from
     */
    public void removeInfoFromModule(String moduleName, boolean generateDelta) {
        if(DebugSettings.DEBUG_ANALYSIS_REQUESTS){
            Log.toLogFile(this, "Removing ast info from: "+moduleName);
        }
        synchronized (lock) {
            removeInfoFromMap(moduleName, topLevelInitialsToInfo);
            removeInfoFromMap(moduleName, innerInitialsToInfo);
        }
        
    }

    /**
     * @param moduleName
     * @param initialsToInfo
     */
    private void removeInfoFromMap(String moduleName, TreeMap<String, List<IInfo>> initialsToInfo) {
        Iterator<List<IInfo>> itListOfInfo = initialsToInfo.values().iterator();
        while (itListOfInfo.hasNext()) {

            Iterator<IInfo> it = itListOfInfo.next().iterator();
            while (it.hasNext()) {

                IInfo info = it.next();
                if(info != null && info.getDeclaringModuleName() != null){
                    if(info.getDeclaringModuleName().equals(moduleName)){
                        it.remove();
                    }
                }
            }
        }
    }
    


    /**
     * This is the function for which we are most optimized!
     * 
     * @param qualifier the tokens returned have to start with the given qualifier
     * @return a list of info, all starting with the given qualifier
     */
    public List<IInfo> getTokensStartingWith(String qualifier, int getWhat) {
        synchronized (lock) {
            return getWithFilter(qualifier, getWhat, startingWithFilter, true);
        }
    }


    public List<IInfo> getTokensEqualTo(String qualifier, int getWhat) {
        synchronized (lock) {
            return getWithFilter(qualifier, getWhat, equalsFilter, false);
        }
    }
    
    protected List<IInfo> getWithFilter(String qualifier, int getWhat, Filter filter, boolean useLowerCaseQual) {
        synchronized (lock) {
            ArrayList<IInfo> toks = new ArrayList<IInfo>();
            
            if((getWhat & TOP_LEVEL) != 0){
                getWithFilter(qualifier, topLevelInitialsToInfo, toks, filter, useLowerCaseQual);
            }
            if((getWhat & INNER) != 0){
                getWithFilter(qualifier, innerInitialsToInfo, toks, filter, useLowerCaseQual);
            }
            return toks;
        }
    }
        
    

    /**
     * @param qualifier
     * @param initialsToInfo this is where we are going to get the info from (currently: inner or top level list)
     * @param toks (out) the tokens will be added to this list
     * @return
     */
    protected void getWithFilter(String qualifier, TreeMap<String, List<IInfo>> initialsToInfo, List<IInfo> toks, Filter filter, boolean useLowerCaseQual) {
        String initials = getInitials(qualifier);
        String qualToCompare = qualifier;
        if(useLowerCaseQual){
            qualToCompare = qualifier.toLowerCase();
        }
        
        //get until the end of the alphabet
        SortedMap<String, List<IInfo>> subMap = initialsToInfo.subMap(initials, initials+"z");
        
        for (List<IInfo> listForInitials : subMap.values()) {
            
            for (IInfo info : listForInitials) {
                if(filter.doCompare(qualToCompare, info)){
                    toks.add(info);
                }
            }
        }
    }
    

    /**
     * @return all the tokens that are in this info (top level or inner)
     */
    public Collection<IInfo> getAllTokens(){
        synchronized (lock) {
            Collection<List<IInfo>> lInfo = this.topLevelInitialsToInfo.values();
            
            ArrayList<IInfo> toks = new ArrayList<IInfo>();
            for (List<IInfo> list : lInfo) {
                for (IInfo info : list) {
                    toks.add(info);
                }
            }

            lInfo = this.innerInitialsToInfo.values();
            for (List<IInfo> list : lInfo) {
                for (IInfo info : list) {
                    toks.add(info);
                }
            }
            return toks;
        }
    }
    
    /**
     * this can be used to save the file
     */
    public void save() {
        File persistingLocation;
		try {
			persistingLocation = getPersistingLocation();
		} catch (MisconfigurationException e) {
			PydevPlugin.log("Error. Unable to get persisting location for additional interprer info. Configuration may be corrupted.", e);
			return;
		}
        if(DEBUG_ADDITIONAL_INFO){
            System.out.println("Saving to "+persistingLocation);
        }
        saveTo(persistingLocation);
    }

    /**
     * @return the location where we can persist this info.
     * @throws MisconfigurationException 
     */
    protected abstract File getPersistingLocation() throws MisconfigurationException;


    /**
     * save the information contained for the given manager
     */
    public static void saveAdditionalSystemInfo(IInterpreterManager manager, String interpreter) {
        AbstractAdditionalInterpreterInfo info;
		try {
			info = AdditionalSystemInterpreterInfo.getAdditionalSystemInfo(manager, interpreter);
			info.save();
		} catch (MisconfigurationException e) {
			PydevPlugin.log(e);
			return;
		}
    }

    /**
     * @return the path to the folder we want to keep things on
     * @throws MisconfigurationException 
     */
    protected abstract File getPersistingFolder() throws MisconfigurationException;
    

    private void saveTo(File pathToSave) {
        synchronized (lock) {
            if(DEBUG_ADDITIONAL_INFO){
                System.out.println("Saving info "+this.getClass().getName()+" to file (size = "+getAllTokens().size()+") "+pathToSave);
            }
            REF.writeToFile(getInfoToSave(), pathToSave);
        }
    }

    /**
     * @return the information to be saved (if overridden, restoreSavedInfo should be overridden too)
     */
    @SuppressWarnings("unchecked")
    protected Object getInfoToSave(){
        synchronized (lock) {
            return new Tuple3(
                this.topLevelInitialsToInfo, 
                this.innerInitialsToInfo, 
                AbstractAdditionalInterpreterInfo.version);
        }
    }
    
    /**
     * actually does the load
     * @return true if it was successfully loaded and false otherwise
     */
    protected boolean load() {
        synchronized (lock) {
            File file;
			try {
				file = getPersistingLocation();
			} catch (MisconfigurationException e) {
				PydevPlugin.log("Unable to restore previous info... (persisting location not available).",e);
				return false;
			}
            if(file.exists() && file.isFile()){
                try {
                    restoreSavedInfo(IOUtils.readFromFile(file));
                    setAsDefaultInfo();
                    return true;
                } catch (Throwable e) {
                    PydevPlugin.log("Unable to restore previous info... new info should be restored in a thread.",e);
                }
            }
        }
        return false;
    }

    /**
     * Restores the saved info in the object (if overridden, getInfoToSave should be overridden too)
     * @param o the read object from the file
     * @throws MisconfigurationException 
     */
    @SuppressWarnings("unchecked")
    protected void restoreSavedInfo(Object o) throws MisconfigurationException{
        synchronized (lock) {
            Tuple3 readFromFile = (Tuple3) o;
            this.topLevelInitialsToInfo = (TreeMap<String, List<IInfo>>) readFromFile.o1;
            this.innerInitialsToInfo = (TreeMap<String, List<IInfo>>) readFromFile.o2;
            if(AbstractAdditionalInterpreterInfo.version != (Integer)readFromFile.o3){
                throw new RuntimeException("I/O version doesn't match. Rebuilding internal info.");
            }
        }
    }

    /**
     * this method should be overridden so that the info sets itself as the default info given the info it holds
     * (e.g. default for a project, default for python interpreter, etc.)
     */
    protected abstract void setAsDefaultInfo();


    @Override
    public String toString() {
        synchronized (lock) {
            FastStringBuffer buffer = new FastStringBuffer();
            buffer.append("AdditionalInfo{");
    
            buffer.append("topLevel=[");
            entrySetToString(buffer, this.topLevelInitialsToInfo.entrySet());
            buffer.append("]\n");
            buffer.append("inner=[");
            entrySetToString(buffer, this.innerInitialsToInfo.entrySet());
            buffer.append("]");
    
            buffer.append("}");
            return buffer.toString();
        }
    }

    /**
     * @param buffer
     * @param name
     */
    private void entrySetToString(FastStringBuffer buffer, Set<Entry<String, List<IInfo>>> name) {
        synchronized (lock) {
            for (Entry<String, List<IInfo>> entry : name) {
                List<IInfo> value = entry.getValue();
                for (IInfo info : value) {
                    buffer.append(info.toString());
                    buffer.append("\n");
                }
            }
        }
    }




}

class IOUtils {

    /**
     * @param astOutputFile
     * @return
     */
    public static Object readFromFile(File astOutputFile) {
        try {
            InputStream input = new FileInputStream(astOutputFile);
            ObjectInputStream in = new ObjectInputStream(input);
            Object o = in.readObject();
            in.close();
            input.close();
            return o;
        } catch (Exception e) {
            Log.log(e);
            throw new RuntimeException(e);
        }
    }

}