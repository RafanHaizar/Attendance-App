package com.itextpdf.kernel.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

final class XmlUtils {
    XmlUtils() {
    }

    public static void writeXmlDocToStream(Document xmlReport, OutputStream stream) throws TransformerException {
        TransformerFactory tFactory = TransformerFactory.newInstance();
        try {
            tFactory.setAttribute("http://javax.xml.XMLConstants/property/accessExternalDTD", "");
            tFactory.setAttribute("http://javax.xml.XMLConstants/property/accessExternalStylesheet", "");
        } catch (Exception e) {
        }
        Transformer transformer = tFactory.newTransformer();
        transformer.setOutputProperty("indent", "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "0");
        transformer.transform(new DOMSource(xmlReport), new StreamResult(stream));
    }

    public static boolean compareXmls(InputStream xml1, InputStream xml2) throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        dbf.setCoalescing(true);
        dbf.setIgnoringElementContentWhitespace(true);
        dbf.setIgnoringComments(true);
        DocumentBuilder db = dbf.newDocumentBuilder();
        db.setEntityResolver(new SafeEmptyEntityResolver());
        Document doc1 = db.parse(xml1);
        doc1.normalizeDocument();
        Document doc2 = db.parse(xml2);
        doc2.normalizeDocument();
        return doc2.isEqualNode(doc1);
    }

    public static Document initNewXmlDocument() throws ParserConfigurationException {
        return DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
    }

    private static class SafeEmptyEntityResolver implements EntityResolver {
        private SafeEmptyEntityResolver() {
        }

        public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
            return new InputSource(new StringReader(""));
        }
    }
}
