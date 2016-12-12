package de.mas.jnus.lib.utils;


import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.sun.istack.internal.NotNull;

public class XMLParser {
    private Document document;
    
    public void loadDocument(InputStream inputStream) throws ParserConfigurationException, SAXException, IOException{
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(inputStream);
        this.document = document;
        
    }
    
    public long getValueOfElementAsInt(String element,int index){
        return Integer.parseInt(getValueOfElement(element,index));
    }
    
    public long getValueOfElementAsLong(String element,int index){
        return Long.parseLong(getValueOfElement(element,index));
    }
    
    public String getValueOfElement(String element){
        return getValueOfElement(element,0);
    }
    
    public Node getNodeByValue(String element){
        return getNodeByValue(element, 0);
    }
    public Node getNodeByValue(String element,int index){
        if(document == null){
            System.out.println("Please load the document first.");
        }
        NodeList list = document.getElementsByTagName(element);        
        if(list == null){
            return null;
        }
        return list.item(index);
    }
    
    public String getValueOfElementAttribute(String element,int index,String attribute){
        Node node = getNodeByValue(element, index);
        if(node == null){
            //System.out.println("Node is null");
            return "";
        }
        return getAttributeValueFromNode(node,attribute);
    }
    
    public static String getAttributeValueFromNode(@NotNull Node element,String attribute){      
        return element.getAttributes().getNamedItem(attribute).getTextContent().toString();
    }
    
    public String getValueOfElement(String element,int index){
        Node node = getNodeByValue(element, index);
        if(node == null){
            //System.out.println("Node is null");
            return "";
        }
       
        return node.getTextContent().toString(); 
    }
}
