package asrama.ui;

import asrama.enums.PermitStatus;
import asrama.enums.ReportStatus;
import asrama.model.ActivityLogger;
import asrama.model.FacilityReport;
import asrama.model.Gedung;
import asrama.model.IzinKeluar;
import asrama.model.Kamar;
import asrama.model.Mahasiswa;
import asrama.model.Payment;
import asrama.model.SeniorResident;
import asrama.service.AsramaCrudService;
import asrama.ai.AsramaAiAssistant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;

public class AsramaMenu {
    private final Scanner scanner = new Scanner(System.in);
    private final AsramaCrudService crud = new AsramaCrudService();
    private final ActivityLogger logger = new ActivityLogger("LOG-INIT", "Sistem dimulai");
    private final AsramaAiAssistant aiAssistant = new AsramaAiAssistant();

    public void jalankan() {
        if (!asrama.database.DatabaseConnection.testConnection()) {
            System.out.println("Koneksi database gagal. Periksa MySQL dan file config/database.properties.");
            return;
        }

        boolean running = true;
        while (running) {
            cetakMenuUtama();
            int pilihan = bacaInt("Pilih menu");
            switch (pilihan) {
                case 1 -> menuMahasiswa();
                case 2 -> menuGedung();
                case 3 -> menuKamar();
                case 4 -> menuIzin();
                case 5 -> menuLaporan();
                case 6 -> menuPayment();
                case 7 -> menuFiturKhusus();
                case 8 -> menuAiAssistant();
                case 0 -> {
                    System.out.println("Terima kasih. Program selesai.");
                    running = false;
                }
                default -> System.out.println("Menu tidak valid.");
            }
        }
    }

    private void cetakMenuUtama() {
        System.out.println();
        System.out.println("========================================");
        System.out.println("   TELYUSTAY - SISTEM MANAJEMEN ASRAMA");
        System.out.println("========================================");
        System.out.println("1. Kelola Mahasiswa");
        System.out.println("2. Kelola Gedung");
        System.out.println("3. Kelola Kamar");
        System.out.println("4. Kelola Izin Keluar");
        System.out.println("5. Kelola Laporan Fasilitas");
        System.out.println("6. Kelola Pembayaran");
        System.out.println("7. Fitur Khusus (Dashboard & Log)");
        System.out.println("8. Asisten AI (Chat Prompt)");
        System.out.println("0. Keluar");
        System.out.println("----------------------------------------");
    }

    private void menuMahasiswa() {
        boolean kembali = false;
        while (!kembali) {
            System.out.println("\n--- Menu Mahasiswa ---");
            System.out.println("1. Tambah  2. Lihat Semua  3. Cari  4. Ubah  5. Hapus  0. Kembali");
            int pilihan = bacaInt("Pilih");
            switch (pilihan) {
                case 1 -> tambahMahasiswa();
                case 2 -> tampilkanSemuaMahasiswa();
                case 3 -> cariMahasiswa();
                case 4 -> ubahMahasiswa();
                case 5 -> hapusMahasiswa();
                case 0 -> kembali = true;
                default -> System.out.println("Pilihan tidak valid.");
            }
        }
    }

    private void tambahMahasiswa() {
        String id = bacaString("ID Mahasiswa");
        if (crud.readMahasiswa(id) != null) {
            System.out.println("ID sudah terdaftar.");
            return;
        }
        String nama = bacaString("Nama");
        String nim = bacaString("NIM");
        String prodi = bacaString("Prodi");
        String telepon = bacaString("No. Telepon");
        String username = bacaString("Username");
        String password = bacaString("Password");

        Mahasiswa mhs = new Mahasiswa(id, nama, nim);
        mhs.setProdi(prodi);
        mhs.setNoTelepon(telepon);
        mhs.setCredentials(username, password);

        String nomorKamar = bacaString("Nomor kamar (kosongkan jika belum ada)");
        String kodeGedung = nomorKamar.isEmpty() ? "" : bacaString("Kode/nama gedung (misal: GA / Gedung Anggrek)");
        crud.createMahasiswa(
                mhs,
                nomorKamar.isEmpty() ? null : nomorKamar,
                kodeGedung.isEmpty() ? null : kodeGedung);
        System.out.println("Mahasiswa berhasil ditambahkan.");
        logger.catatLog(mhs, "Registrasi mahasiswa baru");
    }

