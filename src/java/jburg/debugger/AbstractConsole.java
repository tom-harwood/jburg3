package jburg.debugger;

public abstract class AbstractConsole
{
    public abstract void println(String s);
    public abstract void exception(String operation, Exception ex);
    public abstract void status(String format, Object... args);
}
