import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.TreeMap;

/**
 * Die Klasse implementiert einen einfachen Assembler, der einen Quelltext
 * aus einer Datei liest und daraus ein Abbild des Hauptspeichers mit dem
 * übersetzen Programm generiert. Syntax einer Quelltextzeile:
 *
 * <pre>
 * line   ::= instr [ ';' comment ]
 * instr  ::= label ':'
 *          | 'MRI' reg ',' addr
 *          | 'MRR' reg ',' reg
 *          | 'MRM' reg ',' '(' reg ')'
 *          | 'MMR' '(' reg ')' ',' reg
 *          | 'ADD' reg ',' reg
 *          | 'SUB' reg ',' reg
 *          | 'MUL' reg ',' reg
 *          | 'DIV' reg ',' reg
 *          | 'MOD' reg ',' reg
 *          | 'AND' reg ',' reg
 *          | 'OR' reg ',' reg
 *          | 'XOR' reg ',' reg
 *          | 'ISZ' reg ',' reg
 *          | 'ISP' reg ',' reg
 *          | 'ISN' reg ',' reg
 *          | 'JPC' reg ',' addr
 *          | 'SYS' addr ',' addr
 *          | 'DAT' number, addr
 * label  ::= ident
 * reg    ::= 'R'number
 * addr   ::= label
 *          | number
 * ident  ::= letter { letter | digit }
 * number ::= [ '-' ] digit { digit }
 * letter ::= 'A' .. 'Z' | 'a' .. 'z' | '_'
 * digit  :: = '0' .. '9'
 * </pre>
 */
class Assembler {
	/** Alle gültigen Instruktionen. Die Position im Feld entspricht ihrer Kodierung. */
	private static final String[] instructions = {
		"MRI", "MRR", "MRM", "MMR", "ADD", "SUB", "MUL", "DIV", "MOD", "AND",
		"OR", "XOR", "ISZ", "ISP", "ISN", "JPC", "SYS"
	};

	/** Die Zuordnung von textuellen Marken zu Speicheradressen. */
	private TreeMap<String, Integer> labels;

	/** Der Datenstrom, aus dem der Quelltext gelesen wird. */
	private InputStreamReader reader;

	/**
	 * Das zuletzt gelesenen Zeichen. Wird durch Aufruf von {@link #nextChar() nextChar}
	 * aktualisiert.
	 */
	private int c;

	/**
	 * Die aktuell gelesene Zeile wird in diesem Puffer für eine mögliche Ausgabe
	 * zwischengespeichert.
	 */
	private String line;

	/**
	 * In dieses Feld wird im zweiten Durchgang das Programm generiert. Im ersten Durchgang ist es
	 * null.
	 */
	private int[] output;

	/** Die Adresse der nächsten zu beschreibenden Speicherzelle. */
	private int writePos;

	/** Soll eine Bildschirmausgabe während des ersten Durchgangs erfolgen? */
	private final boolean showFirst;

	/** Soll eine Bildschirmausgabe während des zweiten Durchgangs erfolgen? */
	private final boolean showSecond;

	/** Soll eine Bildschirmausgabe während des aktuellen Durchgangs erfolgen? */
	private boolean showCode;

	/**
	 * Die Methode erlaubt, zwischen dem ersten und dem zweiten Assemblierungslauf
	 * zu unterscheiden.
	 *
	 * @return Ist der aktuelle Assemblierungsdurchgang der erste?
	 */
	private boolean isFirstPass() {
		return this.output == null;
	}

	/**
	 * Die Methode liest das nächste Zeichen aus der Eingabedatei.
	 * Es wird im Attribut {@link #c c} bereitgestellt.
	 * Wenn während der Assemblierung eine Ausgabe erfolgen soll, werden
	 * zusätzlich die eingelesenen Zeichen im Attribut {@link #line line} gesammelt und jeweils beim
	 * Lesen eines Zeilenendes ausgegeben.
	 * Das ermöglicht es, an anderer Stelle der Ausgabe noch den erzeugten
	 * Code voranzustellen.
	 *
	 * @throws IOException
	 *         Die Ausnahme wird bei Leseproblemen der Datei erzeugt.
	 */
	private void nextChar() throws IOException {
		this.c = this.reader.read();
		if (this.showCode) {
			if (this.c != -1) {
				this.line += (char) this.c;
			}
			if (this.c == '\n') {
				System.out.print(this.line);
				this.line = "";
			}
		}
	}

