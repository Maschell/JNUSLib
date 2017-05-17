package de.mas.wiiu.jnus.implementations;

import java.io.IOException;
import java.io.InputStream;

import de.mas.wiiu.jnus.NUSTitle;
import de.mas.wiiu.jnus.Settings;
import de.mas.wiiu.jnus.entities.TMD;
import de.mas.wiiu.jnus.entities.content.Content;
import de.mas.wiiu.jnus.implementations.wud.parser.WUDGamePartition;
import de.mas.wiiu.jnus.implementations.wud.parser.WUDInfo;
import de.mas.wiiu.jnus.implementations.wud.parser.WUDPartitionHeader;
import de.mas.wiiu.jnus.implementations.wud.reader.WUDDiscReader;
import lombok.Getter;
import lombok.extern.java.Log;

@Log
public class NUSDataProviderWUD extends NUSDataProvider {
    @Getter private final WUDInfo WUDInfo;

    private final TMD tmd;

    public NUSDataProviderWUD(NUSTitle title, WUDInfo wudinfo) {
        super(title);
        this.WUDInfo = wudinfo;
        this.tmd = TMD.parseTMD(getRawTMD());
    }

    public long getOffsetInWUD(Content content) {
        if (content.getContentFSTInfo() == null) {
            return getAbsoluteReadOffset();
        } else {
            return getAbsoluteReadOffset() + content.getContentFSTInfo().getOffset();
        }
    }

    public long getAbsoluteReadOffset() {
        return (long) Settings.WIIU_DECRYPTED_AREA_OFFSET + getGamePartition().getPartitionOffset();
    }

    @Override
    public InputStream getInputStreamFromContent(Content content, long fileOffsetBlock) throws IOException {
        WUDDiscReader discReader = getDiscReader();
        long offset = getOffsetInWUD(content) + fileOffsetBlock;
        return discReader.readEncryptedToInputStream(offset, content.getEncryptedFileSize());
    }

    @Override
    public byte[] getContentH3Hash(Content content) throws IOException {

        if (getGamePartitionHeader() == null) {
            log.warning("GamePartitionHeader is null");
            return null;
        }

        if (!getGamePartitionHeader().isCalculatedHashes()) {
            log.info("Calculating h3 hashes");
            getGamePartitionHeader().calculateHashes(getTMD().getAllContents());

        }
        return getGamePartitionHeader().getH3Hash(content);
    }

    public TMD getTMD() {
        return tmd;
    }

    @Override
    public byte[] getRawTMD() {
        return getGamePartition().getRawTMD();
    }

    @Override
    public byte[] getRawTicket() {
        return getGamePartition().getRawTicket();
    }

    @Override
    public byte[] getRawCert() throws IOException {
        return getGamePartition().getRawCert();
    }

    public WUDGamePartition getGamePartition() {
        return getWUDInfo().getGamePartition();
    }

    public WUDPartitionHeader getGamePartitionHeader() {
        return getGamePartition().getPartitionHeader();
    }

    public WUDDiscReader getDiscReader() {
        return getWUDInfo().getWUDDiscReader();
    }

    @Override
    public void cleanup() {
        // We don't need it
    }

    @Override
    public String toString() {
        return "NUSDataProviderWUD [WUDInfo=" + WUDInfo + "]";
    }
}
