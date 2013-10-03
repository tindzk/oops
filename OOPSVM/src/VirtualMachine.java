/**
 * Die Klasse implementiert eine virtuelle Maschine für einen einfachen
 * Satz von Maschinenbefehlen. Alle Befehle haben zwei Parameter, so
 * dass sie immer aus drei Maschinenworten bestehen. Die virtuelle Maschine
 * hat einen Registersatz (R0 ... Rn), wobei das Register R0 der Instruktionszeiger
 * ist, d.h. R0 zeigt immer auf die nächste auszuführenden Instruktion.
 * Verlässt R0 den gültigen Bereich des Hauptspeichers, ist das Programm beendet.
 */
class VirtualMachine {
	/**
	 * MRI reg, num.
	 * Diese Instruktion speichert die Zahl num im Register reg.
	 */
	private final int MRI = 0;

	/**
	 * MRR reg1, reg2.
	 * Diese Instruktion speichert den Inhalt von Register <i>reg1</i> im Register <i>reg2</i>.
	 */
	private final int MRR = 1;

	/**
	 * MRM reg1, (reg2).
	 * Diese Instruktion speichert den Inhalt der Speicherstelle, auf die Register <i>reg2</i>
	 * zeigt, im Register <i>reg1</i>.
	 */
	private final int MRM = 2;

	/**
	 * MMR (reg1), reg2.
	 * Diese Instruktion speichert den Inhalt von Register <i>reg2</i> in der Speicherstelle,
	 * auf die Register <i>reg1</i> zeigt.
	 */
	private final int MMR = 3;

	/**
	 * ADD reg1, reg2.
	 * Diese Instruktion addiert den Inhalt von Register <i>reg2</i> zum Register <i>reg1</i>.
	 */
	private final int ADD = 4;

	/**
	 * SUB reg1, reg2.
	 * Diese Instruktion subtrahiert den Inhalt von Register <i>reg2</i> vom Register <i>reg1</i>.
	 */
	private final int SUB = 5;

	/**
	 * MUL reg1, reg2.
	 * Diese Instruktion multipliziert den Inhalt von Register <i>reg2</i> zum Register <i>reg1</i>.
	 */
	private final int MUL = 6;

	/**
	 * DIV reg1, reg2.
	 * Diese Instruktion dividiert das Register <i>reg1</i> durch den Inhalt von Register
	 * <i>reg2</i>.
	 */
	private final int DIV = 7;

	/**
	 * MOD reg1, reg2.
	 * Diese Instruktion speichert den Divisionsrest von Register <i>reg1</i> durch Register
	 * <i>reg2</i>
	 * in Register <i>reg1</i>.
	 */
	private final int MOD = 8;

	/**
	 * AND reg1, reg2.
	 * Diese Instruktion und-verknüpft den Inhalt von Register <i>reg2</i> in das Register
	 * <i>reg1</i>.
	 */
	private final int AND = 9;

	/**
	 * OR reg1, reg2.
	 * Diese Instruktion oder-verknüpft den Inhalt von Register <i>reg2</i> in das Register
	 * <i>reg1</i>.
	 */
	private final int OR = 10;

	/**
	 * XOR reg1, reg2.
	 * Diese Instruktion exklusiv-oder-verknüpft den Inhalt von Register <i>reg2</i> in das Register
	 * <i>reg1</i>.
	 */
	private final int XOR = 11;

	/**
	 * ISZ reg1, reg2.
	 * Diese Instruktion setzt das Register <i>reg1</i> auf eins, wenn der Inhalt des Registers
	 * <i>reg2</i>
	 * null ist, ansonsten auf null.
	 */
	private final int ISZ = 12;

	/**
	 * ISP reg1, reg2.
	 * Diese Instruktion setzt das Register <i>reg1</i> auf eins, wenn der Inhalt des Registers
	 * <i>reg2</i>
	 * größer als null ist, ansonsten auf null.
	 */
	private final int ISP = 13;

	/**
	 * ISP reg1, reg2.
	 * Diese Instruktion setzt das Register <i>reg1</i> auf eins, wenn der Inhalt des Registers
	 * <i>reg2</i>
	 * kleiner als null ist, ansonsten auf null.
	 */
	private final int ISN = 14;

	/**
	 * JPC reg1, addr.
	 * Diese Instruktion schreibt <i>addr</i> in den Instruktionszeiger (Register R0), wenn der
	 * Inhalt des
	 * Registers <i>reg1</i> ungleich null ist.
	 */
	private final int JPC = 15;

