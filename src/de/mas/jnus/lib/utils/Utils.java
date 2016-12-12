package de.mas.jnus.lib.utils;

import java.io.File;

public class Utils {
    
    public static long align(long numToRound, int multiple){
        if((multiple>0) && ((multiple & (multiple -1)) == 0)){
            return alignPower2(numToRound, multiple);
        }else{
            return alignGeneriv(numToRound,multiple);
        }
    }
    
    private static long alignGeneriv(long numToRound,int multiple){
        int isPositive = 0;
        if(numToRound >=0){
            isPositive = 1;
        }            
        return ((numToRound + isPositive * (multiple - 1)) / multiple) * multiple;
    }
    
    private static long alignPower2(long numToRound, int multiple) 
    {
        if(!((multiple>0) && ((multiple & (multiple -1)) == 0))) return 0L;
        return (numToRound + (multiple - 1)) & ~(multiple - 1);
    }
    
    public static String ByteArrayToString(byte[] ba)
    {
      if(ba == null) return null;
      StringBuilder hex = new StringBuilder(ba.length * 2);
      for(byte b : ba){
        hex.append(String.format("%02X", b));
      }
      return hex.toString();
    }

    public static byte[] StringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                                 + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }
    
    public static boolean createDir(String path){
        if(path == null || path.isEmpty()){
            return false;
        }
        File pathFile =  new File(path);
        if(!pathFile.exists()){
            boolean succes = new File(path).mkdirs();        
            if(!succes){
                System.out.println("Creating directory \"" +path + "\" failed.");
                return false;
            }
        }else if(!pathFile.isDirectory()){
            System.out.println("\"" +path + "\" already exists but is no directoy");
            return false;
        }
        return true;
    }
}
