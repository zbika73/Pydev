package com.python.pydev.refactoring.ui;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.python.pydev.core.cache.PyPreferencesCache;

import com.python.pydev.refactoring.RefactoringPlugin;

public class MarkOccurrencesPreferencesPage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {
    
    public static final String USE_MARK_OCCURRENCES = "USE_MARK_OCCURRENCES";
    public static final boolean DEFAULT_USE_MARK_OCCURRENCES = true;
    
    public static final String USE_MARK_OCCURRENCES_IN_STRINGS = "USE_MARK_OCCURRENCES_IN_STRINGS";
    public static final boolean DEFAULT_USE_MARK_OCCURRENCES_IN_STRINGS = true;
    
    
    private static PyPreferencesCache cache;

    public MarkOccurrencesPreferencesPage() {
        super(FLAT);
        IPreferenceStore prefs = RefactoringPlugin.getDefault().getPreferenceStore();
        setPreferenceStore(prefs);
    }
    
    protected void createFieldEditors() {
        Composite p = getFieldEditorParent();

        addField(new BooleanFieldEditor(USE_MARK_OCCURRENCES, "Mark Occurrences?", p));
        addField(new BooleanFieldEditor(USE_MARK_OCCURRENCES_IN_STRINGS, "Mark Occurrences in strings and comments?", p));
    }
    
    public void init(IWorkbench workbench) {
    }
    
    public static boolean useMarkOccurrences() {
        if(cache == null){
            cache = new PyPreferencesCache(RefactoringPlugin.getDefault().getPreferenceStore());
        }
        return cache.getBoolean(USE_MARK_OCCURRENCES);
    }
    
    public static boolean useMarkOccurrencesInStrings() {
        if(cache == null){
            cache = new PyPreferencesCache(RefactoringPlugin.getDefault().getPreferenceStore());
        }
        return cache.getBoolean(USE_MARK_OCCURRENCES_IN_STRINGS);
    }
}

