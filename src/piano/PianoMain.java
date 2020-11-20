package piano;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.net.URI;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import static javax.sound.midi.ShortMessage.NOTE_OFF;
import static javax.sound.midi.ShortMessage.NOTE_ON;
import javax.sound.midi.Synthesizer;
import javax.sound.midi.Track;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.UIManager;

public class PianoMain extends JFrame {

    public static Key[] keys;
    public static final int SCREENWIDTH = Key.WHITEKEYWIDTH * 11;
    public static final int SCREENHEIGHT = 600;
    public static int FRAMEWIDTH = SCREENWIDTH;
    public static int FRAMEHEIGHT = SCREENHEIGHT;
    DrawPanel mainPanel;
    private boolean playing;
    private boolean playingSong;
    private boolean pauseSong;
    private static boolean recording;
    private int distFromLeft;
    private final int widthOfKeyboard = 52 * Key.WHITEKEYWIDTH;
    private static MidiChannel[] channels;
    private static Synthesizer synth;
    private static int volume;
    private static int duration;
    private static int channel;
    private File midiFile;
    private int counter;
    private Timer timer;
    private int mode;
    private final int KEYSMODE = 1, CLICKMODE = 2;
    private JFileChooser fileChooser;
    private String[] instruments;
    private Image playButton, pauseButton, stopButton, fileButton, instrumentsButton, upButton, downButton, modeToggle, record;
    private ImageIcon temp;
    private int clickedKey = -1;
    private static int recordingTick;
    private static ArrayList<String[]> recordingList;
    private boolean recordingTimerEnabled;
    String filePath;
    
    public PianoMain() {
        super("Piano");
        this.setIconImage(Toolkit.getDefaultToolkit().getImage(Piano.class.getResource("/images/piano.png")));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setPreferredSize(new Dimension(SCREENWIDTH, SCREENHEIGHT));
        pack();
        FRAMEHEIGHT = SCREENHEIGHT + (SCREENHEIGHT - getContentPane().getHeight());
        FRAMEWIDTH = SCREENWIDTH + (SCREENWIDTH - getContentPane().getWidth());
        setPreferredSize(new Dimension(FRAMEWIDTH, FRAMEHEIGHT));
        getContentPane().setPreferredSize(new Dimension(FRAMEWIDTH, FRAMEHEIGHT));
        setResizable(false);
        mode = KEYSMODE;
        keys = new Key[88];
        int white = 0;
        for (int i = 1; i <= keys.length; i++) {
            keys[i - 1] = new Key(i);
            if (keys[i - 1].getColor().equals("WHITE")) {
                keys[i - 1].setWhiteNumber(white);
                white++;
            } else {
                keys[i - 1].setWhiteNumber(white - 1);
            }
        }

        volume = 200;
        duration = 1000;
        channel = 0;

        playing = true;
        try {
            synth = MidiSystem.getSynthesizer();
            synth.open();
            channels = synth.getChannels();
            channels[9].programChange(-1);
            instruments = new String[128];
            for (int i = 0; i < 128; i++) {
                instruments[i] = synth.getAvailableInstruments()[i].getName() + " " + synth.getAvailableInstruments()[i].getPatch().getProgram();
            }

        } catch (Exception e) {
            System.out.println("ERROR:" + e);
        }
        
        mainPanel = new DrawPanel();
        mainPanel.setListeners();
        add(mainPanel);
        mainPanel.requestFocusInWindow();
        
        distFromLeft = 18 * Key.WHITEKEYWIDTH;
        
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            playButton = Toolkit.getDefaultToolkit().getImage(Piano.class.getResource("/images/play.png"));
            pauseButton = Toolkit.getDefaultToolkit().getImage(Piano.class.getResource("/images/pause.png"));
            stopButton = Toolkit.getDefaultToolkit().getImage(Piano.class.getResource("/images/stop.png"));
            fileButton = Toolkit.getDefaultToolkit().getImage(Piano.class.getResource("/images/fileIcon.png"));
            instrumentsButton = Toolkit.getDefaultToolkit().getImage(Piano.class.getResource("/images/instruments.png"));
            upButton = Toolkit.getDefaultToolkit().getImage(Piano.class.getResource("/images/up.png"));
            downButton = Toolkit.getDefaultToolkit().getImage(Piano.class.getResource("/images/down.png"));
            modeToggle = Toolkit.getDefaultToolkit().getImage(Piano.class.getResource("/images/click.png"));
            record = Toolkit.getDefaultToolkit().getImage(Piano.class.getResource("/images/recordStart.png"));
//            if(!System.getProperty("oa.name").toLowerCase().contains("windows"))
//                copy("./songs","/songs");
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
        
        fileChooser = new JFileChooser("songs");

        setVisible(true);
        start();
    }
    