    private void tampilkanSemuaMahasiswa() {
        List<Mahasiswa> daftar = crud.readAllMahasiswa();
        if (daftar.isEmpty()) {
            System.out.println("Belum ada data mahasiswa.");
            return;
        }
        System.out.println("\nDaftar Mahasiswa:");
        for (Mahasiswa mhs : daftar) {
            System.out.printf("- %s | %s | NIM: %s | Prodi: %s | Kamar: %s%n",
                    mhs.getId(),
                    mhs.getNama(),
                    mhs.getNim(),
                    mhs.getProdi() == null ? "-" : mhs.getProdi(),
                    mhs.getKamar() == null ? "-" : mhs.getKamar().getNomorKamar());
        }
    }

    private void cariMahasiswa() {
        Mahasiswa mhs = crud.readMahasiswa(bacaString("ID Mahasiswa"));
        if (mhs == null) {
            System.out.println("Data tidak ditemukan.");
            return;
        }
        System.out.println("ID      : " + mhs.getId());
        System.out.println("Nama    : " + mhs.getNama());
        System.out.println("NIM     : " + mhs.getNim());
        System.out.println("Prodi   : " + mhs.getProdi());
        System.out.println("Telepon : " + mhs.getNoTelepon());
        System.out.println("Kamar   : " + (mhs.getKamar() == null ? "-" : mhs.getKamar().getNomorKamar()));
    }

    private void ubahMahasiswa() {
        String id = bacaString("ID Mahasiswa");
        if (crud.readMahasiswa(id) == null) {
            System.out.println("Data tidak ditemukan.");
            return;
        }
        String nama = bacaString("Nama baru");
        String prodi = bacaString("Prodi baru");
        String telepon = bacaString("No. Telepon baru");
        if (crud.updateMahasiswa(id, nama, prodi, telepon)) {
            System.out.println("Data mahasiswa berhasil diperbarui.");
        }
    }

    private void hapusMahasiswa() {
        String id = bacaString("ID Mahasiswa");
        if (crud.deleteMahasiswa(id)) {
            System.out.println("Data mahasiswa berhasil dihapus.");
        } else {
            System.out.println("Data tidak ditemukan.");
        }
    }

    private void menuGedung() {
        boolean kembali = false;
        while (!kembali) {
            System.out.println("\n--- Menu Gedung ---");
            System.out.println("1. Tambah  2. Lihat Semua  3. Cari  4. Ubah  5. Hapus  0. Kembali");
            int pilihan = bacaInt("Pilih");
            switch (pilihan) {
                case 1 -> tambahGedung();
                case 2 -> tampilkanSemuaGedung();
                case 3 -> cariGedung();
                case 4 -> ubahGedung();
                case 5 -> hapusGedung();
                case 0 -> kembali = true;
                default -> System.out.println("Pilihan tidak valid.");
            }
        }
    }

    private void tambahGedung() {
        String kode = bacaString("Kode Gedung");
        if (crud.readGedung(kode) != null) {
            System.out.println("Kode gedung sudah ada.");
            return;
        }
        String nama = bacaString("Nama Gedung");
        String jenis = bacaString("Jenis Gedung");
        Gedung gedung = new Gedung(kode, nama);
        gedung.setJenisGedung(jenis);
        crud.createGedung(gedung);
        System.out.println("Gedung berhasil ditambahkan.");
    }

    private void tampilkanSemuaGedung() {
        List<Gedung> daftar = crud.readAllGedung();
        if (daftar.isEmpty()) {
            System.out.println("Belum ada data gedung.");
            return;
        }
        for (Gedung gedung : daftar) {
            System.out.printf("- %s | %s | Jenis: %s | Kamar: %d | Penghuni: %d%n",
                    gedung.getKodeGedung(),
                    gedung.getNamaGedung(),
                    gedung.getJenisGedung() == null ? "-" : gedung.getJenisGedung(),
                    gedung.getDaftarKamar().size(),
                    gedung.hitungTotalPenghuni());
        }
    }

    private void cariGedung() {
        Gedung gedung = crud.readGedung(bacaString("Kode Gedung"));
        if (gedung == null) {
            System.out.println("Data tidak ditemukan.");
            return;
        }
        System.out.println("Kode : " + gedung.getKodeGedung());
        System.out.println("Nama : " + gedung.getNamaGedung());
        System.out.println("Jenis: " + gedung.getJenisGedung());
        System.out.println("Total kamar: " + gedung.getDaftarKamar().size());
    }

