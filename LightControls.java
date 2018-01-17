/*  Luqhasal
 *  Event-Driven Computing
 */

//Swing libraries
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.Timer;
import javax.swing.border.*;
import java.util.TimerTask;

//Class declaration Light Controls - now extending JPanel and ActionListener
//The program will receive Actions itself by implementing ActionListener interface
public class LightControls extends JPanel implements ActionListener {

    //++++++TIME_SCALE is a multiplier factor to convert miliseconds to minutes++++++
    //======For testing timing to speed up======
    //private int TIME_SCALE = 500;                 //0.5 second speed
    //private int WORK_LIMIT = 2 * 180;             //00:06:00 -> start at 6 mins 
    //private int COOLINGDOWN_LAPSE = 2 * 60;       //2 minutes for cool down + 15 seconds for long audible alert

    //======For real timing as specified by the requirement======
    private int TIME_SCALE = 1000;                //1 second speed
    private int WORK_LIMIT = 2 * 60 * 60;         //Two hours using seconds scale (2:00:00)
    private int COOLINGDOWN_LAPSE = 8 * 60;       //8 minutes for cool down
    //+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

    private int DISPLAY_START = WORK_LIMIT - 5;     //5 seconds in the seconds scale (downcounting)
    private int WARNING_LIMIT = 60 * 5;             //The BEEP message starts to blink in the last 5 minutes of the countdown
    private int BEEP_LAPSE = 15;                    //Show the BEEP message every 15 seconds
    private int BEEP_OFF = 14;                      //Hide the BEEP message in 14 seconds of the 15 seconds
    private int COOLINGDOWN_WARNING = COOLINGDOWN_LAPSE - BEEP_LAPSE;   // Hide the BEEP message for cooling down after 15 seconds long audible alert
    
    //Newly added - enum states
    private State state;

    //Using color for easier coding - previously used icon pictures
    private static Color LIGHTS_OFF = new Color(0, 0, 0);       //black 
    private static Color LIGHTS_ON = new Color(255, 255, 0);    //yellow
    private static Color BEEP_ALERT = new Color(255, 0, 0);     //red

    //Elements declaration
    private JLabel D;                   //display status
    private JLabel S;                   //speaker switch
    private JLabel L;                   //light color status
    private JToggleButton T;            //toggle switch  
    private JButton B;                  //reset switch  
    private Timer warming_timer;        //15mins 100% peak output
    private Timer counting_down;        //1:59:55
    private Timer cooling_down;         //8 minutes
    private int working_counter;        //alternatively function as tick()
    private int warning_counter;        //alternatively function as tick()
    private int coolingdown_counter;    //alternatively function as tick()
    private boolean mouseDown = false;  //mouseEvent

    //GUI implementation
    public void prepareGUI(Container pane) {

        //Layout manager using GridBagLayout - previously used Flow style
        //More flexibility with elements layout and position
        pane.setLayout(new GridBagLayout());
        GridBagConstraints mainFrame = new GridBagConstraints();
        mainFrame.fill = GridBagConstraints.HORIZONTAL;
        mainFrame.insets = new Insets(10, 30, 10, 30);
        mainFrame.weightx = 1;
        mainFrame.weighty = 1;

        Border margin = new EmptyBorder(20, 10, 20, 10);

        //Toggle switch component
        T = new JToggleButton("On / Off");
        T.setBorder(new CompoundBorder(T.getBorder(), margin));
        T.setActionCommand("button_t");     //firing value
        T.addActionListener(this);
        
        //Reset switch component
        B = new JButton("Reset");
        B.setBorder(new CompoundBorder(B.getBorder(), margin));
        B.setActionCommand("reset");     //firing value
        B.addActionListener(this);
        B.addMouseListener(
            new MouseAdapter() {
                private java.util.Timer t;
                public void mousePressed(MouseEvent e) {
                    if (t == null) {
                        t = new java.util.Timer();
                    }
                    t.schedule(new TimerTask() {
                        public void run() {
                            mouseDown = true;
                            B.setActionCommand("reset");     //firing value
                        }
                    },5000,1000);
                }

                public void mouseReleased(MouseEvent e) {
                    if (t != null) {
                        t.cancel();
                        t = null;
                    }
                }
            }
            );

        //Light status color component
        L = new JLabel();
        L.setBorder(new LineBorder(Color.LIGHT_GRAY));
        L.setBorder(new CompoundBorder(L.getBorder(), margin));
        L.setOpaque(true);
        L.setBackground(LIGHTS_OFF);

        //Speaker switch component
        S = new JLabel(" ");
        S.setOpaque(true);
        S.setForeground(BEEP_ALERT);
        S.setHorizontalAlignment(JLabel.CENTER);
        S.setVerticalAlignment(JLabel.CENTER);
        S.setBorder(new LineBorder(Color.LIGHT_GRAY));
        S.setBorder(new CompoundBorder(S.getBorder(), margin));

        //Display status component
        D = new JLabel("OFF");
        D.setFont(new java.awt.Font("Liberation Mono", Font.BOLD, 20));
        D.setForeground(Color.LIGHT_GRAY);
        D.setHorizontalAlignment(JLabel.CENTER);
        D.setVerticalAlignment(JLabel.CENTER);
        D.setBorder(new LineBorder(Color.LIGHT_GRAY));
        D.setBorder(new CompoundBorder(D.getBorder(), margin));

        //positioning
        mainFrame.gridx = 0; mainFrame.gridy = 0; mainFrame.gridwidth = 2; pane.add(L, mainFrame);
        mainFrame.gridx = 0; mainFrame.gridy = 1; mainFrame.gridwidth = 2; pane.add(S, mainFrame);
        mainFrame.gridx = 0; mainFrame.gridy = 2; mainFrame.gridwidth = 2; pane.add(D, mainFrame);
        mainFrame.gridx = 0; mainFrame.gridy = 3; mainFrame.gridwidth = 1; pane.add(T, mainFrame);
        mainFrame.gridx = 1; mainFrame.gridy = 3;                          pane.add(B, mainFrame);
    }

