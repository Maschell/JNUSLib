package de.mas.jnus.lib.entities.fst;

import java.util.ArrayList;
import java.util.List;

import de.mas.jnus.lib.entities.content.Content;
import lombok.Getter;
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
  
    @Getter private final String filename;
    @Getter private final String path;
    @Getter private final FSTEntry parent;
    
    @Getter private final List<FSTEntry> children = new ArrayList<>();
       
    @Getter private final short flags;
      
    @Getter private final long fileSize;	
    @Getter private final long fileOffset; 	
	
	@Getter private final Content content;

	@Getter private final boolean isDir;
	@Getter private final boolean isRoot;
	@Getter private final boolean isNotInPackage;
	
	@Getter private final short contentFSTID; 
    
	protected FSTEntry(FSTEntryParam fstParam){
	    this.filename = fstParam.getFilename();
	    this.path = fstParam.getPath();
	    this.flags = fstParam.getFlags();
	    this.parent = fstParam.getParent();
	    this.fileSize = fstParam.getFileSize();
	    this.fileOffset = fstParam.getFileOffset();
	    this.content = fstParam.getContent();
	    content.addEntry(this);
	    this.isDir = fstParam.isDir();
	    this.isRoot = fstParam.isRoot();
	    this.isNotInPackage = fstParam.isNotInPackage();
	    this.contentFSTID = fstParam.getContentFSTID();
	}
	
	/**
	 * Creates and returns a new FST Entry
	 * @return
	 */
	public static FSTEntry getRootFSTEntry(){
	    FSTEntryParam param = new FSTEntryParam();
	    param.setRoot(true);
	    param.setDir(true);
	    return new FSTEntry(param);
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
                + ", notInPackage=" + isNotInPackage + "]";
    }
}
