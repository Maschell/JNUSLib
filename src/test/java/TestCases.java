import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Map.Entry;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import de.mas.wiiu.jnus.NUSTitle;
import de.mas.wiiu.jnus.NUSTitleLoaderLocal;
import de.mas.wiiu.jnus.entities.TMD.Content;
import de.mas.wiiu.jnus.interfaces.NUSDataProcessor;
import de.mas.wiiu.jnus.utils.HashUtil;
import de.mas.wiiu.jnus.utils.Utils;

public class TestCases {
    @Rule public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void nonHashedFileRead() throws IOException {
        String resourceName = "out";
        String resourceName2 = "tmp";

        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource(resourceName).getFile());
        File file2 = new File(classLoader.getResource(resourceName2).getFile());
        String absolutePath = file.getAbsolutePath();
        NUSTitle title = null;
        try {
            title = NUSTitleLoaderLocal.loadNUSTitle(absolutePath, Utils.StringToByteArray("00000000000000000000000000000000"));
        } catch (Exception e) {
            e.printStackTrace();
            assertTrue(false);
        }
        NUSDataProcessor dp = title.getDataProcessor();
        Content c = title.getTMD().getContentByID(2);

        File dec = new File(file2.getAbsolutePath() + File.separator + String.format("%08X.dec", c.getID()));

        RandomAccessFile fr = new RandomAccessFile(dec, "r");

        int blockSize = 0x1000000;

