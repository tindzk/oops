import static org.junit.Assert.fail;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class TestSuite {

	private final String path;
	private Program p;

	public TestSuite(String file) {
		this.path = file;
	}

	/**
	 * Performs syntax and context analysis.
	 *
	 * @throws Exception
	 */
	@Test
	public void testFile() throws Exception {
		boolean supposedToFail = this.path.contains("_se");

		try {
			this.p = new SyntaxAnalysis(this.path, false).parse();
			this.p.contextAnalysis();

			/* TODO Test code generation. */
		} catch (CompileException e) {
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