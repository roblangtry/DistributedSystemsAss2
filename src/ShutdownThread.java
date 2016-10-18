/**
 * Shutdown Thread
 * A thread to be used in the runtime hook if ^C is entered
 */
public class ShutdownThread extends Thread{
    private Main mainclass;
    public ShutdownThread(Main main){
        this.mainclass = main;
    }

    @Override
    public void run() {
        mainclass.shutdown();
    }
}