    private void ubahGedung() {
        String kode = bacaString("Kode Gedung");
        String jenis = bacaString("Jenis Gedung baru");
        if (crud.updateGedung(kode, jenis)) {
            System.out.println("Data gedung berhasil diperbarui.");
        } else {
            System.out.println("Data tidak ditemukan.");
        }
    }

    private void hapusGedung() {
        String kode = bacaString("Kode Gedung");
        if (crud.deleteGedung(kode)) {
            System.out.println("Data gedung berhasil dihapus.");
        } else {
            System.out.println("Data tidak ditemukan.");
        }
    }

    private void menuKamar() {
        boolean kembali = false;
        while (!kembali) {
            System.out.println("\n--- Menu Kamar ---");
            System.out.println("1. Tambah  2. Lihat Semua  3. Cari  4. Kelola Penghuni  5. Hapus  0. Kembali");
            int pilihan = bacaInt("Pilih");
            switch (pilihan) {
                case 1 -> tambahKamar();
                case 2 -> tampilkanSemuaKamar();
                case 3 -> cariKamar();
                case 4 -> kelolaPenghuniKamar();
                case 5 -> hapusKamar();
                case 0 -> kembali = true;
                default -> System.out.println("Pilihan tidak valid.");
            }
        }
    }

    private void tambahKamar() {
        String nomor = bacaString("Nomor Kamar");
        if (crud.readKamar(nomor) != null) {
            System.out.println("Nomor kamar sudah ada.");
            return;
        }
        int kapasitas = bacaInt("Kapasitas");
        Kamar kamar = new Kamar(nomor, kapasitas);
        String kodeGedung = bacaString("Kode gedung (kosongkan jika tidak ada)");
        crud.createKamar(kamar, kodeGedung.isEmpty() ? null : kodeGedung);
        System.out.println("Kamar berhasil ditambahkan.");
    }

    private void tampilkanSemuaKamar() {
        List<Kamar> daftar = crud.readAllKamar();
        if (daftar.isEmpty()) {
            System.out.println("Belum ada data kamar.");
            return;
        }
        for (Kamar kamar : daftar) {
            System.out.printf("- Kamar %s | Kapasitas: %d | Penghuni: %d | Sisa slot: %d%n",
                    kamar.getNomorKamar(),
                    kamar.getKapasitas(),
                    kamar.getJumlahPenghuni(),
                    kamar.getSisaSlot());
        }
    }

    private void cariKamar() {
        Kamar kamar = crud.readKamar(bacaString("Nomor Kamar"));
        if (kamar == null) {
            System.out.println("Data tidak ditemukan.");
            return;
        }
        System.out.println("Nomor    : " + kamar.getNomorKamar());
        System.out.println("Kapasitas: " + kamar.getKapasitas());
        System.out.println("Penghuni : " + kamar.getJumlahPenghuni());
        System.out.println("Sisa slot: " + kamar.getSisaSlot());
    }

    private void kelolaPenghuniKamar() {
        String nomorKamar = bacaString("Nomor Kamar");
        String idMahasiswa = bacaString("ID Mahasiswa");
        Mahasiswa mhs = crud.readMahasiswa(idMahasiswa);
        if (mhs == null) {
            System.out.println("Mahasiswa tidak ditemukan.");
            return;
        }
        System.out.println("1. Tambah penghuni  2. Hapus penghuni");
        int aksi = bacaInt("Pilih aksi");
        boolean tambah = aksi == 1;
        if (crud.updateKamar(nomorKamar, mhs, tambah)) {
            System.out.println("Data penghuni kamar berhasil diperbarui.");
        } else {
            System.out.println("Gagal memperbarui penghuni kamar.");
        }
    }

    private void hapusKamar() {
        String nomor = bacaString("Nomor Kamar");
        if (crud.deleteKamar(nomor)) {
            System.out.println("Data kamar berhasil dihapus.");
        } else {
            System.out.println("Data tidak ditemukan.");
        }
    }

    private void menuIzin() {
        boolean kembali = false;
        while (!kembali) {
            System.out.println("\n--- Menu Izin Keluar ---");
            System.out.println("1. Ajukan  2. Lihat Semua  3. Cari  4. Validasi Ulang  5. Hapus  0. Kembali");
            int pilihan = bacaInt("Pilih");
            switch (pilihan) {
                case 1 -> ajukanIzin();
                case 2 -> tampilkanSemuaIzin();
                case 3 -> cariIzin();
                case 4 -> validasiUlangIzin();
                case 5 -> hapusIzin();
                case 0 -> kembali = true;
                default -> System.out.println("Pilihan tidak valid.");
            }
        }
    }