	/**
	 * SYS num1, num2.
	 * Diese Instruktion ruft eine Systemfunktion auf. <i>num1</i> ist dabei die Nummer der
	 * Funktion,
	 * <i>num2</i> ein funktionsabhängiger Parameter. Momentan sind nur zwei Funktionen definiert:
	 * <ul>
	 * <li>0: Es wird ein Zeichen von der Konsole eingelesen. Das Zeichen wird in dem Register mit
	 * der Nummer <i>num2</i> abgelegt. Das Ende des Eingabestroms wird durch das Zeichen -1
	 * symbolisiert.</li>
	 * <li>1: Es wird ein Zeichen auf der Konsole ausgegeben. Das Zeichen wird aus dem Register mit
	 * der Nummer <i>num2</i> gelesen.</li>
	 * </ul>
	 */
	private final int SYS = 16;

	/** Der Hauptspeicher. Er enthält das Programm und alle Daten. */
	private final int[] memory;

	/** Der Registersatz. */
	private final int[] registers;

	/** Sollen die ausgeführten Instruktionen angezeigt werden? */
	private final boolean showInstructions;

	/** Soll der Speicherinhalt nach jeder ausgeführten Instruktion angezeigt werden? */
	private final boolean showMemory;

	/** Soll der Registersatz nach jeder ausgeführten Instruktion angezeigt werden? */
	private final boolean showRegisters;

	/** Zeige R2 Speicherauszug vorwärts */
	private final boolean showR2f;

	/** Zeige R2 Speicherauszug rückwärts */
	private final boolean showR2b;

	/** Zeige R4 Speicherauszug vorwärts */
	private final boolean showR4f;

	/** Zeige R4 Speicherauszug rückwärts */
	private final boolean showR4b;

	/**
	 * Die Methode gibt die aktuelle Instruktion aus, wenn {@link #showInstrutions showInstrutions}
	 * aktiviert ist.
	 *
	 * @param text
	 *        Die Instruktion mit ihren Parametern.
	 */
	private void printInstruction(String text) {
		if (this.showInstructions) {
			System.out.format("%08x  %s%n", this.registers[0] - 3, text);
		}
	}

	/**
	 * Die Methode gibt den Hauptspeicher aus, wenn {@link #showMemory showMemory} aktiviert ist.
	 */
	private void printMemory() {
		if (this.showMemory) {
			String text = "";
			for (int m : this.memory) {
				text += m + " ";
			}
			System.out.println(text);
		}
	}

	/**
	 * Die Methode gibt den Registersatz aus, wenn {@link #showRegisters showRegisters} aktiviert
	 * ist.
	 */
	private void printRegisters() {
		if (this.showRegisters) {
			String text = "";
			for (int i = 0; i < this.registers.length; ++i) {
				text += "R" + i + "=" + this.registers[i] + " ";
			}
			System.out.println(text);
		}
	}

	/**
	 * Hilfsfunktion für {@link VirtualMachine#printStacks() printStacks}.
	 *
	 * @param register
	 *        Nummer des Registers, das die Basisadresse enthält
	 * @param upDown
	 *        ist -1 oder 1 für Abwärts/Aufwärts.
	 * @return Einen Text, der einen Ausschnitt des Stacks in lesbarer Form
	 *         enthält, die Adresse 'register' zusätzlich mit '*' markiert.
	 */
	private String mem(int register, int upDown) {
		int limit = 20;
		String t = "";
		for (int i = -2; i < limit; i++) {
			int r = this.registers[register] + upDown * i;
			t += i == 0 ? "*" : "";
			if (r < 0) {
				t += "- ";
			} else if (r > this.memory.length) {
				t += "+ ";
			} else {
				t += this.memory[r] + " ";
			}
		}
		return t;
	}

	/**
	 * Ausgabe von Speicherauszügen ausgehend von den in Registern R2 bzw.
	 * R4 gespeicherten Adressen. Dies kann man zur Anzeige von Stack-Inhalten
	 * benutzen.
	 */
	private void printStacks() {
		if (this.showR2f) {
			System.out.println("R2 forward: " + this.mem(2, 1));
		}
		if (this.showR2b) {
			System.out.println("R2 backward: " + this.mem(2, -1));
		}
		if (this.showR4f) {
			System.out.println("R4 forward: " + this.mem(4, 1));
		}
		if (this.showR4b) {
			System.out.println("R4 backward: " + this.mem(4, -1));
		}
	}

