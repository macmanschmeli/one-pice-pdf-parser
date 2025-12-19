public class CommandLineProcessBar {
    private int chapter;
    private String processbar="";
    private String errors="";
    private String praeamble="";
    private boolean isActive=false;
    private boolean isDone= false;
    public synchronized boolean IsActive(){return isActive;}
    public synchronized boolean IsDone(){return isDone;}
    public synchronized void setActive(){isActive=true;}
    public synchronized void setDone(){isDone=true;}

    public synchronized void setProcessbar(String processbar) {
        this.processbar = processbar;
    }
    public synchronized void setErrors(String errors){
        this.errors=errors;
    }
    public synchronized void setPraeamble(int chapter){
        this.praeamble="Downloading chapter " +chapter;
        this.chapter=chapter;
    }
    public synchronized int getChapter(){return chapter;}
    public synchronized String getOutput(){
        return praeamble+" | "+ processbar+" | "+errors;
    }
}
