package asrama.ai;

import asrama.enums.PermitStatus;
import asrama.enums.ReportStatus;
import asrama.model.FacilityReport;
import asrama.model.Gedung;
import asrama.model.IzinKeluar;
import asrama.model.Kamar;
import asrama.model.Mahasiswa;
import asrama.model.Payment;
import asrama.service.AsramaCrudService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AsramaAiAssistant {
    private static final int MAX_PROMPT_LENGTH = 500;
    private static final int MAX_HASIL_TAMPIL = 15;

    private static final Pattern ID_MAHASISWA_PATTERN =
            Pattern.compile("\\b(MHS\\d{3,}|U\\d{3,})\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern NOMOR_KAMAR_PATTERN =
            Pattern.compile("\\b([A-Z]{2}\\d{3,}|\\d{3,})\\b", Pattern.CASE_INSENSITIVE);

    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
            "yang", "ada", "apa", "aja", "saja", "berikan", "tampilkan", "cari", "tolong",
            "dong", "nih", "kak", "aku", "saya", "mau", "bisa", "kah", "untuk", "dengan",
            "dari", "dan", "atau", "di", "ke", "nya", "tidak", "adakah", "siapa", "berapa",
            "semua", "data", "daftar", "minta", "kasih", "lihat", "tunjukkan", "info",
            "informasi", "dong", "deh", "please", "list", "show", "give", "me"
    ));

    private static final Set<String> KATA_PENDEK_FASILITAS = new HashSet<>(Arrays.asList(
            "ac", "wc", "wifi", "air"
    ));

    private static final Set<String> KATA_SAPAAN = new HashSet<>(Arrays.asList(
            "halo", "hallo", "hello", "hai", "hi", "hey", "hei", "hoi",
            "pagi", "siang", "sore", "malam", "permisi", "assalamualaikum",
            "makasih", "thanks", "thx", "bye", "dadah", "oke", "ok", "sip", "siap",
            "mantap", "baik", "noted", "paham", "ngerti", "test", "tes"
    ));

    private static final Set<String> KATA_PANGGILAN = new HashSet<>(Arrays.asList(
            "kak", "ka", "bang", "mas", "mbak", "min", "gan", "bro", "sis", "dong", "deh", "nih"
    ));

    private final AsramaCrudService crud = new AsramaCrudService();
    private final Random acak = new Random();

    public String prosesPrompt(String prompt) {
        if (prompt == null || prompt.trim().isEmpty()) {
            return "Silakan ketik pertanyaan Anda. Contoh: \"Tampilkan kamar yang masih kosong\"";
        }

        if (prompt.length() > MAX_PROMPT_LENGTH) {
            return "Pertanyaan terlalu panjang (maks. " + MAX_PROMPT_LENGTH + " karakter). "
                    + "Coba ringkas pertanyaan Anda.";
        }

        try {
            return prosesPromptInternal(prompt);
        } catch (RuntimeException ex) {
            return pesanErrorRamah(ex);
        }
    }

    private String prosesPromptInternal(String prompt) {
        String input = normalisasiInput(prompt);
        if (isHelp(input)) {
            return bantuan();
        }

        if (isBasaBasi(input) || terlihatSepertiSapaan(input)) {
            return jawabBasaBasi(input);
        }

        Map<String, Integer> skor = hitungSkorIntent(input);

        if (skor.getOrDefault("STATISTIK", 0) >= 2) {
            return ringkasanStatistik();
        }

        if (skor.getOrDefault("FASILITAS", 0) >= 1
                || containsAny(input, "laporan", "kerusakan", "rusak", "helpdesk", "masalah")) {
            String hasil = cekFasilitasKamar(input);
            if (!hasil.startsWith("Tidak ada")) {
                return hasil;
            }
        }

        if (isMahasiswaIzinSpesifik(input) || skor.getOrDefault("IZIN", 0) >= 2) {
            return cekIzin(input);
        }

        if (skor.getOrDefault("PEMBAYARAN", 0) >= 2) {
            return cekPembayaran(input);
        }

        if (skor.getOrDefault("KAMAR_KOSONG", 0) >= 2) {
            return cekKamarKosong(input);
        }

        if (skor.getOrDefault("MAHASISWA", 0) >= 2 || cariIdMahasiswa(input) != null) {
            return cekMahasiswa(input);
        }

        if (skor.getOrDefault("GEDUNG", 0) >= 2) {
            return cekGedung(input);
        }

        String universal = pencarianUniversal(input, prompt.trim());
        if (universal != null) {
            return universal;
        }

        return bantuanKontekstual(prompt.trim());
    }

    private String pesanErrorRamah(RuntimeException ex) {
        String pesan = ex.getMessage() == null ? "" : ex.getMessage().toLowerCase(Locale.ROOT);
        if (pesan.contains("database") || pesan.contains("sql") || pesan.contains("connection")) {
            return "Maaf, saya tidak dapat mengakses database asrama saat ini.\n\n"
                    + "Pastikan MySQL berjalan dan koneksi diatur dengan benar, lalu coba lagi.";
        }
        return "Terjadi kesalahan saat memproses pertanyaan Anda. Silakan coba lagi dalam beberapa saat.";
    }

    private String catatanHasilTerbatas(int total, int ditampilkan) {
        if (total <= ditampilkan) {
            return "";
        }
        return "\n\n... dan " + (total - ditampilkan)
                + " data lainnya. Gunakan filter lebih spesifik untuk hasil lengkap.";
    }

    private String normalisasiInput(String prompt) {
        String s = prompt.trim().toLowerCase(Locale.ROOT);
        s = s.replace("nggak", "tidak").replace("ngga", "tidak").replace("gak", "tidak")
                .replace("gk", "tidak").replace(" ga ", " tidak ").replace(" g ", " tidak ")
                .replace("ngga", "tidak").replace("kaga", "tidak").replace("belom", "belum");
        return s;
    }

    private Map<String, Integer> hitungSkorIntent(String input) {
        Map<String, Integer> skor = new HashMap<>();

        if (containsAny(input, "kamar kosong", "kamar tersedia", "slot kosong", "sisa slot",
                "kamar available", "masih kosong", "belum penuh", "bisa diisi")) {
            skor.merge("KAMAR_KOSONG", 4, Integer::sum);
        }
        if (containsAny(input, "kamar", "room", "hunian")) {
            skor.merge("KAMAR", 1, Integer::sum);
        }

        if (containsAny(input, "ac", "lampu", "wifi", "kran", "air", "bocor", "rusak", "mati",
                "dingin", "mampet", "pintu", "listrik", "colokan", "ventilasi", "wc", "shower",
                "keran", "stop kontak", "flush", "fasilitas", "laporan", "kerusakan", "helpdesk")) {
            skor.merge("FASILITAS", 3, Integer::sum);
        }

        if (containsAny(input, "izin", "keluar", "perizinan", "pulang")) {
            skor.merge("IZIN", 3, Integer::sum);
        }
        if (containsAny(input, "mahasiswa", "penghuni", "nim", "mahasiswi")) {
            skor.merge("MAHASISWA", 2, Integer::sum);
        }
        if (containsAny(input, "pembayaran", "tagihan", "bayar", "denda", "payment", "lunas", "tunggak")) {
            skor.merge("PEMBAYARAN", 3, Integer::sum);
        }
        if (containsAny(input, "statistik", "ringkasan", "dashboard", "overview", "total data", "berapa jumlah")) {
            skor.merge("STATISTIK", 3, Integer::sum);
        }
        if (containsAny(input, "gedung", "asrama", "bangunan")) {
            skor.merge("GEDUNG", 2, Integer::sum);
        }

        return skor;
    }

    private String cekFasilitasKamar(String input) {
        List<String> keywords = extractKataKunciFasilitas(input);
        List<FacilityReport> laporanList = crud.readAllReport();
        String gedungFilter = cariNamaGedung(input);
        String nomorKamar = cariNomorKamar(input);
        ReportStatus statusFilter = detectReportStatus(input);
        boolean hanyaAktif = containsAny(input, "belum selesai", "belum", "aktif", "baru", "diproses", "ongoing");

        StringBuilder sb = new StringBuilder("Hasil pencarian masalah fasilitas");
        if (!keywords.isEmpty()) {
            sb.append(" [").append(String.join(", ", keywords)).append("]");
        }
        sb.append(":\n\n");

        int total = 0;
        int ditampilkan = 0;
        for (FacilityReport report : laporanList) {
            if (hanyaAktif && report.getStatus() == ReportStatus.SELESAI) {
                continue;
            }
            if (statusFilter != null && report.getStatus() != statusFilter) {
                continue;
            }
            if (!keywords.isEmpty()
                    && !teksCocokKataKunci(report.getDeskripsi(), keywords)
                    && !teksCocokKataKunci(report.getLokasi(), keywords)) {
                continue;
            }
            if (gedungFilter != null && !lokasiCocokGedung(report.getLokasi(), gedungFilter)) {
                continue;
            }
            if (nomorKamar != null && !report.getLokasi().toUpperCase(Locale.ROOT).contains(nomorKamar.toUpperCase(Locale.ROOT))) {
                continue;
            }

            total++;
            if (ditampilkan >= MAX_HASIL_TAMPIL) {
                continue;
            }
            ditampilkan++;
            Mahasiswa mhs = crud.readMahasiswa(report.getMahasiswaId());
            String nama = mhs == null ? "-" : mhs.getNama();
            String kamar = resolveKamarDariLaporan(report, mhs);

            sb.append("- Kamar/Lokasi: ").append(kamar)
                    .append("\n  Masalah   : ").append(report.getDeskripsi())
                    .append("\n  Status    : ").append(report.getStatus())
                    .append("\n  Pelapor   : ").append(nama);
            if (mhs != null && mhs.getKamar() != null) {
                sb.append(" (Kamar ").append(mhs.getKamar().getNomorKamar()).append(")");
            }
            sb.append("\n\n");
        }

        if (total == 0) {
            return tidakAdaHasilFasilitas(input, keywords);
        }
        sb.append("Total: ").append(total).append(" laporan ditemukan.");
        sb.append(catatanHasilTerbatas(total, ditampilkan));
        return sb.toString().trim();
    }

    private String tidakAdaHasilFasilitas(String input, List<String> keywords) {
        if (!keywords.isEmpty()) {
            return "Tidak ada laporan fasilitas terkait \""
                    + String.join(", ", keywords)
                    + "\" saat ini. Coba cek tab Laporan Fasilitas untuk detail lengkap.";
        }
        return "Tidak ada laporan fasilitas yang cocok dengan pertanyaan Anda saat ini.";
    }

    private List<String> extractKataKunciFasilitas(String input) {
        LinkedHashSet<String> keywords = new LinkedHashSet<>();

        String[][] frasaFasilitas = {
                {"ac tidak dingin", "ac", "dingin"},
                {"ac ga dingin", "ac", "dingin"},
                {"ac gak dingin", "ac", "dingin"},
                {"ac ngga dingin", "ac", "dingin"},
                {"ac nya tidak dingin", "ac", "dingin"},
                {"ac nya dingin", "ac", "dingin"},
                {"lampu mati", "lampu", "mati"},
                {"lampu koridor mati", "lampu", "mati"},
                {"kran air bocor", "kran", "bocor"},
                {"air bocor", "air", "bocor"},
                {"wifi lemah", "wifi"},
                {"kamar mandi mampet", "mampet"},
                {"pintu rusak", "pintu"},
                {"pintu susah", "pintu"},
                {"stop kontak", "kontak"},
                {"colokan rusak", "colokan"},
                {"ventilasi tersumbat", "ventilasi"},
                {"keran shower", "shower"},
                {"flush wc", "flush"},
                {"lantai licin", "lantai"},
        };

        for (String[] frasa : frasaFasilitas) {
            if (input.contains(frasa[0])) {
                for (int i = 1; i < frasa.length; i++) {
                    keywords.add(frasa[i]);
                }
            }
        }

        if (keywords.isEmpty()) {
            String[] kataFasilitas = {
                    "ac", "lampu", "wifi", "kran", "air", "bocor", "pintu", "listrik", "colokan",
                    "ventilasi", "wc", "shower", "keran", "flush", "mampet", "dingin", "mati", "rusak"
            };
            for (String kata : kataFasilitas) {
                if (input.contains(kata)) {
                    keywords.add(kata);
                }
            }
        }

        return new ArrayList<>(keywords);
    }

    private boolean teksCocokKataKunci(String teks, List<String> keywords) {
        if (keywords == null || keywords.isEmpty()) {
            return true;
        }
        String lower = teks.toLowerCase(Locale.ROOT);
        for (String keyword : keywords) {
            if (!lower.contains(keyword.toLowerCase(Locale.ROOT))) {
                return false;
            }
        }
        return true;
    }

    private boolean lokasiCocokGedung(String lokasi, String gedungFilter) {
        String lok = lokasi.toLowerCase(Locale.ROOT);
        if (lok.contains(gedungFilter)) {
            return true;
        }
        for (Gedung gedung : crud.readAllGedung()) {
            if (gedung.getNamaGedung().toLowerCase(Locale.ROOT).contains(gedungFilter)
                    && lok.contains(gedung.getKodeGedung().toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private String resolveKamarDariLaporan(FacilityReport report, Mahasiswa mhs) {
        if (mhs != null && mhs.getKamar() != null) {
            return report.getLokasi() + " / Kamar " + mhs.getKamar().getNomorKamar();
        }
        return report.getLokasi();
    }

    private ReportStatus detectReportStatus(String input) {
        if (containsAny(input, "selesai", "sudah selesai", "done")) {
            return ReportStatus.SELESAI;
        }
        if (containsAny(input, "diproses", "proses", "ditangani")) {
            return ReportStatus.DIPROSES;
        }
        if (containsAny(input, "baru", "belum ditangani")) {
            return ReportStatus.BARU;
        }
        return null;
    }

    private String pencarianUniversal(String input, String promptAsli) {
        List<String> terms = extractKataKunciUmum(input);
        if (terms.isEmpty()) {
            return null;
        }

        StringBuilder sb = new StringBuilder("Hasil pencarian untuk \"")
                .append(promptAsli).append("\":\n\n");
        int total = 0;

        total += tambahHasilMahasiswa(sb, terms);
        total += tambahHasilIzin(sb, terms);
        total += tambahHasilLaporan(sb, terms);
        total += tambahHasilKamar(sb, terms);
        total += tambahHasilPembayaran(sb, terms);

        if (total == 0) {
            return null;
        }
        sb.append("\nTotal ditemukan: ").append(total).append(" data.");
        return sb.toString().trim();
    }

    private List<String> extractKataKunciUmum(String input) {
        LinkedHashSet<String> terms = new LinkedHashSet<>();
        for (String word : input.split("\\s+")) {
            word = word.replaceAll("[^a-z0-9]", "");
            if (word.isEmpty()) {
                continue;
            }
            if (STOP_WORDS.contains(word) || KATA_SAPAAN.contains(word) || KATA_PANGGILAN.contains(word)) {
                continue;
            }
            if (word.length() >= 3 || KATA_PENDEK_FASILITAS.contains(word)) {
                terms.add(word);
            }
        }
        return new ArrayList<>(terms);
    }

    private int tambahHasilMahasiswa(StringBuilder sb, List<String> terms) {
        List<Mahasiswa> list = crud.readAllMahasiswa();
        StringBuilder bagian = new StringBuilder();
        int total = 0;
        int ditampilkan = 0;
        for (Mahasiswa mhs : list) {
            if (!cocokMahasiswa(mhs, terms)) {
                continue;
            }
            total++;
            if (ditampilkan >= MAX_HASIL_TAMPIL) {
                continue;
            }
            ditampilkan++;
            bagian.append("  - ").append(mhs.getNama())
                    .append(" (").append(mhs.getId()).append(")")
                    .append(" | NIM: ").append(mhs.getNim())
                    .append(" | Prodi: ").append(mhs.getProdi())
                    .append(" | Kamar: ")
                    .append(mhs.getKamar() == null ? "-" : mhs.getKamar().getNomorKamar())
                    .append("\n");
        }
        if (total > 0) {
            sb.append("Mahasiswa (").append(total).append("):\n").append(bagian);
            if (total > ditampilkan) {
                sb.append("  ... dan ").append(total - ditampilkan).append(" mahasiswa lainnya.\n");
            }
            sb.append("\n");
        }
        return total;
    }

    private boolean cocokMahasiswa(Mahasiswa mhs, List<String> terms) {
        String gabungan = (mhs.getNama() + " " + mhs.getId() + " " + mhs.getNim() + " "
                + mhs.getProdi() + " " + (mhs.getKamar() == null ? "" : mhs.getKamar().getNomorKamar()))
                .toLowerCase(Locale.ROOT);
        return terms.stream().allMatch(gabungan::contains);
    }

    private int tambahHasilIzin(StringBuilder sb, List<String> terms) {
        List<IzinKeluar> list = crud.readAllIzin();
        StringBuilder bagian = new StringBuilder();
        int total = 0;
        int ditampilkan = 0;
        for (IzinKeluar izin : list) {
            String gabungan = (izin.getTujuan() + " " + izin.getStatus() + " " + izin.getMahasiswaId())
                    .toLowerCase(Locale.ROOT);
            if (!terms.stream().allMatch(gabungan::contains)) {
                continue;
            }
            total++;
            if (ditampilkan >= MAX_HASIL_TAMPIL) {
                continue;
            }
            ditampilkan++;
            Mahasiswa mhs = crud.readMahasiswa(izin.getMahasiswaId());
            String nama = mhs == null ? izin.getMahasiswaId() : mhs.getNama();
            bagian.append("  - ").append(nama)
                    .append(" | Tujuan: ").append(izin.getTujuan())
                    .append(" | Status: ").append(izin.getStatus()).append("\n");
        }
        if (total > 0) {
            sb.append("Izin Keluar (").append(total).append("):\n").append(bagian);
            if (total > ditampilkan) {
                sb.append("  ... dan ").append(total - ditampilkan).append(" izin lainnya.\n");
            }
            sb.append("\n");
        }
        return total;
    }

    private int tambahHasilLaporan(StringBuilder sb, List<String> terms) {
        List<FacilityReport> list = crud.readAllReport();
        StringBuilder bagian = new StringBuilder();
        int total = 0;
        int ditampilkan = 0;
        for (FacilityReport report : list) {
            String gabungan = (report.getDeskripsi() + " " + report.getLokasi() + " " + report.getStatus())
                    .toLowerCase(Locale.ROOT);
            if (!terms.stream().allMatch(gabungan::contains)) {
                continue;
            }
            total++;
            if (ditampilkan >= MAX_HASIL_TAMPIL) {
                continue;
            }
            ditampilkan++;
            bagian.append("  - ").append(report.getLokasi())
                    .append(" | ").append(report.getDeskripsi())
                    .append(" | Status: ").append(report.getStatus()).append("\n");
        }
        if (total > 0) {
            sb.append("Laporan Fasilitas (").append(total).append("):\n").append(bagian);
            if (total > ditampilkan) {
                sb.append("  ... dan ").append(total - ditampilkan).append(" laporan lainnya.\n");
            }
            sb.append("\n");
        }
        return total;
    }

    private int tambahHasilKamar(StringBuilder sb, List<String> terms) {
        List<Kamar> list = crud.readAllKamar();
        StringBuilder bagian = new StringBuilder();
        int total = 0;
        int ditampilkan = 0;
        for (Kamar kamar : list) {
            String gabungan = (kamar.getNomorKamar() + " "
                    + (kamar.getNamaGedung() == null ? "" : kamar.getNamaGedung()) + " "
                    + (kamar.getKodeGedung() == null ? "" : kamar.getKodeGedung()))
                    .toLowerCase(Locale.ROOT);
            if (!terms.stream().allMatch(gabungan::contains)) {
                continue;
            }
            total++;
            if (ditampilkan >= MAX_HASIL_TAMPIL) {
                continue;
            }
            ditampilkan++;
            bagian.append("  - Kamar ").append(kamar.getNomorKamar())
                    .append(" | Gedung: ").append(kamar.getNamaGedung() == null ? "-" : kamar.getNamaGedung())
                    .append(" | Penghuni: ").append(kamar.getJumlahPenghuni())
                    .append("/").append(kamar.getKapasitas()).append("\n");
        }
        if (total > 0) {
            sb.append("Kamar (").append(total).append("):\n").append(bagian);
            if (total > ditampilkan) {
                sb.append("  ... dan ").append(total - ditampilkan).append(" kamar lainnya.\n");
            }
            sb.append("\n");
        }
        return total;
    }

    private int tambahHasilPembayaran(StringBuilder sb, List<String> terms) {
        List<Payment> list = crud.readAllPayment();
        StringBuilder bagian = new StringBuilder();
        int total = 0;
        int ditampilkan = 0;
        for (Payment payment : list) {
            Mahasiswa mhs = payment.getMahasiswaId() == null
                    ? null : crud.readMahasiswa(payment.getMahasiswaId());
            String nama = mhs == null ? "" : mhs.getNama();
            String gabungan = (payment.getPaymentId() + " " + payment.getStatus() + " " + nama)
                    .toLowerCase(Locale.ROOT);
            if (!terms.stream().allMatch(gabungan::contains)) {
                continue;
            }
            total++;
            if (ditampilkan >= MAX_HASIL_TAMPIL) {
                continue;
            }
            ditampilkan++;
            bagian.append("  - ").append(payment.getPaymentId())
                    .append(" | ").append(nama.isEmpty() ? "-" : nama)
                    .append(" | Rp ").append((long) payment.hitungTotalBayar())
                    .append(" | ").append(payment.getStatus()).append("\n");
        }
        if (total > 0) {
            sb.append("Pembayaran (").append(total).append("):\n").append(bagian);
            if (total > ditampilkan) {
                sb.append("  ... dan ").append(total - ditampilkan).append(" pembayaran lainnya.\n");
            }
            sb.append("\n");
        }
        return total;
    }

    private String bantuanKontekstual(String promptAsli) {
        String input = normalisasiInput(promptAsli);
        if (!adaIntentData(input) && (isBasaBasi(input) || terlihatSepertiSapaan(input))) {
            return jawabBasaBasi(input);
        }

        return "Saya sudah mencari di database, tetapi tidak menemukan data yang cocok untuk: \""
                + promptAsli + "\".\n\n"
                + "Coba pertanyaan seperti:\n"
                + "- \"Kamar yang AC-nya tidak dingin\"\n"
                + "- \"Mahasiswa izin keperluan keluarga\"\n"
                + "- \"Tagihan yang belum lunas\"\n"
                + "- \"Kamar kosong di Gedung Anggrek\"\n"
                + "- \"Statistik asrama\"\n"
                + "- Ketik \"Bantuan\" untuk daftar lengkap.";
    }

    private String cekGedung(String input) {
        List<Gedung> gedungList = crud.readAllGedung();
        String filter = cariNamaGedung(input);
        StringBuilder sb = new StringBuilder("Data gedung asrama:\n\n");
        int count = 0;
        for (Gedung gedung : gedungList) {
            if (filter != null) {
                String nama = gedung.getNamaGedung().toLowerCase(Locale.ROOT);
                String kode = gedung.getKodeGedung().toLowerCase(Locale.ROOT);
                if (!nama.contains(filter) && !kode.contains(filter)) {
                    continue;
                }
            }
            count++;
            sb.append("- ").append(gedung.getNamaGedung())
                    .append(" (").append(gedung.getKodeGedung()).append(")")
                    .append(" | Jenis: ").append(gedung.getJenisGedung())
                    .append(" | Kamar: ").append(gedung.getDaftarKamar().size())
                    .append(" | Penghuni: ").append(gedung.hitungTotalPenghuni())
                    .append("\n");
        }
        if (count == 0) {
            return "Gedung tidak ditemukan.";
        }
        return sb.toString().trim();
    }

    private boolean isHelp(String input) {
        return containsAny(input, "bantuan", "help", "apa bisa", "contoh", "cara pakai");
    }

    private boolean isBasaBasi(String input) {
        if (adaIntentData(input)) {
            return false;
        }

        String bersih = bersihkanTeksBasaBasi(input);
        if (bersih.isEmpty()) {
            return false;
        }

        if (polaBasaBasiTepat(bersih) || hanyaKataSapaan(bersih)) {
            return true;
        }

        return containsAny(bersih,
                "apa kabar", "gimana kabar", "gimana kabarnya", "how are you",
                "terima kasih", "makasih", "siapa kamu", "kamu siapa",
                "sampai jumpa", "lagi ngapain", "lagi apa");
    }

    private String bersihkanTeksBasaBasi(String input) {
        return input.replaceAll("[?!.,]", "").trim();
    }

    private boolean hanyaKataSapaan(String teks) {
        String[] kata = teks.split("\\s+");
        if (kata.length == 0 || kata.length > 4) {
            return false;
        }
        for (String k : kata) {
            String bersih = k.replaceAll("[^a-z]", "");
            if (bersih.isEmpty()) {
                continue;
            }
            if (!KATA_SAPAAN.contains(bersih) && !KATA_PANGGILAN.contains(bersih)) {
                return false;
            }
        }
        return true;
    }

    private boolean terlihatSepertiSapaan(String input) {
        if (adaIntentData(input)) {
            return false;
        }
        String bersih = bersihkanTeksBasaBasi(input);
        if (bersih.isEmpty() || bersih.length() > 40) {
            return false;
        }
        for (String sapaan : KATA_SAPAAN) {
            if (bersih.equals(sapaan) || bersih.startsWith(sapaan + " ") || bersih.endsWith(" " + sapaan)) {
                return true;
            }
        }
        return hanyaKataSapaan(bersih);
    }

    private boolean adaIntentData(String input) {
        Map<String, Integer> skor = hitungSkorIntent(input);
        for (int nilai : skor.values()) {
            if (nilai >= 2) {
                return true;
            }
        }
        if (containsAny(input, "laporan", "kerusakan", "rusak", "helpdesk", "masalah",
                "tampilkan", "cari", "berapa", "daftar", "data", "statistik", "siapa penghuni",
                "penghuni kamar", "kamar ", "kamar", "izin", "tagihan", "bayar", "gedung", "asrama",
                "mahasiswa", "penghuni", "laporan", "fasilitas", "ac ", "ac-", "ac nya")) {
            return true;
        }
        if (input.matches(".*\\d.*") || ID_MAHASISWA_PATTERN.matcher(input).find()) {
            return cariIdMahasiswa(input) != null || cariNomorKamar(input) != null;
        }
        return false;
    }

    private boolean polaBasaBasiTepat(String input) {
        String[][] grupPola = {
                {"halo", "hai", "hi", "hey", "halo kak", "hai kak", "halo ka", "hai ka",
                        "permisi", "assalamualaikum"},
                {"apa kabar", "gimana kabar", "gimana kabarnya", "how are you",
                        "lagi ngapain", "lagi apa"},
                {"terima kasih", "makasih", "thanks", "thank you", "thx", "tq", "mksh"},
                {"siapa kamu", "kamu siapa", "anda siapa", "siapa anda", "kamu ini siapa",
                        "kamu siapa sih", "lu siapa"},
                {"sampai jumpa", "sampai ketemu", "bye", "dadah", "see you", "goodbye"},
                {"oke", "ok", "baik", "sip", "siap", "mantap", "noted", "paham", "ngerti"},
                {"selamat pagi", "selamat siang", "selamat sore", "selamat malam"},
        };

        for (String[] grup : grupPola) {
            for (String kata : grup) {
                if (input.equals(kata) || input.startsWith(kata + " ") || input.endsWith(" " + kata)) {
                    return true;
                }
            }
        }
        return false;
    }

    private String jawabBasaBasi(String input) {
        if (containsAny(input, "terima kasih", "makasih", "thanks", "thank you", "thx", "tq", "mksh")) {
            return pilihAcak(
                    "Sama-sama! Senang bisa membantu.\n\nKalau ada yang mau dicek lagi, tinggal tanya ya.",
                    "Terima kasih kembali! Saya selalu siap bantu urusan data asrama.",
                    "Sama-sama! Kabari saja kalau butuh info kamar, izin, atau tagihan lagi."
            );
        }

        if (containsAny(input, "siapa kamu", "kamu siapa", "anda siapa", "siapa anda",
                "kamu ini siapa", "lu siapa")) {
            return pilihAcak(
                    "Saya **Telyu AI**, asisten virtual **TelyuStay**.\n\n"
                            + "Saya bantu cari data kamar, mahasiswa, izin, laporan fasilitas, "
                            + "dan pembayaran langsung dari database asrama.",
                    "Perkenalkan! Saya **Telyu AI** — partner kamu mengelola data asrama.\n\n"
                            + "Tanya apa saja, misalnya \"Statistik asrama\" atau \"Kamar kosong\".",
                    "Nama saya **Telyu AI**. Saya di sini untuk mempermudah pencarian data asrama "
                            + "pakai bahasa natural — tanpa perlu buka banyak menu."
            );
        }

        if (containsAny(input, "apa kabar", "gimana kabar", "gimana kabarnya", "how are you")) {
            return pilihAcak(
                    "Baik, terima kasih! Saya siap membantu hari ini. Ada yang mau dicek dari data asrama?",
                    "Alhamdulillah baik! Gimana, ada data asrama yang perlu kamu cek sekarang?",
                    "Fit dan siap kerja! Mau mulai dari statistik, kamar kosong, atau izin keluar?"
            );
        }

        if (containsAny(input, "sampai jumpa", "sampai ketemu", "bye", "dadah", "see you", "goodbye")) {
            return pilihAcak(
                    "Sampai jumpa! Kalau butuh info asrama lagi, saya ada di sini.",
                    "Dadah! Semangat aktivitasnya — chat lagi kapan saja ya.",
                    "Sampai ketemu! Telyu AI selalu on untuk bantu urusan asrama kamu."
            );
        }

        if (containsAny(input, "oke", "ok", "baik", "sip", "siap", "mantap", "noted", "paham", "ngerti")) {
            return pilihAcak(
                    "Siap! Ada lagi yang bisa saya bantu?",
                    "Oke! Tinggal ketik pertanyaan berikutnya.",
                    "Noted! Saya standby kalau kamu butuh data asrama lagi."
            );
        }

        if (containsAny(input, "selamat malam")) {
            return pilihAcak(
                    "Selamat malam! Masih begadang mengurus asrama? Saya siap bantu cek data.",
                    "Selamat malam! Ada yang perlu dicek sebelum istirahat?",
                    "Malam! Kalau butuh ringkasan data asrama, tinggal tanya saja."
            );
        }
        if (containsAny(input, "selamat sore")) {
            return pilihAcak(
                    "Selamat sore! Mau cek apa dari TelyuStay hari ini?",
                    "Selamat sore! Izin, laporan, atau tagihan — mau yang mana dulu?",
                    "Sore! Saya siap bantu data asrama kamu."
            );
        }
        if (containsAny(input, "selamat siang")) {
            return pilihAcak(
                    "Selamat siang! Ada yang bisa saya bantu dari data asrama?",
                    "Selamat siang! Kamar, izin, atau laporan fasilitas — pilih saja.",
                    "Siang! Telyu AI siap membantu."
            );
        }
        if (containsAny(input, "selamat pagi", "pagi", "assalamualaikum")) {
            return pilihAcak(
                    "Selamat pagi! Semoga harimu produktif. Saya Telyu AI — siap bantu data asrama.",
                    "Pagi! Ada yang ingin kamu cek dari TelyuStay?",
                    "Selamat pagi! Mulai hari dengan cek statistik asrama, yuk?"
            );
        }

        if (containsAny(input, "lagi ngapain", "lagi apa")) {
            return pilihAcak(
                    "Saya standby, siap bantu kelola data asrama! Mau lihat statistik atau kamar kosong?",
                    "Lagi siap-siap bantu kamu cari data! Ada yang mau dicek?",
                    "Di sini nunggu pertanyaan kamu soal asrama — kamar, izin, tagihan, apa aja!"
            );
        }

        return pilihAcak(
                "Halo! Senang bertemu denganmu.\n\n"
                        + "Saya **Telyu AI**, asisten TelyuStay. Tanyakan apa saja tentang asrama — "
                        + "kamar, izin, laporan fasilitas, pembayaran, atau statistik penghuni.",
                "Hai! Ada yang bisa saya bantu hari ini?\n\n"
                        + "Saya bisa carikan data kamar kosong, izin keluar, tagihan, sampai laporan fasilitas.",
                "Halo! Gimana kabarnya?\n\n"
                        + "Saya Telyu AI — tanya saja pakai bahasa sehari-hari, "
                        + "misalnya \"Kamar kosong\" atau \"Tagihan belum lunas\".",
                "Hey! Selamat datang di TelyuStay.\n\n"
                        + "Mau mulai dari mana? Coba \"Statistik asrama\" atau \"Laporan fasilitas aktif\"."
        );
    }

    private String pilihAcak(String... pilihan) {
        return pilihan[acak.nextInt(pilihan.length)];
    }

    private boolean isMahasiswaIzinSpesifik(String input) {
        return containsAny(input, "mahasiswa", "penghuni")
                && extractTujuanFilter(input) != null;
    }

    private String bantuan() {
        return """
                Halo! Saya Asisten AI TelyuStay. Tanyakan apa saja tentang data asrama:
                
                - "Kamar yang AC-nya tidak dingin"
                - "Lampu mati di kamar mana saja?"
                - "Mahasiswa izin keperluan keluarga"
                - "Tagihan yang belum lunas"
                - "Kamar kosong di Gedung Anggrek"
                - "Siapa penghuni kamar GA101?"
                - "Statistik asrama"
                
                Saya akan mencari langsung dari database asrama Anda.
                """;
    }

    private String cekKamarKosong(String input) {
        String gedungFilter = cariNamaGedung(input);
        List<Kamar> kamarList = crud.readAllKamar();
        StringBuilder sb = new StringBuilder("Berikut kamar yang masih memiliki slot kosong:\n\n");

        int total = 0;
        int ditampilkan = 0;
        for (Kamar kamar : kamarList) {
            if (!kamar.cekKetersediaan()) {
                continue;
            }
            if (gedungFilter != null) {
                String namaGedung = kamar.getNamaGedung() == null ? "" : kamar.getNamaGedung().toLowerCase();
                String kodeGedung = kamar.getKodeGedung() == null ? "" : kamar.getKodeGedung().toLowerCase();
                if (!namaGedung.contains(gedungFilter) && !kodeGedung.contains(gedungFilter)) {
                    continue;
                }
            }
            total++;
            if (ditampilkan >= MAX_HASIL_TAMPIL) {
                continue;
            }
            ditampilkan++;
            sb.append("- Kamar ").append(kamar.getNomorKamar());
            if (kamar.getNamaGedung() != null && !kamar.getNamaGedung().isEmpty()) {
                sb.append(" (").append(kamar.getNamaGedung()).append(")");
            }
            sb.append(" | Kapasitas: ").append(kamar.getKapasitas());
            sb.append(" | Penghuni: ").append(kamar.getJumlahPenghuni());
            sb.append(" | Sisa slot: ").append(kamar.getSisaSlot()).append("\n");
        }

        if (total == 0) {
            if (gedungFilter != null) {
                return "Tidak ada kamar kosong di gedung yang Anda maksud, atau semua kamar sudah penuh.";
            }
            return "Saat ini tidak ada kamar dengan slot tersedia. Semua kamar sudah penuh.";
        }

        sb.append("\nTotal: ").append(total).append(" kamar masih bisa diisi penghuni baru.");
        sb.append(catatanHasilTerbatas(total, ditampilkan));
        return sb.toString().trim();
    }

    private String cekIzin(String input) {
        List<IzinKeluar> izinList = crud.readAllIzin();
        String tujuanFilter = extractTujuanFilter(input);
        PermitStatus status = detectIzinStatus(input);
        String idMahasiswa = cariIdMahasiswa(input);

        if (idMahasiswa != null) {
            if (tujuanFilter != null) {
                return filterIzinAdvanced(izinList, tujuanFilter, status, idMahasiswa,
                        "izin mahasiswa " + idMahasiswa + " dengan tujuan \"" + tujuanFilter + "\"");
            }
            return izinPerMahasiswa(idMahasiswa, izinList);
        }

        if (tujuanFilter != null) {
            return filterIzinAdvanced(izinList, tujuanFilter, status, null,
                    labelIzinTujuan(tujuanFilter, status));
        }

        if (status != null) {
            return filterIzinByStatus(izinList, status, labelIzinStatus(status));
        }

        return filterIzinByStatus(izinList, null, "seluruh data izin keluar");
    }

    private PermitStatus detectIzinStatus(String input) {
        if (containsAny(input, "disetujui", "setuju", "sudah disetujui")) {
            return PermitStatus.DISETUJUI;
        }
        if (containsAny(input, "ditolak", "tolak", "direject")) {
            return PermitStatus.DITOLAK;
        }
        if (containsAny(input, "selesai", "sudah kembali", "sudah pulang")) {
            return PermitStatus.SELESAI;
        }
        if (containsAny(input, "diajukan", "pending", "menunggu", "sedang mengajukan")) {
            return PermitStatus.DIAJUKAN;
        }
        return null;
    }

    private String labelIzinStatus(PermitStatus status) {
        switch (status) {
            case DISETUJUI:
                return "izin yang sudah disetujui";
            case DITOLAK:
                return "izin yang ditolak";
            case SELESAI:
                return "izin yang sudah selesai";
            case DIAJUKAN:
            default:
                return "izin yang masih diajukan / menunggu validasi";
        }
    }

    private String labelIzinTujuan(String tujuanFilter, PermitStatus status) {
        if (status != null) {
            return "mahasiswa dengan izin tujuan \"" + tujuanFilter + "\" dan status " + status.name();
        }
        return "mahasiswa dengan izin tujuan \"" + tujuanFilter + "\"";
    }

    private String extractTujuanFilter(String input) {
        String[][] frasaTujuan = {
                {"keperluan keluarga", "keperluan keluarga"},
                {"pulang ke rumah orang tua", "pulang ke rumah orang tua"},
                {"pulang ke rumah", "pulang"},
                {"pulang akhir pekan", "pulang"},
                {"liburan akhir pekan", "liburan"},
                {"acara wisuda saudara", "wisuda"},
                {"acara wisuda", "wisuda"},
                {"kunjungan rumah sakit", "rumah sakit"},
                {"rumah sakit", "rumah sakit"},
                {"urusan bank", "bank"},
                {"keperluan akademik", "akademik"},
                {"mengikuti seminar", "seminar"},
                {"keperluan seminar", "seminar"},
                {"menghadiri pernikahan", "pernikahan"},
                {"pernikahan kerabat", "pernikahan"},
                {"urusan kependudukan", "kependudukan"},
                {"kampus pusat", "kampus"},
                {"keperluan pribadi", "pribadi"},
                {"keperluan mendesak", "mendesak"},
        };

        for (String[] frasa : frasaTujuan) {
            if (input.contains(frasa[0])) {
                return frasa[1];
            }
        }

        String[] polaEkstraksi = {
                "yang izin ", "izin dengan tujuan ", "izin untuk ", "izin keperluan ",
                "izin tujuan ", "tujuan izin ", "tujuan ", "keperluan ", "untuk keperluan ",
        };

        for (String pola : polaEkstraksi) {
            int idx = input.indexOf(pola);
            if (idx < 0) {
                continue;
            }
            String sisa = input.substring(idx + pola.length()).trim();
            sisa = bersihkanKataKunci(sisa);
            if (sisa.length() >= 3 && !isKataUmum(sisa)) {
                return sisa;
            }
        }

        return null;
    }

    private String bersihkanKataKunci(String teks) {
        String hasil = teks.replaceAll("[?.!,].*$", "").trim();
        String[] stopSuffix = {
                " yang disetujui", " yang ditolak", " yang diajukan", " yang selesai",
                " status disetujui", " status ditolak", " status diajukan",
        };
        for (String suffix : stopSuffix) {
            if (hasil.endsWith(suffix)) {
                hasil = hasil.substring(0, hasil.length() - suffix.length()).trim();
            }
        }
        return hasil;
    }

    private boolean isKataUmum(String teks) {
        return containsAny(teks, "mahasiswa", "penghuni", "siapa", "berapa", "semua", "data", "daftar");
    }

    private boolean tujuanCocok(String tujuan, String keyword) {
        if (keyword == null || keyword.isEmpty()) {
            return true;
        }
        String tujuanLower = tujuan.toLowerCase(Locale.ROOT);
        String keywordLower = keyword.toLowerCase(Locale.ROOT);

        if (tujuanLower.contains(keywordLower)) {
            return true;
        }

        String[] kataKunci = keywordLower.split("\\s+");
        int cocok = 0;
        int signifikan = 0;
        for (String kata : kataKunci) {
            if (kata.length() < 3 || isKataFilterAbaikan(kata)) {
                continue;
            }
            signifikan++;
            if (tujuanLower.contains(kata)) {
                cocok++;
            }
        }
        return signifikan > 0 && cocok == signifikan;
    }

    private boolean isKataFilterAbaikan(String kata) {
        return containsAny(kata, "yang", "dengan", "untuk", "dari", "ke", "di", "dan", "atau", "izin", "tujuan");
    }

    private String filterIzinAdvanced(List<IzinKeluar> izinList, String tujuanFilter,
            PermitStatus status, String mahasiswaId, String label) {
        StringBuilder sb = new StringBuilder("Data ").append(label).append(":\n\n");
        int total = 0;
        int ditampilkan = 0;
        for (IzinKeluar izin : izinList) {
            if (mahasiswaId != null && !mahasiswaId.equalsIgnoreCase(izin.getMahasiswaId())) {
                continue;
            }
            if (status != null && izin.getStatus() != status) {
                continue;
            }
            if (!tujuanCocok(izin.getTujuan(), tujuanFilter)) {
                continue;
            }
            total++;
            if (ditampilkan >= MAX_HASIL_TAMPIL) {
                continue;
            }
            ditampilkan++;
            Mahasiswa mhs = crud.readMahasiswa(izin.getMahasiswaId());
            String nama = mhs == null ? izin.getMahasiswaId() : mhs.getNama();
            sb.append("- ").append(nama)
                    .append(" (").append(izin.getMahasiswaId()).append(")")
                    .append(" | Tujuan: ").append(izin.getTujuan())
                    .append(" | Status: ").append(izin.getStatus())
                    .append("\n");
        }

        if (total == 0) {
            return "Tidak ada " + label + " saat ini.";
        }
        sb.append("\nTotal: ").append(total).append(" data.");
        sb.append(catatanHasilTerbatas(total, ditampilkan));
        return sb.toString().trim();
    }

    private String izinPerMahasiswa(String idMahasiswa, List<IzinKeluar> izinList) {
        Mahasiswa mhs = crud.readMahasiswa(idMahasiswa);
        if (mhs == null) {
            return "Mahasiswa dengan ID " + idMahasiswa + " tidak ditemukan.";
        }

        StringBuilder sb = new StringBuilder("Riwayat izin untuk ")
                .append(mhs.getNama()).append(" (").append(idMahasiswa).append("):\n\n");
        int count = 0;
        for (IzinKeluar izin : izinList) {
            if (!idMahasiswa.equalsIgnoreCase(izin.getMahasiswaId())) {
                continue;
            }
            count++;
            sb.append("- ").append(izin.getIzinId())
                    .append(" | Tujuan: ").append(izin.getTujuan())
                    .append(" | Status: ").append(izin.getStatus());
            if (izin.getAlasanPenolakan() != null && !izin.getAlasanPenolakan().isEmpty()) {
                sb.append(" | Catatan: ").append(izin.getAlasanPenolakan());
            }
            sb.append("\n");
        }

        if (count == 0) {
            return mhs.getNama() + " belum pernah mengajukan izin keluar.";
        }
        return sb.toString().trim();
    }

    private String filterIzinByStatus(List<IzinKeluar> izinList, PermitStatus status, String label) {
        StringBuilder sb = new StringBuilder("Data ").append(label).append(":\n\n");
        int total = 0;
        int ditampilkan = 0;
        for (IzinKeluar izin : izinList) {
            if (status != null && izin.getStatus() != status) {
                continue;
            }
            total++;
            if (ditampilkan >= MAX_HASIL_TAMPIL) {
                continue;
            }
            ditampilkan++;
            Mahasiswa mhs = crud.readMahasiswa(izin.getMahasiswaId());
            String nama = mhs == null ? izin.getMahasiswaId() : mhs.getNama();
            sb.append("- ").append(nama)
                    .append(" | Tujuan: ").append(izin.getTujuan())
                    .append(" | Status: ").append(izin.getStatus())
                    .append("\n");
        }

        if (total == 0) {
            return "Tidak ada " + label + " saat ini.";
        }
        sb.append("\nTotal: ").append(total).append(" data.");
        sb.append(catatanHasilTerbatas(total, ditampilkan));
        return sb.toString().trim();
    }

    private String cekMahasiswa(String input) {
        String nomorKamar = cariNomorKamar(input);
        if (nomorKamar != null) {
            return penghuniKamar(nomorKamar);
        }

        String kataKunci = cariNamaMahasiswa(input);
        if (kataKunci != null) {
            return cariMahasiswaByNama(kataKunci);
        }

        String prodiFilter = extractProdiFilter(input);
        if (prodiFilter != null) {
            return cariMahasiswaByProdi(prodiFilter);
        }

        List<Mahasiswa> list = crud.readAllMahasiswa();
        if (list.isEmpty()) {
            return "Belum ada data mahasiswa terdaftar.";
        }

        StringBuilder sb = new StringBuilder("Daftar mahasiswa penghuni asrama:\n\n");
        int total = 0;
        int ditampilkan = 0;
        for (Mahasiswa mhs : list) {
            total++;
            if (ditampilkan >= MAX_HASIL_TAMPIL) {
                continue;
            }
            ditampilkan++;
            sb.append("- ").append(mhs.getNama())
                    .append(" (").append(mhs.getId()).append(")")
                    .append(" | NIM: ").append(mhs.getNim())
                    .append(" | Kamar: ")
                    .append(mhs.getKamar() == null ? "-" : mhs.getKamar().getNomorKamar())
                    .append("\n");
        }
        sb.append("\nTotal: ").append(total).append(" mahasiswa.");
        sb.append(catatanHasilTerbatas(total, ditampilkan));
        return sb.toString().trim();
    }

    private String extractProdiFilter(String input) {
        String[] prodi = {
                "informatika", "sistem informasi", "teknik elektro", "teknik telekomunikasi",
                "manajemen", "ilmu komunikasi", "desain komunikasi visual", "teknik industri", "akuntansi"
        };
        for (String p : prodi) {
            if (input.contains(p)) {
                return p;
            }
        }
        return null;
    }

    private String cariMahasiswaByProdi(String prodi) {
        List<Mahasiswa> list = crud.readAllMahasiswa();
        StringBuilder sb = new StringBuilder("Mahasiswa prodi ").append(prodi).append(":\n\n");
        int total = 0;
        int ditampilkan = 0;
        for (Mahasiswa mhs : list) {
            if (mhs.getProdi() != null && mhs.getProdi().toLowerCase(Locale.ROOT).contains(prodi)) {
                total++;
                if (ditampilkan >= MAX_HASIL_TAMPIL) {
                    continue;
                }
                ditampilkan++;
                sb.append("- ").append(mhs.getNama())
                        .append(" | NIM: ").append(mhs.getNim())
                        .append(" | Kamar: ")
                        .append(mhs.getKamar() == null ? "-" : mhs.getKamar().getNomorKamar())
                        .append("\n");
            }
        }
        if (total == 0) {
            return "Tidak ada mahasiswa prodi " + prodi + ".";
        }
        sb.append("\nTotal: ").append(total).append(" mahasiswa.");
        sb.append(catatanHasilTerbatas(total, ditampilkan));
        return sb.toString().trim();
    }

    private String penghuniKamar(String nomorKamar) {
        List<Mahasiswa> list = crud.readAllMahasiswa();
        StringBuilder sb = new StringBuilder("Penghuni kamar ").append(nomorKamar).append(":\n\n");
        int count = 0;
        for (Mahasiswa mhs : list) {
            if (mhs.getKamar() != null && nomorKamar.equalsIgnoreCase(mhs.getKamar().getNomorKamar())) {
                count++;
                sb.append("- ").append(mhs.getNama())
                        .append(" (").append(mhs.getId()).append(")")
                        .append(" | NIM: ").append(mhs.getNim())
                        .append("\n");
            }
        }
        if (count == 0) {
            Kamar kamar = crud.readKamar(nomorKamar);
            if (kamar == null) {
                return "Kamar " + nomorKamar + " tidak ditemukan di database.";
            }
            return "Kamar " + nomorKamar + " belum memiliki penghuni.";
        }
        return sb.toString().trim();
    }

    private String cariMahasiswaByNama(String kataKunci) {
        List<Mahasiswa> list = crud.readAllMahasiswa();
        String[] terms = kataKunci.toLowerCase(Locale.ROOT).split("\\s+");
        StringBuilder sb = new StringBuilder("Hasil pencarian mahasiswa \"").append(kataKunci).append("\":\n\n");
        int total = 0;
        int ditampilkan = 0;
        for (Mahasiswa mhs : list) {
            if (!cocokKataKunciMahasiswa(mhs, terms)) {
                continue;
            }
            total++;
            if (ditampilkan >= MAX_HASIL_TAMPIL) {
                continue;
            }
            ditampilkan++;
            sb.append("- ").append(mhs.getNama())
                    .append(" | ID: ").append(mhs.getId())
                    .append(" | NIM: ").append(mhs.getNim())
                    .append(" | Kamar: ")
                    .append(mhs.getKamar() == null ? "-" : mhs.getKamar().getNomorKamar())
                    .append("\n");
        }
        if (total == 0) {
            return "Mahasiswa dengan kata kunci \"" + kataKunci + "\" tidak ditemukan.";
        }
        sb.append("\nTotal: ").append(total).append(" mahasiswa.");
        sb.append(catatanHasilTerbatas(total, ditampilkan));
        return sb.toString().trim();
    }

    private boolean cocokKataKunciMahasiswa(Mahasiswa mhs, String[] terms) {
        String gabungan = (mhs.getNama() + " " + mhs.getId() + " "
                + (mhs.getNim() == null ? "" : mhs.getNim())).toLowerCase(Locale.ROOT);
        for (String term : terms) {
            if (term.isEmpty() || !gabungan.contains(term)) {
                return false;
            }
        }
        return true;
    }

    private String cekPembayaran(String input) {
        List<Payment> payments = crud.readAllPayment();
        boolean belumLunas = containsAny(input, "belum", "belum lunas", "belum bayar", "tertunggak", "tunggak");
        boolean terlambat = containsAny(input, "terlambat", "denda");

        StringBuilder sb = new StringBuilder();
        if (belumLunas) {
            sb.append("Tagihan yang belum lunas:\n\n");
        } else if (terlambat) {
            sb.append("Tagihan terlambat / berdenda:\n\n");
        } else {
            sb.append("Data pembayaran asrama:\n\n");
        }

        int total = 0;
        int ditampilkan = 0;
        for (Payment payment : payments) {
            if (belumLunas && payment.getStatus().name().equals("LUNAS")) {
                continue;
            }
            if (terlambat && !payment.getStatus().name().equals("TERLAMBAT")) {
                continue;
            }
            total++;
            if (ditampilkan >= MAX_HASIL_TAMPIL) {
                continue;
            }
            ditampilkan++;
            Mahasiswa mhs = payment.getMahasiswaId() == null
                    ? null : crud.readMahasiswa(payment.getMahasiswaId());
            String nama = mhs == null ? "-" : mhs.getNama();
            sb.append("- ").append(payment.getPaymentId())
                    .append(" | ").append(nama)
                    .append(" | Total: Rp ").append((long) payment.hitungTotalBayar())
                    .append(" | Status: ").append(payment.getStatus())
                    .append("\n");
        }

        if (total == 0) {
            return belumLunas ? "Semua tagihan sudah lunas." : "Belum ada data pembayaran yang cocok.";
        }
        sb.append("\nTotal: ").append(total).append(" data pembayaran.");
        sb.append(catatanHasilTerbatas(total, ditampilkan));
        return sb.toString().trim();
    }

    private String ringkasanStatistik() {
        Map<String, Integer> stats = crud.getStats();
        List<Gedung> gedungList = crud.readAllGedung();
        int kamarKosong = 0;
        for (Kamar kamar : crud.readAllKamar()) {
            if (kamar.cekKetersediaan()) {
                kamarKosong++;
            }
        }

        StringBuilder sb = new StringBuilder("Ringkasan statistik asrama:\n\n");
        sb.append("- Mahasiswa   : ").append(stats.get("mahasiswa")).append("\n");
        sb.append("- Gedung      : ").append(stats.get("gedung")).append("\n");
        sb.append("- Kamar       : ").append(stats.get("kamar")).append("\n");
        sb.append("- Kamar kosong: ").append(kamarKosong).append("\n");
        sb.append("- Izin keluar : ").append(stats.get("izin")).append("\n");
        sb.append("- Laporan     : ").append(stats.get("laporan")).append("\n");
        sb.append("- Pembayaran  : ").append(stats.get("payment")).append("\n\n");

        sb.append("Detail per gedung:\n");
        for (Gedung gedung : gedungList) {
            sb.append("- ").append(gedung.getNamaGedung())
                    .append(" (").append(gedung.getKodeGedung()).append(")")
                    .append(" | Kamar: ").append(gedung.getDaftarKamar().size())
                    .append(" | Penghuni: ").append(gedung.hitungTotalPenghuni())
                    .append("\n");
        }
        return sb.toString().trim();
    }

    private String cariNamaGedung(String input) {
        List<Gedung> gedungList = crud.readAllGedung();
        for (Gedung gedung : gedungList) {
            String nama = gedung.getNamaGedung().toLowerCase();
            String kode = gedung.getKodeGedung().toLowerCase();
            if (input.contains(nama) || input.contains(kode)) {
                return nama;
            }
        }
        String[][] alias = {
                {"anggrek", "anggrek"}, {"bougenville", "bougenville"}, {"bougenvil", "bougenville"},
                {"cendana", "cendana"}, {"dahlia", "dahlia"}, {"eucalyptus", "eucalyptus"},
                {"flamboyan", "flamboyan"},
        };
        for (String[] a : alias) {
            if (input.contains(a[0])) {
                return a[1];
            }
        }
        return null;
    }

    private String cariIdMahasiswa(String input) {
        Matcher matcher = ID_MAHASISWA_PATTERN.matcher(input);
        if (matcher.find()) {
            return matcher.group(1).toUpperCase(Locale.ROOT);
        }
        return null;
    }

    private String cariNomorKamar(String input) {
        Matcher matcher = NOMOR_KAMAR_PATTERN.matcher(input);
        while (matcher.find()) {
            String kandidat = matcher.group(1).toUpperCase(Locale.ROOT);
            if (crud.readKamar(kandidat) != null) {
                return kandidat;
            }
        }
        if (input.matches(".*\\bkamar\\s+\\d+.*")) {
            String[] parts = input.split("\\s+");
            for (int i = 0; i < parts.length - 1; i++) {
                if ("kamar".equals(parts[i]) && parts[i + 1].matches("\\d+")) {
                    return parts[i + 1];
                }
            }
        }
        return null;
    }

    private String cariNamaMahasiswa(String input) {
        String dariPola = ekstrakNamaDariPola(input);
        if (dariPola != null) {
            return dariPola;
        }

        for (Mahasiswa mhs : crud.readAllMahasiswa()) {
            String nama = mhs.getNama().toLowerCase(Locale.ROOT);
            String id = mhs.getId().toLowerCase(Locale.ROOT);
            String nim = mhs.getNim() == null ? "" : mhs.getNim().toLowerCase(Locale.ROOT);
            if (input.contains(nama) || input.contains(id) || (!nim.isEmpty() && input.contains(nim))) {
                return nama;
            }
        }

        return cariNamaFuzzy(input);
    }

    private String ekstrakNamaDariPola(String input) {
        String[] pola = {
                "cari mahasiswa ",
                "mahasiswa bernama ",
                "siapa mahasiswa ",
                "siapa penghuni ",
                "info mahasiswa ",
                "data mahasiswa ",
                "profil mahasiswa ",
                "nama mahasiswa ",
                "tentang mahasiswa ",
        };

        for (String awalan : pola) {
            int idx = input.indexOf(awalan);
            if (idx < 0) {
                continue;
            }
            String nama = bersihkanKataKunci(input.substring(idx + awalan.length()).trim());
            if (nama.length() >= 2 && !isKataUmum(nama)) {
                return nama;
            }
        }

        if (input.startsWith("siapa ")
                && !containsAny(input, "penghuni kamar", "di kamar", "kamar ", "yang izin", "izin ")) {
            String nama = bersihkanKataKunci(input.substring(5).trim());
            if (nama.length() >= 2 && !isKataUmum(nama)) {
                return nama;
            }
        }

        return null;
    }

    private String cariNamaFuzzy(String input) {
        List<String> terms = extractKataKunciUmum(input);
        if (terms.isEmpty()) {
            return null;
        }

        Mahasiswa terbaik = null;
        int skorTerbaik = 0;
        for (Mahasiswa mhs : crud.readAllMahasiswa()) {
            String gabungan = (mhs.getNama() + " " + mhs.getId() + " "
                    + (mhs.getNim() == null ? "" : mhs.getNim())).toLowerCase(Locale.ROOT);
            int skor = 0;
            for (String term : terms) {
                if (gabungan.contains(term)) {
                    skor++;
                }
            }
            if (skor > skorTerbaik) {
                skorTerbaik = skor;
                terbaik = mhs;
            }
        }

        if (terbaik == null || skorTerbaik == 0) {
            return null;
        }

        long signifikan = terms.stream().filter(t -> t.length() >= 3).count();
        if (signifikan >= 2 && skorTerbaik >= 2) {
            return terbaik.getNama().toLowerCase(Locale.ROOT);
        }
        if (terms.size() == 1 && terms.get(0).length() >= 4) {
            return terbaik.getNama().toLowerCase(Locale.ROOT);
        }
        return null;
    }

    private boolean containsAny(String input, String... keywords) {
        for (String keyword : keywords) {
            if (input.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
