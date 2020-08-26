package JBDT.util;

import JBDT.classifier.tree.Tree;
import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;

public class XMLParser {

    int indent = 0;

    private void printlnCommon(Node n) {
        PrintStream out = System.out;
        out.print(" nodeName=\"" + n.getNodeName() + "\"");

        String val = n.getNamespaceURI();
        if (val != null) {
            out.print(" uri=\"" + val + "\"");
        }

        val = n.getPrefix();

        if (val != null) {
            out.print(" pre=\"" + val + "\"");
        }

        val = n.getLocalName();
        if (val != null) {
            out.print(" local=\"" + val + "\"");
        }

        val = n.getNodeValue();
        if (val != null) {
            out.print(" nodeValue=");
            if (val.trim().equals("")) {
                // Whitespace
                out.print("[WS]");
            }
            else {
                out.print("\"" + n.getNodeValue() + "\"");
            }
        }
        out.println();
    }

    void outputIndentation() {
        for (int i = 0; i < indent; i++) {
            System.out.print(" ");
        }
    }

    private void echo(Node n) {
        outputIndentation();
        PrintStream out = System.out;
        int type = n.getNodeType();

        switch (type) {
            case Node.ATTRIBUTE_NODE:
                out.print("ATTR:");
                printlnCommon(n);
                break;

            case Node.CDATA_SECTION_NODE:
                out.print("CDATA:");
                printlnCommon(n);
                break;

            case Node.COMMENT_NODE:
                out.print("COMM:");
                printlnCommon(n);
                break;

            case Node.DOCUMENT_FRAGMENT_NODE:
                out.print("DOC_FRAG:");
                printlnCommon(n);
                break;

            case Node.DOCUMENT_NODE:
                out.print("DOC:");
                printlnCommon(n);
                break;

            case Node.DOCUMENT_TYPE_NODE:
                out.print("DOC_TYPE:");
                printlnCommon(n);
                NamedNodeMap nodeMap = ((DocumentType)n).getEntities();
                indent += 2;
                for (int i = 0; i < nodeMap.getLength(); i++) {
                    Entity entity = (Entity)nodeMap.item(i);
                    echo(entity);
                }
                indent -= 2;
                break;

            case Node.ELEMENT_NODE:
                out.print("ELEM:");
                printlnCommon(n);

                NamedNodeMap atts = n.getAttributes();
                indent += 2;
                for (int i = 0; i < atts.getLength(); i++) {
                    Node att = atts.item(i);
                    echo(att);
                }
                indent -= 2;
                break;

            case Node.ENTITY_NODE:
                out.print("ENT:");
                printlnCommon(n);
                break;

            case Node.ENTITY_REFERENCE_NODE:
                out.print("ENT_REF:");
                printlnCommon(n);
                break;

            case Node.NOTATION_NODE:
                out.print("NOTATION:");
                printlnCommon(n);
                break;

            case Node.PROCESSING_INSTRUCTION_NODE:
                out.print("PROC_INST:");
                printlnCommon(n);
                break;

            case Node.TEXT_NODE:
                out.print("TEXT:");
                printlnCommon(n);
                break;

            default:
                out.print("UNSUPPORTED NODE: " + type);
                printlnCommon(n);
                break;
        }

        indent++;
        for (Node child = n.getFirstChild(); child != null;
             child = child.getNextSibling()) {
            echo(child);
        }
        indent--;
    }

    void printElem(Node node) {
        System.out.println(node.getNodeName()+" "+node.getNodeValue());
        NodeList nodeList = node.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            if(node.getNodeType() == Node.ELEMENT_NODE)
                printElem(nodeList.item(i));
        }
    }



    void buildTree(String fn) {
        try {
            File file = new File("/Users/yandong/tmp/gbrank.xml");
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document document = db.parse(file);
            ArrayList<String> path = new ArrayList<String>();
            path.add("MlrFunction");
            path.add("DecisionTree");
            Node parent = Util.findElement(document, path, 0);
            System.out.println(parent);
            if(parent!=null) {
                Node node_loss = parent.getAttributes().getNamedItem("loss");
                System.out.println(node_loss.getNodeValue());
            }
            path.add("Forest");
            parent = Util.findElement(document, path, 0);
            System.out.println(parent.getNodeName());
            //get tree nodes
            NodeList nodes = parent.getChildNodes();
            for (int i = 0; i < nodes.getLength(); i++) {
                Node node = nodes.item(i);
                System.out.println(node.getNodeName());
                if(node.getNodeType() == Node.ELEMENT_NODE&&node.getNodeName().equals("Tree")) {

                    Tree tmp = new Tree();
                    tmp.readXML(node, false);
                    //_trees.push
                    //_shrinkrage
                }
            }
            //we can also do following:
//            NodeList treeNodes = ((Element)parent).getElementsByTagName("Tree");
//            System.out.println("treeNodes#:"+treeNodes.getLength());
        } catch ( Exception ex) {
            ex.printStackTrace();
        }
    }

    void test() {
        try {
            File file = new File("/Users/yandong/tmp/gbrank.xml");
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document document = db.parse(file);
            Element e = document.getDocumentElement();

//            NodeList nodeList = document.getDocumentElement().getChildNodes();
            NodeList nodeList = document.getChildNodes();
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);
                echo(node);
                if(node.getNodeType() == Node.ELEMENT_NODE) {
//                    System.out.println("element");
//                    printElem(node);
                } else  if(node.getNodeType() == Node.ATTRIBUTE_NODE) {
//                    System.out.println("attribute");
                } else if (node.getNodeType() == Node.ENTITY_NODE) {
//                    System.out.println("entity");
                } else if(node.getNodeType() == Node.DOCUMENT_NODE) {
//                    System.out.println("node");
                } else {
                    System.out.println(node.getNodeType());
                }
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void main(String[] args) {
        new XMLParser().buildTree("/Users/yandong/tmp/gbrank.xml");
    }
}