	/**
	 * Die Methode liest ein Token ein. Dies sind ",", ":", "(", ")"
	 * sowie Zeichenketten, die den nicht-Terminalen <i>ident</i> und <i>number</i>
	 * aus der oben angegebenen Grammatik entsprechen. Alle Zeichen ab einem Semikolon
	 * werden bis zum Zeilenende ignoriert, d.h. als Kommentar behandelt.
	 *
	 * @return Das Token als Zeichenkette.
	 * @throws IOException
	 *         Die Ausnahme wird bei Leseproblemen der Datei erzeugt.
	 * @throws Exception
	 *         Ein ungültiges Zeichen wurde gelesen.
	 */
	private String readToken() throws IOException, Exception {
		while (this.c != -1) {
			while (this.c != -1 && Character.isWhitespace((char) this.c)) {
				this.nextChar();
			}

			switch (this.c) {
				case -1: // Dateiende
					break;
				case ',':
				case ':':
				case '(':
				case ')':
					String token = "" + (char) this.c;
					this.nextChar();
					return token;

				case ';': // Kommentar: ignorieren bis Zeilenende
					while (this.c != -1 && this.c != '\n') {
						this.nextChar();
					}
					break;
				default: // number oder ident
					if (this.c == '-' || Character.isDigit((char) this.c)) {
						String number = "" + (char) this.c;
						this.nextChar();
						while (this.c != -1 && Character.isDigit((char) this.c)) {
							number += (char) this.c;
							this.nextChar();
						}
						if (number.equals("-")) {
							throw new Exception(
									"Zahl muss mindestens eine Ziffer haben: -");
						}
						return number;
					} else if (this.c == '_'
							|| Character.isLetter((char) this.c)) {
						String identifier = "" + (char) this.c;
						this.nextChar();
						while (this.c != -1
								&& (this.c == '_' || Character
										.isLetterOrDigit((char) this.c))) {
							identifier = identifier + (char) this.c;
							this.nextChar();
						}
						return identifier;
					} else {
						throw new Exception("Unerwartetes Zeichen: "
								+ (char) this.c + " (" + this.c + ")");
					}
			}
		}
		return ""; // Dateiende
	}

	/**
	 * Die Methode wandelt einen Parameter in eine Zahl um.
	 * Dabei gibt es drei Fälle: Wenn der Parameter ein Register sein soll,
	 * wird das "R" entfernt und die Zahl dahinter zurückgeliefert. Soll
	 * der Parameter kein Register sein und die Zeichenkette enthält eine
	 * Zahl, so wird diese direkt zurückgegeben. Enthält die Zeichenkette
	 * hingegen einen Bezeichner, so wird die zugeordnete Adresse aus der
	 * Tabelle der definierten Marken entnommen. Das passiert nur im 2.
	 * Assemblierungslauf, im ersten wird stattdessen 0 zurückgeliefert.
	 *
	 * @param word
	 *        Der Parameter als Zeichenkette.
	 * @param register
	 *        Soll der Parameter ein Register sein?
	 * @return Die dem Parameter entsprechende Zahl.
	 * @throws Exception
	 *         Die Zeichenkette ist ungültig.
	 */
	private int parseParam(String word, boolean register) throws Exception {
		if (word.equals("")) {
			throw new Exception("Parameter fehlt");
		} else if (register) {
			if (word.charAt(0) == 'R') {
				int num = Integer.parseInt(word.substring(1));
				if (!word.equals("R" + num)) {
					throw new Exception("Falsches Register: " + word);
				}
				return num;
			} else {
				throw new Exception("Register erwartet: " + word);
			}
		} else if (Character.isLetter(word.charAt(0)) || word.charAt(0) == '_') {
			if (this.isFirstPass()) {
				return 0;
			} else {
				Integer address = this.labels.get(word);
				if (address == null) {
					throw new Exception("Marke " + word + " nicht gefunden");
				} else {
					return address;
				}
			}
		} else {
			return Integer.parseInt(word);
		}
	}

	/**
	 * Die Methode schreibt den generierten Code in den Speicher.
	 * Im ersten Assemblierungslauf wird kein Code geschrieben,
	 * sondern nur mitgezählt, wie viel Platz verbraucht würde.
	 * Dadurch lassen sich die Adressen der verwendeten Marken
	 * bestimmen.
	 *
	 * @param code
	 *        Der Code, der in den Speicher geschrieben wird.
	 */
	private void writeCode(int code) {
		if (!this.isFirstPass()) {
			this.output[this.writePos] = code;
		}
		++this.writePos;
	}

