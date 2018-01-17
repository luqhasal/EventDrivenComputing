/*  Luqhasal
 *  Event-Driven Computing
 */

//State changes declared as Enum Object-Oriented
public enum State {
    STARTING,           // Starting, the warming up period
    READY,              // Ready to be used
    WORKING,            // The controller is in use
    WARNING,            // Warning period, reset timer is possible
    COOLING_DOWN,       // The controller goes into cooling down period
};
