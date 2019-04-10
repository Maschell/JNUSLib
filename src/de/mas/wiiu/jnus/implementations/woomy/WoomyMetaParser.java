/****************************************************************************
 * Copyright (C) 2016-2019 Maschell
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ****************************************************************************/
package de.mas.wiiu.jnus.implementations.woomy;

import java.io.InputStream;
import java.text.ParseException;
import java.util.Optional;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import de.mas.wiiu.jnus.utils.XMLParser;
import lombok.extern.java.Log;

@Log
public final class WoomyMetaParser extends XMLParser {
    private static final String WOOMY_METADATA_NAME = "name";
    private static final String WOOMY_METADATA_ICON = "icon";

    private static final String WOOMY_METADATA_ENTRIES = "entries";
    private static final String WOOMY_METADATA_ENTRY_NAME = "name";
    private static final String WOOMY_METADATA_ENTRY_FOLDER = "folder";
    private static final String WOOMY_METADATA_ENTRY_ENTRIES = "entries";

    /**
     * Overwrite the default constructor to force the user to use the factory.
     */
    private WoomyMetaParser() {
    }

    public static WoomyMeta parseMeta(InputStream data) throws ParseException {
        XMLParser parser = new WoomyMetaParser();
        String resultName = "";
        int resultIcon = 0;
        try {
            parser.loadDocument(data);
        } catch (Exception e) {
            log.info("Error while loading the data into the WoomyMetaParser");
            throw new ParseException("Error while loading the data into the WoomyMetaParser", 0);
        }

        resultName = parser.getValueOfElement(WOOMY_METADATA_NAME).orElse("");

        resultIcon = parser.getValueOfElementAsInt(WOOMY_METADATA_ICON).orElse(0);

        WoomyMeta result = new WoomyMeta(resultName, resultIcon);

        Optional<Node> entries_node = parser.getNodeByValue(WOOMY_METADATA_ENTRIES);
        if (entries_node.isPresent()) {
            NodeList entry_list = entries_node.get().getChildNodes();
            for (int i = 0; i < entry_list.getLength(); i++) {
                Node node = entry_list.item(i);

                String folder = getAttributeValueFromNode(node, WOOMY_METADATA_ENTRY_FOLDER);
                String entry_name = getAttributeValueFromNode(node, WOOMY_METADATA_ENTRY_NAME);
                String entry_count = getAttributeValueFromNode(node, WOOMY_METADATA_ENTRY_ENTRIES);
                int entry_count_val = Integer.parseInt(entry_count);
                result.addEntry(entry_name, folder, entry_count_val);
            }
        }

        return result;
    }

}
