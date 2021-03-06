package org.python.pydev.django.debug.ui.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.window.Window;
import org.python.pydev.editor.actions.PyAction;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.ui.dialogs.SelectExistingOrCreateNewDialog;
import org.python.pydev.ui.dialogs.TreeSelectionDialog;


/**
 * Command to execute a custom (not predefined) django action.
 */
public class DjangoCustomCommand extends DjangoAction {

	private static final String SHELL_MEMENTO_ID = "org.python.pydev.django.debug.ui.actions.DjangoCustomCommand.shell";
	private static final String DJANGO_CUSTOM_COMMANDS_PREFERENCE_KEY = "DJANGO_CUSTOM_COMMANDS";

	public void run(IAction action) {
    	try {
    		
    		String command = chooseCommand();
    		if(command != null){
    			launchDjangoCommand(command, true);
    		}
    	} catch (Exception e) {
    		throw new RuntimeException(e);
    	}
    }

	/**
	 * Opens a dialog so that the user can enter a command to be executed.
	 * 
	 * @return the command to be executed or null if no command was selected for execution.
	 */
	private String chooseCommand() {
        final IPreferenceStore preferenceStore = PydevPlugin.getDefault().getPreferenceStore();
        
		TreeSelectionDialog dialog = new SelectExistingOrCreateNewDialog(
				PyAction.getShell(), 
				preferenceStore, 
				DJANGO_CUSTOM_COMMANDS_PREFERENCE_KEY,
				SHELL_MEMENTO_ID);
		
		dialog.setTitle("Select the command to run or enter a new command");
		dialog.setMessage("Select the command to run or enter a new command");
		dialog.setInitialFilter("");
		
		int open = dialog.open();
		if(open != Window.OK){
		    return null;
		}
		Object[] result = dialog.getResult();
		if(result != null && result.length == 1){
			return result[0].toString();
		}
		return null;
	}
}


