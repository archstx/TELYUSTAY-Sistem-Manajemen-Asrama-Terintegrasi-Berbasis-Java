package asrama.service;

import asrama.database.DatabaseAsramaDao;
import asrama.enums.ReportStatus;
import asrama.model.FacilityReport;
import asrama.model.Gedung;
import asrama.model.IzinKeluar;
import asrama.model.Kamar;
import asrama.model.Mahasiswa;
import asrama.model.Payment;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class AsramaCrudService {
    private final DatabaseAsramaDao dao = new DatabaseAsramaDao();

    public void createMahasiswa(Mahasiswa mahasiswa, String nomorKamar, String kodeGedung) {
        run(() -> dao.createMahasiswa(
                mahasiswa,
                mahasiswa.getUsername(),
                mahasiswa.getPassword(),
                nomorKamar,
                kodeGedung));
    }

    public void createMahasiswa(Mahasiswa mahasiswa, String nomorKamar) {
        createMahasiswa(mahasiswa, nomorKamar, null);
    }

    public void createMahasiswa(Mahasiswa mahasiswa) {
        createMahasiswa(mahasiswa, null);
    }

    public Mahasiswa readMahasiswa(String id) {
        return run(() -> dao.readMahasiswa(id));
    }

    public List<Mahasiswa> readAllMahasiswa() {
        return run(() -> dao.readAllMahasiswa());
    }

    public boolean updateMahasiswa(String id, String nama, String prodi, String noTelepon) {
        Mahasiswa existing = readMahasiswa(id);
        if (existing == null) {
            return false;
        }
        return updateMahasiswa(
                id,
                nama,
                existing.getNim(),
                prodi,
                noTelepon,
                existing.getUsername(),
                existing.getPassword(),
                existing.getKamar() == null ? null : existing.getKamar().getNomorKamar(),
                existing.getKamar() == null ? null : existing.getKamar().getKodeGedung());
    }

    public boolean updateMahasiswa(String id, String nama, String nim, String prodi, String noTelepon,
            String username, String password, String nomorKamar, String kodeGedung) {
        return run(() -> dao.updateMahasiswa(
                id, nama, nim, prodi, noTelepon, username, password, nomorKamar, kodeGedung));
    }

    public boolean updateMahasiswa(String id, String nama, String nim, String prodi, String noTelepon,
            String username, String password, String nomorKamar) {
        return updateMahasiswa(id, nama, nim, prodi, noTelepon, username, password, nomorKamar, null);
    }

    public String getNamaPenghuniKamar(String nomorKamar) {
        return run(() -> dao.getNamaPenghuniKamar(nomorKamar));
    }

    public boolean deleteMahasiswa(String id) {
        return run(() -> dao.deleteMahasiswa(id));
    }

    public void createGedung(Gedung gedung) {
        run(() -> dao.createGedung(gedung));
    }

    public Gedung readGedung(String kodeGedung) {
        return run(() -> dao.readGedung(kodeGedung));
    }

    public List<Gedung> readAllGedung() {
        return run(() -> dao.readAllGedung());
    }

    public boolean updateGedung(String kodeGedung, String jenisGedung) {
        return run(() -> dao.updateGedung(kodeGedung, jenisGedung));
    }

    public boolean deleteGedung(String kodeGedung) {
        return run(() -> dao.deleteGedung(kodeGedung));
    }

    public void createKamar(Kamar kamar) {
        createKamar(kamar, null);
    }

    public void createKamar(Kamar kamar, String kodeGedung) {
        run(() -> dao.createKamar(kamar, kodeGedung));
    }

    public Kamar readKamar(String nomorKamar) {
        return run(() -> dao.readKamar(nomorKamar));
    }

    public List<Kamar> readAllKamar() {
        return run(() -> dao.readAllKamar());
    }

    public boolean updateKamar(String nomorKamar, Mahasiswa mahasiswa, boolean tambahPenghuni) {
        return run(() -> dao.updateKamarPenghuni(nomorKamar, mahasiswa, tambahPenghuni));
    }

    public boolean deleteKamar(String nomorKamar) {
        return run(() -> dao.deleteKamar(nomorKamar));
    }

    public void createIzin(IzinKeluar izin) {
        run(() -> dao.createIzin(izin));
    }

    public IzinKeluar readIzin(String izinId) {
        return run(() -> dao.readIzin(izinId));
    }

    public List<IzinKeluar> readAllIzin() {
        return run(() -> dao.readAllIzin());
    }

    public boolean updateIzin(String izinId, Mahasiswa mahasiswa) {
        return run(() -> {
            IzinKeluar izin = dao.readIzin(izinId);
            if (izin == null) {
                return false;
            }
            boolean valid = izin.applyPermit(mahasiswa);
            dao.updateIzin(izin);
            return valid;
        });
    }

    public boolean deleteIzin(String izinId) {
        return run(() -> dao.deleteIzin(izinId));
    }

    public void createReport(FacilityReport report) {
        run(() -> dao.createReport(report));
    }

    public FacilityReport readReport(String reportId) {
        return run(() -> dao.readReport(reportId));
    }

    public List<FacilityReport> readAllReport() {
        return run(() -> dao.readAllReport());
    }

    public boolean updateReport(String reportId, ReportStatus status) {
        return run(() -> dao.updateReport(reportId, status));
    }

    public boolean deleteReport(String reportId) {
        return run(() -> dao.deleteReport(reportId));
    }

    public void createPayment(Payment payment) {
        run(() -> dao.createPayment(payment));
    }

    public Payment readPayment(String paymentId) {
        return run(() -> dao.readPayment(paymentId));
    }

    public List<Payment> readAllPayment() {
        return run(() -> dao.readAllPayment());
    }

    public boolean updatePayment(String paymentId, double dendaTambahan, boolean prosesBayar) {
        return run(() -> {
            Payment payment = dao.readPayment(paymentId);
            if (payment == null) {
                return false;
            }
            payment.tambahDenda(dendaTambahan);
            if (prosesBayar) {
                payment.prosesPembayaran();
            }
            return dao.updatePayment(payment);
        });
    }

    public boolean deletePayment(String paymentId) {
        return run(() -> dao.deletePayment(paymentId));
    }

    public Map<String, Integer> getStats() {
        return run(() -> dao.getStats());
    }

    private void run(SqlRunnable action) {
        try {
            action.run();
        } catch (SQLException ex) {
            throw new RuntimeException("Operasi database gagal: " + ex.getMessage(), ex);
        }
    }

    private <T> T run(SqlSupplier<T> action) {
        try {
            return action.get();
        } catch (SQLException ex) {
            throw new RuntimeException("Operasi database gagal: " + ex.getMessage(), ex);
        }
    }

    @FunctionalInterface
    private interface SqlRunnable {
        void run() throws SQLException;
    }

    @FunctionalInterface
    private interface SqlSupplier<T> {
        T get() throws SQLException;
    }
}
