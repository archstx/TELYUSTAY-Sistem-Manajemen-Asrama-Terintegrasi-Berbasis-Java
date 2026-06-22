package asrama.model;

import java.util.ArrayList;
import java.util.List;

public class PengelolaGedung extends User {
    private final String nomorPegawai;
    private final List<Gedung> gedungKelolaan;
    private final List<FacilityReport> laporanKerusakan;

    public PengelolaGedung(String id, String nama, String nomorPegawai) {
        super(id, nama, "PengelolaGedung");
        this.nomorPegawai = nomorPegawai;
        this.gedungKelolaan = new ArrayList<>();
        this.laporanKerusakan = new ArrayList<>();
    }

    public void kelolaGedung(Gedung gedung) {
        if (!gedungKelolaan.contains(gedung)) {
            gedungKelolaan.add(gedung);
        }
    }

    public void kelolaKamar(Kamar kamar) {
        // Placeholder hook for future room-specific operations.
        if (kamar != null) {
            System.out.println("Mengelola kamar " + kamar.getNomorKamar());
        }
    }

    public List<FacilityReport> lihatLaporanKerusakan() {
        return new ArrayList<>(laporanKerusakan);
    }

    public void tambahLaporanKerusakan(FacilityReport report) {
        laporanKerusakan.add(report);
    }

    @Override
    public void tampilkanDashboard() {
        System.out.println("=== Dashboard Pengelola Gedung ===");
        System.out.println("Nama: " + getNama() + " | Nomor Pegawai: " + nomorPegawai);
        System.out.println("Gedung dikelola: " + gedungKelolaan.size());
        System.out.println("Laporan kerusakan: " + laporanKerusakan.size());
    }
}
