package com.company;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class KeyInput extends KeyAdapter {
    Chip8 chip8;
    public KeyInput(Chip8 chip8){
        this.chip8 = chip8;
    }
    public void keyPressed(KeyEvent e){
        chip8.keyPressed(e);
    }
    public void keyReleased(KeyEvent e) { chip8.keyReleased(e);}
}