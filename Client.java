import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class Client {
    public static void main(String[] args) {
        Scanner input;
        PrintWriter output;
        Scanner consoleInput;
        String response;
        String loginCommand;
        try {
            Socket soc = new Socket("localhost", 3360);

            System.out.print("        *=============     Welcome to Uprise Sacco     =============*\n");
            input = new Scanner(soc.getInputStream());
            output = new PrintWriter(soc.getOutputStream(), true);
            consoleInput = new Scanner(System.in);

            // Login flow
            boolean loggedIn = false;

            while (!loggedIn) {
                System.out.println("         Enter login command (login <username> <password>):");
                loginCommand = consoleInput.nextLine();
                // Send login command to the server
                output.println(loginCommand);

                response = input.nextLine();

                if (response.equals("Login successful!")) {
                    loggedIn = true;
                    System.out.println(
                            "\n        *************   Login successful! Chose an Option.   *****************\n");
                    System.out
                            .println("        * Deposit:        deposit <amount> <date_deposited> <receipt_number> *");
                    System.out
                            .println("        * CheckStatement: CheckStatement <dateFrom> <dateTo>                 *");
                    System.out
                            .println("        * Loan Request:   requestLoan <amount> <paymentPeriod_in_months>     *");
                    System.out
                            .println("        * Request Status: LoanRequestStatus <loan_application_number>        *");
                    System.out.println(
                            "        * Exit Program:   exit                                               *\n");
                    System.out
                            .println("        **********************************************************************");

                } else if (response.equals("Login failed.")) {

                    System.out.println(" ");
                    System.out.println("Login failed. Member recovery required.");
                    System.out.println(" ");
                    System.out.print("Enter memberID: ");
                    String memberID = consoleInput.nextLine();
                    System.out.print("Enter phone number: ");
                    String phoneNumber = consoleInput.nextLine();

                    // Send member recovery command to the server
                    output.println("recover " + memberID + " " + phoneNumber);
                    response = input.nextLine();
                    System.out.println(" ");
                    System.out.println(response);

                } else {
                    System.out.println("Login failed. Please try again.");
                }
            }

            // Handle user commands after successful login
            String command;
            while (loggedIn && consoleInput.hasNextLine()) {
                command = consoleInput.nextLine();
                output.println(command);
                response = input.nextLine();
                System.out.println("        ******" + response + "*******");
            }

            soc.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
