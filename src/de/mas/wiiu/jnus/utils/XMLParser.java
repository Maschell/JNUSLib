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
import java.util.Optional;

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

    public Optional<Integer> getValueOfElementAsInt(String element) {
        return getValueOfElementAsInt(element, 0);
    }

    public Optional<Integer> getValueOfElementAsInt(String element, int index) {
        return getValueOfElement(element, index).map(intStr -> Integer.parseInt(intStr));
    }

    public Optional<Long> getValueOfElementAsLong(String element, int index) {
        return getValueOfElement(element, index).map(longStr -> Long.parseLong(longStr));
    }

    public Optional<String> getValueOfElement(String element) {
        return getValueOfElement(element, 0);
    }

    public Optional<Node> getNodeByValue(String element) {
        return getNodeByValue(element, 0);
    }

    public Optional<Node> getNodeByValue(String element, int index) {
        if (document == null) {
            log.info("Please load the document first.");
        }
        NodeList list = document.getElementsByTagName(element);
        if (list == null) {
            return Optional.empty();
        }
        Node res = list.item(index);
        if (res == null) {
            return Optional.empty();
        }
        return Optional.of(res);
    }

    public Optional<String> getValueOfElementAttribute(String element, int index, String attribute) {
        return getNodeByValue(element, index).map(node -> getAttributeValueFromNode(node, attribute));
    }

    public static String getAttributeValueFromNode(@NonNull Node element, String attribute) {
        return element.getAttributes().getNamedItem(attribute).getTextContent().toString();
    }

    public Optional<String> getValueOfElement(String element, int index) {
        return getNodeByValue(element, index).map(node -> node.getTextContent().toString());
    }

}
