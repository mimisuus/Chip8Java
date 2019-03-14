package com.company;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.ThreadLocalRandom;

//
// Used Cowgod's technical reference sheet in the making of this emulator
// http://devernay.free.fr/hacks/chip8/C8TECH10.HTM
//

public class Chip8 extends JFrame implements ActionListener {
    private int[] memory = new int[4096];
    private byte[] byteArray = new byte[3584];
    private short delayTimer;
    private int programCounter;
    private int l = 0, stackPointer = 0;
    private short[] stack = new short[16];
    private int[] dataRegister = new int[16];
    private int windowWidth = 64;
    private int windowHeight = 32;
    private short keyHeld;
    private boolean flipped;
    private javax.swing.Timer timer;
    private int decrement = 0;
    private Image doubleBuffer;
    private Graphics doubleBufferG;
    //2D Array representing each pixel on the screen
    private byte[][] screenGrid = new byte[windowHeight][windowWidth];
    private File rom;


    public void paint(Graphics g){
        //double buffer to reduce flicker
        doubleBuffer = createImage(650, 350);
        doubleBufferG = doubleBuffer.getGraphics();
        paintComponent(doubleBufferG);
        g.drawImage(doubleBuffer, 0, 0,this);
    }

    public void paintComponent(Graphics g){
        //Draw each pixel as either white ON "1" or black OFF "0"
        for(int h = 0; h < windowHeight; h++){
            for(int w = 0; w < windowWidth; w++){
                if(screenGrid[h][w] == 1){
                    g.setColor(Color.WHITE);
                    g.fillRect(w*10 + 10,h*10 + 30,10,10);
                } else {
                    g.setColor(Color.BLACK);
                    g.fillRect(w*10 + 10,h *10 + 30,10,10);
                }
            }
        }
    }