    public void start(){
        Timer run = new Timer();
        run.schedule(new TimerTask() {
            @Override
            public void run() {
                play();
            }
        }, 0, Math.round(1000/60));
    }
    
    public void copy(String to, String from){
        try{
            File songsCopy = new File(to);

            URI uri = Piano.class.getResource(from).toURI();
            if (!uri.toString().startsWith("file:")) {
                Map<String, String> env = new HashMap<>();
                env.put("create", "true");
                FileSystems.newFileSystem(uri, env);
            }
            Path songsSrc = Paths.get(uri);

            if (!songsCopy.exists()) {
                songsCopy.mkdir();
                try(DirectoryStream<Path> paths = Files.newDirectoryStream(songsSrc)) {
                    for (final Path child : paths) {
                        try {
                            String targetPath = songsCopy.getAbsolutePath() + File.separator + child.getFileName().toString();
                            Files.copy(child, Paths.get(targetPath), StandardCopyOption.REPLACE_EXISTING);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }catch(Exception ex){
            ex.printStackTrace();
        }
    }
    
    private void play() {
            mainPanel.repaint();
            int whiteCount = 0;
            int blackCount = 0;
            if (mode == KEYSMODE) {
                setPreferredSize(new Dimension(SCREENWIDTH, SCREENHEIGHT));
                pack();
                FRAMEHEIGHT = SCREENHEIGHT + (SCREENHEIGHT - getContentPane().getHeight());
                FRAMEWIDTH = SCREENWIDTH + (SCREENWIDTH - getContentPane().getWidth());
                setPreferredSize(new Dimension(FRAMEWIDTH, FRAMEHEIGHT));
                getContentPane().setPreferredSize(new Dimension(FRAMEWIDTH, FRAMEHEIGHT));
                setResizable(false);
                for (int i = 0; i < keys.length; i++) {

                    if (keys[i].getWhiteNumber() >= distFromLeft / Key.WHITEKEYWIDTH/* && keys[i].getNumber() <= (distFromLeft/Key.WHITEKEYWIDTH) + (getWidth() / Key.WHITEKEYWIDTH)*/) {
                        //KEY ON SCREEN
                        if (keys[i].getColor().equals("WHITE")) {
                            //WHITE KEY
                            switch (whiteCount) {
                                case 0:
                                    keys[i].setButton(KeyEvent.VK_A);
                                    break;
                                case 1:
                                    keys[i].setButton(KeyEvent.VK_S);
                                    break;
                                case 2:
                                    keys[i].setButton(KeyEvent.VK_D);
                                    break;
                                case 3:
                                    keys[i].setButton(KeyEvent.VK_F);
                                    break;
                                case 4:
                                    keys[i].setButton(KeyEvent.VK_G);
                                    break;
                                case 5:
                                    keys[i].setButton(KeyEvent.VK_H);
                                    break;
                                case 6:
                                    keys[i].setButton(KeyEvent.VK_J);
                                    break;
                                case 7:
                                    keys[i].setButton(KeyEvent.VK_K);
                                    break;
                                case 8:
                                    keys[i].setButton(KeyEvent.VK_L);
                                    break;
                                case 9:
                                    keys[i].setButton(KeyEvent.VK_SEMICOLON);
                                    break;
                                case 10:
                                    keys[i].setButton(KeyEvent.VK_QUOTE);
                                    break;
                                default:
                                    keys[i].setButton(0);
                            }
                            whiteCount++;
                        } else {
                            //BLACK KEY
                            switch (whiteCount) {
                                case 1:
                                    keys[i].setButton(KeyEvent.VK_W);
                                    break;
                                case 2:
                                    keys[i].setButton(KeyEvent.VK_E);
                                    break;
                                case 3:
                                    keys[i].setButton(KeyEvent.VK_R);
                                    break;
                                case 4:
                                    keys[i].setButton(KeyEvent.VK_T);
                                    break;
                                case 5:
                                    keys[i].setButton(KeyEvent.VK_Y);
                                    break;
                                case 6:
                                    keys[i].setButton(KeyEvent.VK_U);
                                    break;
                                case 7:
                                    keys[i].setButton(KeyEvent.VK_I);
                                    break;
                                case 8:
                                    keys[i].setButton(KeyEvent.VK_O);
                                    break;
                                case 9:
                                    keys[i].setButton(KeyEvent.VK_P);
                                    break;
                                case 10:
                                    keys[i].setButton(KeyEvent.VK_OPEN_BRACKET);
                                    break;
                                case 11:
                                    keys[i].setButton(KeyEvent.VK_CLOSE_BRACKET);
                                    break;
                                default:
                                    keys[i].setButton(0);
                            }
                            blackCount++;
                        }//end if white else black
                    }//end if onscreen
                    else {
                        //KEY OFF SCREEN
                        keys[i].setButton(0);
                    }//end Else   
                }//end FOR
            }//end if KEYSMODE
            else {
                /*clikmode*/
                setResizable(true);
                for (int i = 0; i < keys.length; i++) {
                    keys[i].setButton(0);
                    //SWITCH MODES AAND ADD CLICK LISTENER
                }
            }//END ELSE
            
            if(recording){
                if(!recordingTimerEnabled){
                    startRecordingTimer();
                }
            }
    }

    public static void playSound(int number) {
        channels[PianoMain.channel].noteOn(number + 20, PianoMain.volume);
        keys[number - 1].setPlaying(true);
        
        if(recording){
            recordingList.get(recordingTick)[number-1] = "ON";
        }
    }

    public static void stopSound(int number) {
        channels[PianoMain.channel].noteOff(number + 20);
        keys[number - 1].setPlaying(false);
        
        if(recording){
            recordingList.get(recordingTick)[number-1] = "OFF";
        }
    }

    public void playSong() {
        try {
            if (midiFile != null) {
                System.out.println("Playing Song: " + midiFile);
                playingSong = true;
                pauseSong = false;
                
                Sequence sequence = MidiSystem.getSequence(midiFile);

                int trackNumber = 0;

                String[][] songArray = new String[(int) sequence.getTickLength() + 1][88];
                for (String[] currentTick : songArray) {
                    for (int i = 0; i < currentTick.length; i++) {
                        currentTick[i] = "";
                    }
                }

                for (Track track : sequence.getTracks()) {
                    trackNumber++;
                    for (int i = 0; i < track.size(); i++) {
                        MidiEvent event = track.get(i);
                        MidiMessage message = event.getMessage();
                        if (message instanceof ShortMessage) {
                            ShortMessage sm = (ShortMessage) message;
                            switch (sm.getCommand()) {
                                case NOTE_ON: {
                                    int number = sm.getData1();
                                    int velocity = sm.getData2();
                                    if (velocity != 0) {
//                                    System.out.println("On Command");
                                        songArray[(int) event.getTick()][number - 21] = "start";
                                    } else {
//                                    System.out.println("off Command");
                                        songArray[(int) event.getTick()][number - 21] = "stop";
                                    }
                                    break;
                                }
                                case NOTE_OFF: {
                                    int number = sm.getData1();
                                    int velocity = sm.getData2();
                                    songArray[(int) event.getTick()][number - 21] = "stop";
                                    break;
                                }
                                default:
                                    break;
                            }//end switch
                        }//end if instance of
                    }//end for Track.size
                }//end for getTracks
                counter = 0;
                timer = new Timer();
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        if (!pauseSong) {
                            for (int i = 0; i < 88; i++) {
                                if (songArray[counter][i].equals("start")) {
                                    playSound(i + 1);
                                } else if (songArray[counter][i].equals("stop")) {
                                    stopSound(i + 1);
                                }
                            }
                            counter++;
                            if (counter >= sequence.getTickLength()) {
                                playingSong = false;
                                timer.cancel();
                            }
                            if (!playingSong) {
                                timer.cancel();
                                for (Key key : keys) {
                                    stopSound(key.getNumber());
                                }
                            }
                        } else {
                            for (Key key : keys) {
                                stopSound(key.getNumber());
                            }
                        }
                    }
                }, 0, Math.round((sequence.getMicrosecondLength() / 1000.0) / sequence.getTickLength()));
            } else {
                chooseFile();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    public void pauseSong() {
        pauseSong = true;
    }

    public void stopSong() {
        playingSong = false;
    }
    
    public void startRecord(){
        record = Toolkit.getDefaultToolkit().getImage(Piano.class.getResource("/images/recordStop.png"));
        System.out.println("RECORDING");
        recordingList = new ArrayList<>();
        recordingList.add(new String[88]);
        recordingTick = 0;        
        recording = true;
    }
    
    public void stopRecord(){
        recording = false;
        record = Toolkit.getDefaultToolkit().getImage(Piano.class.getResource("/images/recordStart.png"));
        try{ 
            //FIND OUT WHAT TO PUT IN TICKS
            Sequence recordSequence = new Sequence(Sequence.PPQ, 24);
            recordSequence.createTrack();
                        
            for (int i = 0; i < recordingList.size(); i++) {
                for (int j = 0; j < recordingList.get(i).length; j++) {
                    String string = recordingList.get(i)[j];
                    if(string != null && string.equals("ON")){
                        recordSequence.getTracks()[0].add(new MidiEvent(new ShortMessage(ShortMessage.NOTE_ON, channel, j + 21, 20), (long) i));
                    }
                    if(string != null && string.equals("OFF")){
                        recordSequence.getTracks()[0].add(new MidiEvent(new ShortMessage(ShortMessage.NOTE_OFF, channel, j + 21, 0), (long) i));
                    }
                }
            }
            for (int i = 0; i < 88; i++) {
                recordSequence.getTracks()[0].add(new MidiEvent(new ShortMessage(ShortMessage.NOTE_OFF, i + 21, 0), recordingList.size()));
            }
            File recordingfolder = new File("./recordedSongs");
            if(!recordingfolder.exists())
                recordingfolder.mkdir();
            
            String fileName  = JOptionPane.showInputDialog(mainPanel, "File Name:", "File Name", JOptionPane.DEFAULT_OPTION);
            if(fileName != null && fileName.equals("")){
                JOptionPane.showConfirmDialog(mainPanel, "NOT VALID FILE NAME");
            }else if(fileName != null && fileName.length()>3 && fileName.substring(fileName.length()-4, fileName.length()).equals(".mid")){
                MidiSystem.write(recordSequence, 0, new File(recordingfolder.getPath() + "/" + fileName));
            }else {
                MidiSystem.write(recordSequence, 0, new File(recordingfolder.getPath() + "/" + fileName + ".mid"));
            }
            
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("STOP RECORD");
    }
   
    public void chooseFile() {
        fileChooser.showDialog(this, "Open");
        if (fileChooser.getSelectedFile() != null) {
            midiFile = fileChooser.getSelectedFile();
        }
    }

    public void changeInstruments() {
        try {
            int changedChannel = 0;
            int program = 0;
            String[] channelStringArr;
            channelStringArr = new String[16];
            for (int i = 0; i < channels.length; i++) {
                if (i != 9) {
                    channelStringArr[i] = i + ": " + instruments[channels[i].getProgram()];
                } else {
                    channelStringArr[i] = "9: DONT CHOOSE THIS";
                }
            }

            String channelString = (String) JOptionPane.showInputDialog(mainPanel, "Channel(1-16)", "Choose Channel", JOptionPane.QUESTION_MESSAGE, null, channelStringArr, channelStringArr[0]);
            changedChannel = Integer.parseInt(channelString.substring(0, channelString.indexOf(':')));

            String instrument = (String) JOptionPane.showInputDialog(mainPanel, "Which Instrument?", "Choose Intsrument", JOptionPane.QUESTION_MESSAGE, null, instruments, instruments[channels[changedChannel].getProgram()]);
            program = Integer.parseInt(instrument.substring(instrument.lastIndexOf(' ') + 1));

            channels[changedChannel].programChange(program);
        } catch (Exception e) {

        }
    }
    
    public void toggleMode(){
        if(mode == KEYSMODE){
            mode = CLICKMODE;
            modeToggle = Toolkit.getDefaultToolkit().getImage(Piano.class.getResource("/images/keys.png"));
        } else if (mode == CLICKMODE){
            mode = KEYSMODE;
            modeToggle = Toolkit.getDefaultToolkit().getImage(Piano.class.getResource("/images/click.png"));
        }
    }

    private void startRecordingTimer() {
        recordingTimerEnabled = true;
        Timer timer = new Timer();
        timer.schedule(new TimerTask(){
            @Override
            public void run() {
                if(recording){    
                    recordingTick++;
                    recordingList.add(new String[88]);
                }
                else{
                    recordingTimerEnabled = false;
                    timer.cancel();
                }
            }
        }, 0, 21);
        
    }
        

    class DrawPanel extends JPanel implements KeyListener, MouseListener{

        Rectangle playRect, pauseRect, stopRect, fileRect, instrumentRect, upRect, downRect, modeRect, recordRect;

        public DrawPanel() {
            super();
        }

        public void setListeners() {
            addKeyListener(this);
            addMouseListener(this);
        }

//        Doesn't do anything        
//        @Override
//        public void update(Graphics g) {
//            paintComponent(g);
//        }

        @Override
        public void paintComponent(Graphics g) {
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, getWidth(), getHeight());
            int whiteCount = 0;
            for (int i = 0; i < keys.length; i++) {
                Key key = keys[i];
                if (key.getColor().equals("WHITE")) {
                    key.drawKey(g, Key.WHITEKEYWIDTH * whiteCount - distFromLeft, getHeight() - Key.WHITEKEYHEIGHT);
                    whiteCount++;
                }
            }
            whiteCount = 0;
            for (int i = 0; i < keys.length; i++) {
                Key key = keys[i];
                if (key.getColor().equals("WHITE")) {
                    whiteCount++;
                } else {
                    key.drawKey(g, Key.WHITEKEYWIDTH * whiteCount - Key.BLACKKEYWIDTH / 2 - distFromLeft, getHeight() - Key.WHITEKEYHEIGHT);
                }
            }
            g.setFont(new Font("font", 1, 20));
            if (channel != 9) {
                g.drawString("Channel: " + channel + ", Curret Intstrument: " + instruments[channels[channel].getProgram()].substring(0, instruments[channels[channel].getProgram()].lastIndexOf(" ")), 0, getHeight() - Key.WHITEKEYHEIGHT);
            } else {
                g.drawString("Channel: " + channel + ", Curret Intstrument: Percussion", 0, getHeight() - Key.WHITEKEYHEIGHT);
            }
            g.setFont(new Font("font", 1, 200));
            drawCenteredString(g, "PIANO", getWidth() / 2, getHeight() / 2, g.getFont());

            g.drawImage(playButton, 10, getHeight() - Key.WHITEKEYHEIGHT - 40, 20, 20, this);
            playRect = new Rectangle(10, getHeight() - Key.WHITEKEYHEIGHT - 40, 20, 20);
            g.drawImage(pauseButton, 40, getHeight() - Key.WHITEKEYHEIGHT - 40, 20, 20, this);
            pauseRect = new Rectangle(40, getHeight() - Key.WHITEKEYHEIGHT - 40, 20, 20);
            g.drawImage(stopButton, 70, getHeight() - Key.WHITEKEYHEIGHT - 40, 20, 20, this);
            stopRect = new Rectangle(70, getHeight() - Key.WHITEKEYHEIGHT - 40, 20, 20);
            g.drawImage(fileButton, getWidth() - 30, getHeight() - Key.WHITEKEYHEIGHT - 30, 20, 20, this);
            fileRect = new Rectangle(getWidth() - 30, getHeight() - Key.WHITEKEYHEIGHT - 30, 20, 20);
            g.drawImage(instrumentsButton, getWidth() - 60, getHeight() - Key.WHITEKEYHEIGHT - 30, 20, 20, this);
            instrumentRect = new Rectangle(getWidth() - 60, getHeight() - Key.WHITEKEYHEIGHT - 30, 20, 20);
            g.drawImage(modeToggle, getWidth() - 90, getHeight() - Key.WHITEKEYHEIGHT - 30, 20, 20, this);
            modeRect = new Rectangle(getWidth() - 90, getHeight() - Key.WHITEKEYHEIGHT - 30, 20, 20);
            g.drawImage(upButton, 100, getHeight() - Key.WHITEKEYHEIGHT - 40, 20, 20, this);
            upRect = new Rectangle(100, getHeight() - Key.WHITEKEYHEIGHT - 40, 20, 20);
            g.drawImage(downButton, 130, getHeight() - Key.WHITEKEYHEIGHT - 40, 20, 20, this);
            downRect = new Rectangle(130, getHeight() - Key.WHITEKEYHEIGHT - 40, 20, 20);
            g.drawImage(record, getWidth() - 120, getHeight() - Key.WHITEKEYHEIGHT - 30, 20, 20, this);
            recordRect = new Rectangle(getWidth() - 120, getHeight() - Key.WHITEKEYHEIGHT - 30, 20, 20);

        }

        public void drawCenteredString(Graphics g, String str, int x, int y, Font font) {
            FontMetrics m = g.getFontMetrics();
            int nX, nY;
            nX = x - (m.stringWidth(str) / 2);
            nY = y - (m.getHeight() / 2);

            g.drawString(str, nX, nY + 10);
        }
        
        @Override
        public void keyTyped(KeyEvent e) {
        }

        @Override
        public void keyPressed(KeyEvent e) {
            int key = e.getKeyCode();
            if (key == KeyEvent.VK_RIGHT && (widthOfKeyboard - distFromLeft) > getWidth()) {
                //DO STUFF HERE
                distFromLeft += Key.WHITEKEYWIDTH;
                if (mode == KEYSMODE) {
                    for (int i = 0; i < keys.length; i++) {
                        stopSound(keys[i].getNumber());
                    }
                }
            }
            if (key == KeyEvent.VK_LEFT && distFromLeft > 0) {
                //DO STUFF HERE
                distFromLeft -= Key.WHITEKEYWIDTH;
                if (mode == KEYSMODE) {
                    for (int i = 0; i < keys.length; i++) {
                        stopSound(keys[i].getNumber());
                    }
                }
            }
            for (int i = 0; i < keys.length; i++) {
                Key key1 = keys[i];
                key1.keyPressed(key);
            }
            if (key == KeyEvent.VK_UP) {
                mode = CLICKMODE;
            }
            if (key == KeyEvent.VK_DOWN) {
                mode = KEYSMODE;
            }
            if (key == KeyEvent.VK_X && channel < channels.length - 1) {
                channel++;
            }
            if (key == KeyEvent.VK_Z && channel > 0) {
                channel--;
            }
            if (key == KeyEvent.VK_C) {
                changeInstruments();
            }
           
        }

        @Override
        public void keyReleased(KeyEvent e) {
            int key = e.getKeyCode();
            for (Key current : keys) {
                current.keyReleased(e.getKeyCode());
            }
            if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                if (!playingSong) {
                    playSong();
                } else {
                    stopSong();
                }
            }
            if (e.getKeyCode() == KeyEvent.VK_ALT) {
                chooseFile();
            }
            if (e.getKeyCode() == KeyEvent.VK_Q) {
                changeInstruments();
            }
            if(key == KeyEvent.VK_N){
                if(recording){
                    stopRecord();
                }else{
                    startRecord();
                }
            }
            
        }

        @Override
        public void mouseClicked(MouseEvent e) {
        }

        @Override
        public void mousePressed(MouseEvent e) {
            for (Key key : keys) {
                if (key.getColor().equals("BLACK") && key.mousePressed(e)) {
                    clickedKey = key.getNumber() - 1;
                    return;
                }
            }
            for (int i = 0; i < keys.length; i++) {
                Key key = keys[i];
                if (key.getColor().equals("WHITE") && key.mousePressed(e)) {
                    clickedKey = key.getNumber() - 1;
                    return;
                }
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            Point point = e.getPoint();
            if (playRect.contains(point)) {
                if (!playingSong) {
                    playSong();
                } else if (pauseSong) {
                    pauseSong = false;
                }
            } else if (pauseRect.contains(point)) {
                pauseSong();
            } else if (stopRect.contains(point)) {
                stopSong();
            } else if (fileRect.contains(point)) {
                chooseFile();
            } else if (instrumentRect.contains(point)) {
                changeInstruments();
            } else if (modeRect.contains(point)) {
                toggleMode();
            } else if (recordRect.contains(point)) {
                if(recording){
                    stopRecord();
                }else{
                    startRecord();
                }
            } else if (upRect.contains(point) && channel < channels.length) {
                channel++;
            } else if (downRect.contains(point) && channel > 0) {
                channel--;
            } else {
                if(clickedKey != -1){
                    keys[clickedKey].mouseReleased(e);
                    clickedKey = -1;
                }    
            }

        }

        @Override
        public void mouseEntered(MouseEvent e) {
        }

        @Override
        public void mouseExited(MouseEvent e) {
        }

    }

}
