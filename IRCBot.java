import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.Thread;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.time.LocalTime;
import java.util.Timer;
import java.util.TimerTask;

//import org.tensorflow.*;

public class IRCBot {

  // Instance variables
  static BufferedWriter writer;
  static BufferedReader reader;

  static final String server = "irc.atw-inter.net";
  static String nick = "jL3b";
  static String login = "jL3b";
  static final String channel = "#joensuu"; // Joensuu!
  //static String channel = "#bottitest";
  static boolean leeting = false;
  static boolean precisionLeeted = false;
  static Socket socket;
  static boolean leeted = false;

  // Offset: .032482500 (possibly! 17.1.23)
  // Offset: .029330700

  public static void main(final String[] args) {
    try {
      socket = new Socket(server, 6667);

      writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
      reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
      connect(); // Connect to the server

      // Start leetTimeChecking and leet when it is leet time.
      //leetTimeChecking(LocalTime.of(13, 37, 13, 371337133), LocalTime.of(13, 37, 14, 371337133));
      leeted = false;
      // Continue reading the stream - the main program is running here.
      while (true) { // Read continuously
        read();
      }
    } catch (SocketException | UnknownHostException e) {
      e.printStackTrace(); // ignore
    } catch (final IOException e) {
      e.printStackTrace(); // ignore
    } finally {
      try { // Close the socket correctly
        socket.close();
      } catch (final IOException e) {
        e.printStackTrace();
      }
      System.out.println("Bot exited successfully.");
    }
  }

  public static void leetTimeChecking(LocalTime targetTime, LocalTime timeLimit) {
    System.out.println("Leet Timer started @ " + LocalTime.now());
    final Timer timer = new Timer();
    timer.scheduleAtFixedRate(
      new TimerTask() {
        @Override
        public void run() {
          if (!leeted) { // Leet only once!
            if (!leeting) { // Make sure we are not running duplicated requests
              if (LocalTime.now().isAfter(timeLimit)) { // Limit reached! See you tomorrow...
                System.out.println("[LIMIT] Time is: " + LocalTime.now() + " >Limit: " + timeLimit);
                this.cancel(); // Stop the timer
              } else if (LocalTime.now().isAfter(targetTime.now().minusNanos(900000000))) { // It is soon time to leet! Call the SendLeetMessage! 1000 nanoseconds to leet!
                System.out.println("TT:" + targetTime);
                System.out.print("[LEET!] Time is: " + LocalTime.now() + " @Target: " + targetTime.minusNanos(900000000));
                System.out.println("TT:" + targetTime);
                // Added .minusNanos(900000000) (18.1.23)
                SendLeetMessage(channel, "leet@", targetTime); // Start SuperChecking the time and leet when targetTime is reached.
                leeted = true;
                //precisionLeeted = false; // Let's leet multiple times?! Bad idea!
                System.out.print("[!LEET] Time is: " + LocalTime.now() + " @Target: " + targetTime);
              } else {
                System.out.println("[Wait:] Time is: " + LocalTime.now() + " <Target: " + targetTime + " <Limit: " + timeLimit);
              }
            } else {
              //System.out.print("Some other thread is leeting already!");
              System.out.print(".");
            }
          }
        }
      },
      0,
      1 * 100 // check every 0.1s
    );
  }

  // Sends the leet message
  public static void SendLeetMessage(final String channel, final String message, LocalTime target) {
    leeting = true; // Tell other methods that we are leeting
    System.out.println("SendLeetMessage() started.");
    int rounds = 0;
    //LocalTime limit = LocalTime.now().plusSeconds(1);
    //System.out.println("I will leet @ " + targetTime + " | SuperChecking leet time @ " + LocalTime.now()); // Debug
    while (true) {
      if (!precisionLeeted) {
        rounds++;
        //System.out.print(target);
        //System.out.print(LocalTime.now());
        if (LocalTime.now().isAfter(target)) {
          //if (LocalTime.now().isBefore(limit)) {
          writeMessage("PRIVMSG " + channel + " " + message + LocalTime.now()); // Send the leet message here
          System.out.println("Leeted @ " + LocalTime.now());
          precisionLeeted = true;
          break; // Exit loop?
          //} else {
          //break; // Limit reached
          //}
        }
        //System.out.print(rounds);
      }
      /* try {
        Thread.sleep(1); // Let the computer sleep
      } catch (final InterruptedException e) { // Ignore
        System.out.println("Got InterruptedException @ SendLeetMessage() Thread.sleep(1)");
        leeting = false;
        precisionLeeted = false;
        break;
      } */
    }
    System.out.println("SuperChecking finished. Took " + rounds + " rounds.");
    precisionLeeted = false;
    leeting = false;
  }

