import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class TestSuite {

	private final File file;
	private Program p;

	public TestSuite(File file) {
		this.file = file;
	}

	/**
	 * Performs syntax and context analysis.
	 * @throws Exception
	 */
	@Test
	public void testFile() throws Exception {
		boolean supposedToFail = this.file.getPath().contains("_se");

		try {
			this.p = new SyntaxAnalysis(this.file.getPath(), false).parse();
			this.p.contextAnalysis();

			/* TODO Test code generation. */
		} catch(CompileException e) {
			if (supposedToFail) {
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
		final Collection<Object[]> data = new ArrayList<>();

		try {
			Files.walkFileTree(Paths.get("tests/"),
					new SimpleFileVisitor<Path>() {
						@Override
						public FileVisitResult visitFile(Path file,
								BasicFileAttributes attrs) throws IOException {
							if (file.toString().endsWith(".oops")) {
								data.add(new Object[] {
									file.toFile()
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

		return data;
	}

}