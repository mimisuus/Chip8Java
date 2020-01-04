package com.company;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.ThreadLocalRandom;
import javax.sound.midi.*;

//
// Used Cowgod's technical reference sheet in the making of this emulator
// http://devernay.free.fr/hacks/chip8/C8TECH10.HTM
//

public class Chip8 extends JFrame implements ActionListener {
    public static int[] memory = new int[4096];
    //used for loading roms to system memory above
    private byte[] byteArray = new byte[3584];
    //used to track progress of each timer
    private static short delayTimer, soundTimer;
    private static int programCounter;
    private int I = 0, stackPointer = 0;
    private short[] stack = new short[16];
    private int[] dataRegister = new int[16];
    private final int windowWidth = 64;
    private final int windowHeight = 32;
    public static short keyHeld;
    //flag to see if draw function flipped any pixels last time
    private boolean flipped;
    //Timer used for delay and sound timer
    private javax.swing.Timer timer;
    private int decrement = 0;
    //Color for each ON and OFF pixel
    private Color light = new Color(222, 228, 231, 255);
    private Color dark = new Color(55, 71, 79, 255);
    //Scale for each pixel
    private final int scale = 10;
    //2D Array representing each pixel on the screen
    private byte[][] screenGrid = new byte[windowHeight][windowWidth];
    //Sound channels
    private MidiChannel[] channels;
    private File rom;


    public void paint(Graphics g){
        //double buffer to reduce flicker
        Image doubleBuffer = createImage(650, 350);
        Graphics doubleBufferG = doubleBuffer.getGraphics();
        paintComponent(doubleBufferG);
        g.drawImage(doubleBuffer, 0, 0,this);
    }

    public void paintComponent(Graphics g){
        //Set the screen black, then paint every pixel that is 1
        g.setColor(dark);
        g.fillRect(0,0, windowWidth * scale + 10, windowHeight * scale + 30);
        g.setColor(light);
        for(int h = 0; h < windowHeight; h++){
            for(int w = 0; w < windowWidth; w++){
                if(screenGrid[h][w] == 1){
                    g.fillRect(w * scale + 10,h * scale + 30,10,10);
                } //else {
                    //g.setColor(dark);
                    //g.fillRect(w * scale + 10,h * scale + 30,10,10);
                //}
            }
        }
    }

