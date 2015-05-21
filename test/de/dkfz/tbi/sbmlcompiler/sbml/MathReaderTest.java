package de.dkfz.tbi.sbmlcompiler.sbml;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import de.dkfz.tbi.sbmlcompiler.sbml.AstNode.NodeType;

public class MathReaderTest implements MathReader.MathContainer {

	private AstNode root = null;
	
	@Test
	public void testMathML() throws IOException, SAXException {
		XMLReader xml_reader = XMLReaderFactory.createXMLReader();
		ContentHandler handler = new MathReader(xml_reader, this);
		InputStream is = this.getClass().getResourceAsStream("/math.xml");
		xml_reader.setContentHandler(handler);
		try {
			xml_reader.parse(new InputSource(is));
		}
		finally {
			is.close();
		}
		if (root == null) {
			fail();
		}
		assertEquals(root.getType(), NodeType.AST_PLUS);
		assertEquals(root.getNumChildren(), 2);
		assertTrue(root.getChild(0).isName());
		assertEquals(root.getChild(1).getNumber().intValue(), 3);
	}
	
	@Test
	public void testAstNumberType() {
		AstNode node = new AstNode(42);
		assertTrue(node.isNumber());
		assertFalse(node.isFunction());
		assertFalse(node.isName());
		assertFalse(node.isOperator());
	}

	@Test
	public void testAstFunctionType() {
		AstNode node = new AstNode(NodeType.AST_FUNCTION_DELAY);
		assertFalse(node.isNumber());
		assertTrue(node.isFunction());
		assertFalse(node.isName());
		assertFalse(node.isOperator());
	}
	
	@Test
	public void testAstNameType() {
		AstNode node = new AstNode(NodeType.AST_NAME_TIME);
		assertFalse(node.isNumber());
		assertFalse(node.isFunction());
		assertTrue(node.isName());
		assertFalse(node.isOperator());
	}
	
	@Test
	public void testAstOperatorType() {
		AstNode node = new AstNode(NodeType.AST_MINUS);
		assertFalse(node.isNumber());
		assertFalse(node.isFunction());
		assertFalse(node.isName());
		assertTrue(node.isOperator());
	}
	
	@Test
	public void testFlattenAst() {
		for (int k = 0; k < 2; k ++) {
			AstNode a = new AstNode(NodeType.AST_PLUS);
			a.appendNode(new AstNode(42.0));
			AstNode b;
			if (k == 0) {
				b = new AstNode(NodeType.AST_PLUS);
			}
			else {
				b = new AstNode(NodeType.AST_TIMES);
			}
			a.appendNode(b);
			b.appendNode(new AstNode(4.0));
			b.appendNode(new AstNode(13.0));
			a.flattenTree();
			if (k == 0) {
				assertEquals(3, a.getNumChildren());
			}
			else {
				assertEquals(2, a.getNumChildren());
			}
			a.reduceToBinary();
			assertEquals(2, a.getNumChildren());
		}
	}
	
	@Override
	public void setRootNode(AstNode node) {
		root = node;
	}
}
