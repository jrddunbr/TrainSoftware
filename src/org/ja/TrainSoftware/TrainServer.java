package org.ja.TrainSoftware;

import java.io.IOException;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;
import pcduino.test.GPIO_Pin;

//button code from http://css3buttongenerator.com/

/**
 * TrainServer is the class that runs the GUI system of the interface.
 *
 * @author jared
 */
public class TrainServer {

    private static int port;//port number of interface
    private static final boolean IS_VERTICAL = true;
    //these are our pins on the pcDuino from the other class.
    private static GPIO_Pin EA, EB, FWA, RVA, SWA, SWB;
    private static double pwmA;//pwm variable from 0.0 to 1.0
    private static final double PERCISION = 10;//percision is the percision of the PWM signal out of 1
    private static boolean sw = false;//switch toggle state
    private static double speed;//speed of the train last gotten from http

    /**
     * Main
     *
     * The main program..
     *
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        pwmA = 0.0;
        EA = new GPIO_Pin(9);
        EB = new GPIO_Pin(10);
        FWA = new GPIO_Pin(8);
        RVA = new GPIO_Pin(11);
        SWA = new GPIO_Pin(12);
        SWB = new GPIO_Pin(13);
        port = 80;//set default port
        //check if there's a port in the args
        if (args.length == 1) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (Exception e) {
            }
        }
        initTrain();
        //runs the web server thread
        Thread run = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        runHTTP(port);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        //run the PWM thread - custom PWM code below.
        Thread pwma = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while (true) {
                        //calculate the time from the duty cycle percentage.
                        //pwmA is between 0.0 and 1.0
                        double duty = Math.abs(PERCISION * pwmA);
                        double noDuty = Math.abs(PERCISION * (1.0 - pwmA));
                        if (duty != 0) {
                            EA.setHIGH();
                            Thread.sleep((int) duty);
                        }
                        if (noDuty != 0) {
                            EA.setLOW();
                            Thread.sleep((int) noDuty);
                        }
                    }
                } catch (Exception ex) {
                }
            }
        });
        pwma.start();
        run.start();
        Scanner reader = new Scanner(System.in);
        //if someone puts text on the line and presses enter, the program stops.
        //this is so that someone can stop it without CTL-C while running in a CLI
        while (true) {
            if (reader.hasNext()) {
                System.exit(0);
            }
        }
    }

    /**
     * runHTTP()
     *
     * Runs a mini HTTP server for web interface
     *
     * @param port the server port to use for the web interface
     * @throws IOException
     */
    private static void runHTTP(int port) throws IOException {
        ServerSocket socket = new ServerSocket(port);
        //made a server, let's get the clients.
        while (true) {
            Socket accept = socket.accept();
            Scanner in = new Scanner(accept.getInputStream());
            PrintStream out = new PrintStream(accept.getOutputStream());
            String command = "";
            if (in.hasNextLine()) {
                command = in.nextLine();
            }
            //System.out.println("[" + command + "]");//raw headers from browser
            if (command.isEmpty()) {
                System.out.println("Closing Connection - erronous request.");
                accept.close();
            } else {
                String mode = command.substring(0, command.indexOf(" "));
                String path = command.substring(command.indexOf(" ") + 1, command.indexOf(" ", command.indexOf(" ") + 1));
                path = path.substring(1).trim();
                //print out anything that is a command.
                if (!path.equalsIgnoreCase("favicon.ico") && !path.isEmpty()) {
                    System.out.println(mode + " " + path);
                }

                //do we have a speed?
                try {
                    speed = Double.parseDouble(path);
                    controlTrain(speed);
                    System.out.println("Speed: " + speed);
                } catch (Exception e) {
                }
                
                //is there some reason that someone panics and puts stop in the CMD?
                if (path.equalsIgnoreCase("stop")) {
                    controlTrain(0);
                    System.out.println("Stopped.");
                }
                
                //switches the track when toggled. This blocks for 1 second
                if (path.equalsIgnoreCase("sw")) {
                    switchTracks();
                    System.out.print("Switched Tracks: ");
                    if (sw) {
                        System.out.println("Siding");
                    } else {
                        System.out.println("Mainline");
                    }
                    sw = !sw;
                }

                String output = "<!DOCTYPE html>\n"
                        + "<html>\n"
                        + "<head>\n"
                        + "<meta charset=\"UTF-8\">\n"
                        + "<title>Train Control</title>\n"
                        + "<style>.btn {\n"
                        + "  -webkit-border-radius: 4;\n"
                        + "  -moz-border-radius: 4;\n"
                        + "  border-radius: 4px;\n"
                        + "  color: #ffffff;\n"
                        + "  font-size: 20px;\n"
                        + "  background: #111111;\n"
                        + "  padding: 5px 5px 5px 5px;\n"
                        + "  border: solid #444444 2px;\n"
                        + "  text-decoration: none;\n"
                        + "}\n"
                        + ".ind {\n"
                        + "  -webkit-border-radius: 4;\n"
                        + "  -moz-border-radius: 4;\n"
                        + "  border-radius: 4px;\n"
                        + "  color: #ffffff;\n"
                        + "  font-size: 20px;\n"
                        + "  background: #111111;\n"
                        + "  padding: 5px 5px 5px 5px;\n"
                        + "  border: solid #444444 2px;\n"
                        + "  text-decoration: none;\n"
                        + "  text-align: center;\n"
                        + "}\n"
                        + "\n"
                        + ".btn:hover {\n"
                        + "  background: #3cb0fd;\n"
                        + "  text-decoration: none;\n"
                        + "}\nhtml{background:black;}\n"
                        + "</style>"
                        + "</head>\n"
                        + "<body>\n<table><tr><td>"
                        + "<div style=\"padding: 20px 10px 20px 10px;\">"
                        + "<a href=\"10\" class=\"btn\">10</a>\n"
                        + "<br/><br/><a href=\"8\" class=\"btn\">8</a>\n"
                        + "<br/><br/><a href=\"6\" class=\"btn\">6</a>\n"
                        + "<br/><br/><a href=\"4\" class=\"btn\">4</a>\n"
                        + "<br/><br/><a href=\"2\" class=\"btn\">2</a>\n"
                        + "<br/><br/><a href=\"0\" class=\"btn\">0</a>\n"
                        + "<br/><br/><a href=\"-2\" class=\"btn\">-2</a>\n"
                        + "<br/><br/><a href=\"-4\" class=\"btn\">-4</a>\n"
                        + "<br/><br/><a href=\"-6\" class=\"btn\">-6</a>\n"
                        + "<br/><br/><a href=\"-8\" class=\"btn\">-8</a>\n"
                        + "<br/><br/><a href=\"-10\" class=\"btn\">-10</a>\n"
                        + "</div></td><td>"
                        //replace http://ja13.tk/ with your own hostname if you wish
                        //todo - make the builtin web server serve out the images.
                        + "<a href=\"sw\"><img src=\"http://ja13.tk/";
                if (sw) {
                    output += "siding.png";
                } else {
                    output += "mainline.png";
                }
                output += "\"></img></a><br/><div class=\"ind\">" + (int)speed + "</div></td></tr></table>\n"
                        + "    </body>\n"
                        + "</html>\n";

                /* 
If we are in the root action directory (or approved directory),
then return some info, otherwise do a 302 redirect to known territory

Typically we leave known territory when making requests, and they
bump us right back to the root thanks to the 302. The browser gives us the
ability to collect data thanks to the redirect since it calls a function
from the path that we specified in the initial redirect link that we click.
                 */
                if (path.isEmpty()) {
                    //basic HTTP response with success. Makes the browser happy.
                    out.println("HTTP/1.1 200 OK");
                    out.println("Connection: close");
                    out.println("Content-Type: text/html");
                    out.println("Content-Length: " + output.length());
                    out.println();
                    out.println(output);
                } else if (path.equals("favicon.ico")) {
                    //well, browsers like to get favicons so let's just not.
                    out.println("HTTP/1.1 400 NOT FOUND");
                } else {
                    //redirect to the root directory within the browser,
                    //the user doesn't see anything.
                    out.println("HTTP/1.1 302 Found");
                    out.println("Location: /");
                }

                accept.close();
            }
        }
    }