    /**
     * Replaced showEvent() with start() method. This method is invoked from the
     * event-dispatching thread to ensure thread synchronization
     */
    private void Start() {

        JFrame frame = new JFrame("Control Interface");         //window title
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);   //similar to System.exit(0)
        frame.setLocationRelativeTo(null);                      //window center location

        //Add the components to the interface
        prepareGUI(frame.getContentPane());

        //For testing purpose 1 second is used for warming up
        warming_timer = new Timer(1 * TIME_SCALE, this);
        warming_timer.setActionCommand("loaded");               //firing value

        warming_timer.start();

        //Initialize and display it all initial states
        state = State.STARTING;
        working_counter = WORK_LIMIT;
        warning_counter = BEEP_LAPSE;
        coolingdown_counter = COOLINGDOWN_LAPSE;
        frame.pack();
        frame.setVisible(true);
        frame.setResizable(false);      //disable resize
    }

    //Timer #1 implementation
    private void startWorkingTimer() {
        counting_down = new Timer(1 * TIME_SCALE, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                
                //decrement counter
                --working_counter;
                
                //within the range
                if (working_counter <= DISPLAY_START) {
                    
                    //IF remaining 5 mins then goto state
                    if (working_counter == WARNING_LIMIT) {                 
                        state = State.WARNING;                             
                        counting_down.setActionCommand("warning");          //firing value
                    
                    //IF timer expires then goto state
                    } else if (working_counter == 0) {
                        state = State.COOLING_DOWN;
                        counting_down.setActionCommand("long audible alert");     //firing value
                    
                    //IF current state in WARNING
                    } else if (state == State.WARNING) {
                        //decrement counter
                        --warning_counter;

                        //BEEP OFF interval
                        if (warning_counter > 0) {
                            if (warning_counter == BEEP_OFF) {
                                counting_down.setActionCommand("beep_off"); //firing value
                            }
                        ////BEEP ON interval    
                        } else {
                            warning_counter = BEEP_LAPSE;
                            counting_down.setActionCommand("beep_on");      //firing value
                        }
                    }

                    //While the operations running...
                    if (state == State.WORKING || state == State.WARNING) {
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                int hours = working_counter / 3600;
                                int minutes = (working_counter % 3600) / 60;
                                int seconds = working_counter % 60;

                                D.setText(String.valueOf(String.format("%02d:%02d:%02d", hours, minutes, seconds)));
                            }
                        });
                    }
                }
            }
        });
        counting_down.setActionCommand("tick");                          //firing value
        counting_down.addActionListener(this);
        counting_down.start();
    }

    //Timer #2 implementation
    private void startCoolingDownTimer() {
        cooling_down = new Timer(1 * TIME_SCALE, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                
                //IF current state in Cooling down
                if (state == State.COOLING_DOWN) {

                    //decrement counter
                    --coolingdown_counter;

                    //IF cooling down timer expires then goto state
                    if (coolingdown_counter == 0) {
                        cooling_down.setActionCommand("cooled");            //firing value
                    
                    //at the end of 15 seconds long audible alert
                    } else if (coolingdown_counter == COOLINGDOWN_WARNING) {
                        cooling_down.setActionCommand("cooling_down");//firing value

                    } else {
                        cooling_down.setActionCommand("cooling");           //firing value
                        //System.out.println(String.valueOf(coolingdown_counter));      //tracking the difference timing between long audible alert and cooldown
                    }
                }
            }
        });
        cooling_down.setActionCommand("cooling");                           //firing value
        cooling_down.addActionListener(this);
        cooling_down.start();
    }

    //Main class
    public static void main(String[] args) {
        //Schedule the GUI display as task for the event-dispatching thread
        java.awt.EventQueue.invokeLater(new LightControls()::Start);
    }

   /* MEALY
    * when the action event occurs, that object's actionPerformed method is invoked.
    * There are exit-actions associated with the transition from each state, that change the value of
    * the local variable called timer. Exception error message added for testing purpose 
    * in the command line
    */
    @Override
    public void actionPerformed(ActionEvent e) {
        
        //capture the value from the button pressed for tracking firing values and monitoring tick() 
        System.out.println("Command: " + e.getActionCommand());
        
        //states
        switch (e.getActionCommand()) {
            
            case "beep_on":
                S.setText("BEEP");
                return;
            
            case "beep_off":
                S.setText(" ");
                return;
            
            //initial state
            case "loaded":
                ((Timer) e.getSource()).stop();
                D.setForeground(Color.BLACK);
                state = State.READY;
                return;
            
            //when pressed
            case "button_t":
                if (state == State.READY) {
                    state = State.WORKING;
                    D.setText("ON");
                    L.setBackground(LIGHTS_ON);
                    T.setEnabled(true);
                    startWorkingTimer();
                } else if (state == State.WORKING || state == State.WARNING) {
                    state = State.COOLING_DOWN;
                    counting_down.stop();
                    S.setText("BEEP");
                    D.setText(" ");
                    L.setBackground(LIGHTS_ON);
                    T.setEnabled(false);
                    B.setEnabled(false);
                    startCoolingDownTimer();
                } else {
                    System.out.println("Not available function");
                    T.setSelected(false);
                }
                return;
            
            case "reset":           
                if (state == State.WARNING) {
                    state = State.WORKING;
                    working_counter = WORK_LIMIT;
                    warning_counter = BEEP_LAPSE;
                    counting_down.setActionCommand("Tick");
                    D.setText("ON");
                    S.setText(" ");
                    S.setOpaque(true);
                    L.setBackground(LIGHTS_ON);
                } else if (state == State.WORKING && mouseDown == true) {
                    state = State.COOLING_DOWN;
                    counting_down.stop();
                    S.setText("BEEP");
                    D.setText(" ");
                    L.setBackground(LIGHTS_ON);
                    T.setEnabled(false);
                    B.setEnabled(false);
                    startCoolingDownTimer();
                } else {
                    System.out.println("Not available function");
                }
                return;
            
            //already reach 5 mins remaining
            case "warning":         //5 mins warning state
                S.setText("BEEP");
                return;
            
            case "long audible alert":
                counting_down.stop();
                L.setBackground(LIGHTS_ON);
                S.setText("BEEP");
                startCoolingDownTimer();
                D.setText(" ");
                T.setSelected(false);
                B.setEnabled(false);
                return;
            
            case "cooling_down":
                L.setBackground(LIGHTS_OFF);
                S.setText(" ");
                D.setText("COOLING DOWN");
                T.setSelected(false);
                B.setEnabled(false);
                return;
            
            case "cooled":
                cooling_down.stop();
                D.setText("OFF");
                D.setForeground(Color.LIGHT_GRAY);
                T.setEnabled(true);
                B.setEnabled(true);

                warming_timer = new Timer(1 * TIME_SCALE, this);
                warming_timer.setActionCommand("loaded");

                warming_timer.start();

                state = State.STARTING;
                working_counter = WORK_LIMIT;
                warning_counter = BEEP_LAPSE;
                coolingdown_counter = COOLINGDOWN_LAPSE;
                return;
        }
    }
}
