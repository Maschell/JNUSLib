# JNUSLib - All in one library to handle NUS content

JNUSLib is a library written in Java to handle NUS Contents (.app, tmd, tik, cert files) from different sources. It can be easily used in other Applications and is directed to devolpers. It's based on JNUSTool, but is heavily rewritten and extented with even more features.

## Features
Loading NUS Contents from different sources. Currently implemented:
   * Local files
   * Files from the NUS Server (Only the part that are needed will be downloaded)
   * .woomy files (https://github.com/shinyquagsire23/makefst)
   * WUD Images. Also compressed (.wux https://gbatemp.net/threads/wii-u-image-wud-compression-tool.397901/) and splitted WUD's (parts dumped from wudumper)
   
   
    
For files loaded from one of the sources, the following features can be used.  
* Only on-the-fly actions. Files will be downloaded only on demand, splitted files can be handled natively, and .wux have to be decompressed!
* Extracting all files. Needed decryption will be done on the fly.
* Extracting single files. Needed decryption will be done on the fly.
* Extracting the content files (.app, .h3, tmd, etc.) Useful for all sources except "local".
* This means title downloading, and .woomy, .wux, .wud and splitted .wud extracting into the "installable" format.
* Providing a InputStream to the decrypted data of a file.
* Decrypt a full content (.app) file.
  
For WUD files, following additional operations are possible:  
* Compressing into .wux file format (from .wud and splitted .wud)
* Verifing (comparing) to different wud images.
  
## How to use it
At first you have to import the jnuslib.jar and the common key in the Settings class.
```
Settings.commonKey = Utils.StringToByteArray("12345678901234567890123456789012");
```
Then for each different source type, you need to use a an own NUSTitleLoader.

```
//REMOTE

        //Loading a title with a public ticket
        NUSTitle nusRemote = NUSTitleLoaderRemote.loadNUSTitle(0x0005000E12345678L);
        
        //Loading a title with an own ticket (key, titleid)
        Ticket ticket = Ticket.createTicket(Utils.StringToByteArray("12345678901234567890123456789012"), 0x0005000E12345678L);
        NUSTitle nusRemoteWithTicket = NUSTitleLoaderRemote.loadNUSTitle(0x0005000E12345678L);
//LOCAL

        //With ticket on disk
        NUSTitle nusLocal = NUSTitleLoaderLocal.loadNUSTitle("path-to-app-files");
        
         //Loading a title with an own ticket (key, titleid)
        Ticket ticket = Ticket.createTicket(Utils.StringToByteArray("12345678901234567890123456789012"), 0x0005000E12345678L);
        NUSTitle nusRemoteWithTicket = NUSTitleLoaderLocal.loadNUSTitle("path-to-app-files");

//Loading a .woomy file
        NUSTitle nusWoomy = NUSTitleLoaderWoomy.loadNUSTitle("testfile.woomy");
        
//WUD
        //Loading a uncompressed WUD
        NUSTitle nusWUD = NUSTitleLoaderWUD.loadNUSTitle("game.wud");
        //Loading a compressed WUD (WUX)
        NUSTitle nusWUX = NUSTitleLoaderWUD.loadNUSTitle("game.wux");
        //Loading a uncompressed splitted WUD (2gb parts)
        NUSTitle nusWUDSplitted = NUSTitleLoaderWUD.loadNUSTitle("game_part1.wud");
```

Once the title is loaded, you can use one of the services to extract and decrypt files.  
Here are some of the operations you can do. Look at the code for the full list of methods.

### Decryption:

```
DecryptionService decrypt = DecryptionService.getInstance(title);

//Decrypt the whole FST into a folder called "DecryptedTitle"
decrypt.decryptAllFSTEntriesTo("DecryptedTitle");

//Decrypt the code folder into a folder called "code_folder"
decrypt.decryptFSTEntriesTo("/code/.*", code_folder);

//Decrypt all .js files into a folder called "js_files"
decrypt.decryptFSTEntriesTo(".*.js", js_files);


//Decrypt all .app files into a folder called "decrypted_contents"
decrypt.decryptAllPlainContents("decrypted_contents");


//Use decrypting inputstream. Data will be only loaded/decrypted on demand.

//Display the app.xml as hex dump
FSTEntry appXMLEntry = title.getFSTEntryByFullPath("code/app.xml");
decrypt.getDecryptedOutputAsInputStream(appXMLEntry);
//Lets just print the app.xml as hex data
int BUFFER_SIZE = 0x40;
byte[] buffer = new byte[BUFFER_SIZE];
int i = 0;            
while(in.read(buffer) > 0){
    System.out.println(String.format("0x%04X: ", (i++ * BUFFER_SIZE)) + Utils.ByteArrayToString(buffer));
}
in.close();
[...]
```

### Extraction:
```
//Get the Service for the NUSTitle
ExtractionService extract = ExtractionService.getInstance(title);

//Saving all .app/.h3/tmd/tik/cert files into the folder "encryptedFiles"
extract.extractAll("encryptedFiles");

//Save all .h3 files into "contentHashes"
extract.extractAllEncrpytedContentFileHashes("contentHashes");

//Save all -.app files into "contents"
extract.extractAllEncryptedContentFilesWithoutHashesTo("contents");

//Save tmd, cert and ticket
extract.extractTMDTo(output);
extract.extractTickeTo(output);
extract.extractCertTo(output);

[...]
```
### WUD Services
Example for compressing and verifing into .wux files.

```
    WUDImage imageUncompressed = new WUDImage(new File("game_part1.wud")); //Splitted and not splitted .wud possible here
    
    WUDService.compressWUDToWUX(imageUncompressed, "compressedImage","game.wux");
    
    WUDImage imageCompressed = new WUDImage(new File("compressedImage" + File.separator + "game.wux"));
    
    //Verify compression
    WUDService.compareWUDImage(imageUncompressed, imageCompressed);
```

### Cleanup:
Call the method cleanup() for a NUSTitle to cleanup/close all opened ressources.

# Credits
Maschell

Thanks to:
Crediar for CDecrypt (https://github.com/crediar/cdecrypt)  
All people who have contributed to vgmtoolbox (https://sourceforge.net/projects/vgmtoolbox/)  
Exzap for the .wux file format (https://gbatemp.net/threads/wii-u-image-wud-compression-tool.397901/)  
FIX94 for wudump (https://gbatemp.net/threads/wudump-dump-raw-images-from-a-wiiu-game-disc.451736/)  
The creators of lombok for lombok https://projectlombok.org/index.html  
