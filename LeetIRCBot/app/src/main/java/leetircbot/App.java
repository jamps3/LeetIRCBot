package leetircbot;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.math.RoundingMode;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.time.Instant;
import java.time.LocalTime;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.json.JSONObject;
import org.jsoup.Jsoup;
//import org.jsoup.nodes.Document;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class App {

  // TODO:
  // [DONE] Multiple channels

  // Instance variables
  static BufferedWriter writer; // Stream writer
  static BufferedReader reader; // Stream reader

  // Servers:
  static final String server = "irc.atw-inter.net"; // 32s
  //static final String server = "ircnet.clue.be"; // 33s
  //static final String server = "ircnet.hostsailor.com"; // 32s
  //static final String server = "irc.nlnog.net"; // 32s
  //static final String server = "irc.psychz.net"; // 32s
  //static final String server = "irc.swepipe.net"; // 32s
  //static final String server = "irc.nerv.fi"; // port: 6697
  static int port = 6667;
  //static int port = 6697;
  static Socket socket;
  static String nick = "jL3b2";
  static String login = "jL3b2";
  //static String channel = "#joensuu"; // Joensuu!
  static String channel = "#bottitest";
  static boolean leeting = false;
  static boolean precisionLeeted = false;
  static boolean leeted = false;
  static int leets = 0;
  static int kraks = 0;
  //static int offset = 001223500;
  static int offset = 0;
  static final String weatherApiKey = "e0f8100160453dd795fc0d4ef9fdbda3";
  static String location = "Joensuu,FI";
  static String urlString = "http://api.openweathermap.org/data/2.5/weather?lang=fi&q=";

  // Sample url: https://api.openweathermap.org/data/2.5/weather?q=Joensuu,FI&appid=e0f8100160453dd795fc0d4ef9fdbda3

  /**
   * Main class.
   * 1) Connects to the server
   * 2) Logs in to the server and joins the initial channel
   * 3) Starts reading the server output and responding to it
   * @param args Command line parameters
   */
  public static void main(final String[] args) {
    while (true) { // Loop
      try {
        load(); // Load the saved data
        long startTime = System.currentTimeMillis(); // Start counting the time it takes to connect to the server
        socket = new Socket(server, port); // Connect to server
        //writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8.newEncoder())); // UTF-8 support
        //reader = new BufferedReader(new InputStreamReader(socket.getInputStream()), StandardCharsets.UTF_8);
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8.newDecoder())); // UTF-8 support
        login(); // Log in to the server and join the initial channel
        long endTime = System.currentTimeMillis(); // Stop counting the time it takes to connect to the server
        long elapsedTime = endTime - startTime; // Calculate the elapsed time
        double elapsedTimeInSeconds = elapsedTime / 1000.0; // Convert the elapsed time to seconds
        out("[Connecting took: " + elapsedTimeInSeconds + " seconds.]"); // Log the elapsed time to console

        // Start leetTimeChecking and leet when it is leet time.
        //leetTimeChecking(LocalTime.of(13, 37, 13, 371337133), LocalTime.of(13, 37, 14, 371337133));
        while (true) { // Read continuously
          read(); // Continue reading the stream - the main program is running here.
        }
      } catch (SocketException | UnknownHostException e) {
        e.printStackTrace();
        out("Server error.");
      } catch (IOException e) {
        out("IOException@main()"); // ignore
      } finally {
        try {
          socket.close(); // Close the socket correctly
        } catch (IOException | NullPointerException e) {} // ignore socket close errors
        out("Bot exited successfully.");
      }
    }
  }

  /**
   * Starts checking when the given targetTime is now with 0.1s intervals.
   * When the targetTime is reached this method calls the SendLeetMessage.
   * @param targetTime
   * @param timeLimit
   */
  public static void leetTimeChecking(LocalTime targetTime, String targetChannel) {
    out("leetTimeChecking() started.");
    LocalTime targetTimeMinusNanos = targetTime.minusNanos(1000000000); // It is 1 billion nanoseconds to leet! Prepare the cpu!
    final Timer timer = new Timer();
    timer.scheduleAtFixedRate(
      new TimerTask() {
        @Override
        public void run() {
          if (!leeted) { // Leet only once!
            if (!leeting) { // Make sure we are not running duplicated requests
              if (LocalTime.now().isAfter(targetTime.plusSeconds(1))) { // Limit reached!
                out("[LIMIT] " + targetTime.plusSeconds(1));
                this.cancel(); // Stop the timer
              } else if (LocalTime.now().isAfter(targetTimeMinusNanos)) {
                out("[LEET!] Target: " + targetTime);
                SendLeetMessage(targetChannel, "leet", targetTime); // Start SuperChecking the time and leet when targetTime is reached.
                leeted = true;
                out("[!LEET] Target: " + targetTime);
              } else {} //System.out.print("."); // waiting...
            } else {} //Some other thread is leeting already
          }
        }
      },
      0,
      1 * 100 // check every 0.1s
    );
  }

  /**
   * Sends the leet message to the channel at the target time.
   * @param channel
   * @param message
   * @param target
   */
  public static void SendLeetMessage(final String channel, final String message, LocalTime target) {
    leeting = true; // Tell other methods that we are leeting
    out("SendLeetMessage() started. I will leet @ " + target + " | SuperChecking time now! ->");
    int rounds = 0;
    target = target.minusNanos(949867); // Offset compensation for the time it takes to run this method and send the message
    target = target.minusNanos(405621211); // Offset compensation for the lag between the bots
    target = target.minusNanos(offset); // Offset compensation for extra lag
    if (!precisionLeeted) {
      while (true) {
        LocalTime now = LocalTime.now();
        int comparison = now.compareTo(target);
        if (comparison < 0) {
          //System.out.print(".");
        } else if (comparison >= 0) { // target is before now or times are equal
          writeMessage("PRIVMSG " + channel + " :" + message); // Send the leet message here
          out("We have leeted."); // This should be after the writeMessage() method to be sure that this doesn't take time
          precisionLeeted = true; // We have leeted
          break; // Exit loop
        }
        rounds++;
      }
    }
    out("SuperChecking finished. Took " + rounds + " rounds.");
    precisionLeeted = false;
    leeting = false;
  }

  /**
   * Prints the given string to console with a timestamp.
   * @param string
   */
  private static void out(String string) {
    LocalTime currentTime = LocalTime.now();
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSSSSSS");
    String formattedTime = currentTime.format(formatter);
    System.out.println("|" + formattedTime + "| " + string);
  }

  /**
   * Handles ALL requests like PING.
   */
  public static void read() {
    String message = null;
    try {
      while ((message = reader.readLine()) != null) {
        out(message); // Print all input to console
        if (message.startsWith("PING")) { // PING received
          handlePing(message);
        } else if (message.contains("PRIVMSG")) { // PRIVMSG received so we can get the channel/user from it
          channel = message.substring(message.indexOf("PRIVMSG") + 8, message.indexOf(" :")); // Get the channel/user
          if (channel.compareTo(nick) == 0) { // This is a private conversation so we need to set the channel to the user
            channel = message.substring(1, message.indexOf("!")); // Get the user
          }
        }
        if (message.contains("MJÄLLIT")) { // Laula Kulkuset!
          out("Got MJÄLLIT request: " + message);
          writeMessage("PRIVMSG " + channel + " :Kulkuset! Kulkuset! Riemuin helkkäilee!");
        } else if (message.toLowerCase().contains("sano jou")) { // Sano Jou
          out("Got Jou request: " + message);
          writeMessage("PRIVMSG " + channel + " Jou");
        } else if (message.toLowerCase().contains("!aika")) { // Kerro aika
          if (message.toLowerCase().lastIndexOf("!s") > message.indexOf("PRIVMSG")) { // Make sure the !s is not in the ident/username.
            out("Got !aika request: " + message);
            writeMessage("PRIVMSG " + channel + " " + LocalTime.now());
          }
        } else if (message.toLowerCase().contains("asetaviive()")) { // Aseta viive
          out("Got asetaviive() request " + message);
          int index = message.indexOf("asetaviive()");
          offset = Integer.parseInt(message.substring(index + 13, index + 22));
          out("Offset: " + offset);
        } else if (message.toLowerCase().contains("krak") | message.toLowerCase().contains("kr0k") | message.toLowerCase().contains("kr1k")) { // Realtime krak counter!
          kraks++; // Count kraks
          out("Got krak: " + message + " @ " + " Total kraks: " + kraks);
          save(); // Save the kraks to file
        } else if (message.contains("!leettaa@")) { // Set leet timer
          String s = "!leettaa@";
          int index = message.indexOf(s);
          LocalTime lt = LocalTime.of(Integer.parseInt(message.substring(index + 9, index + 11)), Integer.parseInt(message.substring(index + 11, index + 13)));
          lt = lt.withSecond(13);
          lt = lt.withNano(offset);
          out("Target: " + lt + " offset: " + offset);
          leeted = false; // Reset leeted flag - important for starting leetTimeChecking again!
          channel = message.substring(index + 13);
          out(channel);
          leetTimeChecking(lt, channel);
        } else if (message.contains("leet")) { // Count leets
          leets++;
          out("Got leet: " + message + " Total leets: " + leets);
          save(); // Save the leets to file
        } else if (message.contains("!kaiku ")) { // Echo messages
          out("Got echo request: " + message); // :user!id@domain.com PRIVMSG jL3b :!kaiku lol
          message = message.substring(message.indexOf("!kaiku") + 7); // Parse the beginning of the message out.
          writeMessage("PRIVMSG " + channel + " :" + message);
        } else if (message.contains("!join@")) { // Join channel (with key)
          out("Got /JOIN channel: " + message);
          String kanavalle = message.substring(message.indexOf("!join@") + 6);
          writeMessage("JOIN " + kanavalle); // Join channel
          if (message.lastIndexOf(" ") < message.indexOf("!join@")) {
            channel = message.substring(message.indexOf("!join@") + 6); // Update channel variable
          } else {
            channel = message.substring(message.indexOf("!join@") + 6, message.lastIndexOf(" ")); // Update channel variable, need to strip the channel key
          }
        } else if (message.contains("!sahko@")) { // Get current electricity price
          out("Got !sahko@ request: " + message);
          int position = LocalTime.now().getHour();
          if (!message.substring(message.indexOf("!sahko@") + 7).isEmpty()) {
            if (message.substring(message.indexOf("!sahko@") + 7).length() < 3) {
              try {
                position = Integer.parseInt(message.substring(message.indexOf("!sahko@") + 7));
                if (position > 24) {
                  position = 24;
                }
                if (position < 1) {
                  position = 1;
                }
              } catch (java.lang.NumberFormatException e) {}
            }
          }
          try {
            Instant now = Instant.now();
            ZonedDateTime zonedDateTime = now.atZone(ZoneId.systemDefault());
            int year = now.atZone(ZoneId.systemDefault()).getYear();
            Month month = now.atZone(ZoneId.systemDefault()).getMonth();
            String formattedMonth = String.format("%02d", month.getValue());
            int dayOfMonth = zonedDateTime.getDayOfMonth();
            String formattedDayOfMonth = String.format("%02d", dayOfMonth);
            //String start = "202302042300";
            String start = year + formattedMonth + formattedDayOfMonth + "0000";
            //String end = "202302052300";
            String formattedDayOfMonthPlusOne = String.format("%02d", dayOfMonth + 1);
            String end = year + formattedMonth + formattedDayOfMonthPlusOne + "2300";
            Document result = getElectricityPrices(start, end);
            if (result != null) {
              DecimalFormat df = new DecimalFormat("#.##");
              df.setRoundingMode(RoundingMode.HALF_UP);
              String priceAmount = getHourPriceAmount(position, result);
              double priceAmountd = Double.parseDouble(priceAmount);
              priceAmountd = priceAmountd / 10;
              out("Price amount for position " + position + " is " + df.format(priceAmountd));
              int position2;
              if (position == 24) {
                position2 = 0;
              } else {
                position2 = position + 1;
              }
              writeMessage(
                "PRIVMSG " +
                channel +
                " :" +
                "Pörssisähkön hinta " +
                dayOfMonth +
                "." +
                month.getValue() +
                ". klo " +
                position +
                "-" +
                position2 +
                ": " +
                df.format(priceAmountd * 1.1) +
                " snt/kWh (alv 10 %)"
              );
            }
          } catch (Exception e) { // Should ignore this Traceback later!
            e.printStackTrace();
          }
        } else if (message.contains("!suorita@")) { // Custom commands
          out("[COMMAND] Got command request: " + message);
          String command = message.substring(message.indexOf("!suorita@") + 9);
          writeMessage(command); // Send command
        } else if (message.toLowerCase().contains("!s")) { // Tell weather
          if (message.toLowerCase().lastIndexOf("!s") > message.indexOf("PRIVMSG")) { // Make sure the !s is not in the ident/username.
            // out("!s: " + message.toLowerCase().indexOf("!s"));
            out("Got Weather request: " + message); // :user!id@domain.com PRIVMSG jL3b :!s
            String city = "Joensuu"; // Default city
            if (message.lastIndexOf("!s") + 3 < message.length()) {
              city = message.substring(message.lastIndexOf("!s") + 3); // Parse the city/station name
            }
            if (city.toLowerCase().compareTo("riäkkylä") == 0) {
              writeMessage("PRIVMSG " + channel + " :Tämä on mainos!"); // Tell that this is an advertisement
            } else {
              JSONObject json = getWeather(city); // Get the JSON from the API
              if (json == null) { // If the city is not found
                writeMessage("PRIVMSG " + channel + " :City not found!"); // Tell that the city was not found
                return; // Return from the method
              }
              city = json.getString("name") + "," + json.getJSONObject("sys").getString("country");
              out(city);
              float temperature = json.getJSONObject("main").getFloat("temp"); // Gets the temperature
              float tempFeelsLike = json.getJSONObject("main").getFloat("feels_like"); // Gets the feels like temperature
              int humidity = json.getJSONObject("main").getInt("humidity"); // Gets the humidity
              int visibility = json.getInt("visibility"); // Gets the visibility
              int visibilityf = Math.round(visibility / 1000f);
              float tempCelsius = temperature - 272.15f; // Convert Kelvin to Celsius
              float tempFeelsLikeCelsius = tempFeelsLike - (float) 272.15; // Convert Kelvin to Celsius
              float roundedC = (float) (Math.round(tempCelsius * 100) / 100.0);
              float roundedFLC = (float) (Math.round(tempFeelsLikeCelsius * 100) / 100.0);
              float wind = json.getJSONObject("wind").getFloat("speed"); // Gets the wind speed
              writeMessage(
                "PRIVMSG " + channel + " :" + city + ": " + roundedC + "°C (~ " + roundedFLC + "°CFL), 💦 " + humidity + "%, 👁 " + visibilityf + "km, 🍃 " + wind + " m/s"
              );
            }
          }
        } else if (message.toLowerCase().contains("http://") | message.toLowerCase().contains("https://") | message.toLowerCase().contains("www.")) { // Tell the page title from URL
          if (!message.contains(server)) { // Filter out server hello message urls
            out("Got title request from " + message);
            String title = null;
            if (message.toLowerCase().contains("http://")) {
              int space = message.indexOf(" ", message.toLowerCase().indexOf("http://")); // Find the next space after the url
              if (space != -1) {
                title = getTitle(message.substring(message.indexOf("http://"), space)); // Get the title from the url, parse the end of the message out
              } else {
                title = getTitle(message.substring(message.indexOf("http://"))); // Get the title from the url, parse the end of the message out
              }
            } else if (message.toLowerCase().contains("https://")) {
              int space = message.indexOf(" ", message.toLowerCase().indexOf("https://")); // Find the next space after the url
              if (space != -1) {
                title = getTitle(message.substring(message.indexOf("https://"), space)); // Get the title from the url, parse the end of the message out
              } else {
                title = getTitle(message.substring(message.indexOf("https://"))); // Get the title from the url, parse the end of the message out
              }
            } else if (message.toLowerCase().contains("www.")) {
              int space = message.indexOf(" ", message.toLowerCase().indexOf("www.")); // Find the next space after the url
              if (space != -1) {
                title = getTitle("http://" + message.substring(message.indexOf("www."), space)); // Get the title from the url, parse the end of the message out
              } else {
                title = getTitle("http://" + message.substring(message.indexOf("www."))); // Get the title from the url, parse the end of the message out
              }
            }
            if (title != null) {
              writeMessage("PRIVMSG " + channel + " :Sivun otsikko: " + title);
            }
          }
        } else if (message.contains("!potku perseelle!")) { // Exit
          out("Got exit request: " + message);
          //System.exit(0); // Exit ignored for now
        } else { // handle other messages
          // happily ignore everything else :)
        }
      }
    } catch (final IOException e) {} // Catch readLine() IOException and ignore
  }

  /**
   * Extracts the page title from the given URL
   * @param urlString
   * @return
   */
  public static String getTitle(String url) throws IOException {
    try {
      URL urlObj = new URL(url);
      HttpURLConnection conn = (HttpURLConnection) urlObj.openConnection();
      conn.setInstanceFollowRedirects(false);

      int statusCode = conn.getResponseCode();
      if (statusCode == HttpURLConnection.HTTP_MOVED_PERM || statusCode == HttpURLConnection.HTTP_MOVED_TEMP) { // Handle redirects
        String redirectUrl = conn.getHeaderField("Location");
        conn.disconnect();

        urlObj = new URL(redirectUrl);
        conn = (HttpURLConnection) urlObj.openConnection();
        conn.setInstanceFollowRedirects(true);
      }
      // read data from the connection input stream as before
      BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
      String inputLine;
      StringBuilder content = new StringBuilder();
      while ((inputLine = in.readLine()) != null) {
        content.append(inputLine);
      }
      in.close();
      Pattern titlePattern = Pattern.compile("<title[^>]*>([\\s\\S]*?)</title>", Pattern.CASE_INSENSITIVE);
      Matcher titleMatcher = titlePattern.matcher(content);
      if (titleMatcher.find()) {
        return titleMatcher.group(1);
      }
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
    return null;
  }

  public static String getPDFTitle(String url) throws IOException {
    URL pdfUrl = new URL(url);
    URLConnection urlConnection = pdfUrl.openConnection();
    InputStream inputStream = urlConnection.getInputStream();
    byte[] buffer = new byte[256];
    StringBuilder sb = new StringBuilder();
    int bytesRead = 0;
    while ((bytesRead = inputStream.read(buffer)) != -1) {
      sb.append(new String(buffer, 0, bytesRead));
    }
    inputStream.close();
    Pattern pattern = Pattern.compile("<title>(.*)</title>");
    Matcher matcher = pattern.matcher(sb.toString());
    if (matcher.find()) {
      return matcher.group(1);
    } else {
      return null;
    }
  }

  /* BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
    String inputLine;
    //Pattern titlePattern = Pattern.compile("<title>(.*?)</title>");
    Pattern titlePattern = Pattern.compile("<title>([\\s\\S]*?)</title>");
    // <title>(11) Etusivu / Twitter</title>
    while ((inputLine = br.readLine()) != null) {
      Matcher titleMatcher = titlePattern.matcher(inputLine);
      if (titleMatcher.find()) {
        return titleMatcher.group(1);
      }
    }
    br.close();
    return ""; */

  /**
   * Gets the electricity price for the given hour from the Document and returns it as a String
   * @param position
   * @param doc
   * @return
   * @throws Exception
   */
  public static String getHourPriceAmount(int position, Document doc) throws Exception {
    //optional, but recommended
    //read this - http://stackoverflow.com/questions/13786607/normalization-in-dom-parsing-with-java-how-does-it-work
    //doc.getDocumentElement().normalize();
    //printDocument(doc);

    NodeList pointNodes = doc.getElementsByTagName("Point");

    for (int i = 0; i < pointNodes.getLength(); i++) {
      Node pointNode = pointNodes.item(i);
      NodeList children = pointNode.getChildNodes();
      for (int j = 0; j < children.getLength(); j++) {
        Node child = children.item(j);
        if (child.getNodeName().equals("position")) {
          int pos = Integer.parseInt(child.getTextContent());
          if (pos == position) {
            NodeList pointNodeChildren = pointNode.getChildNodes();
            for (int k = 0; k < pointNodeChildren.getLength(); k++) {
              Node childNode = pointNodeChildren.item(k);
              if (childNode.getNodeName().equals("price.amount")) {
                String priceAmount = childNode.getTextContent(); // Get text content from price.amount Node
                // out("priceAmount: " + priceAmount); // Output to console
                return priceAmount; // Return the price amount
              }
            }
          }
        }
      }
    }
    return "0";
  }

  /**
   * Prints the XML Document object to the console.
   * @param doc
   * @throws TransformerFactoryConfigurationError
   * @throws TransformerConfigurationException
   * @throws TransformerException
   */
  private static void printDocument(Document doc) throws TransformerFactoryConfigurationError, TransformerConfigurationException, TransformerException {
    TransformerFactory transformerFactory = TransformerFactory.newInstance();
    Transformer transformer = transformerFactory.newTransformer();
    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
    transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
    StringWriter writer = new StringWriter();
    transformer.transform(new DOMSource(doc), new StreamResult(writer));
    String output = writer.getBuffer().toString();
    out(output);
  }

  /**
   * Writes message to the server.
   * @param message
   */
  public static void writeMessage(final String message) {
    try {
      writer.write(message + "\r\n"); // Write the message
      writer.flush(); // Flush the buffer(actual write)
      out("[Sent:] " + message); // Print all output to console
    } catch (final IOException e) { // Ignore
      out("Got IOException @ writeMessage()");
    }
  }

  /**
   * Handles the PING message.
   * @param pingMessage
   */
  public static void handlePing(final String pingMessage) { // Handles ping requests
    final String pongMessage = "PONG " + pingMessage.substring(5); // Create the PING reply
    writeMessage(pongMessage); // Send the PING reply
  }

  /**
   * Log in and join the initial channel
   */
  public static void login() {
    out("Logging in...");
    writeMessage("NICK " + nick); // Set the NICK
    writeMessage("USER " + login + " 0 * :Leet Bot"); // Set the USER

    out("Read lines from the server until it tells us we have connected.");
    String line = null;
    try {
      while ((line = reader.readLine()) != null) {
        out(line);
        if (line.indexOf("004") >= 0) { // We are now logged in.
          break;
        } else if (line.indexOf("433") >= 0) {
          out("Nickname is already in use."); // Nickname is already in use.
          return;
        } else if (line.indexOf("ERROR") >= 0) { // Something went wrong
          out("Got ERROR! " + line); // Show the error message
          return;
        }
      }
    } catch (final IOException e1) { // Catch IOExceptions
      e1.printStackTrace(); // Print the error message and exit
    }
    out("[Successfully connected!]"); // Successfully connected
    // Join the initial channel
    writeMessage("JOIN " + channel);
    //writeMessage("PRIVMSG " + channel + " hello!"); // Say "hello!" to the channel
  }

  /**
   * Gets the current weather, currently from openweathermap.
   * @return JSONObject containing the current weather information.
   */
  public static JSONObject getWeather(String location) {
    try {
      URL url = new URL(urlString + URLEncoder.encode(location, "UTF-8") + "&appid=" + weatherApiKey);
      URLConnection connection = url.openConnection();
      String contentType = connection.getHeaderField("Content-Type");
      System.out.println("Content-Type: " + contentType);
      InputStream inputStream = connection.getInputStream();
      byte[] buffer = new byte[1024];
      int bytesRead;
      StringBuilder stringBuilder = new StringBuilder();
      while ((bytesRead = inputStream.read(buffer)) > 0) {
        stringBuilder.append(new String(buffer, 0, bytesRead, StandardCharsets.UTF_8)); // We need UTF-8 here!
      }
      JSONObject json = new JSONObject(stringBuilder.toString());
      out(stringBuilder.toString());
      return json;
    } catch (IOException | NullPointerException e) { // Catch IOExceptions and fail
      //e.printStackTrace();
    }
    return null; // Something went wrong, we must return a null JSONObject.
  }

  /**
   * Returns the day-ahead prices for electricity at the specified range of start-end.
   * @param start
   * @param end
   * @return Document
   */
  public static Document getElectricityPrices(String start, String end) {
    // Browser interface: https://web-api.tp.entsoe.eu/transmission-domain/r2/dayAheadPrices/show?name=&defaultValue=true&viewType=GRAPH&areaType=BZN&atch=false&dateTime.dateTime=06.02.2023+00:00|CET|DAY&biddingZone.values=CTY|10YFI-1--------U!BZN|10YFI-1--------U&resolution.values=PT15M&resolution.values=PT30M&resolution.values=PT60M&dateTime.timezone=CET_CEST&dateTime.timezone_input=CET+(UTC+1)+/+CEST+(UTC+2)
    // Old API URL (until 14.02.2023): https://transparency.entsoe.eu/api?securityToken=8ce02483-b620-44c6-af76-a26350fb0333&documentType=A44&in_Domain=10YFI-1--------U&out_Domain=10YFI-1--------U&periodStart=202302060000&periodEnd=202302062300
    // New API Example URL: https://web-api.tp.entsoe.eu/api
    String apiKey = "8ce02483-b620-44c6-af76-a26350fb0333";
    String urlString =
      "https://web-api.tp.entsoe.eu/api?securityToken=" +
      apiKey +
      "&documentType=A44&in_Domain=10YFI-1--------U&out_Domain=10YFI-1--------U&periodStart=" +
      start + // start example: 202302050000
      "&periodEnd=" +
      end; // end example: 202302052300

    URL url;
    HttpURLConnection con;
    try {
      url = new URL(urlString);
      con = (HttpURLConnection) url.openConnection();
      con.setRequestMethod("GET");
      DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
      DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
      Document doc = dBuilder.parse(con.getInputStream());
      return doc;
    } catch (IOException | SAXException | ParserConfigurationException e) { // ignore
      //e.printStackTrace(); // ProtocolException, MalformedURLException, FilenotFoundException too
    }
    return null; // Something went wrong, we must return a null Document
  }

  public static void save() { // Save the values to a file
    try (FileOutputStream fos = new FileOutputStream("values.bin"); ObjectOutputStream oos = new ObjectOutputStream(fos)) {
      oos.writeInt(kraks);
      oos.writeInt(leets);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static void load() { // Read the values from the file
    try (FileInputStream fis = new FileInputStream("values.bin"); ObjectInputStream ois = new ObjectInputStream(fis)) {
      kraks = ois.readInt();
      leets = ois.readInt();
      out("Kraks: " + kraks);
      out("Leets: " + leets);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