    private void ajukanIzin() {
        String idMahasiswa = bacaString("ID Mahasiswa");
        Mahasiswa mhs = crud.readMahasiswa(idMahasiswa);
        if (mhs == null) {
            System.out.println("Mahasiswa tidak ditemukan.");
            return;
        }
        String tujuan = bacaString("Tujuan");
        int jamKeluar = bacaInt("Jam dari sekarang untuk keluar");
        int jamKembali = bacaInt("Jam dari sekarang untuk kembali");

        IzinKeluar izin = mhs.ajukanIzin(
                tujuan,
                LocalDateTime.now().plusHours(jamKeluar),
                LocalDateTime.now().plusHours(jamKembali)
        );
        crud.createIzin(izin);
        System.out.println("Izin diajukan. ID: " + izin.getIzinId());
        System.out.println("Status: " + izin.getStatus());
        if (izin.getAlasanPenolakan() != null) {
            System.out.println("Catatan: " + izin.getAlasanPenolakan());
        }
        logger.catatLog(mhs, "Mengajukan izin keluar");
    }

    private void tampilkanSemuaIzin() {
        List<IzinKeluar> daftar = crud.readAllIzin();
        if (daftar.isEmpty()) {
            System.out.println("Belum ada data izin.");
            return;
        }
        for (IzinKeluar izin : daftar) {
            System.out.printf("- %s | Tujuan: %s | Status: %s%n",
                    izin.getIzinId(), izin.getTujuan(), izin.getStatus());
        }
    }

    private void cariIzin() {
        IzinKeluar izin = crud.readIzin(bacaString("ID Izin"));
        if (izin == null) {
            System.out.println("Data tidak ditemukan.");
            return;
        }
        System.out.println("ID     : " + izin.getIzinId());
        System.out.println("Tujuan : " + izin.getTujuan());
        System.out.println("Status : " + izin.getStatus());
        if (izin.getAlasanPenolakan() != null) {
            System.out.println("Alasan : " + izin.getAlasanPenolakan());
        }
    }

    private void validasiUlangIzin() {
        String izinId = bacaString("ID Izin");
        String idMahasiswa = bacaString("ID Mahasiswa");
        Mahasiswa mhs = crud.readMahasiswa(idMahasiswa);
        if (mhs == null) {
            System.out.println("Mahasiswa tidak ditemukan.");
            return;
        }
        if (crud.updateIzin(izinId, mhs)) {
            IzinKeluar izin = crud.readIzin(izinId);
            System.out.println("Validasi ulang berhasil. Status: " + izin.getStatus());
        } else {
            System.out.println("Gagal validasi izin.");
        }
    }

    private void hapusIzin() {
        String izinId = bacaString("ID Izin");
        if (crud.deleteIzin(izinId)) {
            System.out.println("Data izin berhasil dihapus.");
        } else {
            System.out.println("Data tidak ditemukan.");
        }
    }

    private void menuLaporan() {
        boolean kembali = false;
        while (!kembali) {
            System.out.println("\n--- Menu Laporan Fasilitas ---");
            System.out.println("1. Buat  2. Lihat Semua  3. Cari  4. Ubah Status  5. Hapus  0. Kembali");
            int pilihan = bacaInt("Pilih");
            switch (pilihan) {
                case 1 -> buatLaporan();
                case 2 -> tampilkanSemuaLaporan();
                case 3 -> cariLaporan();
                case 4 -> ubahStatusLaporan();
                case 5 -> hapusLaporan();
                case 0 -> kembali = true;
                default -> System.out.println("Pilihan tidak valid.");
            }
        }
    }

    private void buatLaporan() {
        String idMahasiswa = bacaString("ID Mahasiswa");
        Mahasiswa mhs = crud.readMahasiswa(idMahasiswa);
        if (mhs == null) {
            System.out.println("Mahasiswa tidak ditemukan.");
            return;
        }
        String deskripsi = bacaString("Deskripsi kerusakan");
        String lokasi = bacaString("Lokasi");
        FacilityReport report = mhs.buatLaporan(deskripsi, lokasi);
        crud.createReport(report);
        System.out.println("Laporan berhasil dibuat. ID: " + report.getReportId());
        logger.catatLog(mhs, "Membuat laporan fasilitas");
    }

