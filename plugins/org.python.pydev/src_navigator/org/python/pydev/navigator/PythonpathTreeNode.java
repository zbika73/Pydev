package org.python.pydev.navigator;

import java.io.File;
import java.util.List;
import java.util.zip.ZipFile;

import org.eclipse.swt.graphics.Image;
import org.python.pydev.core.bundle.ImageCache;
import org.python.pydev.core.structure.TreeNode;
import org.python.pydev.editor.codecompletion.revisited.PythonPathHelper;
import org.python.pydev.navigator.elements.ISortedElement;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.ui.UIConstants;
import org.python.pydev.ui.filetypes.FileTypesPreferencesPage;

/**
 * This class represents nodes in the tree that are below the interpreter pythonpath information
 * (i.e.: modules in the pythonpath for the system interpreter)
 * 
 * It sets packages with a package icon and python files with a python icon (other files/folders
 * have default icons)
 */
public class PythonpathTreeNode extends TreeNode<LabelAndImage> implements ISortedElement{

	/**
	 * The file/folder we're wrapping here.
	 */
	public final File file;
	
	/**
	 * Identifies whether we already calculated the children
	 */
	private boolean calculated = false;
	
	/**
	 * Is this a file for a directory?
	 */
	private boolean isDir;
	
	/**
	 * Is it added as a package if a directory? (all parents must also be packages and it needs
	 * the __init__ file)
	 */
	private boolean isPackage;
	
	/**
	 * The files beneath this directory (if not a directory, it remains null)
	 */
	private File[] dirFiles;

	private ZipFile zipFile;

	
	
	public PythonpathTreeNode(TreeNode<LabelAndImage> parent, File file) {
		this(parent, file, null, false);
	}
	
	public PythonpathTreeNode(TreeNode<LabelAndImage> parent, File file, Image icon, boolean isPythonpathRoot) {
		super(parent, new LabelAndImage(getLabel(file, isPythonpathRoot), icon));
		this.file = file;
		this.isDir = file.isDirectory();
		if(isDir){
			dirFiles = file.listFiles();
			//This one can only be a package if its parent is a root or if it's also a package.
			if(isPythonpathRoot){
				isPackage = true;
				
			}else if(parent instanceof PythonpathTreeNode && ((PythonpathTreeNode)parent).isPackage){
				for (File file2 : dirFiles) {
					if(PythonPathHelper.isValidInitFile(file2.getName())){
						isPackage=true;
						break;
					}
				}
				
			}
		}else if(file.isFile() && FileTypesPreferencesPage.isValidZipFile(file.getName())){
			try {
				this.zipFile = new ZipFile(file);
			} catch (Exception e) {
				//Just ignore
			}
		}
		
		//Update the icon if it wasn't received.
		if(icon == null){
			ImageCache imageCache = PydevPlugin.getImageCache();
			if(isDir){
				if(isPackage){
					this.getData().o2 = imageCache.get(UIConstants.FOLDER_PACKAGE_ICON);
				}else{
					this.getData().o2 = imageCache.get(UIConstants.FOLDER_ICON);
				}
			}else{
				if(PythonPathHelper.isValidSourceFile(file.getName())){
					this.getData().o2 = imageCache.get(UIConstants.PY_FILE_ICON);
				}else{
					this.getData().o2 = imageCache.get(UIConstants.FILE_ICON);
				}
			}
		}
	}
	
	private static String getLabel(File file, boolean isPythonpathRoot) {
		if(isPythonpathRoot){
			return file.getAbsolutePath();
		}else{
			return file.getName();
		}
	}

	public boolean hasChildren() {
		return (isDir && dirFiles != null && dirFiles.length > 0) || (!isDir && zipFile != null);
	}

	public int getRank() {
		return isDir?ISortedElement.RANK_PYTHON_FOLDER:ISortedElement.RANK_PYTHON_FILE;
	}
	
	
	public synchronized List<TreeNode<LabelAndImage>> getChildren() {
		if(!calculated){
			this.calculated = true;
			if(isDir && dirFiles != null){
				for (File file : dirFiles) {
					//just creating it will already add it to the children
					new PythonpathTreeNode(this, file);
				}
			}else if(!isDir && zipFile != null){
				ZipStructure zipStructure = new ZipStructure(file, zipFile);
				for(String content : zipStructure.contents("")){
					//just creating it will already add it to the children
					new PythonpathZipChildTreeNode(this, zipStructure, content, null, true);
				}
			}
		}
		return super.getChildren();
	}

}