    public Chip8() throws IOException {
        setSize(windowWidth * scale + 20, windowHeight * scale + 35);
        setTitle("Chip 8");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);
        JFileChooser chooser = new JFileChooser();
            int returnValue = chooser.showOpenDialog(getParent());
            if (returnValue == JFileChooser.APPROVE_OPTION) {
                rom = chooser.getSelectedFile();
                setTitle("Chip 8 - " + rom.getName());
            }
        try {
            // Easiest way to read a byte at a time from a file
            // Is to do it to a byteArray, but bytes in Java
            // are always signed. So I convert them to positive
            // integers in the fromByteToIntArray() function
            FileInputStream romStream = new FileInputStream(rom);
            romStream.read(byteArray);
            romStream.close();
            // Implement sound, which is just a single
            // beep when the soundTimer isn't 0
            Synthesizer synthesizer = MidiSystem.getSynthesizer();
            synthesizer.open();
            Instrument[] instruments = synthesizer.getDefaultSoundbank().getInstruments();
            channels = synthesizer.getChannels();
            channels[0].programChange(instruments[4].getPatch().getProgram());
        } catch (Exception e){
            System.out.print(e);
        }
        timer = new javax.swing.Timer(1, this);
        timer.start();
        Font.loadFont();
        addKeyListener(new KeyInput());
        programCounter = 0x200;
        fromByteToIntArray();
    }

    public void fromByteToIntArray(){
        for(int i = 0; i < byteArray.length; i++){
            memory[programCounter + i ] = byteArray[i] & 0xFF;
        }
    }

    public void Opcode(){
        // Each opcode is 2 bytes long and represented with a hexadecimal
        // First byte of each instruction is at an even address in memory
        // The meaning behind each opcode can be found in Cowgod's chip-8
        // reference.
        int opcode = (memory[programCounter] << 8 | memory[programCounter+1]);
        switch(opcode & 0xF000){
            //00E-
            case(0x000):
                switch(opcode & 0x000F){
                    //00E0
                    case(0x0000):
                        for(int h = 0; h < windowHeight; h++) {
                            for (int w = 0; w < windowWidth; w++) {
                                screenGrid[h][w] = 0;
                            }
                        }
                        break;
                    //00EE
                    case(0x000E):
                        programCounter = stack[stackPointer];
                        --stackPointer;
                        break;
                }
                break;
                //1NNN
            case(0x1000):
                    programCounter = opcode & 0x0FFF;
                    programCounter -= 2;
                    break;
                //2NNN
            case(0x2000):
                    ++stackPointer;
                    stack[stackPointer] = (short)programCounter;
                    programCounter = opcode & 0x0FFF;
                    programCounter -= 2;
                    break;
                //3XNN
            case(0x3000):
                if(dataRegister[(opcode & 0x0F00) >> 8] == (opcode & 0x00FF)){
                    programCounter+=2;
                }
                break;
                //4XNN
            case(0x4000):
                if(dataRegister[(opcode & 0x0F00) >> 8] != (opcode & 0x00FF)){
                    programCounter+=2;
                }
                break;
                //5XYO
            case(0x5000):
                if(dataRegister[(opcode & 0x0F00) >> 8] == dataRegister[(opcode & 0x00F0) >> 4]){
                    programCounter+=2;
                }
                break;
                //6XNN
            case(0x6000):
                dataRegister[(opcode & 0x0F00) >> 8] = opcode & 0x00FF;
                break;
                //7XNN
            case(0x7000):
                dataRegister[(opcode & 0x0F00) >> 8] += opcode & 0x00FF;
                dataRegister[(opcode & 0x0F00) >> 8] &= 0xFF;
                break;
                //8XY-
            case(0x8000):
                switch(opcode & 0x000F){
                    //8XY0
                    case(0x0000):
                        dataRegister[(opcode & 0x0F00) >> 8] = dataRegister[(opcode & 0x00F0) >> 4];
                    //8XY1
                    case(0x0001):
                        dataRegister[(opcode & 0x0F00) >> 8] |= dataRegister[(opcode & 0x00F0) >> 4];
                        break;
                    //8XY2
                    case(0x0002):
                        dataRegister[(opcode & 0x0F00) >> 8] &= dataRegister[(opcode & 0x00F0) >> 4];
                        break;
                    //8XY3
                    case(0x0003):
                        dataRegister[(opcode & 0x0F00) >> 8] ^= dataRegister[(opcode & 0x00F0) >> 4];
                        break;
                    //8XY4
                    case(0x0004):
                        if((dataRegister[(opcode & 0x0F00) >> 8] + dataRegister[(opcode & 0x00F0) >> 4]) > 0xFF){
                            dataRegister[0xF] = 1;
                        } else {
                            dataRegister[0xF] = 0;
                        }
                        dataRegister[(opcode & 0x0F00) >> 8] += dataRegister[(opcode & 0x00F0) >> 4];
                        dataRegister[(opcode & 0x0F00) >> 8] &= 0xFF;
                        break;
                    //8XY5
                    case(0x0005):
                        if(dataRegister[(opcode & 0x0F00) >> 8] < dataRegister[(opcode & 0x00F0) >> 4]){
                            dataRegister[0xF] = 0;
                        } else {
                            dataRegister[0xF] = 1;
                        }
                        dataRegister[(opcode & 0x0F00) >> 8] -= dataRegister[(opcode & 0x00F0) >> 4];
                        dataRegister[(opcode & 0x0F00) >> 8] &= 0xFF;
                        break;
                    //8XY6
                    case(0x0006):
                        dataRegister[0xF] = dataRegister[(opcode & 0x0F00) >> 8] & 0b1;
                        dataRegister[(opcode & 0x0F00) >> 8] >>= 1;
                        break;
                    //8XY7
                    case(0x0007):
                        if(dataRegister[(opcode & 0x00F0) >> 4] < dataRegister[(opcode & 0x0F00) >> 8]){
                            dataRegister[0xF] = 0;
                        } else {
                            dataRegister[0xF] = 1;
                        }
                        dataRegister[(opcode & 0x0F00) >> 8] = dataRegister[(opcode & 0x00F0) >> 4] - dataRegister[(opcode & 0x0F00) >> 8];
                        dataRegister[(opcode & 0x0F00) >> 8] &= 0xFF;
                        break;
                    case(0x000E):
                        dataRegister[0xF] = dataRegister[(opcode & 0x0F00) >> 8] >> 7;
                        dataRegister[(opcode & 0x0F00) >> 8] <<= 1;
                        break;
                }
                break;
                //9XY0
            case(0x9000):
                if(dataRegister[(opcode & 0x0F00) >> 8] != dataRegister[(opcode & 0x00F0) >> 4]){
                    programCounter+=2;
                }
                break;
                //ANNN
            case(0xA000):
                I = opcode & 0x0FFF;
                break;
                //BNNN
            case(0xB000):
                programCounter = dataRegister[0] + (opcode & 0x0FFF);
                programCounter -= 2;
                break;
                //CXNN
            case(0xC000):
                dataRegister[(opcode & 0x0F00) >> 8] = ThreadLocalRandom.current().nextInt(1,255) & (opcode & 0x00FF);
                break;
                //DXYN
            case(0xD000):
                flipped = false;
                int startX = dataRegister[(opcode & 0x0F00) >> 8];
                int startY = dataRegister[(opcode & 0x00F0) >> 4];
                int height = opcode & 0x000F;
                for(int h = 0; h < height; h++){
                    for(int w = 0; w < 8; w++){
                        // Goes through each bit of the specified memory address & each pixel at the x,y coordinate of the opcode is xor'd with them.
                        // If they are on they are set back to off then the flipped flag is set to true
                        if((screenGrid[(startY + h) % windowHeight][(startX + w) % windowWidth]) * ((memory[I +h] >> (7-w)) & 0b1) == 1){
                            flipped = true;
                        }
                        screenGrid[(startY + h) % windowHeight][(startX + w) % windowWidth] ^= ((memory[I +h] >> (7-w)) & 0b1);
                    }
                }
                if(flipped){
                    dataRegister[0xF] = 1;
                } else {
                    dataRegister[0xF] = 0;
                }
                break;
                //EX--
            case(0xE000):
                switch(opcode & 0x000F){
                    //EX9E
                    case(0x000E):
                        if(keyHeld == dataRegister[(opcode & 0x0F00) >> 8]){
                            programCounter+=2;
                        }
                        break;
                    //EXA1
                    case(0x0001):
                        if(keyHeld != dataRegister[(opcode & 0x0F00) >> 8]){
                            programCounter+=2;
                        }
                        break;
                }
                break;
                //FX--
            case(0xF000):
                switch(opcode & 0x00FF){
                    //FX07
                    case(0x0007):
                        dataRegister[(opcode & 0x0F00) >> 8] = delayTimer;
                        break;
                    //FX0A
                    case(0x000A):
                        if(keyHeld == 0xFF){
                            programCounter-=2;
                        }
                        dataRegister[(opcode & 0x0F00) >> 8] = keyHeld;
                        break;
                        //FX15
                    case(0x0015):
                        delayTimer = (short)dataRegister[(opcode & 0x0F00) >> 8];
                        break;
                        //FX18
                    case(0x0018):
                        soundTimer = (short)dataRegister[(opcode & 0x0F00) >> 8];
                        break;
                        //FX1E
                    case(0x001E):
                        I += dataRegister[(opcode & 0x0F00) >> 8];
                        I &= 0xFFFF;
                        break;
                        //FX29
                    case(0x0029):
                        I = dataRegister[(opcode & 0x0F00) >> 8] * 5;
                        break;
                        //FX33
                    case(0x0033):
                        memory[I] = (dataRegister[(opcode & 0x0F00) >> 8] / 100);
                        memory[I +1] = ((dataRegister[(opcode & 0x0F00) >> 8] / 10) % 10);
                        memory[I +2] = (dataRegister[(opcode & 0x0F00) >> 8] % 10);
                        break;
                        //FX55
                    case(0x0055):
                        for(int i=0; i <= ((opcode & 0x0F00) >> 8); i++){
                            memory[I +i] = dataRegister[i];
                        }
                        I += 1 + ((opcode & 0x0F00) >> 8);
                        break;
                        //FX65
                    case(0x0065):
                        for(int i=0; i <= ((opcode & 0x0F00) >> 8); i++){
                            dataRegister[i] = memory[I +i];
                        }
                        I += 1 + ((opcode & 0x0F00) >> 8);
                        break;
                }
                break;
        }
    }


    public void actionPerformed(ActionEvent e) {
        if (soundTimer > 0) {
            channels[0].noteOn(70, 20);
            --soundTimer;
        }
            if (delayTimer > 0 && decrement == 0) {
                --delayTimer;
            }
            decrement++;
            decrement %= 16;
            repaint();
            Opcode();
            programCounter += 2;
    }
}
