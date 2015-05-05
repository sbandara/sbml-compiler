package de.dkfz.tbi.sbmlcompiler;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.BeforeClass;
import org.junit.Test;

public class SbmlCompilerTest {
	
	@BeforeClass
	public static void setUp() {
    	System.loadLibrary("sbmlj");
	}

	@Test
	public void testTyson1991() throws SbmlCompilerException {
		SbmlCompiler cmplr = new SbmlCompiler("/Users/svb6/Tyson1991-L2V1.xml",
				false);
		Map<String, FortranCoder> bnds = cmplr.getInVivoBindings();
		ArrayList<FortranFunction> fn = cmplr.compile(bnds, "",
				new HashSet<String>());
		String code = fn.get(0).toString();
		HashSet<String> params = new HashSet<String>();
		Matcher param = Pattern.compile("\\$(\\w+)\\$").matcher(code);
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
	}
}
