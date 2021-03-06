/*
 * Author: atotic
 * Created on May 7, 2004
 * License: Common Public License v1.0
 */
package org.python.pydev.debug.model.remote;

import org.python.pydev.debug.model.AbstractDebugTarget;

/**
 * Run command
 */
public class RunCommand extends AbstractDebuggerCommand {

    public RunCommand(AbstractDebugTarget debugger) {
        super(debugger);
    }

    public String getOutgoing() {
        return makeCommand(CMD_RUN, sequence, "");
    }
}
