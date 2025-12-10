package mars.mips.instructions.customlangs;

import mars.*;
import mars.mips.hardware.*;
import mars.mips.instructions.*;
import mars.simulator.Exceptions;

public class GundamAssembly extends CustomAssembly {

    // --- Special Gundam state ---
    // SR status bits (see PDF)
    private static final int EN_REG = 25;
    private static final int MS_REG = 26;
    private static final int SR_REG = 27;

    // Bit positions in SR
    private static final int FLAG_Z        = 0;
    private static final int FLAG_N        = 1;
    private static final int FLAG_C        = 2;  // not used here, but reserved
    private static final int FLAG_V        = 3;  // not used here, but reserved
    private static final int FLAG_SHIELD   = 4;
    private static final int FLAG_LOCKON   = 5;
    private static final int FLAG_TRANSAM  = 6;
    private static final int FLAG_DANGER   = 7;
    private static final int FLAG_CALLBASE = 8;

    // Link / return register: R7
    private static final int LINK_REGISTER = 7;

    // --- Helpers for SR bits ---
    private static boolean getFlag(int bit) {
    int sr = RegisterFile.getValue(SR_REG);
    return (sr & (1 << bit)) != 0;
}

private static void setFlag(int bit, boolean value) {
    int sr = RegisterFile.getValue(SR_REG);
    if (value) {
        sr |= (1 << bit);
    } else {
        sr &= ~(1 << bit);
    }
    RegisterFile.updateRegister(SR_REG, sr);
}

private static void updateZNFlags(int result) {
    int sr = RegisterFile.getValue(SR_REG);

    if (result == 0) sr |=  (1 << FLAG_Z);
    else             sr &= ~(1 << FLAG_Z);

    if (result < 0)  sr |=  (1 << FLAG_N);
    else             sr &= ~(1 << FLAG_N);

    RegisterFile.updateRegister(SR_REG, sr);
}

    @Override
    public String getName() {
        return "Gundam Assembly";
    }

    @Override
    public String getDescription() {
        return "Gundam-themed assembly: cockpit consoles, status flags, and mobile suit controls.";
    }