    private void tampilkanSemuaLaporan() {
        List<FacilityReport> daftar = crud.readAllReport();
        if (daftar.isEmpty()) {
            System.out.println("Belum ada laporan.");
            return;
        }
        for (FacilityReport report : daftar) {
            System.out.printf("- %s | %s | Lokasi: %s | Status: %s%n",
                    report.getReportId(),
                    report.getDeskripsi(),
                    report.getLokasi(),
                    report.getStatus());
        }
    }

    private void cariLaporan() {
        FacilityReport report = crud.readReport(bacaString("ID Laporan"));
        if (report == null) {
            System.out.println("Data tidak ditemukan.");
            return;
        }
        System.out.println("ID      : " + report.getReportId());
        System.out.println("Deskripsi: " + report.getDeskripsi());
        System.out.println("Lokasi  : " + report.getLokasi());
        System.out.println("Status  : " + report.getStatus());
        System.out.println("Tanggal : " + report.getTanggalLapor());
    }

    private void ubahStatusLaporan() {
        String reportId = bacaString("ID Laporan");
        System.out.println("Status: 1.BARU  2.DIPROSES  3.SELESAI");
        int statusInput = bacaInt("Pilih status");
        ReportStatus status;
        if (statusInput == 2) {
            status = ReportStatus.DIPROSES;
        } else if (statusInput == 3) {
            status = ReportStatus.SELESAI;
        } else {
            status = ReportStatus.BARU;
        }
        if (crud.updateReport(reportId, status)) {
            System.out.println("Status laporan diperbarui menjadi " + status);
        } else {
            System.out.println("Data tidak ditemukan.");
        }
    }

    private void hapusLaporan() {
        String reportId = bacaString("ID Laporan");
        if (crud.deleteReport(reportId)) {
            System.out.println("Laporan berhasil dihapus.");
        } else {
            System.out.println("Data tidak ditemukan.");
        }
    }

    private void menuPayment() {
        boolean kembali = false;
        while (!kembali) {
            System.out.println("\n--- Menu Pembayaran ---");
            System.out.println("1. Tambah  2. Lihat Semua  3. Cari  4. Bayar/Denda  5. Hapus  0. Kembali");
            int pilihan = bacaInt("Pilih");
            switch (pilihan) {
                case 1 -> tambahPayment();
                case 2 -> tampilkanSemuaPayment();
                case 3 -> cariPayment();
                case 4 -> prosesPayment();
                case 5 -> hapusPayment();
                case 0 -> kembali = true;
                default -> System.out.println("Pilihan tidak valid.");
            }
        }
    }

    private void tambahPayment() {
        String paymentId = bacaString("ID Pembayaran (kosongkan untuk auto)");
        if (paymentId.isEmpty()) {
            paymentId = "PAY-" + UUID.randomUUID().toString().substring(0, 8);
        }
        if (crud.readPayment(paymentId) != null) {
            System.out.println("ID pembayaran sudah ada.");
            return;
        }
        double jumlah = bacaDouble("Jumlah tagihan");
        Payment payment = new Payment(paymentId, jumlah);
        crud.createPayment(payment);

        String idMahasiswa = bacaString("ID Mahasiswa (opsional)");
        Mahasiswa mhs = crud.readMahasiswa(idMahasiswa);
        if (mhs != null) {
            mhs.tambahTagihan(payment);
        }
        System.out.println("Tagihan berhasil ditambahkan. ID: " + paymentId);
    }

    private void tampilkanSemuaPayment() {
        List<Payment> daftar = crud.readAllPayment();
        if (daftar.isEmpty()) {
            System.out.println("Belum ada data pembayaran.");
            return;
        }
        for (Payment payment : daftar) {
            System.out.printf("- %s | Total: %.0f | Status: %s%n",
                    payment.getPaymentId(),
                    payment.hitungTotalBayar(),
                    payment.getStatus());
        }
    }

    private void cariPayment() {
        Payment payment = crud.readPayment(bacaString("ID Pembayaran"));
        if (payment == null) {
            System.out.println("Data tidak ditemukan.");
            return;
        }
        System.out.println("ID     : " + payment.getPaymentId());
        System.out.println("Total  : " + payment.hitungTotalBayar());
        System.out.println("Status : " + payment.getStatus());
        System.out.println("Tanggal bayar: " + payment.getTanggalBayar());
    }