  // Handles PING and other leet requests
  public static void read() {
    String message = null;
    try {
      while ((message = reader.readLine()) != null) {
        if (message.startsWith("PING")) {
          handlePing(message, writer);
        } else if (message.toLowerCase().contains("sano jou")) {
          System.out.println("Got Jou request " + message + " @ " + LocalTime.now());
          writeMessage("PRIVMSG " + channel + " Jou");
          System.out.println("Leeted @ " + LocalTime.now());
        } else if (message.contains("leettaa @")) {
          String s = "leettaa @";
          int index = message.indexOf(s);
          System.out.println("length:" + message.length());
          System.out.println("10-12:" + message.substring(index + 10, index + 12));
          System.out.println("13-15:" + message.substring(index + 13, index + 15));

          LocalTime lt = LocalTime.of(Integer.parseInt(message.substring(index + 10, index + 12)), Integer.parseInt(message.substring(index + 13, index + 15)));
          System.out.println(lt.now());
          lt = lt.withSecond(13);
          lt = lt.withNano(371337133);
          System.out.println(lt);
          leetTimeChecking(lt, lt.plusSeconds(3));
        } else if (message.contains("leet")) {
          System.out.println("Got leet from " + message + " @ " + LocalTime.now());
        } else if (message.contains("echota")) {
          // TODO: Parse the beginning of the message out!
          System.out.println(message);
          message = message.substring(message.indexOf("echota") + 7);
          System.out.println("Got echo request from " + message + " @ " + LocalTime.now());
          writeMessage("PRIVMSG " + channel + " " + message);
        } else if (message.contains("joinaa kanavalle")) {
          String kanava = message.substring(message.indexOf("joinaa kanavalle") + 17);
          writeMessage("JOIN " + kanava);
        } else if (message.contains("potku perseelle!")) {
          System.out.println("Got potku perseelle from " + message + " @ " + LocalTime.now());
          System.exit(0); // Exit
        } else { // handle other messages
          // ignore everything else
        }
        System.out.println(message); // Debug: print all input requests
      }
    } catch (final IOException e) {} // Catch readLine() IOException and ignore
  }

  public static void writeMessage(final String message) {
    try {
      writer.write(message + "\r\n");
      writer.flush();
      System.out.print(" | Wrote:" + message + " | ");
    } catch (final IOException e) { // Ignore
      System.out.println("Got IOException @ writeMessage()");
    }
  }

  public static void handlePing(final String pingMessage, final BufferedWriter writer) {
    final String pongMessage = "PONG " + pingMessage.substring(5); // System.out.println(pongMessage);
    writeMessage(pongMessage);
  }

  public static void connect() { // Log on to the server
    System.out.println("Logging in...");
    writeMessage("NICK " + nick);
    writeMessage("USER " + login + " 0 * :Java Leet Bot");

    // Read lines from the server until it tells us we have connected
    // System.out.println("Read lines from the server until it tells us we have connected.");
    String line = null;
    try {
      while ((line = reader.readLine()) != null) {
        System.out.println(line);
        if (line.indexOf("004") >= 0) { // We are now logged in.
          break;
        } else if (line.indexOf("433") >= 0) {
          System.out.println("Nickname is already in use."); // Nickname is already in use.
          return;
        } else if (line.indexOf("ERROR") >= 0) { // Something went wrong
          System.out.println("Got ERROR! " + line);
          return;
        }
      }
    } catch (final IOException e1) {
      e1.printStackTrace();
    }
    /* try {
      Thread.sleep(5000); // Wait 5s for the server to send the welcome message
      //Thread.sleep(5000); // Wait 5s for the server to send the welcome message
    } catch (final InterruptedException e) {} // Ignore */
    System.out.println("Successfully connected!"); // Successfully connected

    // Join the channel
    writeMessage("JOIN " + channel);
    //writeMessage("PRIVMSG " + channel + " hello! @" + LocalTime.now()); // Say "hello!" to the channel
  }
}
