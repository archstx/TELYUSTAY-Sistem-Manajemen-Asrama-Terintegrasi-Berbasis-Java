package asrama.model;

import asrama.enums.PermitStatus;
import asrama.enums.ReportStatus;
import java.util.ArrayList;
import java.util.List;

public class SeniorResident extends User {
    private final String nomorSR;
    private Gedung gedungTugas;

    public SeniorResident(String id, String nama, String nomorSR) {
        super(id, nama, "SeniorResident");
        this.nomorSR = nomorSR;
    }

    public boolean validasiIzin(IzinKeluar izin) {
        if (izin.getStatus() == PermitStatus.DIAJUKAN) {
            izin.setStatus(PermitStatus.DISETUJUI);
            return true;
        }
        return false;
    }

    public void ubahStatusLaporan(FacilityReport report, ReportStatus status) {
        report.ubahStatus(status);
    }

    public List<Mahasiswa> lihatDaftarPenghuni() {
        List<Mahasiswa> penghuni = new ArrayList<>();
        if (gedungTugas == null) {
            return penghuni;
        }
        for (Kamar kamar : gedungTugas.getDaftarKamar()) {
            penghuni.addAll(kamar.getPenghuni());
        }
        return penghuni;
    }

    public String getNomorSR() {
        return nomorSR;
    }

    public Gedung getGedungTugas() {
        return gedungTugas;
    }

    public void setGedungTugas(Gedung gedungTugas) {
        this.gedungTugas = gedungTugas;
    }

    @Override
    public void tampilkanDashboard() {
        System.out.println("=== Dashboard Senior Resident ===");
        System.out.println("Nama: " + getNama() + " | Nomor SR: " + nomorSR);
        System.out.println("Gedung tugas: " + (gedungTugas == null ? "-" : gedungTugas.getNamaGedung()));
        System.out.println("Total penghuni pantauan: " + lihatDaftarPenghuni().size());
    }
}