    private void prosesPayment() {
        String paymentId = bacaString("ID Pembayaran");
        double denda = bacaDouble("Denda tambahan");
        String bayar = bacaString("Proses pembayaran sekarang? (y/n)");
        boolean prosesBayar = bayar.equalsIgnoreCase("y");
        if (crud.updatePayment(paymentId, denda, prosesBayar)) {
            Payment payment = crud.readPayment(paymentId);
            System.out.println("Pembayaran diperbarui. Status: " + payment.getStatus());
        } else {
            System.out.println("Data tidak ditemukan.");
        }
    }

    private void hapusPayment() {
        String paymentId = bacaString("ID Pembayaran");
        if (crud.deletePayment(paymentId)) {
            System.out.println("Data pembayaran berhasil dihapus.");
        } else {
            System.out.println("Data tidak ditemukan.");
        }
    }

    private void menuAiAssistant() {
        System.out.println("\n=== Asisten AI TelyuStay ===");
        System.out.println("Ketik pertanyaan Anda. Ketik 'keluar' untuk kembali.");
        System.out.println("Contoh: Kamar apa yang masih kosong? | Siapa yang izin keluar?");
        boolean chatting = true;
        while (chatting) {
            String prompt = bacaString("\nAnda");
            if ("keluar".equalsIgnoreCase(prompt.trim()) || "exit".equalsIgnoreCase(prompt.trim())) {
                chatting = false;
                continue;
            }
            System.out.println("\nAI:\n" + aiAssistant.prosesPrompt(prompt));
        }
    }

    private void menuFiturKhusus() {
        System.out.println("\n--- Fitur Khusus ---");
        System.out.println("1. Login & Dashboard");
        System.out.println("2. Validasi Izin oleh Senior Resident");
        System.out.println("3. Cetak Log Aktivitas");
        int pilihan = bacaInt("Pilih");
        switch (pilihan) {
            case 1 -> loginDashboard();
            case 2 -> validasiIzinSr();
            case 3 -> logger.cetakLog();
            default -> System.out.println("Pilihan tidak valid.");
        }
    }

    private void loginDashboard() {
        String id = bacaString("ID User");
        String username = bacaString("Username");
        String password = bacaString("Password");

        Mahasiswa mhs = crud.readMahasiswa(id);
        if (mhs != null && mhs.login(username, password)) {
            mhs.tampilkanDashboard();
            logger.catatLog(mhs, "Login mahasiswa");
            return;
        }

        System.out.println("Login mahasiswa gagal. Coba mode demo SR/Pengelola.");
        String mode = bacaString("Mode demo (sr/pg, kosongkan untuk batal)");
        if ("sr".equalsIgnoreCase(mode)) {
            SeniorResident sr = new SeniorResident("SR-DEMO", "Senior Resident Demo", "SR-99");
            String kodeGedung = bacaString("Kode gedung tugas");
            sr.setGedungTugas(crud.readGedung(kodeGedung));
            sr.tampilkanDashboard();
        } else if ("pg".equalsIgnoreCase(mode)) {
            System.out.println("Mode pengelola: tampilkan total laporan = " + crud.readAllReport().size());
        }
    }

    private void validasiIzinSr() {
        String izinId = bacaString("ID Izin");
        IzinKeluar izin = crud.readIzin(izinId);
        if (izin == null) {
            System.out.println("Izin tidak ditemukan.");
            return;
        }
        SeniorResident sr = new SeniorResident("SR-TEMP", "Senior Resident", "SR-TEMP");
        if (sr.validasiIzin(izin)) {
            System.out.println("Izin disetujui. Status: " + PermitStatus.DISETUJUI);
        } else {
            System.out.println("Izin tidak dapat disetujui. Status: " + izin.getStatus());
        }
    }

    private String bacaString(String label) {
        System.out.print(label + ": ");
        return scanner.nextLine().trim();
    }

    private int bacaInt(String label) {
        while (true) {
            try {
                System.out.print(label + ": ");
                return Integer.parseInt(scanner.nextLine().trim());
            } catch (NumberFormatException ex) {
                System.out.println("Input harus berupa angka.");
            }
        }
    }

    private double bacaDouble(String label) {
        while (true) {
            try {
                System.out.print(label + ": ");
                return Double.parseDouble(scanner.nextLine().trim());
            } catch (NumberFormatException ex) {
                System.out.println("Input harus berupa angka.");
            }
        }
    }
}