        try {
            int curOffset = 0;
            long totalSize = 0;
            while (true) {
                byte[] contentDataLib = dp.readPlainDecryptedContent(c, curOffset, blockSize, false);
                
                if (contentDataLib.length == 0) {
                    break;
                }
                byte[] hashLib = HashUtil.hashSHA1(contentDataLib);

                fr.seek(curOffset);
                byte[] buffer = new byte[contentDataLib.length];
                fr.read(buffer);

                byte[] hashFile = HashUtil.hashSHA1(buffer);
                // System.out.println(Utils.ByteArrayToString(hashFile) + " " + Utils.ByteArrayToString(hashLib));
                Assert.assertArrayEquals(hashFile, hashLib);
                curOffset += blockSize;
                totalSize += contentDataLib.length;
                //System.out.println(totalSize + " " + totalSize * 1.0f / dec.length());
            }
            assertEquals(dec.length(), totalSize);

        } catch (NoSuchAlgorithmException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            fr.close();
        }
    }

    @Test
    public void nonHashedFileReadStream() throws IOException {

        String resourceName = "out";
        String resourceName2 = "tmp";

        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource(resourceName).getFile());
        File file2 = new File(classLoader.getResource(resourceName2).getFile());
        String absolutePath = file.getAbsolutePath();
        NUSTitle title = null;
        try {
            title = NUSTitleLoaderLocal.loadNUSTitle(absolutePath, Utils.StringToByteArray("00000000000000000000000000000000"));
        } catch (Exception e) {
            e.printStackTrace();
            assertTrue(false);
        }
        NUSDataProcessor dp = title.getDataProcessor();
        Content c = title.getTMD().getContentByID(2);

        File dec = new File(file2.getAbsolutePath() + File.separator + String.format("%08X.dec", c.getID()));
        //System.out.println(dec.getAbsolutePath());
        InputStream fileIn = new BufferedInputStream(new FileInputStream(dec));

        int blockSize = 0x1000000;
        byte[] buffer = new byte[blockSize];
        try {
            int curOffset = 0;
            long totalSize = 0;

            InputStream libIn = dp.readPlainDecryptedContentAsStream(c, false);
            while (true) {

                int res1 = 0;
                while (res1 < blockSize) {
                    int res = libIn.read(buffer, res1, blockSize - res1);
                    if (res < 0) {
                        break;
                    }
                    res1 += res;
                }
                byte[] hashLib = HashUtil.hashSHA1(Arrays.copyOfRange(buffer, 0, res1));
                try (FileOutputStream fos = new FileOutputStream("test.1")) {
                    // fos.write(buffer);
                    // fos.close(); There is no more need for this line since you had created the instance of "fos" inside the try. And this will automatically
                    // close the OutputStream
                }
                int res2 = 0;
                while (res2 < blockSize) {
                    int res = fileIn.read(buffer, res2, blockSize - res2);
                    if (res < 0) {
                        break;
                    }
                    res2 += res;
                }

                try (FileOutputStream fos = new FileOutputStream("test.2")) {
                    // fos.write(buffer);
                    // fos.close(); There is no more need for this line since you had created the instance of "fos" inside the try. And this will automatically
                    // close the OutputStream
                }
                //System.out.println(res1);
                byte[] hashFile = HashUtil.hashSHA1(Arrays.copyOfRange(buffer, 0, res2));
                //System.out.println(res2 + " " + res1);
                assertEquals(res2, res1);
                if (res1 == 0) {
                    break;
                }
                //System.out.println(Utils.ByteArrayToString(hashFile) + " " + Utils.ByteArrayToString(hashLib));
                Assert.assertArrayEquals(hashFile, hashLib);
                curOffset += blockSize;
                totalSize += res1;
                //System.out.println(totalSize + " " + totalSize * 1.0f / dec.length());
            }
            assertEquals(dec.length(), totalSize);
            libIn.close();
        } catch (NoSuchAlgorithmException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            fileIn.close();

        }
    }

    @Test
    public void HashedFileRead() throws IOException {
        String resourceName = "out";
        String resourceName2 = "tmp";

        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource(resourceName).getFile());
        File file2 = new File(classLoader.getResource(resourceName2).getFile());
        String absolutePath = file.getAbsolutePath();
        NUSTitle title = null;
        try {
            title = NUSTitleLoaderLocal.loadNUSTitle(absolutePath, Utils.StringToByteArray("00000000000000000000000000000000"));
        } catch (Exception e) {
            e.printStackTrace();
            assertTrue(false);
        }
        NUSDataProcessor dp = title.getDataProcessor();
        Content c = title.getTMD().getContentByID(3);

        File dec = new File(file2.getAbsolutePath() + File.separator + String.format("%08X.dec", c.getID()));
        RandomAccessFile fr = new RandomAccessFile(dec, "r");

        int blockSize = 0x10337;

        try {
            long curOffset = 0;
            long totalSize = 0;
            while (true) {
                byte[] contentDataLib = dp.readPlainDecryptedContent(c, curOffset, blockSize, false);

                try (FileOutputStream fos = new FileOutputStream("test.1")) {
                    fos.write(contentDataLib);
                }

                if (contentDataLib.length == 0) {
                    break;
                }

                byte[] hashLib = HashUtil.hashSHA1(contentDataLib);

                fr.seek(curOffset);
                byte[] buffer = new byte[blockSize];

                int res = fr.read(buffer);
                if (res < 0) {
                    assertTrue(false);
                }
                buffer = Arrays.copyOf(buffer, res);
                try (FileOutputStream fos = new FileOutputStream("test.2")) {
                    fos.write(buffer);
                }

                Assert.assertEquals(buffer.length, contentDataLib.length);

                byte[] hashFile = HashUtil.hashSHA1(buffer);
                // System.out.println(Utils.ByteArrayToString(hashFile) + " " + Utils.ByteArrayToString(hashLib));
                Assert.assertArrayEquals(hashFile, hashLib);
                curOffset += blockSize;
                totalSize += contentDataLib.length;

                //System.out.println(totalSize + " " + (int) ((totalSize * 1.0f / dec.length()) * 100) + "%");
            }
            assertEquals(dec.length(), totalSize);

        } catch (NoSuchAlgorithmException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            fr.close();
        }
    }

    @Test
    public void HashedFileReadDec() throws IOException {
        String resourceName = "out";
        String resourceName1 = "out_dec";

        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource(resourceName).getFile());
        File file1 = new File(classLoader.getResource(resourceName1).getFile());
        NUSTitle title = null;
        try {
            title = NUSTitleLoaderLocal.loadNUSTitle(file.getAbsolutePath(), Utils.StringToByteArray("00000000000000000000000000000000"));
        } catch (Exception e) {
            e.printStackTrace();
            assertTrue(false);
        }

        NUSDataProcessor dp = title.getDataProcessor();
        for(Entry<Integer, Content> e : title.getTMD().getAllContents().entrySet()) {
            Content c = e.getValue();
            File dec = new File(file1.getAbsolutePath() + File.separator + String.format("%08X.app", c.getID()));

            RandomAccessFile fr = new RandomAccessFile(dec, "r");

            int blockSize = 0x10337;

            try {
                long curOffset = 0;
                long totalSize = 0;
                while (true) {
                    byte[] contentDataLib = dp.readDecryptedContent(c, curOffset, blockSize);
                    if (contentDataLib.length == 0) {
                        break;
                    }

                    try (FileOutputStream fos = new FileOutputStream("test.1")) {
                        fos.write(contentDataLib);
                    }

                    byte[] hashLib = HashUtil.hashSHA1(contentDataLib);

                    fr.seek(curOffset);
                    byte[] buffer = new byte[blockSize];

                    int res = fr.read(buffer);
                    if (res < 0) {
                        assertTrue(false);
                    }
                    buffer = Arrays.copyOf(buffer, res);
                    try (FileOutputStream fos = new FileOutputStream("test.2")) {
                        // fos.write(buffer);
                    }
                    Assert.assertEquals(res, contentDataLib.length);

                    Assert.assertEquals(buffer.length, contentDataLib.length);

                    byte[] hashFile = HashUtil.hashSHA1(buffer);
                    Assert.assertArrayEquals(Utils.ByteArrayToString(hashFile) + " != " + Utils.ByteArrayToString(hashLib), hashLib,hashFile);

                    curOffset += blockSize;
                    totalSize += contentDataLib.length;
                    //System.out.println(totalSize + " " + (int) ((totalSize * 1.0f / dec.length()) * 100) + "%");
                }
                assertEquals(dec.length(), totalSize);

            } catch (NoSuchAlgorithmException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            } finally {
                fr.close();
            }
        }
        
    }

    @Test
    public void HashedFileReadStream() throws IOException {

        String resourceName = "out";
        String resourceName2 = "tmp";

        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource(resourceName).getFile());
        File file2 = new File(classLoader.getResource(resourceName2).getFile());
        String absolutePath = file.getAbsolutePath();
        NUSTitle title = null;
        try {
            title = NUSTitleLoaderLocal.loadNUSTitle(absolutePath, Utils.StringToByteArray("00000000000000000000000000000000"));
        } catch (Exception e) {
            e.printStackTrace();
            assertTrue(false);
        }
        NUSDataProcessor dp = title.getDataProcessor();
        Content c = title.getTMD().getContentByID(3);

        File dec = new File(file2.getAbsolutePath() + File.separator + String.format("%08X.dec", c.getID()));
        InputStream fileIn = new BufferedInputStream(new FileInputStream(dec));

        int blockSize = 0x10337;
        byte[] buffer = new byte[blockSize];
        try {
            int curOffset = 0;
            long totalSize = 0;

            InputStream libIn = dp.readPlainDecryptedContentAsStream(c, false);
            while (true) {

                int res1 = 0;
                while (res1 < blockSize) {
                    int res = libIn.read(buffer, res1, blockSize - res1);
                    if (res < 0) {
                        break;
                    }
                    res1 += res;
                }
                byte[] hashLib = HashUtil.hashSHA1(Arrays.copyOfRange(buffer, 0, res1));
                try (FileOutputStream fos = new FileOutputStream("test.1")) {
                    fos.write(buffer);
                    // fos.close(); There is no more need for this line since you had created the instance of "fos" inside the try. And this will automatically
                    // close the OutputStream
                }
                int res2 = 0;
                while (res2 < blockSize) {
                    int res = fileIn.read(buffer, res2, blockSize - res2);
                    if (res < 0) {
                        break;
                    }
                    res2 += res;
                }

                try (FileOutputStream fos = new FileOutputStream("test.2")) {
                    fos.write(buffer);
                    // fos.close(); There is no more need for this line since you had created the instance of "fos" inside the try. And this will automatically
                    // close the OutputStream
                }
                //System.out.println(res1);
                byte[] hashFile = HashUtil.hashSHA1(Arrays.copyOfRange(buffer, 0, res2));
                assertEquals(res1, res2);
                if (res1 == 0) {
                    break;
                }
                //System.out.println(Utils.ByteArrayToString(hashFile) + " " + Utils.ByteArrayToString(hashLib));
                Assert.assertArrayEquals(hashFile, hashLib);
                curOffset += blockSize;
                totalSize += res1;
                //System.out.println(totalSize + " " + totalSize * 1.0f / dec.length());
            }
            assertEquals(dec.length(), totalSize);
            libIn.close();
        } catch (NoSuchAlgorithmException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            fileIn.close();

        }
    }
}
