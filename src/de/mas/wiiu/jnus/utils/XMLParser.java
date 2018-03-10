/****************************************************************************
 * Copyright (C) 2016-2018 Maschell
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
package de.mas.wiiu.jnus.utils;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import lombok.NonNull;
import lombok.extern.java.Log;

@Log
public class XMLParser {
    private Document document;

    public void loadDocument(InputStream inputStream) throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(inputStream);
        this.document = document;

    }

    public long getValueOfElementAsInt(String element, int index) {
        return Integer.parseInt(getValueOfElement(element, index));
    }

    public long getValueOfElementAsLong(String element, int index) {
        return Long.parseLong(getValueOfElement(element, index));
    }

    public String getValueOfElement(String element) {
        return getValueOfElement(element, 0);
    }

    public Node getNodeByValue(String element) {
        return getNodeByValue(element, 0);
    }

    public Node getNodeByValue(String element, int index) {
        if (document == null) {
            log.info("Please load the document first.");
        }
        NodeList list = document.getElementsByTagName(element);
        if (list == null) {
            return null;
        }
        return list.item(index);
    }

    public String getValueOfElementAttribute(String element, int index, String attribute) {
        Node node = getNodeByValue(element, index);
        if (node == null) {
            // log.info("Node is null");
            return "";
        }
        return getAttributeValueFromNode(node, attribute);
    }

    public static String getAttributeValueFromNode(@NonNull Node element, String attribute) {
        return element.getAttributes().getNamedItem(attribute).getTextContent().toString();
    }

    public String getValueOfElement(String element, int index) {
        Node node = getNodeByValue(element, index);
        if (node == null) {
            // log.info("Node is null");
            return "";
        }

        return node.getTextContent().toString();
    }
}
