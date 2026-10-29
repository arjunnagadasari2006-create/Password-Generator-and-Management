
package com.password;

import java.util.Random;
import java.util.Scanner;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;

public class Lanch {

    // =========================
    // RANDOM PASSWORD
    // =========================

    public static String Randompass() {

        String L = "abcdefghijklmnopqrstuvwxyz";
        String UP = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String N = "0123456789";
        String S = "!@#$%^&*()_-*=<>?/";

        String all = L + UP + N + S;

        Random random = new Random();

        StringBuilder pass = new StringBuilder();

        for (int i = 0; i < 8; i++) {

            int index = random.nextInt(all.length());

            pass.append(all.charAt(index));
        }

        return pass.toString();
    }


    // =========================
    // PASSWORD CHECK
    // =========================

    public static boolean check(char arr[]) {

        boolean lower = false;
        boolean upper = false;
        boolean digit = false;
        boolean special = false;

        for (int i = 0; i < arr.length; i++) {

            if (Character.isLowerCase(arr[i])) {
                lower = true;
            }

            if (Character.isUpperCase(arr[i])) {
                upper = true;
            }

            if (Character.isDigit(arr[i])) {
                digit = true;
            }

            if (!Character.isLetterOrDigit(arr[i])) {
                special = true;
            }
        }


        if (!lower) {
            System.out.println("Need lowercase letter");
        }

        if (!upper) {
            System.out.println("Need uppercase letter");
        }

        if (!digit) {
            System.out.println("Need digit");
        }

        if (!special) {
            System.out.println("Need special symbol");
        }

        return lower && upper && digit && special;
    }


    // =========================
    // MAIN
    // =========================

    @SuppressWarnings("deprecation")
    public static void main(String[] args) {

        Scanner sc = new Scanner(System.in);

        Configuration cfg = new Configuration();

        cfg.configure();

        cfg.addAnnotatedClass(Strongpassword.class);

        Session session =
                cfg.buildSessionFactory().openSession();

        Transaction transaction =
                session.beginTransaction();


        int id = 1;

        boolean flag = true;


        while (flag) {

            System.out.println("\n==============================");
            System.out.println("     PASSWORD MANAGER");
            System.out.println("==============================");


            // USERNAME

            System.out.print("Enter user name: ");

            String user = sc.nextLine();


            // RANDOM PASSWORD

            String randomPass = Randompass();

            System.out.println(
                    "Random Password : " + randomPass
            );


            // PROCEED / CHANGE

            System.out.print(
                    "Type 'proceed' to use random password "
                    + "or 'change' to create your own: "
            );

            String change = sc.nextLine();


            String finalPassword = null;


            // =========================
            // CHANGE PASSWORD
            // =========================

            if (change.equalsIgnoreCase("change")) {

                System.out.println(
                        "\nMinimum password length: 8"
                );

                System.out.println(
                        "Password must contain:"
                );

                System.out.println("Uppercase");
                System.out.println("Lowercase");
                System.out.println("Digit");
                System.out.println("Special symbol");


                System.out.print("\nEnter password: ");

                String pass = sc.nextLine();


                if (pass.length() < 8) {

                    System.out.println(
                            "Password must contain at least 8 characters."
                    );

                    continue;
                }


                if (check(pass.toCharArray())) {

                    System.out.print(
                            "Confirm password: "
                    );

                    String conform = sc.nextLine();


                    if (conform.equals(pass)) {

                        finalPassword = conform;

                        System.out.println(
                                "Password generated successfully!"
                        );

                    } else {

                        System.out.println(
                                "Password mismatch!"
                        );

                        continue;
                    }

                } else {

                    System.out.println(
                            "Password does not meet requirements."
                    );

                    continue;
                }
            }


            // =========================
            // RANDOM PASSWORD
            // =========================

            else if (change.equalsIgnoreCase("proceed")) {

                finalPassword = randomPass;

                System.out.println(
                        "Random password selected."
                );

            }


            // INVALID OPTION

            else {

                System.out.println(
                        "Invalid option. Enter proceed or change."
                );

                continue;
            }


            // =========================
            // CREATE NEW OBJECT
            // =========================
            Strongpassword d =
                    new Strongpassword();

            d.setId(id++);

            d.setUser(user);
           
            d.setPassword(finalPassword);


            // =========================
            // SAVE TO MYSQL
            // =========================

            session.save(d);

            System.out.println(
                    "\nUser saved successfully!"
            );

            System.out.println(d);


            // =========================
            // ADD ANOTHER USER
            // =========================

            System.out.print(
                    "\nADD ANOTHER USER (YES / NO): "
            );

            String answer = sc.nextLine();


            if (!answer.equalsIgnoreCase("yes")) {

                flag = false;
            }
        }


        // =========================
        // COMMIT
        // =========================

        transaction.commit();

        session.close();

        sc.close();


        System.out.println(
                "\n================================"
        );

        System.out.println(
                "All users saved to MySQL successfully."
        );

        System.out.println(
                "================================"
        );
    }
}