	/**
	 * Die Methode liest eine Instruktion aus dem Hauptspeicher und führt sie aus.
	 *
	 * @throws Exception
	 *         Ein Fehler ist aufgetreten (Instruktion, Speicherstelle
	 *         oder Systemaufruf ungültig).
	 */
	private void executeInstruction() throws Exception {
		int instruction = this.memory[this.registers[0]++];
		int param1 = this.memory[this.registers[0]++];
		int param2 = this.memory[this.registers[0]++];
		switch (instruction) {
			case MRI:
				this.printInstruction("MRI R" + param1 + ", " + param2);
				this.registers[param1] = param2;
				break;
			case MRR:
				this.printInstruction("MRR R" + param1 + ", R" + param2);
				this.registers[param1] = this.registers[param2];
				break;
			case MRM:
				this.printInstruction("MRM R" + param1 + ", (R" + param2 + ")");
				if (this.registers[param2] < 0
						|| this.registers[param2] >= this.memory.length) {
					throw new Exception(
							"Zugriff auf nicht existierende Speicherstelle "
									+ this.registers[param2] + " an Adresse "
									+ (this.registers[0] - 3));
				}
				this.registers[param1] = this.memory[this.registers[param2]];
				break;
			case MMR:
				this.printInstruction("MMR (R" + param1 + "), R" + param2);
				if (this.registers[param1] < 0
						|| this.registers[param1] >= this.memory.length) {
					throw new Exception(
							"Zugriff auf nicht existierende Speicherstelle "
									+ this.registers[param1] + " an Adresse "
									+ (this.registers[0] - 3));
				}
				this.memory[this.registers[param1]] = this.registers[param2];
				break;
			case ADD:
				this.printInstruction("ADD R" + param1 + ", R" + param2);
				this.registers[param1] += this.registers[param2];
				break;
			case SUB:
				this.printInstruction("SUB R" + param1 + ", R" + param2);
				this.registers[param1] -= this.registers[param2];
				break;
			case MUL:
				this.printInstruction("MUL R" + param1 + ", R" + param2);
				this.registers[param1] *= this.registers[param2];
				break;
			case DIV:
				this.printInstruction("DIV R" + param1 + ", R" + param2);
				this.registers[param1] /= this.registers[param2];
				break;
			case MOD:
				this.printInstruction("MOD R" + param1 + ", R" + param2);
				this.registers[param1] %= this.registers[param2];
				break;
			case AND:
				this.printInstruction("AND R" + param1 + ", R" + param2);
				this.registers[param1] &= this.registers[param2];
				break;
			case OR:
				this.printInstruction("OR R" + param1 + ", R" + param2);
				this.registers[param1] |= this.registers[param2];
				break;
			case XOR:
				this.printInstruction("XOR R" + param1 + ", R" + param2);
				this.registers[param1] ^= this.registers[param2];
				break;
			case ISZ:
				this.printInstruction("ISZ R" + param1 + ", R" + param2);
				this.registers[param1] = this.registers[param2] == 0 ? 1 : 0;
				break;
			case ISP:
				this.printInstruction("ISP R" + param1 + ", R" + param2);
				this.registers[param1] = this.registers[param2] > 0 ? 1 : 0;
				break;
			case ISN:
				this.printInstruction("ISN R" + param1 + ", R" + param2);
				this.registers[param1] = this.registers[param2] < 0 ? 1 : 0;
				break;
			case JPC:
				this.printInstruction("JPC R" + param1 + ", " + param2);
				if (this.registers[param1] != 0) {
					this.registers[0] = param2;
				}
				break;
			case SYS:
				this.printInstruction("SYS " + param1 + ", " + param2);
				switch (param1) {
					case 0:
						this.registers[param2] = System.in.read();
						break;
					case 1:
						System.out.print((char) this.registers[param2]);
						break;
					default:
						throw new Exception("Illegaler Systemaufruf: " + param1);
				}
				break;
			default:
				throw new Exception("Illegale Instruktion: " + instruction
						+ " an Adresse " + (this.registers[0] - 3));
		}
	}

	/**
	 * Konstruiert eine virtuelle Maschine.
	 *
	 * @param memory
	 *        Der Hauptspeicher. Er enthält das Programm und alle Daten.
	 * @param registers
	 *        Der Registersatz.
	 * @param showInstructions
	 *        Sollen die ausgeführten Instruktionen angezeigt werden?
	 * @param showMemory
	 *        Soll der Speicherinhalt nach jeder ausgeführten Instruktion angezeigt werden?
	 * @param showRegisters
	 *        Soll der Registersatz nach jeder ausgeführten Instruktion angezeigt werden?
	 */
	VirtualMachine(int[] memory, int[] registers, boolean showInstructions,
			boolean showMemory, boolean showRegisters, boolean showR2f,
			boolean showR2b, boolean showR4f, boolean showR4b) {
		this.memory = memory;
		this.registers = registers;
		this.showInstructions = showInstructions;
		this.showMemory = showMemory;
		this.showRegisters = showRegisters;
		this.showR2f = showR2f;
		this.showR2b = showR2b;
		this.showR4f = showR4f;
		this.showR4b = showR4b;
	}

	/**
	 * Die Methode führt das Programm im Hauptspeicher aus.
	 *
	 * @throws Exception
	 *         Ein Fehler ist aufgetreten (Instruktion, Speicherstelle,
	 *         Register oder Systemaufruf ungültig).
	 */
	void run() throws Exception {
		try {
			while (this.registers[0] >= 0
					&& this.registers[0] < this.memory.length) {
				this.executeInstruction();
				this.printMemory();
				this.printRegisters();
				this.printStacks();
			}
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new Exception("Zugriff auf nicht existierendes Register "
					+ e.getMessage() + " an Adresse " + (this.registers[0] - 3));
		}
	}
}