    //this sets all the output pins and disables movement.
    private static void initTrain() {
        EA.setModeOUTPUT();
        EB.setModeOUTPUT();
        FWA.setModeOUTPUT();
        SWA.setModeOUTPUT();
        RVA.setModeOUTPUT();
        SWB.setModeOUTPUT();
        EA.setLOW();
        EB.setLOW();
    }

    //this will get the speed of the train, and set the PWM signal speed.
    //It also will set the direction of the pins for the motor controller.
    private static void controlTrain(double speed) {
        if (speed < 0) {
            FWA.setHIGH();
            RVA.setLOW();
            pwmA = Math.abs(speed) / PERCISION;
        } else if (speed > 0) {
            FWA.setLOW();
            RVA.setHIGH();
            pwmA = Math.abs(speed) / PERCISION;
        } else {
            FWA.setLOW();
            RVA.setLOW();
            pwmA = 0.0;
        }
    }

    //this will cause the switch to get a 1000ms pulse to switch directions.
    private static void switchTracks() {
        if (sw) {
            SWA.setLOW();
            SWB.setHIGH();
            EB.setHIGH();
            try {
                Thread.sleep(500);
            } catch (Exception e) {
            }
            EB.setLOW();
        } else {
            SWA.setHIGH();
            SWB.setLOW();
            EB.setHIGH();
            try {
                Thread.sleep(500);
            } catch (Exception e) {
            }
            EB.setLOW();
        }
    }
}
