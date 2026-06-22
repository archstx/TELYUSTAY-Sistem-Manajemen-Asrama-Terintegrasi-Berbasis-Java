package asrama.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class Mahasiswa extends User {
    private final String nim;
    private String prodi;
    private String noTelepon;
    private Kamar kamar;
    private final List<IzinKeluar> daftarIzin;
    private final List<FacilityReport> daftarLaporan;
    private final List<Payment> daftarTagihan;

    public Mahasiswa(String id, String nama, String nim) {
        super(id, nama, "Mahasiswa");
        this.nim = nim;
        this.daftarIzin = new ArrayList<>();
        this.daftarLaporan = new ArrayList<>();
        this.daftarTagihan = new ArrayList<>();
    }

    public String getNim() {
        return nim;
    }

    public Kamar getKamar() {
        return kamar;
    }

    public void setKamar(Kamar kamar) {
        this.kamar = kamar;
    }

    public IzinKeluar ajukanIzin(String tujuan, LocalDateTime waktuKeluar, LocalDateTime waktuKembali) {
        IzinKeluar izin = new IzinKeluar("IZN-" + UUID.randomUUID(), tujuan);
        izin.setWaktuKeluar(waktuKeluar);
        izin.setWaktuKembali(waktuKembali);
        izin.setMahasiswaId(getId());
        izin.applyPermit(this);
        daftarIzin.add(izin);
        return izin;
    }

    public FacilityReport buatLaporan(String deskripsi, String lokasi) {
        FacilityReport report = new FacilityReport("RPT-" + UUID.randomUUID(), deskripsi, lokasi);
        report.setMahasiswaId(getId());
        daftarLaporan.add(report);
        return report;
    }

    public boolean bayarTagihan(Payment payment) {
        if (!daftarTagihan.contains(payment)) {
            daftarTagihan.add(payment);
        }
        return payment.prosesPembayaran();
    }

    public void tambahTagihan(Payment payment) {
        payment.setMahasiswaId(getId());
        daftarTagihan.add(payment);
    }

    public List<IzinKeluar> getDaftarIzin() {
        return Collections.unmodifiableList(daftarIzin);
    }

    public List<FacilityReport> getDaftarLaporan() {
        return Collections.unmodifiableList(daftarLaporan);
    }

    public List<Payment> getDaftarTagihan() {
        return Collections.unmodifiableList(daftarTagihan);
    }

    public String getProdi() {
        return prodi;
    }

    public void setProdi(String prodi) {
        this.prodi = prodi;
    }

    public String getNoTelepon() {
        return noTelepon;
    }

    public void setNoTelepon(String noTelepon) {
        this.noTelepon = noTelepon;
    }

    @Override
    public void tampilkanDashboard() {
        System.out.println("=== Dashboard Mahasiswa ===");
        System.out.println("Nama: " + getNama() + " | NIM: " + nim);
        System.out.println("Kamar: " + (kamar == null ? "-" : kamar.getNomorKamar()));
        System.out.println("Izin diajukan: " + daftarIzin.size());
        System.out.println("Laporan fasilitas: " + daftarLaporan.size());
        System.out.println("Tagihan: " + daftarTagihan.size());
    }
}
