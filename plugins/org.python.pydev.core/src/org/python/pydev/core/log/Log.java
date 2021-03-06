/*
 * Created on 12/06/2005
 */
package org.python.pydev.core.log;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.HashMap;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IOConsoleOutputStream;
import org.eclipse.ui.console.MessageConsole;
import org.python.pydev.core.CorePlugin;
import org.python.pydev.core.FullRepIterable;
import org.python.pydev.core.MisconfigurationException;


/**
 * @author Fabio
 */
public class Log {

    /**
     * Console used to log contents
     */
    private static MessageConsole fConsole;
	private static IOConsoleOutputStream fOutputStream;

    /**
     * @param errorLevel IStatus.[OK|INFO|WARNING|ERROR]
     */
    public static void log(int errorLevel, String message, Throwable e) {
        System.err.println(message);
        if(e != null){
        	if(!(e instanceof MisconfigurationException)){
        		e.printStackTrace();
        	}
        }
        try {
            
            Status s = new Status(errorLevel, CorePlugin.getPluginID(), errorLevel, message, e);
            CorePlugin.getDefault().getLog().log(s);
        } catch (Exception e1) {
            //logging should not fail!
        }
    }

    public static void log(Throwable e) {
        log(IStatus.ERROR, e.getMessage() != null ? e.getMessage() : "No message gotten (null message).", e);
    }

    public static void log(String msg) {
        log(IStatus.ERROR, msg, new RuntimeException(msg));
    }

    
    //------------ Log that writes to a new console

    private final static Object lock = new Object(); 
    private final static StringBuffer logIndent = new StringBuffer();
    
    public static void toLogFile(Object obj, String string) {
        synchronized(lock){
            if(obj == null){
                obj = new Object();
            }
            Class<? extends Object> class1 = obj.getClass();
            toLogFile(string, class1);
        }
    }

    public static void toLogFile(String string, Class<? extends Object> class1) {
        StringBuffer buffer = new StringBuffer();
        buffer.append(logIndent);
        buffer.append(FullRepIterable.getLastPart(class1.getName()));
        buffer.append(": ");
        buffer.append(string);
        
        toLogFile(buffer.toString());
    }

    private static void toLogFile(final String buffer) {
        final Runnable r = new Runnable(){

            public void run() {
                synchronized(lock){
                    try{
                        CorePlugin default1 = CorePlugin.getDefault();
                        if(default1 == null){
                            //in tests
                            System.out.println(buffer);
                            return;
                        }
                        
                        //also print to console
                        System.out.println(buffer);
                        IOConsoleOutputStream c = getConsoleOutputStream();
                        c.write(buffer.toString());
                        c.write("\r\n");
                        
//                IPath stateLocation = default1.getStateLocation().append("PydevLog.log");
//                String file = stateLocation.toOSString();
//                REF.appendStrToFile(buffer+"\r\n", file);
                    }catch(Throwable e){
                        log(e); //default logging facility
                    }
                }
                
            }
        };
        
        Display current = Display.getCurrent();
        if(current != null && current.getThread() == Thread.currentThread ()){
            //ok, just run it
            r.run();
        }else{
            if(current == null){
                current = Display.getDefault();
                current.asyncExec(r);
            }
        }
    }
    
    
    private static IOConsoleOutputStream getConsoleOutputStream(){
        if (fConsole == null){
			fConsole = new MessageConsole("Pydev Logging", CorePlugin.getImageCache().getDescriptor("icons/python_logging.png"));
			
            fOutputStream = fConsole.newOutputStream();
            
			HashMap<IOConsoleOutputStream, String> themeConsoleStreamToColor = new HashMap<IOConsoleOutputStream, String>();
			themeConsoleStreamToColor.put(fOutputStream, "console.output");

            fConsole.setAttribute("themeConsoleStreamToColor", themeConsoleStreamToColor);

            ConsolePlugin.getDefault().getConsoleManager().addConsoles(new IConsole[]{fConsole});
        }
        return fOutputStream;
    }
    
    public static void toLogFile(Exception e) {
        String msg = getExceptionStr(e);
        toLogFile(msg);
    }

    public static String getExceptionStr(Exception e) {
        final ByteArrayOutputStream str = new ByteArrayOutputStream();
        final PrintStream prnt = new PrintStream(str);
        e.printStackTrace(prnt);
        prnt.flush();
        String msg = new String(str.toByteArray());
        return msg;
    }

    public static void addLogLevel() {
        synchronized(lock){
            logIndent.append("    ");
        }        
    }

    public static void remLogLevel() {
        synchronized(lock){
            if(logIndent.length() > 3){
                logIndent.delete(0,4);
            }
        }
    }


}
