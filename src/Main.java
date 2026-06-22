package asrama;

import asrama.database.DatabaseConnection;
import asrama.ui.AsramaMenu;
import asrama.web.WebServer;
import java.awt.Desktop;
import java.net.URI;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("   TELYUSTAY - SISTEM MANAJEMEN ASRAMA");
        System.out.println("========================================");
        System.out.println("1. Mode Console (Input Terminal)");
        System.out.println("2. Mode Web (HTML + Database)");
        System.out.println("0. Keluar");
        System.out.print("Pilih mode: ");

        Scanner scanner = new Scanner(System.in);
        String pilihan = scanner.nextLine().trim();

        if (!DatabaseConnection.testConnection()) {
            System.err.println();
            System.err.println("PERINGATAN: Koneksi database gagal!");
            System.err.println("Langkah perbaikan:");
            System.err.println("1. Pastikan MySQL/MariaDB sudah berjalan");
            System.err.println("2. Import file database_asrama.sql");
            System.err.println("3. Sesuaikan config/database.properties");
            System.err.println();
        }

        switch (pilihan) {
            case "1" -> new AsramaMenu().jalankan();
            case "2" -> {
                try {
                    WebServer webServer = new WebServer();
                    webServer.start();
                    bukaBrowser(webServer.getUrl());
                    System.out.println("Tekan Enter untuk menghentikan server web...");
                    scanner.nextLine();
                } catch (Exception ex) {
                    System.err.println("Gagal menjalankan web server: " + ex.getMessage());
                }
            }
            case "0" -> System.out.println("Program selesai.");
            default -> System.out.println("Pilihan tidak valid.");
        }
    }

    private static void bukaBrowser(String url) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(new URI(url));
            }
        } catch (Exception ex) {
            System.out.println("Buka browser manual: " + url);
        }
    }
}
