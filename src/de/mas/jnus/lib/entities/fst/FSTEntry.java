package de.mas.jnus.lib.entities.fst;

import java.util.ArrayList;
import java.util.List;

import de.mas.jnus.lib.entities.content.Content;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.java.Log;

@Log
/**
 * Represents one FST Entry
 * @author Maschell
 *
 */
public class FSTEntry{
    public static final byte FSTEntry_DIR = (byte)0x01; 
    public static final byte FSTEntry_notInNUS = (byte)0x80; 
  
    @Getter @Setter private String filename = "";
    @Getter @Setter private String path = "";
    @Getter @Setter private FSTEntry parent = null;
    
    private List<FSTEntry> children = null;
       
    @Getter @Setter private short flags;
      
    @Getter @Setter private long fileSize = 0;	
    @Getter @Setter private long fileOffset = 0; 	
	
	@Getter private Content content = null;

	@Getter @Setter private byte[] hash = new byte[0x14];

	@Getter @Setter private boolean isDir = false;
	@Getter @Setter private boolean isRoot = false;
	@Getter @Setter private boolean notInPackage = false;
	
	@Getter @Setter private short contentFSTID = 0; 
    
	public FSTEntry(){
	    
	}
	
	/**
	 * Creates and returns a new FST Entry
	 * @return
	 */
	public static FSTEntry getRootFSTEntry(){
	    FSTEntry entry = new FSTEntry();
	    entry.setRoot(true);
	    return entry;
	}
    
    public String getFullPath() {
        return getPath() + getFilename();
    }      
   
    public int getEntryCount() {
        int count = 1;
        for(FSTEntry entry : getChildren()){
            count += entry.getEntryCount();
        }
        return count;
    }

    public void addChildren(FSTEntry fstEntry) {        
        getChildren().add(fstEntry);
        fstEntry.setParent(this);
    }

    public List<FSTEntry> getChildren() {
    	if(children == null){
    		children = new ArrayList<>();
    	}
    	return children;
    }

    public List<FSTEntry> getDirChildren(){
        return getDirChildren(false);
    }

    public List<FSTEntry> getDirChildren(boolean all){
    	List<FSTEntry> result = new ArrayList<>();
    	for(FSTEntry child : getChildren()){
    		if(child.isDir() && (all || !child.isNotInPackage())){
    			result.add(child);
    		}
    	}
    	return result;
    }

    public List<FSTEntry> getFileChildren(){
        return getFileChildren(false);
    }

    public List<FSTEntry> getFileChildren(boolean all){
    	List<FSTEntry> result = new ArrayList<>();
    	for(FSTEntry child : getChildren()){
    		if((all && !child.isDir() || !child.isDir())){
    			result.add(child);
    		}
    	}
    	return result;
    }

    public List<FSTEntry> getFSTEntriesByContent(Content content) {
    	List<FSTEntry> entries = new ArrayList<>();
    	if(this.content == null){
    	    log.warning("Error in getFSTEntriesByContent, content null");
    	    System.exit(0);
    	}else{
    		if(this.content.equals(content)){
    			entries.add(this);
    		}
    	}
    	for(FSTEntry child : getChildren()){
    		entries.addAll(child.getFSTEntriesByContent(content));
    	}
    	return entries;
    }
	
	public void setContent(Content content) {
	    if(content == null){
	        log.warning("Can't set content for "+ getFilename() + ": Content it null");
	        System.out.println();
	        return;
	    }	  
        this.content = content;
        content.addEntry(this);
	}
    
    public long getFileOffsetBlock() {
        if(getContent().isHashed()){
            return (getFileOffset()/0xFC00) * 0x10000;
        }else{
            return getFileOffset();
        }
    }

    public void printRecursive(int space){
        for(int i = 0;i<space;i++){
            System.out.print(" ");
        }
        System.out.print(getFilename());
        if(isNotInPackage()){
            System.out.print(" (not in package)");
        }
        System.out.println();
        for(FSTEntry child : getDirChildren(true)){
            child.printRecursive(space + 5);
        }
        for(FSTEntry child : getFileChildren(true)){
            child.printRecursive(space + 5);
        }
    }

    @Override
    public String toString() {
        return "FSTEntry [filename=" + filename + ", path=" + path + ", flags=" + flags + ", filesize=" + fileSize
                + ", fileoffset=" + fileOffset + ", content=" + content + ", isDir=" + isDir + ", isRoot=" + isRoot
                + ", notInPackage=" + notInPackage + "]";
    }
}