    @Override
    protected void populate() {

        instructionList.add(
            new BasicInstruction(
                "beamfuse $R0,$R1,$R2",
                "Beamfuse : Add two registers",
                BasicInstructionFormat.R_FORMAT,
                "000000 sssss ttttt fffff 00000 100000",
                new SimulationCode()
               {
                   public void simulate(ProgramStatement statement) throws ProcessingException
                  {
                     int[] operands = statement.getOperands();
                     int add1 = RegisterFile.getValue(operands[1]);
                     int add2 = RegisterFile.getValue(operands[2]);
                     int sum = add1 + add2;
                  // overflow on A+B detected when A and B have same sign and A+B has other sign.
                     if ((add1 >= 0 && add2 >= 0 && sum < 0)
                        || (add1 < 0 && add2 < 0 && sum >= 0))
                     {
                        throw new ProcessingException(statement,
                            "arithmetic overflow",Exceptions.ARITHMETIC_OVERFLOW_EXCEPTION);
                     }
                     RegisterFile.updateRegister(operands[0], sum);
                  }
               }));

        instructionList.add(
            new BasicInstruction(
                "sabercut $R0,$R1,$R2",
                "Sabercut : Subtract two registers",
                BasicInstructionFormat.R_FORMAT,
                "000000 sssss ttttt fffff 00000 100010",
                new SimulationCode() 
                {
                   public void simulate(ProgramStatement statement) throws ProcessingException
                  {
                     int[] operands = statement.getOperands();
                     int sub1 = RegisterFile.getValue(operands[1]);
                     int sub2 = RegisterFile.getValue(operands[2]);
                     int dif = sub1 - sub2;
                  // overflow on A-B detected when A and B have opposite signs and A-B has B's sign
                     if ((sub1 >= 0 && sub2 < 0 && dif < 0)
                        || (sub1 < 0 && sub2 >= 0 && dif >= 0))
                     {
                        throw new ProcessingException(statement,
                            "arithmetic overflow",Exceptions.ARITHMETIC_OVERFLOW_EXCEPTION);
                     }
                     RegisterFile.updateRegister(operands[0], dif);
                  }
               }));

        instructionList.add(
            new BasicInstruction(
                "overdrive $R0,$R1,$R2",
                "Overdrive : Multiply two registers (low 32 bits)",
                BasicInstructionFormat.R_FORMAT,
                "000000 sssss ttttt fffff 00000 011000",
                new SimulationCode() 
                {
                   public void simulate(ProgramStatement statement) throws ProcessingException
                  {
                     int[] operands = statement.getOperands();
                     long product = (long) RegisterFile.getValue(operands[1])
                        * (long) RegisterFile.getValue(operands[2]);
                     RegisterFile.updateRegister(operands[0],
                        (int) ((product << 32) >> 32));
                  }
               }));

        instructionList.add(
            new BasicInstruction(
                "scatter $R0,$R1,label",
                "Scatter : Branch to label if ($R0 != $R1)",
                BasicInstructionFormat.I_BRANCH_FORMAT,
                "000101 fffff sssss tttttttttttttttt",
                new SimulationCode() 
                {
                   public void simulate(ProgramStatement statement) throws ProcessingException
                  {
                     int[] operands = statement.getOperands();
                     if (RegisterFile.getValue(operands[0])
                        != RegisterFile.getValue(operands[1]))
                     {
                        Globals.instructionSet.processBranch(operands[2]);
                     }
                  }
               }));

        instructionList.add(
            new BasicInstruction(
                "callpilot target",
                "Callpilot : Jump and link to target (R7 = return address)",
                BasicInstructionFormat.J_FORMAT,
                "000011 ffffffffffffffffffffffffff",
                new SimulationCode() 
                {
                   public void simulate(ProgramStatement statement) throws ProcessingException
                  {
                     int[] operands = statement.getOperands();
                     Globals.instructionSet.processReturnAddress(7);
                     Globals.instructionSet.processJump(
                        (RegisterFile.getProgramCounter() & 0xF0000000)
                                | (operands[0] << 2));
                  }
               }));

        instructionList.add(
            new BasicInstruction(
                "phasejam $R0,$R1,$R2",
                "Phasejam : Bitwise XOR",
                BasicInstructionFormat.R_FORMAT,
                "000000 sssss ttttt fffff 00000 100110",
                new SimulationCode() 
                {
                   public void simulate(ProgramStatement statement) throws ProcessingException
                  {
                     int[] operands = statement.getOperands();
                     RegisterFile.updateRegister(operands[0],
                        RegisterFile.getValue(operands[1])
                        ^ RegisterFile.getValue(operands[2]));
                  }
               }));

        instructionList.add(
            new BasicInstruction(
                "prioritylock $R0,$R1,$R2",
                "Prioritylock : Set less than",
                BasicInstructionFormat.R_FORMAT,
                "000000 sssss ttttt fffff 00000 101010",
                new SimulationCode() 
                {
                   public void simulate(ProgramStatement statement) throws ProcessingException
                  {
                     int[] operands = statement.getOperands();
                     RegisterFile.updateRegister(operands[0],
                        (RegisterFile.getValue(operands[1])
                        < RegisterFile.getValue(operands[2]))
                                ? 1
                                : 0);
                  }
               }));

        instructionList.add(
            new BasicInstruction(
                "dock $R0",
                "Dock : Jump to address in ($R0)",
                BasicInstructionFormat.R_FORMAT,
                "000000 fffff 00000 00000 00000 001000",
                new SimulationCode() 
                {
                   public void simulate(ProgramStatement statement) throws ProcessingException
                  {
                     int[] operands = statement.getOperands();
                     Globals.instructionSet.processJump(RegisterFile.getValue(operands[0]));
                  }
               }));

        instructionList.add(
            new BasicInstruction(
                "chargeup $R0,$R1,-100",
                "Chargeup : Add immediate to ($R1) and store in ($R0)",
                BasicInstructionFormat.I_FORMAT,
                "001000 sssss fffff tttttttttttttttt",
                new SimulationCode() 
                {
                   public void simulate(ProgramStatement statement) throws ProcessingException
                  {
                     int[] operands = statement.getOperands();
                     int add1 = RegisterFile.getValue(operands[1]);
                     int add2 = operands[2] << 16 >> 16;
                     int sum = add1 + add2;
                  // overflow on A+B detected when A and B have same sign and A+B has other sign.
                     if ((add1 >= 0 && add2 >= 0 && sum < 0)
                        || (add1 < 0 && add2 < 0 && sum >= 0))
                     {
                        throw new ProcessingException(statement,
                            "arithmetic overflow",Exceptions.ARITHMETIC_OVERFLOW_EXCEPTION);
                     }
                     RegisterFile.updateRegister(operands[0], sum);
                  }
               }));

        instructionList.add(
            new BasicInstruction(
                "fullburst $R0,$R1,$R2",
                "Fullburst : Bitwise OR",
                BasicInstructionFormat.R_FORMAT,
                "000000 sssss ttttt fffff 00000 100101",
                new SimulationCode() 
                {
                   public void simulate(ProgramStatement statement) throws ProcessingException
                  {
                     int[] operands = statement.getOperands();
                     RegisterFile.updateRegister(operands[0],
                        RegisterFile.getValue(operands[1])
                        | RegisterFile.getValue(operands[2]));
                  }
               }));

        // --------------------------------------------------------------------
        // Special Gundam Instructions
        // --------------------------------------------------------------------

        instructionList.add(
            new BasicInstruction(
                "lockon $R0,$R1,$R2",
                "Lockon : Set rd and lockon flag if ($R1 == $R2)",
                BasicInstructionFormat.R_FORMAT,
                "000000 sssss ttttt fffff 00000 110001",
                new SimulationCode() {
                    public void simulate(ProgramStatement statement) throws ProcessingException {
                        int[] operands = statement.getOperands();
                        int rs = RegisterFile.getValue(operands[1]);
                        int rt = RegisterFile.getValue(operands[2]);
                        int rdVal;
                        if (rs == rt) {
                            rdVal = 1;
                            setFlag(FLAG_LOCKON, true);
                        } else {
                            rdVal = 0;
                            setFlag(FLAG_LOCKON, false);
                        }
                        RegisterFile.updateRegister(operands[0], rdVal);
                        updateZNFlags(rdVal);
                    }
                }));

        instructionList.add(
            new BasicInstruction(
                "shieldup $R0",
                "Shieldup : Set shield flag from ($R0 != 0)",
                BasicInstructionFormat.R_FORMAT,
                "000000 fffff 00000 00000 00000 110010",
                new SimulationCode() {
                    public void simulate(ProgramStatement statement) throws ProcessingException {
                        int[] operands = statement.getOperands();
                        int val = RegisterFile.getValue(operands[0]);
                        setFlag(FLAG_SHIELD, val != 0);
                    }
                }));

        instructionList.add(
            new BasicInstruction(
                "transam $R0,$R1",
                "Transam : Multiply power by 3 if EN > 0 (and drain EN)",
                BasicInstructionFormat.R_FORMAT,
                "000000 sssss 00000 fffff 00000 110011",
                new SimulationCode() {
                    public void simulate(ProgramStatement statement) throws ProcessingException {
                        int[] operands = statement.getOperands();
                        int rs = RegisterFile.getValue(operands[1]);
                        int en = RegisterFile.getValue(EN_REG);
                        int rdVal;
                        if (en > 0) {
                            rdVal = rs * 3;
                            en--;
                            RegisterFile.updateRegister(EN_REG, en);
                            setFlag(FLAG_TRANSAM, true);
                        } else {
                            rdVal = rs;
                            setFlag(FLAG_TRANSAM, false);
                        }

                        RegisterFile.updateRegister(operands[0], rdVal);
                        updateZNFlags(rdVal);
                        }
                }));

        instructionList.add(
            new BasicInstruction(
                "deploybits $R0,$R1,$R2",
                "Deploybits : rd = rs/rt; clears danger flag if result != 0",
                BasicInstructionFormat.R_FORMAT,
                "000000 sssss ttttt fffff 00000 110100",
                new SimulationCode() {
                    public void simulate(ProgramStatement statement) throws ProcessingException {
                        int[] operands = statement.getOperands();
                        int rs = RegisterFile.getValue(operands[1]);
                        int rt = RegisterFile.getValue(operands[2]);
                        if (rt == 0) {
                            throw new ProcessingException(statement,
                                "Deploybits division by zero in GundamAssembly");
                        }
                        int rdVal = rs / rt;
                        RegisterFile.updateRegister(operands[0], rdVal);
                        updateZNFlags(rdVal);
                        if (rdVal != 0) {
                            setFlag(FLAG_DANGER, false);
                        }
                    }
                }));

        instructionList.add(
            new BasicInstruction(
                "thrust $R0,$R1,-100",
                "Thrust : rt = rs + (imm << 2)",
                BasicInstructionFormat.I_FORMAT,
                "000001 sssss fffff tttttttttttttttt",
                new SimulationCode() {
                    public void simulate(ProgramStatement statement) throws ProcessingException {
                        int[] operands = statement.getOperands();
                        int rs = RegisterFile.getValue(operands[1]);
                        int imm = operands[2] << 16 >> 16;
                        int rtVal = rs + (imm << 2);
                        RegisterFile.updateRegister(operands[0], rtVal);
                        updateZNFlags(rtVal);
                    }
                }));

        instructionList.add(
            new BasicInstruction(
                "energize $R0",
                "Energize : Load EN (energy) from ($R0)",
                BasicInstructionFormat.I_FORMAT,
                "010000 fffff 00000 0000000000000000",
                new SimulationCode() {
                    public void simulate(ProgramStatement statement) throws ProcessingException {
                        int[] operands = statement.getOperands();
                        int val = RegisterFile.getValue(operands[0]);
                        RegisterFile.updateRegister(EN_REG, val);
                    }
                }));

        instructionList.add(
            new BasicInstruction(
                "scan $R0, 0",   // <-- imm is now a *variable* operand
                "Scan : Read internal suit state (0=SR, 1=EN, 2=MS) into ($R0)",
                BasicInstructionFormat.I_FORMAT,
                "010001 00000 fffff ssssssssssssssss",
                new SimulationCode() {
                    public void simulate(ProgramStatement statement) throws ProcessingException {
                        int[] operands = statement.getOperands();
                        int rd  = operands[0];                    // $R0 in prototype
                        int imm = operands[1] << 16 >> 16;        // imm in prototype

                        int val;
                        switch (imm) {
                            case 0:  val = RegisterFile.getValue(SR_REG); break;
                            case 1:  val = RegisterFile.getValue(EN_REG); break;
                            case 2:  val = RegisterFile.getValue(MS_REG); break;
                            default: val = 0; break;
                        }
                        RegisterFile.updateRegister(rd, val);
                        updateZNFlags(val);
                    }
                }));

        instructionList.add(
            new BasicInstruction(
                "autoevade label",
                "Autoevade : Jump to label if danger flag is set",
                BasicInstructionFormat.J_FORMAT,
                "010010 ffffffffffffffffffffffffff",
                new SimulationCode() {
                    public void simulate(ProgramStatement statement) throws ProcessingException {
                        int[] operands = statement.getOperands();
                        if (getFlag(FLAG_DANGER)) {
                            // operands[0] is the 26-bit target field, like 'j'
                            Globals.instructionSet.processJump(
                                (RegisterFile.getProgramCounter() & 0xF0000000)
                                | (operands[0] << 2)
                            );
                        }
                    }
            }));

        instructionList.add(
            new BasicInstruction(
                "callbase label",
                "Callbase : Jump to base routine and set callbase flag (R7 = return address)",
                BasicInstructionFormat.J_FORMAT,
                "111100 ffffffffffffffffffffffffff",
                new SimulationCode() 
                {
                    public void simulate(ProgramStatement statement) throws ProcessingException {
                        int[] operands = statement.getOperands();
                        int pc = RegisterFile.getProgramCounter();
                        int returnAddr = pc + Instruction.INSTRUCTION_LENGTH;

                        // link in R7
                        RegisterFile.updateRegister(LINK_REGISTER, returnAddr);
                        setFlag(FLAG_CALLBASE, true);

                        int target = (pc & 0xF0000000) | (operands[0] << 2);
                        Globals.instructionSet.processJump(target);
                    }
                }));

        instructionList.add(
            new BasicInstruction(
                "boost $R0,$R1",
                "Boost : Double power output (rd = rs << 1)",
                BasicInstructionFormat.R_FORMAT,
                "000000 sssss 00000 fffff 00000 110000",
                new SimulationCode() {
                    public void simulate(ProgramStatement statement) throws ProcessingException {
                        int[] operands = statement.getOperands();
                        int rs = RegisterFile.getValue(operands[1]);
                        int rdVal = rs << 1;
                        RegisterFile.updateRegister(operands[0], rdVal);
                        updateZNFlags(rdVal);
                    }
                }));

        // --------------------------------------------------------------------
        // Extra instructions used in PDF examples (not in the main table)
        // --------------------------------------------------------------------

        instructionList.add(
            new BasicInstruction(
                "sensorlock $R0,$R1,$R2",
                "Sensorlock : Bitwise AND (rd = rs & rt)",
                BasicInstructionFormat.R_FORMAT,
                "000000 sssss ttttt fffff 00000 100100",
                new SimulationCode() 
                {
                   public void simulate(ProgramStatement statement) throws ProcessingException
                  {
                     int[] operands = statement.getOperands();
                     RegisterFile.updateRegister(operands[0],
                        RegisterFile.getValue(operands[1])
                        & RegisterFile.getValue(operands[2]));
                  }
               }));

        instructionList.add(
            new BasicInstruction(
                "formup $R0,$R1,label",
                "Formup : Branch to label if ($R0 == $R1)",
                BasicInstructionFormat.I_BRANCH_FORMAT,
                "000100 fffff sssss tttttttttttttttt",
                new SimulationCode() 
                {
                   public void simulate(ProgramStatement statement) throws ProcessingException
                  {
                     int[] operands = statement.getOperands();
                  
                     if (RegisterFile.getValue(operands[0])
                        == RegisterFile.getValue(operands[1]))
                     {
                        Globals.instructionSet.processBranch(operands[2]);
                     }
                  }
               }));

        instructionList.add(
            new BasicInstruction(
                "hangarload $R0,100($R1)",
                "Hangarload : Load word (like lw)",
                BasicInstructionFormat.I_FORMAT,
                "100011 sssss fffff tttttttttttttttt",
                new SimulationCode() 
                {
                   public void simulate(ProgramStatement statement) throws ProcessingException {
                        int[] operands = statement.getOperands();
                        try {
                            int base   = RegisterFile.getValue(operands[2]);
                            int offset = operands[1] << 16 >> 16;   // sign-extend 16-bit imm
                            int addr   = base + offset;

                            int value = Globals.memory.getWord(addr);
                            RegisterFile.updateRegister(operands[0], value);
                            updateZNFlags(value);
                        } catch (AddressErrorException e) {
                            throw new ProcessingException(statement, e);
                        }
}
               }));

        instructionList.add(
            new BasicInstruction(
                "jumpbase label",
                "Jumpbase : Unconditional jump to label",
                BasicInstructionFormat.J_FORMAT,
                "000010 ffffffffffffffffffffffffff",
                new SimulationCode() 
                {
                   public void simulate(ProgramStatement statement) throws ProcessingException
                  {
                     int[] operands = statement.getOperands();
                     Globals.instructionSet.processJump(
                        ((RegisterFile.getProgramCounter() & 0xF0000000)
                                | (operands[0] << 2)));            
                  }
               }));

               instructionList.add(
                new BasicInstruction("syscall", 
            	 "Issue a system call : Execute the system call specified by value in $v0",
            	 BasicInstructionFormat.R_FORMAT,
                "000000 00000 00000 00000 00000 001100",
                new SimulationCode()
               {
                   public void simulate(ProgramStatement statement) throws ProcessingException
                  {
                     Globals.instructionSet.findAndSimulateSyscall(RegisterFile.getValue(2),statement);
                  }
               }));
    }
}
