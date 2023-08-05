import java.io.*;
import java.net.*;
import java.util.*;
import java.sql.*;

public class Server {
    public static void main(String[] args) {
        try {
            ServerSocket serverSocket = new ServerSocket(3360);
            System.out.println("Server started and listening on port 3360...");

            while (true) {
                Socket soc = serverSocket.accept();
                System.out.println("Client connected: " + soc.getInetAddress().getHostAddress());

                // Start a new thread to handle each client connection
                ClientHandler clientHandler = new ClientHandler(soc);
                clientHandler.start();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

class ClientHandler extends Thread {
    private Socket soc;
    private Scanner input;
    private static PrintWriter output;
    private String command;
    private String response;
    public static String drivers = "com.mysql.cj.jdbc.Driver";
    private String url = "jdbc:mysql://localhost:3300/Uprise_sacco";
    private String userName = "root";
    private String passWord = "";
    String password;
    public static String active_user;
    public static String loanApplicationNumber;
    public static int client_id = 0;

    public ClientHandler(Socket soc) {
        try {
            this.soc = soc;
            input = new Scanner(soc.getInputStream());
            output = new PrintWriter(soc.getOutputStream(), true);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void run() {
        try {
            while (input.hasNextLine()) {
                command = input.nextLine();
                String[] commandParts = command.split(" ");
                response = "";
                Class.forName(drivers);
                Connection con = DriverManager.getConnection(url, userName, passWord);
                // Handle different commands
                switch (commandParts[0]) {

                    case "login":
                        String username = commandParts[1];
                        String password = commandParts[2];
                        response = memberLogin(username, password);
                        break;
                    case "recover":
                        String memberID = commandParts[1];
                        String phoneNumber = commandParts[2];
                        response = recoveryResponse(memberID, phoneNumber);

                        break;
                    case "deposit":
                        double amount = Double.parseDouble(commandParts[1]);
                        String dateDeposited = commandParts[2];
                        String receiptNumber = commandParts[3];
                        response = handleDeposit(amount, dateDeposited, receiptNumber);
                        break;
                    case "CheckStatement":
                        String dateFrom = commandParts[1];
                        String dateTo = commandParts[2];

                        break;
                    case "requestLoan":
                        double loanAmount = Double.parseDouble(commandParts[1]);
                        int paymentPeriod = Integer.parseInt(commandParts[2]);
                        try {
                            int applicationNumber = generateRandom8DigitNumber();
                            String user_id_query = "SELECT client_id from client where username = ?";
                            PreparedStatement userIDStatement = con.prepareStatement(user_id_query);
                            userIDStatement.setString(1, active_user);
                            ResultSet resultSet = userIDStatement.executeQuery();
                            if (resultSet.next()) {
                                client_id = resultSet.getInt("client_id");
                                System.out.println("Client ID: " + client_id);
                                String loanQuery = "INSERT INTO loan (loan_request_amount, payment_period, client_id, loan_application_no) VALUES (?, ?, ?, ?)";
                                PreparedStatement statement = con.prepareStatement(loanQuery);
                                statement.setDouble(1, loanAmount);
                                statement.setInt(2, paymentPeriod);
                                statement.setInt(3, client_id); // Use the actual client_id fetched from the database
                                statement.setInt(4, applicationNumber);
                                statement.executeUpdate(); // Execute the INSERT statement
                            } else {
                                System.out.println("No client found for the given username.");
                            }
                            String resp_detail = ("Loan request for {" + active_user
                                    + "} Loan Application Number:     {" + applicationNumber
                                    + "}   Successfully Sent. ");
                            response = (resp_detail);
                            // response = (resp_detail);

                            System.out.println(resp_detail);
                            con.close();
                        } catch (Exception e) {
                            // e.printStackTrace(output);
                            // More pending validations to be done.
                            String loaned = "SELECT * from loan where client_id = ? and grant_status IN(?,?) and loan_progress_status != 100";
                            PreparedStatement userIDStatement = con.prepareStatement(loaned);
                            String m1 = "No Action Yet";
                            String m2 = "Approved";

                            userIDStatement.setInt(1, client_id);
                            userIDStatement.setString(2, m1);
                            userIDStatement.setString(3, m2);

                            ResultSet resultSet = userIDStatement.executeQuery();
                            if (resultSet.next()) {
                                int loan_request_amount = resultSet.getInt("loan_request_amount");
                                int applicationNumber = resultSet.getInt("loan_application_no");
                                String result = "Request Failed! You have unsettled loan balance  "
                                        + loan_request_amount + " under receipt No. " + applicationNumber + "";
                                response = result;
                            }

                        }
                        break;
                    case "LoanRequestStatus":
                        loanApplicationNumber = commandParts[1];
                        String grant_status = "Approved";
                        String loan_request_status = "SELECT * from loan where loan_application_no = ? AND grant_status = ? AND client_id = ?";
                        PreparedStatement loan_details = con.prepareStatement(loan_request_status);
                        loan_details.setString(1, loanApplicationNumber);
                        loan_details.setString(2, grant_status);
                        loan_details.setInt(3, client_id);
                        // System.out.println(client_id);

                        ResultSet resultSet = loan_details.executeQuery();

                        if (resultSet.next()) {
                            // System.out.println("Loan Has been Approved");
                            String grant = resultSet.getString("grant_status");
                            int clearance_status = resultSet.getInt("clearance_status");
                            int monthly_payment_plan = resultSet.getInt("monthly_payment_plan");
                            int loan_request_amount = resultSet.getInt("loan_request_amount");
                            int payment_period = resultSet.getInt("payment_period");

                            System.out.println("Grant Status: " + grant);
                            System.out.println("Clearance Status: " + clearance_status);
                            System.out.println("Monthly Payment Plan: " + monthly_payment_plan);
                            System.out.println("Loan Request Amount: " + loan_request_amount);
                            // System.out.println("Loan Progress Amount: " + loan_progress_amount);
                            System.out.println("Payment Period: " + payment_period);
                            System.out.println("--------------------------------------");
                            response = "Loan Has been Approved";
                        } else {
                            System.out.println("Loan Not Found or Not Approved");
                            response = "Loan  Not Approved";
                        }
                        break;
                    default:
                        response = "you are logged out";
                        break;
                }

                output.println(response);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String memberLogin(String username, String password) {
        try {
            Class.forName(drivers);
            Connection con = DriverManager.getConnection(url, userName, passWord);
            String sql = "SELECT COUNT(*), client_id  FROM client WHERE username = ? AND password = ?";
            PreparedStatement statement = con.prepareStatement(sql);
            statement.setString(1, username);
            statement.setString(2, password);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                int count = resultSet.getInt(1);
                client_id = resultSet.getInt("client_id");
                System.out.println(client_id);
                if (count > 0) {
                    active_user = username;
                    return "Login successful!";
                } else {
                    return "Login failed.";
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    public String recoveryResponse(String memberID, String phoneNumber) throws SQLException {
        try {
            Class.forName(drivers);
            Connection con = DriverManager.getConnection(url, userName, passWord);

            loanApplicationNumber = generateRandomPassword(8);

            String sql = "SELECT COUNT(*) FROM client WHERE client_id = ? AND phoneNumber = ?";
            PreparedStatement statement = con.prepareStatement(sql);
            statement.setString(1, memberID);
            statement.setString(2, phoneNumber);
            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                int count = resultSet.getInt(1);
                if (count > 0) {
                    password = generateRandomPassword(5);
                    String updateSql = "UPDATE client SET password = ? WHERE client_id = ?";
                    PreparedStatement updateStatement = con.prepareStatement(updateSql);
                    updateStatement.setString(1, password);
                    updateStatement.setString(2, memberID);
                    updateStatement.executeUpdate();
                    return "Use this new password to login :" + password;
                } else {

                    String referencenumber = generateReferenceNumber(8);
                    String insertQuery = "INSERT INTO login_reference (client_id, phoneNumber, ref_number) VALUES (?, ?, ?)";
                    PreparedStatement reference = con.prepareStatement(insertQuery);
                    reference.setString(1, memberID);
                    reference.setString(2, password);
                    reference.setString(3, referencenumber);
                    reference.executeUpdate();
                    return "Please return after a day with reference number :" + referencenumber;

                }

            }

        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return "";
    }

    public String handleDeposit(double Amount, String date, String receiptNumber) {
        try {

            Class.forName(drivers);
            Connection con = DriverManager.getConnection(url, userName, passWord);

            // Check if the receipt number exists in the database
            String checkReceiptQuery = "SELECT COUNT(*) FROM deposit WHERE receiptNumber = ? AND date = ? AND Amount = ?";
            PreparedStatement checkReceiptStatement = con.prepareStatement(checkReceiptQuery);
            checkReceiptStatement.setString(1, receiptNumber);
            checkReceiptStatement.setString(2, date);
            checkReceiptStatement.setDouble(3, Amount);

            ResultSet receiptResult = checkReceiptStatement.executeQuery();
            if (receiptResult.next()) {
                int count = receiptResult.getInt(1);
                if (count > 0) {
                    return "Deposit was made successfully.";
                } else {
                    return "Please check later. New information will be uploaded soon.";
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    /*
     * public String handleCheckStatement(String dateFrom, String dateTo) {
     * try {
     * Class.forName("com.mysql.cj.jdbc.Driver");
     * Connection con = DriverManager.getConnection(url, userName, passWord);
     * 
     * // Query to retrieve loan and contribution details within the specified date
     * range
     * String statementQuery =
     * "SELECT * FROM loan WHERE start date BETWEEN ? AND ?";
     * PreparedStatement statement = con.prepareStatement(statementQuery);
     * statement.setString(1, dateFrom);
     * statement.setString(2, dateTo);
     * ResultSet resultSet = statement.executeQuery();
     *                                                                                                                                                                                                                                                                      
     * StringBuilder statementBuilder = new StringBuilder();
     * double totalLoanProgress = 0.0;
     * double totalContributionProgress = 0.0;
     * int totalMembers = 0;
     * 
     * // Iterate over the result set to process each member's details
     * while (resultSet.next()) {
     * String memberId = resultSet.getString("member_id");
     * String loanStatus = resultSet.getString("loan_status");
     * String contributionStatus = resultSet.getString("contribution_status");
     * int loanMonthsCleared = resultSet.getInt("loan_months_cleared");
     * int loanTotalMonths = resultSet.getInt("loan_total_months");
     * int contributionMonthsCleared =
     * resultSet.getInt("contribution_months_cleared");
     * int contributionTotalMonths = resultSet.getInt("contribution_total_months");
     *  
     * // Calculate loan progress percentage
     * double loanProgress = (double) loanMonthsCleared / loanTotalMonths * 100;
     * totalLoanProgress += loanProgress;
     * 
     * // Calculate contribution progress percentage
     * double contributionProgress = (double) contributionMonthsCleared /
     * contributionTotalMonths * 100;
     * totalContributionProgress += contributionProgress;
     * 
     * // Append member details to the statement
     * statementBuilder.append("Member ID: ").append(memberId).append("\n");
     * statementBuilder.append("Loan Status: ").append(loanStatus).append("\n");
     * statementBuilder.append("Contribution Status: ").append(contributionStatus).
     * append("\n");
     * statementBuilder.append("Loan Progress: ").append(loanProgress).append("%\n")
     * ;
     * statementBuilder.append("Contribution Progress: ").append(
     * contributionProgress).append("%\n");
     * statementBuilder.append("---------------------------------------------\n");
     * 
     * totalMembers++;
     * }
     * 
     * // Calculate average loan progress and contribution progress for the whole
     * sacco
     * double averageLoanProgress = totalLoanProgress / totalMembers;
     * double averageContributionProgress = totalContributionProgress /
     * totalMembers;
     * 
     * // Append total performance details to the statement
     * statementBuilder.append("Total Sacco Performance:\n");
     * statementBuilder.append("Average Loan Progress: ").append(averageLoanProgress
     * ).append("%\n");
     * statementBuilder.append("Average Contribution Progress: ").append(
     * averageContributionProgress).append("%\n");
     * 
     * // Check if warning is needed for members with less than 50% loan progress
     * if (averageLoanProgress < 50) {
     * statementBuilder.append("Warning: Your loan progress is less than 50%!\n");
     * }
     * 
     * return statementBuilder.toString();
     * } catch (Exception e) {
     * e.printStackTrace();
     * }
     * return "";
     * }
     */
    // method generates random passwords for the users
    private String generateRandomPassword(int length) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < length; i++) {
            char randomChar = getRandomCharacter();
            sb.append(randomChar);
        }

        return sb.toString();
    }

    private char getRandomCharacter() {
        int rand = (int) (Math.random() * 62);

        if (rand <= 9) {
            int ascii = rand + 48;
            return (char) ascii;
        } else if (rand <= 35) {
            int ascii = rand + 55;
            return (char) ascii;
        } else {
            int ascii = rand + 61;
            return (char) ascii;
        }
    }

    // Method generates random numbers that are used as reference by the customers
    public String generateReferenceNumber(int length) {
        StringBuilder sb = new StringBuilder();
        Random random = new Random();

        for (int i = 0; i < length; i++) {
            sb.append(random.nextInt(10)); // Append random digits (0 to 9)
        }

        return sb.toString();
    }

    public static int generateRandom8DigitNumber() {
        // Create a Random object
        Random random = new Random();

        // Generate a random integer with 8 digits (between 10000000 and 99999999)
        int min = 10000000;
        int max = 99999999;
        int randomNumber = random.nextInt(max - min + 1) + min;

        return randomNumber;
    }

}
