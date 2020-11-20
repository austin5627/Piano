/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package piano;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

/**
 *
 * @author ah91099
 */
public class Key{
    private final char note;
    private final char accidental;
    private final int octave;
    private final int number;
    private int whiteNumber;
    private final double freqency;
    public static final int WHITEKEYWIDTH = 75;
    public static final int WHITEKEYHEIGHT = 300;
    public static final int BLACKKEYWIDTH = WHITEKEYWIDTH/2;
    public static final int BLACKKEYHEIGHT = WHITEKEYHEIGHT/3*2;
    private boolean playing;
    private int button = 0;
    private int x,y = 0;
    
    
    public Key(int number){
        this.number=number;
        this.freqency=27.5*Math.pow(Math.pow(2, 1.0/12.0), number-1);
        playing = false;

        switch (number%12){
            case 1: note = 'A'; accidental='N'; break;
            case 2: note = 'A'; accidental='#'; break;
            case 3: note = 'B'; accidental='N'; break;
            case 4: note = 'C'; accidental='N'; break;
            case 5: note = 'C'; accidental='#'; break;
            case 6: note = 'D'; accidental='N'; break;
            case 7: note = 'D'; accidental='#'; break;
            case 8: note = 'E'; accidental='N'; break;
            case 9: note = 'F'; accidental='N'; break;
            case 10: note = 'F'; accidental='#'; break;
            case 11: note = 'G'; accidental='N'; break;
            case 0: note = 'G'; accidental='#'; break;
            default: note='X'; accidental='X';
        }
        if(number<=3)
            octave=0;
        else if(note!='B')
            octave = (int)((number-3)/12)+1;
        else
            octave = (int)((number-3)/12);
        
    
    }
    
    public char getNote() {
        return note;
    }
    public char getAccidental() {
        return accidental;
    }
    public int getOctave() {
        return octave;
    }
    public int getNumber() {
        return number;
    }
    public double getFreqency() {
        return freqency;
    }
    public void setButton(int button) {
        this.button = button;
    }
    public int getButton() {
        return button;
    }
    public String getColor(){
        if(accidental == 'N'){
            return "WHITE";
        }
        return "BLACK";
    }
    public int getWhiteNumber() {
        return whiteNumber;
    }
    public void setPlaying(boolean playing){
        this.playing=playing;
    }    
    public void setWhiteNumber(int whiteNumber) {
        this.whiteNumber = whiteNumber;
    }

    public String toString(){
        return note + "" + octave + " " + accidental + " freq:" + freqency + "\n";
    }

    public void keyPressed(int e) {
        if(e == button && !playing && e !=0){
            PianoMain.playSound(number);
            setPlaying(true);
        }
    }
    public void keyReleased(int e) {
        if(e == button && playing && e != 0){
            PianoMain.stopSound(number);
            setPlaying(false);
        }
    }
    
    public boolean mousePressed(MouseEvent e) {
        Rectangle rect;
        if(getColor().equals("WHITE")){
            rect = new Rectangle(x,y,WHITEKEYWIDTH,WHITEKEYHEIGHT);
        }
        else{
            rect = new Rectangle(x, y, BLACKKEYWIDTH, BLACKKEYHEIGHT);
        }
        if(rect.contains(e.getPoint()) && !playing){
            PianoMain.playSound(number);
            playing = true;
            return true;
        }
        return false;
    }
    
    public void mouseReleased(MouseEvent e) {
        PianoMain.stopSound(getNumber());
        playing = false;
    }
    
    public void drawKey(Graphics g, int x, int y){
        this.x=x;
        this.y=y;
        if(accidental == 'N'){
            g.setColor(Color.WHITE);
            g.fillRect(x, y, WHITEKEYWIDTH, WHITEKEYHEIGHT);
            g.setColor(Color.black);
            g.drawRect(x, y, WHITEKEYWIDTH, WHITEKEYHEIGHT);
            g.setColor(Color.red);
            g.drawString(note + "" + accidental + " " + octave, x, y+WHITEKEYHEIGHT);
            if(button !=0){
                drawCenteredString(g, ""+ KeyEvent.getKeyText(button), x + WHITEKEYWIDTH/2, y + WHITEKEYHEIGHT/4*3, g.getFont());
            }
            
        }
        else if(accidental == '#'){
            g.setColor(Color.BLACK);
            g.fillRect(x, y, BLACKKEYWIDTH, BLACKKEYHEIGHT);
            g.setColor(Color.red);
            g.drawString(note + "" + accidental + " " + octave, x, y+BLACKKEYHEIGHT);
            if(button !=0){
                drawCenteredString(g, ""+ KeyEvent.getKeyText(button), x + BLACKKEYWIDTH/2, y + BLACKKEYHEIGHT/4*3, g.getFont());
            }
        }
        else{
            g.setColor(Color.red);
            g.fillRect(x, y, WHITEKEYWIDTH, WHITEKEYHEIGHT);
        }
        if(playing){
            if(accidental == 'N'){
                g.setColor(Color.LIGHT_GRAY);
                g.fillRect(x, y, WHITEKEYWIDTH, WHITEKEYHEIGHT);
                g.setColor(Color.black);
                g.drawRect(x, y, WHITEKEYWIDTH, WHITEKEYHEIGHT);
                g.setColor(Color.red);
                g.drawString(note + "" + accidental + " " + octave, x, y+WHITEKEYHEIGHT);
                if(button !=0){
                    drawCenteredString(g, ""+ KeyEvent.getKeyText(button), x + WHITEKEYWIDTH/2, y + WHITEKEYHEIGHT/4*3, g.getFont());
                }
            }
            else if (accidental == '#') {
                g.setColor(Color.DARK_GRAY);
                g.fillRect(x, y, BLACKKEYWIDTH, BLACKKEYHEIGHT);
                g.setColor(Color.red);
                g.drawString(note + "" + accidental + " " + octave, x, y+BLACKKEYHEIGHT);
                if(button !=0){
                    drawCenteredString(g, ""+ KeyEvent.getKeyText(button), x + BLACKKEYWIDTH/2, y + BLACKKEYHEIGHT/4*3, g.getFont());
                }
            }
        }
    }
    public void drawCenteredString(Graphics g, String str, int x, int y, Font font){
            FontMetrics m = g.getFontMetrics();
            int nX,nY;
            nX = x - (m.stringWidth(str) / 2);
            nY = y - (m.getHeight() / 2);
            
            g.drawString(str, nX, nY+10);
    }

}