	/**
	 * Die Methode parsiert ein Zeile aus dem Quelltext und generiert den
	 * entsprechenden Code. Die Syntax ist oben angegeben.
	 *
	 * @throws IOException
	 *         Die Ausnahme wird bei Leseproblemen der Datei erzeugt.
	 * @throws Exception
	 *         Beim Parsieren ist ein Fehler aufgetreten.
	 */
	private void parseLine() throws IOException, Exception {
		String instruction = this.readToken();
		String word1 = this.readToken();
		String word2;

		if (instruction.equals("")) { // Dateiende
			return;
		} else if (word1.equals(":")) { // Marke
			if (this.isFirstPass()) {
				String label = instruction;
				if (label.charAt(0) != '_'
						&& !Character.isLetter(label.charAt(0))) {
					throw new Exception(
							"Marke beginnt nicht mit einem Buchstaben: "
									+ label + ":");
				} else if (this.labels.get(label) == null) {
					this.labels.put(label, this.writePos);
				} else {
					throw new Exception("Marke " + label
							+ " wurde mehrfach definiert");
				}
			}
		} else { // Instruktion oder DAT
			int i;
			for (i = 0; i < instructions.length; ++i) {
				if (instruction.equals(instructions[i])) { // gültige Instruktion
					if (i == 3) {
						if (!word1.equals("(")) {
							throw new Exception(
									"Erster Parameter von MMR muss geklammert werden");
						}
						word1 = this.readToken();
						word2 = this.readToken();
						if (!word2.equals(")")) {
							throw new Exception(
									"Erster Parameter von MMR muss geklammert werden");
						}
					}
					word2 = this.readToken();
					if (!word2.equals(",")) {
						throw new Exception("Komma erwartet");
					}
					word2 = this.readToken();
					if (i == 2) {
						if (!word2.equals("(")) {
							throw new Exception(
									"Zweiter Parameter von MRM muss geklammert werden");
						}
						word2 = this.readToken();
						String word3 = this.readToken();
						if (!word3.equals(")")) {
							throw new Exception(
									"Zweiter Parameter von MRM muss geklammert werden");
						}
					}
					int param1 = this.parseParam(word1, i != 16);
					int param2 = this.parseParam(word2, i != 0 && i < 15);
					if (this.showCode) {
						System.out.format("%08x  %08x %08x %08x  ",
								this.writePos, i, param1, param2);
					}
					this.writeCode(i);
					this.writeCode(param1);
					this.writeCode(param2);
					break;
				}
			}
			if (i == instructions.length) { // keine gültige Instruktion
				if (instruction.equals("DAT")) { // DAT?
					word2 = this.readToken();
					if (!word2.equals(",")) {
						throw new Exception("Komma erwartet");
					}
					word2 = this.readToken();
					int param1 = this.parseParam(word1, false);
					int param2 = this.parseParam(word2, false);
					if (this.showCode) {
						System.out
								.format("%08x  %08x %3s                ",
										this.writePos, param2,
										param1 == 1 ? "" : "...");
					}
					if (Character.isLetter(word1.charAt(0))) {
						throw new Exception(
								"Erster Parameter von DAT kann keine Marke sein");
					} else if (param1 <= 0) {
						throw new Exception(
								"Erster Parameter von DAT muss groesser als 0 sein");
					} else if (param2 == 0) {
						this.writePos += param1;
					} else {
						for (int j = 0; j < param1; ++j) {
							this.writeCode(param2);
						}
					}
				} else { // ansonsten Fehler
					throw new Exception("Unbekannte Anweisung " + instruction);
				}
			}
		}
	}

	/**
	 * Die Methode führt einen Assemblierungsdurchgang aus.
	 * Im ersten Durchgang wird dabei im Attribut {@link #writePos writePos} lediglich die Größe des
	 * benötigten Speichers ermittelt. Im zweiten
	 * Durchgang wird der Speicher im Attribut {@link #output output} tatsächlich gefüllt.
	 *
	 * @param fileName
	 *        Der Name des Quelltexts.
	 * @throws FileNotFoundException
	 *         Der Quelltext existiert nicht.
	 * @throws IOException
	 *         Die Ausnahme wird bei Leseproblemen der Datei erzeugt.
	 * @throws Exception
	 *         Beim Assemblieren ist ein Fehler aufgetreten.
	 */
	private void pass(InputStream stream) throws IOException, Exception {
		this.reader = new InputStreamReader(stream);
		this.line = "";
		this.nextChar();
		this.writePos = 0;
		while (this.c != -1) {
			this.parseLine();
		}
		if (this.showCode && !this.line.equals("")) {
			System.out.println(this.line);
		}
	}

	/**
	 * Konstruktor.
	 *
	 * @param showFirst
	 *        Soll eine Bildschirmausgabe während des ersten Durchgangs erfolgen?
	 * @param showSecond
	 *        Soll eine Bildschirmausgabe während des zweiten Durchgangs erfolgen?
	 */
	Assembler(boolean showFirst, boolean showSecond) {
		this.showFirst = showFirst;
		this.showSecond = showSecond;
	}

	/**
	 * Die Methode wandelt einen Quelltext in Code um aus.
	 *
	 * @param fileName
	 *        Der Name des Quelltexts.
	 * @return Der Speicher, der das übersetze Programm enthält.
	 * @throws FileNotFoundException
	 *         Der Quelltext existiert nicht.
	 * @throws IOException
	 *         Die Ausnahme wird bei Leseproblemen der Datei erzeugt.
	 * @throws Exception
	 *         Beim Assemblieren ist ein Fehler aufgetreten.
	 */
	int[] assemble(InputStream stream) throws FileNotFoundException,
			IOException, Exception {
		this.labels = new TreeMap<String, Integer>();
		this.output = null;
		this.showCode = this.showFirst;
		stream.mark(1024 * 1024);
		this.pass(stream);
		this.output = new int[this.writePos];
		this.showCode = this.showSecond;
		stream.reset();
		this.pass(stream);
		return this.output;
	}
}
