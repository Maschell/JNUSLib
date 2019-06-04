# JNUSLib - All in one library to handle NUS content

JNUSLib is a library written in Java to handle NUS Contents (.app, tmd, tik, cert files) from different sources. It can be easily used in other Applications and is directed to devolpers. It's based on JNUSTool, but is heavily rewritten and extented with even more features.

## Features
Loading NUS Contents from different sources. Currently implemented:
   * Local files
   * Files from the NUS Server (Only the part that are needed will be downloaded)
   * .woomy files (https://github.com/shinyquagsire23/makefst)
   * WUD Images. Also compressed (.wux https://gbatemp.net/threads/wii-u-image-wud-compression-tool.397901/) and splitted WUD's (parts dumped from wudumper)
   * .wumad files
   
   
    
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
  
## Maven
```
<repository>
  <id>jitpack.io</id>
  <url>https://jitpack.io</url>
</repository>
...
<!-- The core module -->
<dependency>
  <groupId>de.Maschell.JNUSLib</groupId>
  <artifactId>JNUSLib</artifactId>
  <version>%version OR commit%</version>
</dependency>
```

## Usage



### Gettings NUSTitles
Gor each different source type, you need to use a an own NUSTitleLoader.

```
byte[] commonKey = Utils.StringToByteArray("12345678901234567890123456789012");
```
#### Remote
```
// Loading a title with a public ticket
NUSTitle nusRemote = NUSTitleLoaderRemote.loadNUSTitle(0x0005000E12345678L, commonKey);

// Loading a title with an own ticket (key, titleid)
Ticket ticket = Ticket.createTicket(Utils.StringToByteArray("12345678901234567890123456789012"), 0x0005000E12345678L, commonKey);
NUSTitle nusRemoteWithTicket = NUSTitleLoaderRemote.loadNUSTitle(0x0005000E12345678L, commonKey);
```

#### Local
```
// With ticket on disk
NUSTitle nusLocal = NUSTitleLoaderLocal.loadNUSTitle("path-to-app-files", commonKey);

// Loading a title with an own ticket (key, titleid)
Ticket ticket = Ticket.createTicket(Utils.StringToByteArray("12345678901234567890123456789012"), 0x0005000E12345678L, commonKey);
NUSTitle nusLocalWithTicket = NUSTitleLoaderLocal.loadNUSTitle("path-to-app-files", ticket);

// Loading a .woomy file
NUSTitle nusWoomy = NUSTitleLoaderWoomy.loadNUSTitle("testfile.woomy");
```
#### WUD/WUX
```
// WUD

// Loading a uncompressed WUD
WUDInfo wiWUD = WUDLoader.load("game.wud"); // needs a game.key next to the .wud
// Loading a compressed WUD (WUX)
WUDInfo wiWUX = WUDLoader.load("game.wux"); // needs a game.key next to the .wux
// Loading a uncompressed splitted WUD (2gb parts)
WUDInfo wiWUDSplitted = WUDLoader.load("game_part1.wud"); // needs a game.key next to the .wud

// Loading providing the disc key
WUDInfo wiWUXWithDisc = WUDLoader.load("game.wux", Utils.StringToByteArray("12345678901234567890123456789012")); // needs a game.key next to the .wux
// Loading a wud with no titley key (kiosk)
WUDInfo wiKiosk = WUDLoader.loadDev("game.wux");

// Get NUSTitles from WUDInfo
List<NUSTitle> titlesFromWUD = WUDLoader.getGamePartionsAsNUSTitles(wudInfo, commonKey);
```

#### wumad
```
/ Loading a wumad
WumadInfo wumadInfo = WumadLoader.load(new File("game.wud")); 


// Get NUSTitles from wumad
List<NUSTitle> titlesFromWumad = WumadLoader.getGamePartionsAsNUSTitles(wumadInfo, commonKey);

```

Once the title is loaded, you can use one of the services to extract and decrypt files.  

### Decryption:
For the decryption you can use a FSTDataProvider in combinations with a FSTEntry. Example:
```
// Get a FSTDataProvider from NUSTitle
FSTDataProvider fstdataprovider = new FSTDataProviderNUSTitle(nustitle);

// When loading from a WUD, you can get the data of all partitions via
List<FSTDataProvider> partitionsFromWUD = WUDLoader.getPartitonsAsFSTDataProvider(wudInfo, commonKey);
// the includes all non-nustitles like the SI or UP partitions.

// When loading from a Wumad, you can get the data of all partitions via
List<FSTDataProvider> partitionsFromWumad = WumadLoader.getPartitonsAsFSTDataProvider(wumadInfo, commonKey);

FSTEntry fstRoot = fstdataprovider.getRoot();

FSTEntry appxml = FSTUtils.getFSTEntriesByRegEx(fstdataprovider.getRoot(), ".*app.xml").get(0); // get all .rpx files

// Get data as byte array
byte[] appxmlData = fstdataprovider.readFile(appxml);
// Get 1024 bytes from entry appxml from offset 0
byte[] appxmlChunk = fstdataprovider.readFile(appxml, 0, 1024);

// Get data as input stream
InputStream appxmlStream = fstdataprovider.readFileAsStream(appxml);
// Get 1024 bytes from entry appxml from offset 0 as input stream
InputStream appxmlStream = fstdataprovider.readFileAsStream(appxml, 0, 1024);

// Save data to output stream
FileOutputStream appxmlOut = new FileOutputStream(new File(appxml.getFilename()));
if (fstdataprovider.readFileToStream(appxmlOut, appxml)) {
    System.out.println("Okay.");
}
```

Some wrapper functions can be found in the DecryptionService:
```
FSTDataProvider fstdataprovider = new FSTDataProviderNUSTitle(nustitle);
DecryptionService decrypt = DecryptionService.getInstance(fstdataprovider);

// Decrypt the whole FST into a folder called "DecryptedTitle" and skip existing
decrypt.decryptAllFSTEntriesTo("DecryptedTitle", true);

// Decrypt the code folder into a folder called "code_folder" and skip existing
decrypt.decryptFSTEntriesTo("/code/.*", "code_folder", true);
```

### Extraction:
```
//Get the Service for the NUSTitle
ExtractionService extract = ExtractionService.getInstance(nusTitle);

//Saving all .app/.h3/tmd/tik/cert files into the folder "encryptedFiles"
extract.extractAll("encryptedFiles");

//Save all .h3 files into "contentHashes"
extract.extractAllEncrpytedContentFileHashes("contentHashes");

//Save all -.app files into "contents"
extract.extractAllEncryptedContentFilesWithoutHashesTo("contents");

//Save tmd, cert and ticket
extract.extractTMDTo("output");
extract.extractTicketTo("output");
extract.extractCertTo("output");
```
### WUD Services
Example for compressing and verifing .wux files.

```
WUDImage imageUncompressed = new WUDImage(new File("game_part1.wud")); // Splitted and not splitted .wud possible here

Optional<File> compressedWUD = WUDService.compressWUDToWUX(imageUncompressed, "compressedImage", "game.wux", false);

if (compressedWUD.isPresent()) {
    WUDImage imageCompressed = new WUDImage(compressedWUD.get());

    // Verify compression
    if (WUDService.compareWUDImage(imageUncompressed, imageCompressed)) {
        System.out.println("Both images are the same");
    } else {
        System.err.println("The images are different");
    }

    //Turn it back into .wud
    WUDService.decompressWUX(imageCompressed, "newdecompressed", "test.wud", false);
} else {
    System.err.println("Failed to compress wud");
}
```

### Cleanup:
Call the method cleanup() for a NUSTitle to cleanup/close all opened ressources.

# Credits
Maschell for creating the lib
Crediar for [CDecrypt](https://github.com/crediar/cdecrypt)  
All people who have contributed to [vgmtoolbox](https://sourceforge.net/projects/vgmtoolbox/)  
Exzap for the [.wux file format](https://gbatemp.net/threads/wii-u-image-wud-compression-tool.397901/)  
FIX94 for [wudump](https://gbatemp.net/threads/wudump-dump-raw-images-from-a-wiiu-game-disc.451736/)  
The creators of lombok for [lombok](https://projectlombok.org/index.html)  
