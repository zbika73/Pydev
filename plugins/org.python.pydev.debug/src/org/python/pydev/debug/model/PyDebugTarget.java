/*
 * Author: atotic
 * Created on Mar 23, 2004
 * License: Common Public License v1.0
 */
package org.python.pydev.debug.model;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IBreakpointManager;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IProcess;
import org.python.pydev.debug.model.remote.RemoteDebugger;
import org.python.pydev.plugin.PydevPlugin;
/**
 * Debugger class that represents a single python process.
 * 
 * It deals with events from RemoteDebugger.
 * Breakpoint updating.
 */
public class PyDebugTarget extends AbstractDebugTarget {
    //private ILaunch launch;
    public volatile IProcess process;  
    public final IProject project;
    public volatile boolean finishedInit = false;

    public PyDebugTarget(ILaunch launch, IProcess process, IPath[] file, RemoteDebugger debugger, IProject project) {
        this.launch = launch;
        this.process = process;
        this.file = file;
        this.debugger = debugger;
        this.threads = new PyThread[0];
        this.project = project;
        launch.addDebugTarget(this);
        debugger.addTarget(this);
        IBreakpointManager breakpointManager= DebugPlugin.getDefault().getBreakpointManager();
        breakpointManager.addBreakpointListener(this);
        // we have to know when we get removed, so that we can shut off the debugger
        DebugPlugin.getDefault().getLaunchManager().addLaunchListener(this);
    }        

    public void launchRemoved(ILaunch launch) {
        // shut down the remote debugger when parent launch
        if (launch == this.launch) {
            IBreakpointManager breakpointManager= DebugPlugin.getDefault().getBreakpointManager();
            breakpointManager.removeBreakpointListener(this);
            debugger.dispose();
            debugger = null;
        }
    }    

    public IProcess getProcess() {
        return process;
    }

    public boolean canTerminate() {
        if(!finishedInit){
            //We must finish init to terminate
            return false;
        }
        
        // We can always terminate if it's still not terminated.
        return !this.isTerminated();
    }

    public boolean isTerminated() {
        if(!finishedInit){
            //We must finish init to terminate
            return false;
        }
        if(process == null){
            return true;
        }
        return process.isTerminated();
    }

    public void terminate() {
        if(process != null){
            try {
                process.terminate();
            } catch (DebugException e) {
                PydevPlugin.log(e);
            }
            process = null;
        }
        super.terminate();
    }        
    

}
