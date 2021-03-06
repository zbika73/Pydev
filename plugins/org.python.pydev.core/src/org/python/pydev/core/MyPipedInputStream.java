package org.python.pydev.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Special input stream we can write to and read() listeners will get it later.
 * 
 * This class should be thread safe.
 */
public class MyPipedInputStream extends InputStream{

    private boolean closed = false;
    private MyByteArrayOutputStream buf;
    private Object readLock = new Object();
    private Object writeLock = new Object();
    
    public final OutputStream internalOutputStream = new OutputStream() {

        public void write(int b) throws IOException {
            MyPipedInputStream.this.write(b);
        };
        
        public void write(byte[] b) throws IOException {
            MyPipedInputStream.this.write(b);
        }
        
        public void close() throws IOException {
            MyPipedInputStream.this.close();
        };
    };

    public MyPipedInputStream() {
        buf = new MyByteArrayOutputStream();
    }

    @Override
    public int read() throws IOException{
        while(!closed){
            synchronized(writeLock){
                if(buf.size() > 0){
                    return buf.deleteFirst();
                }
            }
            try{
                synchronized(readLock){
                    readLock.notifyAll();
                    readLock.wait(10000);
                }
            }catch(InterruptedException e){
            }
        }
        return -1;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException{
        if(b == null){
            throw new NullPointerException();
        }else if((off < 0) || (off > b.length) || (len < 0) || ((off + len) > b.length) || ((off + len) < 0)){
            throw new IndexOutOfBoundsException();
        }else if(len == 0){
            return 0;
        }
        while(!closed){
            synchronized(writeLock){
                if(buf.size() > 0){
                    return buf.delete(b, off, len);
                }
            }
            try{
                synchronized(readLock){
                    readLock.notifyAll(); //let writers write.
                    readLock.wait(10000);
                }
            }catch(InterruptedException e){
            }
        }

        return -1;
    }

    public void write(int b) throws IOException{
        synchronized(writeLock){
            buf.write(b);
        }
        synchronized(readLock){
            readLock.notifyAll();
        }
    }
    
    public void write(byte[] bytes) throws IOException{
        synchronized(writeLock){
            buf.write(bytes);
        }
        synchronized(readLock){
            readLock.notifyAll();
        }
    }

    @Override
    public void close() throws IOException{
        closed = true;
        synchronized(readLock){
            readLock.notifyAll();
        }
    }

}