    public Chip8() throws IOException {
        setSize(windowWidth * 10 + 20, windowHeight * 10 + 35);
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
        } catch (Exception e){
            System.out.print(e);
        }
        timer = new javax.swing.Timer(2, this);
        timer.start();
        loadFont();
        addKeyListener(new KeyInput(this));
        this.programCounter = 0x200;
        loadFont();
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
                            dataRegister[(opcode & 0x0F00) >> 8] += dataRegister[(opcode & 0x00F0) >> 4];
                            dataRegister[(opcode & 0x0F00) >> 8] %= 0xFF;
                            dataRegister[0xF] = 1;
                        } else {
                            dataRegister[(opcode & 0x0F00) >> 8] += dataRegister[(opcode & 0x00F0) >> 4];
                            dataRegister[0xF] = 0;
                        }
                        break;
                    //8XY5
                    case(0x0005):
                        if(dataRegister[(opcode & 0x0F00) >> 8] < dataRegister[(opcode & 0x00F0) >> 4]){
                            dataRegister[(opcode & 0x0F00) >> 8] -= dataRegister[(opcode & 0x00F0) >> 4];
                            dataRegister[(opcode & 0x0F00) >> 8] &= 0xFF;
                            dataRegister[0xF] = 0;
                        } else {
                            dataRegister[(opcode & 0x0F00) >> 8] -= dataRegister[(opcode & 0x00F0) >> 4];
                            dataRegister[0xF] = 1;
                        }
                        break;
                    //8XY6
                    case(0x0006):
                        dataRegister[0xF] = dataRegister[(opcode & 0x0F00) >> 8] & 0b1;
                        dataRegister[(opcode & 0x0F00) >> 8] >>= 1;
                        break;
                    //8XY7
                    case(0x0007):
                        if(dataRegister[(opcode & 0x00F0) >> 4] < dataRegister[(opcode & 0x0F00) >> 8]){
                            dataRegister[(opcode & 0x0F00) >> 8] = dataRegister[(opcode & 0x00F0) >> 4] - dataRegister[(opcode & 0x0F00) >> 8];
                            dataRegister[(opcode & 0x0F00) >> 8] &= 0xFF;
                            dataRegister[0xF] = 0;
                        } else {
                            dataRegister[(opcode & 0x0F00) >> 8] = dataRegister[(opcode & 0x00F0) >> 4] - dataRegister[(opcode & 0x0F00) >> 8];
                            dataRegister[0xF] = 1;
                        }
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
                l = opcode & 0x0FFF;
                break;
                //BNNN
            case(0xB000):
                programCounter = dataRegister[0] + (opcode & 0x0FFF);
                programCounter -= 2;
                break;
                //CXNN
            case(0xC000):
                int random = ThreadLocalRandom.current().nextInt(0,255+1);
                dataRegister[(opcode & 0x0F00) >> 8] = random & (opcode & 0x00FF);
                break;
                //DXYN
            case(0xD000):
                flipped = false;
                int startX = dataRegister[(opcode & 0x0F00) >> 8];
                int startY = dataRegister[(opcode & 0x00F0) >> 4];
                int height = opcode & 0x000F;
                for(int h = 0; h < height; h++){
                    for(int w = 0; w < 8; w++){
                        if((screenGrid[(startY + h) % windowHeight][(startX + w) % windowWidth]) * ((memory[l+h] >> (7-w)) & 0b1) == 1){
                            flipped = true;
                        }
                        screenGrid[(startY + h) % windowHeight][(startX + w) % windowWidth] ^= ((memory[l+h] >> (7-w)) & 0b1);
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
                        dataRegister[(opcode & 0x0F00) >> 8] = this.keyHeld;
                        break;
                        //FX15
                    case(0x0015):
                        delayTimer = (short)dataRegister[(opcode & 0x0F00) >> 8];
                        break;
                        //FX18
                    case(0x0018):
                        break;
                        //FX1E
                    case(0x001E):
                        l += dataRegister[(opcode & 0x0F00) >> 8];
                        l &= 0xFFFF;
                        break;
                        //FX29
                    case(0x0029):
                        l = dataRegister[(opcode & 0x0F00) >> 8] * 5;
                        break;
                        //FX33
                    case(0x0033):
                        memory[l] = (dataRegister[(opcode & 0x0F00) >> 8] / 100);
                        memory[l+1] = ((dataRegister[(opcode & 0x0F00) >> 8] / 10) % 10);
                        memory[l+2] = (dataRegister[(opcode & 0x0F00) >> 8] % 10);
                        break;
                        //FX55
                    case(0x0055):
                        for(int i=0; i <= ((opcode & 0x0F00) >> 8); i++){
                            memory[l+i] = dataRegister[i];
                        }
                        l += 1 + ((opcode & 0x0F00) >> 8);
                        break;
                        //FX65
                    case(0x0065):
                        for(int i=0; i <= ((opcode & 0x0F00) >> 8); i++){
                            dataRegister[i] = memory[l+i];
                        }
                        l += 1 + ((opcode & 0x0F00) >> 8);
                        break;
                }
                break;
        }
    }

    short[] font = new short[]{
            // Each line represents a sprite
            // of a number between 0-F
            0xF0,0x90,0x90,0x90,0xF0,
            0x20,0x60,0x20,0x20,0x70,
            0xF0,0x10,0xF0,0x80,0xF0,
            0xF0,0x10,0xF0,0x10,0x10,
            0x90,0x90,0xF0,0x10,0x10,
            0xF0,0x80,0xF0,0x10,0xF0,
            0xF0,0x80,0xF0,0x90,0xF0,
            0xF0,0x10,0x20,0x40,0x40,
            0xF0,0x90,0xF0,0x90,0xF0,
            0xF0,0x90,0xF0,0x10,0xF0,
            0xF0,0x90,0xF0,0x90,0x90,
            0xE0,0x90,0xE0,0x90,0xE0,
            0xF0,0x80,0x80,0x80,0xF0,
            0xE0,0x90,0x90,0x90,0xE0,
            0xF0,0x80,0xF0,0x80,0xF0,
            0xF0,0x80,0xF0,0x80,0x80
    };


    // The program doesn't write to the first 0x200
    // addresses of memory, so it is often used
    // for storing the chip-8 font
    public void loadFont(){
        for (int i = 0; i < font.length; i++){
            memory[i] = font[i];
        }
    }

    public void actionPerformed(ActionEvent e){
            if (delayTimer > 0 && decrement == 0) {
                --delayTimer;
                repaint();
            }
            decrement++;
            decrement %= 8;
            Opcode();
            programCounter += 2;
    }
    /*The chip8 keypad is mapped in the following way.
      We must remap the keyboard according to their
      relative position on the chip8 keypad
         CHIP-8             PC KEYBOARD
        |1|2|3|C|            |1|2|3|4|
        ---------            ---------
        |4|5|6|D|            |Q|W|E|R|
        ---------            ---------
        |7|8|9|E|            |A|S|D|F|
        ---------            ---------
        |A|0|B|F|            |Z|X|C|V|
    */

    public void keyPressed(KeyEvent e){
        switch(e.getKeyCode()){
            case(KeyEvent.VK_1):
                this.keyHeld = 0x1;
                break;
            case(KeyEvent.VK_2):
                this.keyHeld = 0x2;
                break;
            case(KeyEvent.VK_3):
                this.keyHeld = 0x3;
                break;
            case(KeyEvent.VK_4):
                this.keyHeld = 0xC;
                break;
            case(KeyEvent.VK_Q):
                this.keyHeld = 0x4;
                break;
            case(KeyEvent.VK_W):
                this.keyHeld = 0x5;
                break;
            case(KeyEvent.VK_E):
                this.keyHeld = 0x6;
                break;
            case(KeyEvent.VK_R):
                this.keyHeld = 0xD;
                break;
            case(KeyEvent.VK_A):
                this.keyHeld = 0x7;
                break;
            case(KeyEvent.VK_S):
                this.keyHeld = 0x8;
                break;
            case(KeyEvent.VK_D):
                this.keyHeld = 0x9;
                break;
            case(KeyEvent.VK_F):
                this.keyHeld = 0xE;
                break;
            case(KeyEvent.VK_Z):
                this.keyHeld = 0xA;
                break;
            case(KeyEvent.VK_X):
                this.keyHeld = 0x0;
                break;
            case(KeyEvent.VK_C):
                this.keyHeld = 0xB;
                break;
            case(KeyEvent.VK_V):
                this.keyHeld = 0xF;
                break;

        }
    }

    public void keyReleased(KeyEvent e){
        this.keyHeld = 0xFF;
    }
}
