/*
 * Copyright (C) 2017 Kasirgalabs
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.kasirgalabs.etumulator.lang;

import com.kasirgalabs.arm.AssemblerBaseVisitor;
import com.kasirgalabs.arm.AssemblerLexer;
import com.kasirgalabs.arm.AssemblerParser;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.TerminalNode;

public class Linker extends AssemblerBaseVisitor<Void> {
    private final Map<String, Integer> definedBranches;
    private final Map<String, Data> definedData;
    private final Set<Integer> addressBook;
    private boolean secondPass;
    private char[][] code;

    public Linker() {
        definedBranches = new HashMap<>(16);
        definedData = new HashMap<>(16);
        addressBook = new HashSet<>(16);
    }

    @Override
    public Void visitB(AssemblerParser.BContext ctx) {
        replaceLabelAddress(ctx, ctx.LABEL());
        return null;
    }

    @Override
    public Void visitBeq(AssemblerParser.BeqContext ctx) {
        replaceLabelAddress(ctx, ctx.LABEL());
        return null;
    }

    @Override
    public Void visitBne(AssemblerParser.BneContext ctx) {
        replaceLabelAddress(ctx, ctx.LABEL());
        return null;
    }

    @Override
    public Void visitBcs(AssemblerParser.BcsContext ctx) {
        replaceLabelAddress(ctx, ctx.LABEL());
        return null;
    }

    @Override
    public Void visitBhs(AssemblerParser.BhsContext ctx) {
        replaceLabelAddress(ctx, ctx.LABEL());
        return null;
    }

    @Override
    public Void visitBcc(AssemblerParser.BccContext ctx) {
        replaceLabelAddress(ctx, ctx.LABEL());
        return null;
    }

    @Override
    public Void visitBlo(AssemblerParser.BloContext ctx) {
        replaceLabelAddress(ctx, ctx.LABEL());
        return null;
    }

    @Override
    public Void visitBmi(AssemblerParser.BmiContext ctx) {
        replaceLabelAddress(ctx, ctx.LABEL());
        return null;
    }

    @Override
    public Void visitBpl(AssemblerParser.BplContext ctx) {
        replaceLabelAddress(ctx, ctx.LABEL());
        return null;
    }

    @Override
    public Void visitBvs(AssemblerParser.BvsContext ctx) {
        replaceLabelAddress(ctx, ctx.LABEL());
        return null;
    }

    @Override
    public Void visitBvc(AssemblerParser.BvcContext ctx) {
        replaceLabelAddress(ctx, ctx.LABEL());
        return null;
    }

    @Override
    public Void visitBhi(AssemblerParser.BhiContext ctx) {
        replaceLabelAddress(ctx, ctx.LABEL());
        return null;
    }

    @Override
    public Void visitBls(AssemblerParser.BlsContext ctx) {
        replaceLabelAddress(ctx, ctx.LABEL());
        return null;
    }

    @Override
    public Void visitBge(AssemblerParser.BgeContext ctx) {
        replaceLabelAddress(ctx, ctx.LABEL());
        return null;
    }

    @Override
    public Void visitBlt(AssemblerParser.BltContext ctx) {
        replaceLabelAddress(ctx, ctx.LABEL());
        return visitChildren(ctx);
    }

    @Override
    public Void visitBgt(AssemblerParser.BgtContext ctx) {
        replaceLabelAddress(ctx, ctx.LABEL());
        return null;
    }

    @Override
    public Void visitBle(AssemblerParser.BleContext ctx) {
        replaceLabelAddress(ctx, ctx.LABEL());
        return null;
    }

    @Override
    public Void visitBal(AssemblerParser.BalContext ctx) {
        replaceLabelAddress(ctx, ctx.LABEL());
        return null;
    }

    @Override
    public Void visitBl(AssemblerParser.BlContext ctx) {
        String label = ctx.LABEL().getText();
        if("uart_read".equalsIgnoreCase(label) | "uart_write".equalsIgnoreCase(label)) {
            return null;
        }
        replaceLabelAddress(ctx, ctx.LABEL());
        return null;
    }

    @Override
    public Void visitRelocationDirective(AssemblerParser.RelocationDirectiveContext ctx) {
        if(!secondPass) {
            return null;
        }
        String label = ctx.LABEL().getText();
        if(!definedData.containsKey(label)) {
            throw new LabelError("\"" + label + "\" is not defined.");
        }
        int lineNumber = ctx.start.getLine() - 1;
        String temp = new String(code[lineNumber]);
        String address = Integer.toString(definedData.get(label).getAddress());
        temp = temp.replace(label, "#" + address);
        code[lineNumber] = temp.toCharArray();
        return null;
    }

    @Override
    public Void visitLabel(AssemblerParser.LabelContext ctx) {
        if(secondPass) {
            return null;
        }
        String label = ctx.LABEL().getText();
        if(definedData.containsKey(label) || definedData.containsKey(label)) {
            throw new LabelError("\"" + label + "\" is already defined.");
        }
        int address = ctx.start.getLine() - 1;
        definedBranches.put(label, address);
        return null;
    }

    @Override
    public Void visitData(AssemblerParser.DataContext ctx) {
        if(secondPass) {
            return null;
        }
        String label = ctx.LABEL().getText();
        if(definedBranches.containsKey(label) || definedData.containsKey(label)) {
            throw new LabelError("\"" + label + "\" is already defined.");
        }
        String asciz = ctx.asciz().STRING().getText().replaceAll("\"", "") + "\0";
        int address = generateAddress(asciz);
        definedData.put(label, new Data(asciz, address));
        return null;
    }

    public ExecutableCode link(String code) throws SyntaxError, LabelError {
        definedBranches.clear();
        definedData.clear();
        addressBook.clear();
        secondPass = false;
        this.code = parseCode(code);
        ANTLRInputStream in = new ANTLRInputStream(code);
        AssemblerLexer lexer = new AssemblerLexer(in);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        AssemblerParser parser = new AssemblerParser(tokens);
        AssemblerParser.ProgContext program = parser.prog();
        if(parser.getNumberOfSyntaxErrors() > 0) {
            throw new SyntaxError("You have error(s) in your code.");
        }
        visit(program);
        secondPass = true;
        visit(program);
        List<Data> temp = new ArrayList<>(definedData.size());
        definedData.forEach((label, data) -> {
            temp.add(data);
        });
        return new ExecutableCode(this.code, temp);
    }

    private char[][] parseCode(String code) {
        String[] parts = code.split("\\n");
        char[][] instructions = new char[parts.length][];
        for(int i = 0; i < instructions.length; i++) {
            instructions[i] = (parts[i] + "\n").toCharArray();
        }
        return instructions;
    }

    private void replaceLabelAddress(ParserRuleContext ctx, TerminalNode terminalNode) {
        if(!secondPass) {
            return;
        }
        String label = terminalNode.getText();
        if(!definedBranches.containsKey(label)) {
            throw new LabelError("\"" + label + "\" is not defined.");
        }
        int lineNumber = ctx.start.getLine() - 1;
        String temp = new String(code[lineNumber]);
        String address = Integer.toString(definedBranches.get(label));
        temp = temp.replace(label, address);
        code[lineNumber] = temp.toCharArray();
    }

    private int generateAddress(String data) {
        boolean addressNotFound = true;
        Random rand = new Random();
        int address = 0;
        while(addressNotFound) {
            address = rand.nextInt(Integer.MAX_VALUE - data.length());
            for(int i = 0; i < data.length(); i++) {
                if(addressBook.contains(address + i)) {
                    break;
                }
                if(i == data.length() - 1) {
                    addressNotFound = false;
                }
            }
        }
        return address;
    }

    public static class ExecutableCode {
        private final char[][] code;
        private final List<Data> data;

        private ExecutableCode(char[][] code, List<Data> data) {
            this.code = new char[code.length][];
            for(int i = 0; i < code.length; i++) {
                this.code[i] = new char[code[i].length];
                System.arraycopy(code[i], 0, this.code[i], 0, code[i].length);
            }
            this.data = new ArrayList<>(data.size());
            for(int i = 0; i < data.size(); i++) {
                this.data.add(new Data(data.get(i)));
            }
        }

        public char[][] getCode() {
            char[][] temp = new char[code.length][];
            for(int i = 0; i < code.length; i++) {
                temp[i] = new char[code[i].length];
                System.arraycopy(code[i], 0, temp[i], 0, code[i].length);
            }
            return temp;
        }

        public List<Data> getData() {
            List<Data> temp = new ArrayList<>(data.size());
            for(int i = 0; i < data.size(); i++) {
                temp.add(new Data(data.get(i)));
            }
            return temp;
        }
    }
}