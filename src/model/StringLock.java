package model;

/**
 * String Lock
 * A simple lock object consisting of an object (string) and an owner
 */
public class StringLock {
    String object;
    String owner;
    public StringLock(String object, String owner){
        this.object = object;
        this.owner = owner;
    }
    public String getObject(){
        return object;
    }
    public String getOwner(){
        return owner;
    }
}
