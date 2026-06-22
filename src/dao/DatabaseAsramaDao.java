package asrama.database;

import asrama.enums.PaymentStatus;
import asrama.enums.PermitStatus;
import asrama.enums.ReportStatus;
import asrama.model.FacilityReport;
import asrama.model.Gedung;
import asrama.model.IzinKeluar;
import asrama.model.Kamar;
import asrama.model.Mahasiswa;
import asrama.model.Payment;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DatabaseAsramaDao {

    public void createMahasiswa(Mahasiswa mahasiswa, String username, String password,
            String nomorKamar, String kodeGedung) throws SQLException {
        if (nomorKamar != null && !nomorKamar.isEmpty()) {
            ensureKamarForMahasiswa(nomorKamar, kodeGedung);
            validateKamarTersedia(nomorKamar);
        }

        String sql = """
                INSERT INTO users (id, nama, role, username, password, nim, prodi, no_telepon, nomor_kamar)
                VALUES (?, ?, 'Mahasiswa', ?, ?, ?, ?, ?, ?)
                """;
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, mahasiswa.getId());
            ps.setString(2, mahasiswa.getNama());
            ps.setString(3, username);
            ps.setString(4, password);
            ps.setString(5, mahasiswa.getNim());
            ps.setString(6, mahasiswa.getProdi());
            ps.setString(7, mahasiswa.getNoTelepon());
            if (nomorKamar == null || nomorKamar.isEmpty()) {
                ps.setNull(8, java.sql.Types.VARCHAR);
            } else {
                ps.setString(8, nomorKamar);
            }
            ps.executeUpdate();
        }
    }

    public Mahasiswa readMahasiswa(String id) throws SQLException {
        String sql = "SELECT * FROM users WHERE id = ? AND role = 'Mahasiswa'";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return mapMahasiswa(rs);
            }
        }
    }

    public List<Mahasiswa> readAllMahasiswa() throws SQLException {
        String sql = "SELECT * FROM users WHERE role = 'Mahasiswa' ORDER BY nama";
        List<Mahasiswa> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapMahasiswa(rs));
            }
        }
        return list;
    }

    public boolean updateMahasiswa(String id, String nama, String nim, String prodi, String noTelepon,
            String username, String password, String nomorKamar, String kodeGedung) throws SQLException {
        if (nomorKamar != null && !nomorKamar.isEmpty()) {
            ensureKamarForMahasiswa(nomorKamar, kodeGedung);
            validateKamarTersediaForUpdate(id, nomorKamar);
        }

        Mahasiswa existing = readMahasiswa(id);
        if (existing == null) {
            return false;
        }

        String finalPassword = (password == null || password.isEmpty())
                ? existing.getPassword()
                : password;

        String sql = """
                UPDATE users
                SET nama = ?, nim = ?, prodi = ?, no_telepon = ?, username = ?, password = ?, nomor_kamar = ?
                WHERE id = ? AND role = 'Mahasiswa'
                """;
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nama);
            ps.setString(2, nim);
            ps.setString(3, prodi);
            ps.setString(4, noTelepon);
            ps.setString(5, username);
            ps.setString(6, finalPassword);
            if (nomorKamar == null || nomorKamar.isEmpty()) {
                ps.setNull(7, java.sql.Types.VARCHAR);
            } else {
                ps.setString(7, nomorKamar);
            }
            ps.setString(8, id);
            return ps.executeUpdate() > 0;
        }
    }

    private void validateKamarTersedia(String nomorKamar) throws SQLException {
        Kamar kamar = readKamar(nomorKamar);
        if (kamar == null) {
            throw new SQLException("Kamar " + nomorKamar + " tidak ditemukan.");
        }
        if (!kamar.cekKetersediaan()) {
            throw new SQLException("Kamar " + nomorKamar + " sudah penuh.");
        }
    }

    private void validateKamarTersediaForUpdate(String mahasiswaId, String nomorKamar) throws SQLException {
        Kamar kamar = readKamar(nomorKamar);
        if (kamar == null) {
            throw new SQLException("Kamar " + nomorKamar + " tidak ditemukan.");
        }

        String sql = "SELECT nomor_kamar FROM users WHERE id = ? AND role = 'Mahasiswa'";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, mahasiswaId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && nomorKamar.equals(rs.getString("nomor_kamar"))) {
                    return;
                }
            }
        }

        if (!kamar.cekKetersediaan()) {
            throw new SQLException("Kamar " + nomorKamar + " sudah penuh.");
        }
    }

    public boolean deleteMahasiswa(String id) throws SQLException {
        String sql = "DELETE FROM users WHERE id = ? AND role = 'Mahasiswa'";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    public void createGedung(Gedung gedung) throws SQLException {
        String sql = "INSERT INTO gedung (kode_gedung, nama_gedung, jenis_gedung) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, gedung.getKodeGedung());
            ps.setString(2, gedung.getNamaGedung());
            ps.setString(3, gedung.getJenisGedung());
            ps.executeUpdate();
        }
    }

    public Gedung readGedung(String kodeGedung) throws SQLException {
        String sql = "SELECT * FROM gedung WHERE kode_gedung = ?";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, kodeGedung);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                Gedung gedung = mapGedung(rs);
                attachKamarToGedung(gedung);
                return gedung;
            }
        }
    }

    public List<Gedung> readAllGedung() throws SQLException {
        String sql = "SELECT * FROM gedung ORDER BY nama_gedung";
        List<Gedung> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Gedung gedung = mapGedung(rs);
                attachKamarToGedung(gedung);
                list.add(gedung);
            }
        }
        return list;
    }

    public boolean updateGedung(String kodeGedung, String jenisGedung) throws SQLException {
        String sql = "UPDATE gedung SET jenis_gedung = ? WHERE kode_gedung = ?";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, jenisGedung);
            ps.setString(2, kodeGedung);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean deleteGedung(String kodeGedung) throws SQLException {
        String sql = "DELETE FROM gedung WHERE kode_gedung = ?";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, kodeGedung);
            return ps.executeUpdate() > 0;
        }
    }

    public void createKamar(Kamar kamar, String kodeGedung) throws SQLException {
        String sql = "INSERT INTO kamar (nomor_kamar, kapasitas, kode_gedung) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, kamar.getNomorKamar());
            ps.setInt(2, kamar.getKapasitas());
            if (kodeGedung != null && !kodeGedung.isEmpty()) {
                ps.setString(3, kodeGedung);
            } else {
                ps.setNull(3, java.sql.Types.VARCHAR);
            }
            ps.executeUpdate();
        }
    }

    public Kamar readKamar(String nomorKamar) throws SQLException {
        String sql = """
                SELECT k.*, g.nama_gedung
                FROM kamar k
                LEFT JOIN gedung g ON k.kode_gedung = g.kode_gedung
                WHERE k.nomor_kamar = ?
                """;
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nomorKamar);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return mapKamar(rs);
            }
        }
    }

    public List<Kamar> readAllKamar() throws SQLException {
        String sql = """
                SELECT k.*, g.nama_gedung
                FROM kamar k
                LEFT JOIN gedung g ON k.kode_gedung = g.kode_gedung
                ORDER BY k.kode_gedung, k.nomor_kamar
                """;
        List<Kamar> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapKamar(rs));
            }
        }
        return list;
    }

    public boolean updateKamarPenghuni(String nomorKamar, Mahasiswa mahasiswa, boolean tambahPenghuni) throws SQLException {
        if (tambahPenghuni) {
            int penghuni = countPenghuniKamar(nomorKamar);
            Kamar kamar = readKamar(nomorKamar);
            if (kamar == null || penghuni >= kamar.getKapasitas()) {
                return false;
            }
            String sql = "UPDATE users SET nomor_kamar = ? WHERE id = ? AND role = 'Mahasiswa'";
            try (Connection conn = DatabaseConnection.getConnection();
                    PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, nomorKamar);
                ps.setString(2, mahasiswa.getId());
                if (ps.executeUpdate() > 0) {
                    mahasiswa.setKamar(kamar);
                    return true;
                }
            }
            return false;
        }

        String sql = "UPDATE users SET nomor_kamar = NULL WHERE id = ? AND nomor_kamar = ?";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, mahasiswa.getId());
            ps.setString(2, nomorKamar);
            if (ps.executeUpdate() > 0) {
                mahasiswa.setKamar(null);
                return true;
            }
        }
        return false;
    }

    public boolean deleteKamar(String nomorKamar) throws SQLException {
        String sql = "DELETE FROM kamar WHERE nomor_kamar = ?";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nomorKamar);
            return ps.executeUpdate() > 0;
        }
    }

    public void createIzin(IzinKeluar izin) throws SQLException {
        String sql = """
                INSERT INTO izin_keluar (izin_id, mahasiswa_id, tujuan, waktu_keluar, waktu_kembali, status, alasan_penolakan)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, izin.getIzinId());
            ps.setString(2, izin.getMahasiswaId());
            ps.setString(3, izin.getTujuan());
            ps.setTimestamp(4, toTimestamp(izin.getWaktuKeluar()));
            ps.setTimestamp(5, toTimestamp(izin.getWaktuKembali()));
            ps.setString(6, izin.getStatus().name());
            ps.setString(7, izin.getAlasanPenolakan());
            ps.executeUpdate();
        }
    }

    public IzinKeluar readIzin(String izinId) throws SQLException {
        String sql = "SELECT * FROM izin_keluar WHERE izin_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, izinId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return mapIzin(rs);
            }
        }
    }

    public List<IzinKeluar> readAllIzin() throws SQLException {
        String sql = "SELECT * FROM izin_keluar ORDER BY waktu_keluar DESC";
        List<IzinKeluar> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapIzin(rs));
            }
        }
        return list;
    }

    public boolean updateIzin(IzinKeluar izin) throws SQLException {
        String sql = """
                UPDATE izin_keluar
                SET status = ?, alasan_penolakan = ?, waktu_keluar = ?, waktu_kembali = ?
                WHERE izin_id = ?
                """;
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, izin.getStatus().name());
            ps.setString(2, izin.getAlasanPenolakan());
            ps.setTimestamp(3, toTimestamp(izin.getWaktuKeluar()));
            ps.setTimestamp(4, toTimestamp(izin.getWaktuKembali()));
            ps.setString(5, izin.getIzinId());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean deleteIzin(String izinId) throws SQLException {
        String sql = "DELETE FROM izin_keluar WHERE izin_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, izinId);
            return ps.executeUpdate() > 0;
        }
    }

    public void createReport(FacilityReport report) throws SQLException {
        String sql = """
                INSERT INTO facility_report (report_id, mahasiswa_id, deskripsi, lokasi, tanggal_lapor, status)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, report.getReportId());
            ps.setString(2, report.getMahasiswaId());
            ps.setString(3, report.getDeskripsi());
            ps.setString(4, report.getLokasi());
            ps.setTimestamp(5, Timestamp.valueOf(report.getTanggalLapor()));
            ps.setString(6, report.getStatus().name());
            ps.executeUpdate();
        }
    }

    public FacilityReport readReport(String reportId) throws SQLException {
        String sql = "SELECT * FROM facility_report WHERE report_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, reportId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return mapReport(rs);
            }
        }
    }

    public List<FacilityReport> readAllReport() throws SQLException {
        String sql = "SELECT * FROM facility_report ORDER BY tanggal_lapor DESC";
        List<FacilityReport> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapReport(rs));
            }
        }
        return list;
    }

    public boolean updateReport(String reportId, ReportStatus status) throws SQLException {
        String sql = "UPDATE facility_report SET status = ? WHERE report_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setString(2, reportId);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean deleteReport(String reportId) throws SQLException {
        String sql = "DELETE FROM facility_report WHERE report_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, reportId);
            return ps.executeUpdate() > 0;
        }
    }

    public void createPayment(Payment payment) throws SQLException {
        String sql = """
                INSERT INTO payment (payment_id, mahasiswa_id, jumlah_tagihan, denda, tanggal_bayar, status)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, payment.getPaymentId());
            ps.setString(2, payment.getMahasiswaId());
            ps.setDouble(3, payment.getJumlahTagihan());
            ps.setDouble(4, payment.getDenda());
            if (payment.getTanggalBayar() != null) {
                ps.setDate(5, java.sql.Date.valueOf(payment.getTanggalBayar()));
            } else {
                ps.setNull(5, java.sql.Types.DATE);
            }
            ps.setString(6, payment.getStatus().name());
            ps.executeUpdate();
        }
    }

    public Payment readPayment(String paymentId) throws SQLException {
        String sql = "SELECT * FROM payment WHERE payment_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, paymentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return mapPayment(rs);
            }
        }
    }

    public List<Payment> readAllPayment() throws SQLException {
        String sql = "SELECT * FROM payment ORDER BY payment_id";
        List<Payment> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapPayment(rs));
            }
        }
        return list;
    }

    public boolean updatePayment(Payment payment) throws SQLException {
        String sql = """
                UPDATE payment
                SET denda = ?, tanggal_bayar = ?, status = ?
                WHERE payment_id = ?
                """;
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, payment.getDenda());
            if (payment.getTanggalBayar() != null) {
                ps.setDate(2, java.sql.Date.valueOf(payment.getTanggalBayar()));
            } else {
                ps.setNull(2, java.sql.Types.DATE);
            }
            ps.setString(3, payment.getStatus().name());
            ps.setString(4, payment.getPaymentId());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean deletePayment(String paymentId) throws SQLException {
        String sql = "DELETE FROM payment WHERE payment_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, paymentId);
            return ps.executeUpdate() > 0;
        }
    }

    public Map<String, Integer> getStats() throws SQLException {
        Map<String, Integer> stats = new HashMap<>();
        stats.put("mahasiswa", countTable("users", "role = 'Mahasiswa'"));
        stats.put("gedung", countTable("gedung", null));
        stats.put("kamar", countTable("kamar", null));
        stats.put("izin", countTable("izin_keluar", null));
        stats.put("laporan", countTable("facility_report", null));
        stats.put("payment", countTable("payment", null));
        return stats;
    }

    private int countTable(String table, String where) throws SQLException {
        String sql = where == null ? "SELECT COUNT(*) FROM " + table : "SELECT COUNT(*) FROM " + table + " WHERE " + where;
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }

    private Mahasiswa mapMahasiswa(ResultSet rs) throws SQLException {
        Mahasiswa mhs = new Mahasiswa(rs.getString("id"), rs.getString("nama"), rs.getString("nim"));
        mhs.setProdi(rs.getString("prodi"));
        mhs.setNoTelepon(rs.getString("no_telepon"));
        mhs.setCredentials(rs.getString("username"), rs.getString("password"));
        String nomorKamar = rs.getString("nomor_kamar");
        if (nomorKamar != null) {
            Kamar kamar = readKamar(nomorKamar);
            mhs.setKamar(kamar);
        }
        return mhs;
    }

    private Gedung mapGedung(ResultSet rs) throws SQLException {
        Gedung gedung = new Gedung(rs.getString("kode_gedung"), rs.getString("nama_gedung"));
        gedung.setJenisGedung(rs.getString("jenis_gedung"));
        return gedung;
    }

    private void attachKamarToGedung(Gedung gedung) throws SQLException {
        String sql = """
                SELECT k.*, g.nama_gedung
                FROM kamar k
                LEFT JOIN gedung g ON k.kode_gedung = g.kode_gedung
                WHERE k.kode_gedung = ?
                """;
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, gedung.getKodeGedung());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    gedung.tambahKamar(mapKamar(rs));
                }
            }
        }
    }

    private Kamar mapKamar(ResultSet rs) throws SQLException {
        Kamar kamar = new Kamar(rs.getString("nomor_kamar"), rs.getInt("kapasitas"));
        kamar.setKodeGedung(rs.getString("kode_gedung"));
        try {
            kamar.setNamaGedung(rs.getString("nama_gedung"));
        } catch (SQLException ignored) {
            kamar.setNamaGedung(null);
        }
        kamar.sinkronJumlahPenghuni(countPenghuniKamar(kamar.getNomorKamar()));
        return kamar;
    }

    private void ensureKamarForMahasiswa(String nomorKamar, String kodeGedungInput) throws SQLException {
        String kodeGedung = resolveGedungKode(kodeGedungInput);
        Kamar existing = readKamar(nomorKamar);

        if (existing == null) {
            if (kodeGedung == null || kodeGedung.isEmpty()) {
                throw new SQLException(
                        "Kamar " + nomorKamar + " belum terdaftar. Isi kode/nama gedung (misal: GA atau Gedung Anggrek).");
            }
            createKamar(new Kamar(nomorKamar, 2), kodeGedung);
            return;
        }

        if (kodeGedung != null && !kodeGedung.isEmpty()) {
            linkKamarToGedung(nomorKamar, kodeGedung);
        }
    }

    private String resolveGedungKode(String input) throws SQLException {
        if (input == null || input.trim().isEmpty()) {
            return null;
        }

        String trimmed = input.trim();
        if (gedungExists(trimmed)) {
            return trimmed;
        }

        String sql = """
                SELECT kode_gedung FROM gedung
                WHERE LOWER(kode_gedung) = LOWER(?) OR LOWER(nama_gedung) = LOWER(?)
                """;
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, trimmed);
            ps.setString(2, trimmed);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("kode_gedung");
                }
            }
        }
        throw new SQLException("Gedung '" + trimmed + "' tidak ditemukan.");
    }

    private boolean gedungExists(String kodeGedung) throws SQLException {
        String sql = "SELECT 1 FROM gedung WHERE kode_gedung = ?";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, kodeGedung);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private void linkKamarToGedung(String nomorKamar, String kodeGedung) throws SQLException {
        if (!gedungExists(kodeGedung)) {
            throw new SQLException("Gedung " + kodeGedung + " tidak ditemukan.");
        }
        String sql = "UPDATE kamar SET kode_gedung = ? WHERE nomor_kamar = ?";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, kodeGedung);
            ps.setString(2, nomorKamar);
            ps.executeUpdate();
        }
    }

    public String getNamaPenghuniKamar(String nomorKamar) throws SQLException {
        String sql = """
                SELECT GROUP_CONCAT(nama ORDER BY nama SEPARATOR ', ') AS penghuni
                FROM users
                WHERE nomor_kamar = ? AND role = 'Mahasiswa'
                """;
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nomorKamar);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("penghuni");
                }
            }
        }
        return "";
    }

    private int countPenghuniKamar(String nomorKamar) throws SQLException {
        String sql = "SELECT COUNT(*) FROM users WHERE nomor_kamar = ? AND role = 'Mahasiswa'";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nomorKamar);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private IzinKeluar mapIzin(ResultSet rs) throws SQLException {
        IzinKeluar izin = new IzinKeluar(rs.getString("izin_id"), rs.getString("tujuan"));
        izin.setMahasiswaId(rs.getString("mahasiswa_id"));
        Timestamp keluar = rs.getTimestamp("waktu_keluar");
        Timestamp kembali = rs.getTimestamp("waktu_kembali");
        if (keluar != null) {
            izin.setWaktuKeluar(keluar.toLocalDateTime());
        }
        if (kembali != null) {
            izin.setWaktuKembali(kembali.toLocalDateTime());
        }
        izin.setStatus(PermitStatus.valueOf(rs.getString("status")));
        izin.setAlasanPenolakan(rs.getString("alasan_penolakan"));
        return izin;
    }

    private FacilityReport mapReport(ResultSet rs) throws SQLException {
        FacilityReport report = new FacilityReport(
                rs.getString("report_id"),
                rs.getString("deskripsi"),
                rs.getString("lokasi"),
                rs.getTimestamp("tanggal_lapor").toLocalDateTime(),
                ReportStatus.valueOf(rs.getString("status"))
        );
        report.setMahasiswaId(rs.getString("mahasiswa_id"));
        return report;
    }

    private Payment mapPayment(ResultSet rs) throws SQLException {
        java.sql.Date tanggal = rs.getDate("tanggal_bayar");
        Payment payment = new Payment(
                rs.getString("payment_id"),
                rs.getDouble("jumlah_tagihan"),
                rs.getDouble("denda"),
                tanggal == null ? null : tanggal.toLocalDate(),
                PaymentStatus.valueOf(rs.getString("status"))
        );
        payment.setMahasiswaId(rs.getString("mahasiswa_id"));
        return payment;
    }

    private Timestamp toTimestamp(LocalDateTime dateTime) {
        return dateTime == null ? null : Timestamp.valueOf(dateTime);
    }
}
