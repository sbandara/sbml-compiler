package de.dkfz.tbi.sbmlcompiler;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Test;

public class SbmlCompilerTest {

	private static final String TYSON_FILE = "/Tyson1991-L2V1.xml",
			CASE973_FILE = "/Case973-L2V1.xml";

	@Test
	public void testWithTyson1991() throws SbmlCompilerException {
		InputStream is = this.getClass().getResourceAsStream(TYSON_FILE);
		SbmlCompiler cmplr;
		try {
			cmplr = new SbmlCompiler(is);
		}
		finally {
			try {
				is.close();
			}
			catch (IOException e) { }
		}
		Bindings bindings = cmplr.getDefaultBindings();
		ArrayList<FortranFunction> fn = cmplr.compile(bindings, "",
				new HashSet<String>());
		String code = fn.get(0).toString();
		HashSet<String> params = new HashSet<String>();
		Matcher param = Pattern.compile("\\$Reaction\\d:(\\w+)\\$").matcher(code);
		while (param.find()) {
			params.add(param.group(1));
		}
		assertTrue(params.contains("k1aa"));
		assertTrue(params.contains("k2"));
		assertTrue(params.contains("k3"));
		assertTrue(params.contains("k4"));
		assertTrue(params.contains("k4prime"));
		assertTrue(params.contains("k5notP"));
		assertTrue(params.contains("k6"));
		assertTrue(params.contains("k7"));
		assertTrue(params.contains("k8notP"));
		assertTrue(params.contains("k9"));
		System.out.println(code);
	}
	
	@Test
	public void testWithCase973() throws SbmlCompilerException {
		InputStream is = this.getClass().getResourceAsStream(CASE973_FILE);
		SbmlCompiler cmplr;
		try {
			cmplr = new SbmlCompiler(is);
		}
		finally {
			try {
				is.close();
			}
			catch (IOException e) { }
		}
		Bindings bindings = cmplr.getDefaultBindings();
		ArrayList<FortranFunction> fn = cmplr.compile(bindings, "",
				new HashSet<String>());
		String code = fn.get(0).toString();
		System.out.println(code);
	}
}
