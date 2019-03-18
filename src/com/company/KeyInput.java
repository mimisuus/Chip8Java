package com.company;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class KeyInput extends KeyAdapter {
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
                Chip8.keyHeld = 0x1;
                break;
            case(KeyEvent.VK_2):
                Chip8.keyHeld = 0x2;
                break;
            case(KeyEvent.VK_3):
                Chip8.keyHeld = 0x3;
                break;
            case(KeyEvent.VK_4):
                Chip8.keyHeld = 0xC;
                break;
            case(KeyEvent.VK_Q):
                Chip8.keyHeld = 0x4;
                break;
            case(KeyEvent.VK_W):
                Chip8.keyHeld = 0x5;
                break;
            case(KeyEvent.VK_E):
                Chip8.keyHeld = 0x6;
                break;
            case(KeyEvent.VK_R):
                Chip8.keyHeld = 0xD;
                break;
            case(KeyEvent.VK_A):
                Chip8.keyHeld = 0x7;
                break;
            case(KeyEvent.VK_S):
                Chip8.keyHeld = 0x8;
                break;
            case(KeyEvent.VK_D):
                Chip8.keyHeld = 0x9;
                break;
            case(KeyEvent.VK_F):
                Chip8.keyHeld = 0xE;
                break;
            case(KeyEvent.VK_Z):
                Chip8.keyHeld = 0xA;
                break;
            case(KeyEvent.VK_X):
                Chip8.keyHeld = 0x0;
                break;
            case(KeyEvent.VK_C):
                Chip8.keyHeld = 0xB;
                break;
            case(KeyEvent.VK_V):
                Chip8.keyHeld = 0xF;
                break;

        }
    }

    public void keyReleased(KeyEvent e){
        Chip8.keyHeld = 0xFF;
    }
}