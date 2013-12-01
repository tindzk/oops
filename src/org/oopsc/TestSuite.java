package org.oopsc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.oopsvm.AsmException;
import org.oopsvm.Assembler;
import org.oopsvm.VMException;
import org.oopsvm.VirtualMachine;

@RunWith(Parameterized.class)
public class TestSuite {

	private final String path;
	private Program p;

	public TestSuite(String file) {
		this.path = file;
	}

	static String readFile(String path, Charset encoding) throws IOException {
		byte[] encoded = Files.readAllBytes(Paths.get(path));
		return encoding.decode(ByteBuffer.wrap(encoded)).toString();
	}

	public String runVM(String asm, String input) throws
			IOException, AsmException, VMException {
		InputStream asmStream = new ByteArrayInputStream(asm.getBytes("UTF-8"));

		VirtualMachine vm = new VirtualMachine(
				new Assembler(false, false).assemble(asmStream), new int[8],
				false, false, false, false, false, false, false);

		ByteArrayOutputStream output = new ByteArrayOutputStream();

		vm.setStreams(new ByteArrayInputStream(input.getBytes()), output);
		vm.run();

		return output.toString("UTF-8");
	}

	@Test
	public void testGrammar() throws Exception {
		FileInputStream stream = new FileInputStream(this.path);
		ANTLRInputStream input = new ANTLRInputStream(stream);
		GrammarLexer lexer = new GrammarLexer(input);
		CommonTokenStream tokens = new CommonTokenStream(lexer);
		GrammarParser parser = new GrammarParser(tokens);

		parser.program();

		boolean supposedToFail = this.path.contains("_se");

		if (supposedToFail) {
			assertNotNull(parser.getNumberOfSyntaxErrors());
		} else {
			assertEquals(0, parser.getNumberOfSyntaxErrors());
		}
	}

	@Test
	public void testVisitor() throws Exception {
		boolean supposedToFail = this.path.contains("_se");

		if (supposedToFail) {
			/* Skip as we cannot use the visitor on malformed code samples. */
			return;
		}

		FileInputStream stream = new FileInputStream(this.path);
		ANTLRInputStream input = new ANTLRInputStream(stream);
		GrammarLexer lexer = new GrammarLexer(input);
		CommonTokenStream tokens = new CommonTokenStream(lexer);
		GrammarParser parser = new GrammarParser(tokens);

		ParseTree tree = parser.program();

		ProgramVisitor visitor = new ProgramVisitor(new Program());
		visitor.visit(tree);
	}

	/**
	 * Performs syntax and context analysis. Also tests the code generation.
	 *
	 * @throws Exception
	 */
	@Test
	public void testFile() throws Exception {
		boolean supposedToFail = this.path.contains("_se");

		String pathExpected = this.path.substring(0, this.path.length() - 5)
				+ ".out";
		String expected = "";

		try {
			expected = readFile(pathExpected, StandardCharsets.UTF_8);
		} catch (NoSuchFileException e) {

		}

		try {
			this.p = new SyntaxAnalysis(this.path, false).parse();
			this.p.semanticAnalysis();
			this.p.printTree();

			/* Test code generation. */
			ByteArrayOutputStream stream = new ByteArrayOutputStream();
			CodeStream code = new CodeStream(stream);
			this.p.generateCode(code, 1000, 1000);
			String asm = stream.toString("UTF-8");

			System.err.println(asm);

			/* Run the VM twice with different inputs. */
			String output = this.runVM(asm, "abc") + this.runVM(asm, "xyz");

			assertEquals(expected, output);
		} catch (UnsupportedEncodingException e) {
			fail();
		} catch (CompileException e) {
			if (supposedToFail) {
				System.err.println(e.getMessage());
				return;
			}

			throw e;
		}

		if (supposedToFail) {
			fail();
		}
	}

	@Parameters(name = "{0}")
	public static Collection<Object[]> data() {
		final List<Object[]> data = new ArrayList<>();

		try {
			Files.walkFileTree(Paths.get("tests/"),
					new SimpleFileVisitor<Path>() {
						@Override
						public FileVisitResult visitFile(Path file,
								BasicFileAttributes attrs) throws IOException {
							if (file.toString().endsWith(".oops")) {
								data.add(new Object[] {
									file.toString()
								});
							}
							return FileVisitResult.CONTINUE;
						}

						@Override
						public FileVisitResult visitFileFailed(Path file,
								IOException exc) throws IOException {
							return FileVisitResult.CONTINUE;
						}
					});
		} catch (IOException e) {
			e.printStackTrace();
		}

		Collections.sort(data, new Comparator<Object[]>() {
			@Override
			public int compare(Object[] o, Object[] o2) {
				return ((String) o[0]).compareTo((String) o2[0]);
			}
		});

		return data;
	}

}