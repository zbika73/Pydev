package org.python.pydev.debug.newconsole.env;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;

import org.python.pydev.core.MyPipedInputStream;
import org.python.pydev.core.log.Log;
import org.python.pydev.jython.IPythonInterpreter;
import org.python.pydev.jython.JythonPlugin;

/**
 * Process used so that we can create an interactive console using the eclipse IDE itself.
 * 
 * Should be handy when experimenting with Eclipse, but can potentially halt the IDE depending on what's done.
 */
public class JythonEclipseProcess extends Process {

    private MyPipedInputStream outputStream;
    private MyPipedInputStream errorStream;
    private Object lock;
    private IPythonInterpreter interpreter;
    private Thread thread;

    public JythonEclipseProcess(final String script, final int port, final int clientPort) {
        super();
        try{

            outputStream = new MyPipedInputStream();
            errorStream = new MyPipedInputStream();

            lock = new Object();
            
            
            thread = new Thread(){
                public void run() {
                    File fileToExec = new File(script);
                    HashMap<String, Object> locals = new HashMap<String, Object>();
                    locals.put("__name__", "__main__");
                    
                    //It's important that the interpreter is created in the Thread and not outside the thread (otherwise
                    //it may be that the output ends up being shared, which is not what we want.)
                    interpreter = JythonPlugin.newPythonInterpreter(false, false);
                    interpreter.setErr(errorStream.internalOutputStream);
                    interpreter.setOut(outputStream.internalOutputStream);
                    Throwable e = JythonPlugin.exec(
                            locals, 
                            interpreter, 
                            fileToExec, 
                            new File[]{fileToExec.getParentFile()}, //pythonpath
                            "''", "'"+port+"'", "'"+clientPort+"'" //args
                            );
                    if(e != null){
                        e.printStackTrace();
                        Log.log(e);
                    }
                };
            };
            thread.start();
            
        }catch(Exception e){
            Log.log(e);
            throw new RuntimeException(e);
        }
    }

    public OutputStream getOutputStream() {
        
        return outputStream.internalOutputStream;
    }

    
    public InputStream getInputStream() {
        
        return outputStream;
    }

    
    public InputStream getErrorStream() {
        
        return errorStream;
    }

    

    @Override
    public int waitFor() throws InterruptedException{
        synchronized(lock){
            lock.wait();
        }

        return 0;
    }

    @Override
    public int exitValue(){
        throw new IllegalThreadStateException();
    }

    @Override
    public void destroy(){
        synchronized(lock){
            lock.notify();
        }

        try{
            if(outputStream != null){
                outputStream.close();
                outputStream = null;
            }
        }catch(Exception e){
            Log.log(e);
        }


        try{
            if(errorStream != null){
                errorStream.close();
                errorStream = null;
            }
        }catch(Exception e){
            Log.log(e);
        }
    }


}
